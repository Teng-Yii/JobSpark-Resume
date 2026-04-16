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
     *
     * @param event 工具调用事件
     */
    public void handleToolExecutionEvent(AgentToolExecutionEvent event) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("ToolInvocationTx-" + event.getTraceId());
        TransactionStatus status = transactionManager.getTransaction(def);
        
        try {
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

        // 更新MySQL
        executionTraceRepository.updateStatus(
                event.getTraceId(), 
                status, 
                event.getEndTime(), 
                event.getDurationMs()
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