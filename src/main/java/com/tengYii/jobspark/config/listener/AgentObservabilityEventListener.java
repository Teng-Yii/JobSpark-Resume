package com.tengYii.jobspark.config.listener;

import com.tengYii.jobspark.domain.event.AgentInvocationEvent;
import com.tengYii.jobspark.domain.event.AgentToolExecutionEvent;
import com.tengYii.jobspark.domain.service.observability.AgentTracePersistService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Agent可观测性事件监听器
 * 异步处理Agent调用和工具执行事件
 *
 * @author Teng-Yii
 * @since 2026-04-14
 */
@Slf4j
@Component
public class AgentObservabilityEventListener {

    @Resource
    private AgentTracePersistService tracePersistService;

    /**
     * 异步处理Agent调用事件
     *
     * @param event Agent调用事件
     */
    @Async("taskExecutor")
    @EventListener
    public void handleAgentInvocationEvent(AgentInvocationEvent event) {
        log.debug("接收到Agent调用事件: traceId={}, type={}", 
                event.getTraceId(), event.getEventType());
        tracePersistService.handleAgentInvocationEvent(event);
    }

    /**
     * 异步处理工具执行事件
     *
     * @param event 工具执行事件
     */
    @Async("taskExecutor")
    @EventListener
    public void handleToolExecutionEvent(AgentToolExecutionEvent event) {
        log.debug("接收到工具执行事件: traceId={}, toolName={}", 
                event.getTraceId(), event.getToolName());
        tracePersistService.handleToolExecutionEvent(event);
    }
}