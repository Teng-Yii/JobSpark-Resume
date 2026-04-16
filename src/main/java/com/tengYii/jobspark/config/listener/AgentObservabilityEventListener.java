package com.tengYii.jobspark.config.listener;

import com.tengYii.jobspark.config.listener.event.AgentInvocationEvent;
import com.tengYii.jobspark.config.listener.event.AgentToolExecutionEvent;
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
     * 注意：事务在Service层通过编程式事务控制
     *
     * @param event Agent调用事件
     */
    @Async("taskExecutor")
    @EventListener
    public void handleAgentInvocationEvent(AgentInvocationEvent event) {
        log.debug("接收到Agent调用事件: traceId={}, type={}", 
                event.getTraceId(), event.getEventType());
        try {
            tracePersistService.handleAgentInvocationEvent(event);
        } catch (Exception e) {
            log.error("处理Agent调用事件失败: traceId={}, error={}", 
                    event.getTraceId(), e.getMessage(), e);
            // 事件处理失败不应影响主流程，只记录日志
        }
    }

    /**
     * 异步处理工具执行事件
     * 注意：事务在Service层通过编程式事务控制
     *
     * @param event 工具执行事件
     */
    @Async("taskExecutor")
    @EventListener
    public void handleToolExecutionEvent(AgentToolExecutionEvent event) {
        log.debug("接收到工具执行事件: traceId={}, toolName={}", 
                event.getTraceId(), event.getToolName());
        try {
            tracePersistService.handleToolExecutionEvent(event);
        } catch (Exception e) {
            log.error("处理工具执行事件失败: traceId={}, toolName={}, error={}", 
                    event.getTraceId(), event.getToolName(), e.getMessage(), e);
            // 事件处理失败不应影响主流程，只记录日志
        }
    }
}