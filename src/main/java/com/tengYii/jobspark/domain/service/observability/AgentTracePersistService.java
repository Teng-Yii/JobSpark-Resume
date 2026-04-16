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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 异步处理Agent调用事件
     *
     * @param event Agent调用事件
     */
    @Async("taskExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void handleAgentInvocationEvent(AgentInvocationEvent event) {
        try {
            switch (event.getEventType()) {
                case START -> handleStartEvent(event);
                case END -> handleEndEvent(event);
                case ERROR -> handleErrorEvent(event);
            }
        } catch (Exception e) {
            log.error("处理Agent调用事件失败: traceId={}, error={}", event.getTraceId(), e.getMessage(), e);
        }
    }

    /**
     * 异步处理工具调用事件
     *
     * @param event 工具调用事件
     */
    @Async("taskExecutor")
    @Transactional(rollbackFor = Exception.class)
    public void handleToolExecutionEvent(AgentToolExecutionEvent event) {
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

            // 同时保存到Redis
            traceStoreService.saveToolInvocation(invocation);

            log.debug("工具调用记录持久化完成: traceId={}, toolName={}", 
                    event.getTraceId(), event.getToolName());
        } catch (Exception e) {
            log.error("处理工具调用事件失败: traceId={}, error={}", event.getTraceId(), e.getMessage(), e);
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