package com.tengYii.jobspark.domain.service.observability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tengYii.jobspark.common.enums.AgentExecutionStatusEnum;
import com.tengYii.jobspark.config.listener.event.AgentInvocationEvent;
import com.tengYii.jobspark.config.listener.event.AgentToolExecutionEvent;
import com.tengYii.jobspark.infrastructure.repo.AgentExecutionTraceRepository;
import com.tengYii.jobspark.infrastructure.repo.AgentSessionStatsRepository;
import com.tengYii.jobspark.infrastructure.repo.AgentToolInvocationRepository;
import com.tengYii.jobspark.infrastructure.store.AgentTraceStoreService;
import com.tengYii.jobspark.model.po.AgentExecutionTracePO;
import com.tengYii.jobspark.model.po.AgentToolInvocationPO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent可观测性MySQL持久化服务
 * 负责将Agent执行轨迹持久化到MySQL
 *
 * @author Teng-Yii
 * @since 2026-04-14
 */
@Slf4j
@Service
public class AgentTracePersistService {

    @Resource
    private AgentExecutionTraceRepository executionTraceRepository;

    @Resource
    private AgentToolInvocationRepository toolInvocationRepository;

    @Resource
    private AgentSessionStatsRepository sessionStatsRepository;

    @Resource
    private AgentTraceStoreService traceStoreService;

    @Resource
    private PlatformTransactionManager transactionManager;

    /**
     * 处理Agent调用事件（由Listener层异步调用）
     * 使用编程式事务，避免@Async和@Transactional组合失效问题
     *
     * @param event Agent调用事件
     */
    public void handleAgentInvocationEvent(AgentInvocationEvent event) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("AgentInvocationTx-" + event.getTraceId());
        TransactionStatus status = transactionManager.getTransaction(def);
        
        try {
            switch (event.getEventType()) {
                case START -> handleStartEvent(event);
                case END -> handleEndEvent(event);
                case ERROR -> handleErrorEvent(event);
            }
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            log.error("处理Agent调用事件失败，事务已回滚: traceId={}, error={}", 
                    event.getTraceId(), e.getMessage(), e);
            throw e; // 抛出异常让上层处理
        }
    }

    /**
     * 处理工具调用事件（由Listener层异步调用）
     * 使用编程式事务，避免@Async和@Transactional组合失效问题
     * <p>
     * 注意：由于异步处理时序问题，工具调用事件可能在Agent执行轨迹之前到达，
     * 因此采用延迟重试机制等待父记录创建，避免产生大量UNKNOWN占位记录。
     *
     * @param event 工具调用事件
     */
    public void handleToolExecutionEvent(AgentToolExecutionEvent event) {
        // 先尝试等待父记录存在（带重试机制）
        boolean parentExists = waitForTraceExists(event.getTraceId(), 5, 100);
        
        if (!parentExists) {
            log.error("Agent执行轨迹不存在，工具调用记录可能无法正确关联: traceId={}, toolName={}", 
                    event.getTraceId(), event.getToolName());
            // 仍然尝试保存，让外键约束失败来暴露问题，而不是创建占位记录
        }

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("ToolInvocationTx-" + event.getTraceId());
        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            // 再次确保父记录存在（事务内检查）
            ensureTraceExists(event);

            // 保存到MySQL
            AgentToolInvocationPO invocation = AgentToolInvocationPO.builder()
                    .traceId(event.getTraceId())
                    .invocationOrder(event.getInvocationOrder())
                    .toolName(event.getToolName())
                    .toolInput(event.getToolInput())
                    .toolOutput(event.getToolOutput())
                    .success(event.isSuccess() ? 1 : 0)
                    .executionTimeMs(event.getExecutionTimeMs())
                    .deleteFlag(0)
                    .createdTime(LocalDateTime.now())
                    .build();
            toolInvocationRepository.save(invocation);

            // 更新统计
            if (event.getSessionId() != null) {
                sessionStatsRepository.incrementToolCall(event.getSessionId());
            }

            // 同时保存到Redis（非事务操作，失败不影响主事务）
            try {
                traceStoreService.saveToolInvocation(invocation);
            } catch (Exception redisEx) {
                log.warn("Redis保存工具调用记录失败（不影响主事务）: traceId={}, error={}", 
                        event.getTraceId(), redisEx.getMessage());
            }

            transactionManager.commit(status);
            log.debug("工具调用记录持久化完成: traceId={}, toolName={}", 
                    event.getTraceId(), event.getToolName());
        } catch (Exception e) {
            transactionManager.rollback(status);
            log.error("处理工具调用事件失败，事务已回滚: traceId={}, error={}", 
                    event.getTraceId(), e.getMessage(), e);
            throw e; // 抛出异常让上层处理
        }
    }

    /**
     * 等待Agent执行轨迹记录存在（带重试机制）
     * 
     * @param traceId 追踪ID
     * @param maxRetries 最大重试次数
     * @param retryIntervalMs 每次重试间隔（毫秒）
     * @return 是否成功找到父记录
     */
    private boolean waitForTraceExists(String traceId, int maxRetries, long retryIntervalMs) {
        for (int i = 0; i < maxRetries; i++) {
            AgentExecutionTracePO existingTrace = executionTraceRepository.getByTraceId(traceId);
            if (existingTrace != null) {
                if (i > 0) {
                    log.debug("Agent执行轨迹在第{}次重试后找到: traceId={}", i + 1, traceId);
                }
                return true;
            }
            
            if (i < maxRetries - 1) {
                try {
                    Thread.sleep(retryIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("等待Agent执行轨迹时被中断: traceId={}", traceId);
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 确保Agent执行轨迹记录存在
     * 如果记录不存在，创建一个占位记录以满足外键约束（仅作为最后兜底）
     *
     * @param event 工具调用事件
     */
    private void ensureTraceExists(AgentToolExecutionEvent event) {
        // 检查trace是否存在
        AgentExecutionTracePO existingTrace = executionTraceRepository.getByTraceId(event.getTraceId());
        if (existingTrace != null) {
            return;
        }

        // 创建占位记录以满足外键约束（仅作为最后兜底，正常情况下不应该走到这里）
        log.error("Agent执行轨迹不存在，创建兜底占位记录（请检查异步时序问题）: traceId={}, toolName={}, sessionId={}", 
                event.getTraceId(), event.getToolName(), event.getSessionId());
        
        AgentExecutionTracePO placeholderTrace = AgentExecutionTracePO.builder()
                .traceId(event.getTraceId())
                .sessionId(event.getSessionId())
                .agentName("ORPHAN_TOOL_EVENT")
                .agentId("ORPHAN")
                .status(AgentExecutionStatusEnum.RUNNING.getCode())
                .startTime(LocalDateTime.now())
                .deleteFlag(0)
                .createdTime(LocalDateTime.now())
                .build();
        
        executionTraceRepository.save(placeholderTrace);
    }

    private void handleStartEvent(AgentInvocationEvent event) {
        AgentExecutionTracePO trace = AgentExecutionTracePO.builder()
                .traceId(event.getTraceId())
                .sessionId(event.getSessionId())
                .memoryId(event.getMemoryId())
                .agentName(event.getAgentName())
                .agentId(event.getAgentId())
                .parentAgentId(event.getParentAgentId())
                .status(AgentExecutionStatusEnum.RUNNING.getCode())
                .startTime(event.getStartTime())
                .inputSummary(truncateInput(event.getInputs()))
                .deleteFlag(0)
                .createdTime(LocalDateTime.now())
                .build();

        // 先写Redis热数据
        traceStoreService.saveStartTrace(trace);

        // 再写MySQL持久化
        executionTraceRepository.save(trace);

        log.info("Agent开始事件持久化完成: traceId={}, agentName={}", 
                event.getTraceId(), event.getAgentName());
    }

    private void handleEndEvent(AgentInvocationEvent event) {
        String status = AgentExecutionStatusEnum.SUCCESS.getCode();
        
        // 更新Redis
        traceStoreService.updateEndTrace(
                event.getTraceId(), 
                status, 
                event.getEndTime(), 
                event.getDurationMs(),
                truncateOutput(event.getOutput())
        );

        // 更新MySQL（包含output_summary）
        executionTraceRepository.updateStatus(
                event.getTraceId(),
                status,
                event.getEndTime(),
                event.getDurationMs(),
                truncateOutput(event.getOutput())
        );

        // 更新统计
        if (event.getSessionId() != null) {
            sessionStatsRepository.incrementAgentCall(
                    event.getSessionId(), 
                    event.getMemoryId(), 
                    true, 
                    event.getDurationMs()
            );
        }

        log.info("Agent结束事件持久化完成: traceId={}, agentName={}, duration={}ms", 
                event.getTraceId(), event.getAgentName(), event.getDurationMs());
    }

    private void handleErrorEvent(AgentInvocationEvent event) {
        // 更新Redis
        traceStoreService.updateErrorTrace(
                event.getTraceId(), 
                event.getErrorMessage(), 
                event.getErrorStackTrace()
        );

        // 更新MySQL
        executionTraceRepository.updateError(
                event.getTraceId(), 
                event.getErrorMessage(), 
                event.getErrorStackTrace()
        );

        // 更新统计
        if (event.getSessionId() != null) {
            sessionStatsRepository.incrementAgentCall(
                    event.getSessionId(), 
                    event.getMemoryId(), 
                    false, 
                    event.getDurationMs()
            );
        }

        log.error("Agent错误事件持久化完成: traceId={}, agentName={}, error={}", 
                event.getTraceId(), event.getAgentName(), event.getErrorMessage());
    }

    private String truncateInput(Map<String, Object> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return null;
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(inputs);
            return json.length() > 500 ? json.substring(0, 500) + "..." : json;
        } catch (Exception e) {
            return inputs.toString();
        }
    }

    private String truncateOutput(Object output) {
        if (output == null) {
            return null;
        }
        String str = output.toString();
        return str.length() > 500 ? str.substring(0, 500) + "..." : str;
    }
}