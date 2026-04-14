package com.tengYii.jobspark.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent调用事件
 * 用于异步记录Agent执行轨迹
 *
 * @author Teng-Yii
 * @since 2026-04-14
 */
@Getter
public class AgentInvocationEvent extends ApplicationEvent {

    /**
     * 唯一追踪ID
     */
    private final String traceId;

    /**
     * 面试会话ID
     */
    private final String sessionId;

    /**
     * Memory ID
     */
    private final String memoryId;

    /**
     * Agent名称
     */
    private final String agentName;

    /**
     * Agent实例ID
     */
    private final String agentId;

    /**
     * 父Agent ID
     */
    private final String parentAgentId;

    /**
     * 事件类型: START, END, ERROR
     */
    private final EventType eventType;

    /**
     * 开始时间
     */
    private final LocalDateTime startTime;

    /**
     * 结束时间
     */
    private final LocalDateTime endTime;

    /**
     * 执行耗时(毫秒)
     */
    private final Long durationMs;

    /**
     * 输入参数摘要
     */
    private final Map<String, Object> inputs;

    /**
     * 输出结果摘要
     */
    private final Object output;

    /**
     * 错误信息
     */
    private final String errorMessage;

    /**
     * 错误堆栈
     */
    private final String errorStackTrace;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        START, END, ERROR
    }

    /**
     * 开始事件构造函数
     */
    public AgentInvocationEvent(Object source, String traceId, String sessionId, String memoryId,
                                String agentName, String agentId, String parentAgentId,
                                LocalDateTime startTime, Map<String, Object> inputs) {
        super(source);
        this.traceId = traceId;
        this.sessionId = sessionId;
        this.memoryId = memoryId;
        this.agentName = agentName;
        this.agentId = agentId;
        this.parentAgentId = parentAgentId;
        this.eventType = EventType.START;
        this.startTime = startTime;
        this.endTime = null;
        this.durationMs = null;
        this.inputs = inputs;
        this.output = null;
        this.errorMessage = null;
        this.errorStackTrace = null;
    }

    /**
     * 结束事件构造函数
     */
    public AgentInvocationEvent(Object source, String traceId, String sessionId, String memoryId,
                                String agentName, String agentId, String parentAgentId,
                                LocalDateTime startTime, LocalDateTime endTime, Long durationMs,
                                Map<String, Object> inputs, Object output) {
        super(source);
        this.traceId = traceId;
        this.sessionId = sessionId;
        this.memoryId = memoryId;
        this.agentName = agentName;
        this.agentId = agentId;
        this.parentAgentId = parentAgentId;
        this.eventType = EventType.END;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMs = durationMs;
        this.inputs = inputs;
        this.output = output;
        this.errorMessage = null;
        this.errorStackTrace = null;
    }

    /**
     * 错误事件构造函数
     */
    public AgentInvocationEvent(Object source, String traceId, String sessionId, String memoryId,
                                String agentName, String agentId, String parentAgentId,
                                LocalDateTime startTime, LocalDateTime endTime, Long durationMs,
                                Map<String, Object> inputs, String errorMessage, String errorStackTrace) {
        super(source);
        this.traceId = traceId;
        this.sessionId = sessionId;
        this.memoryId = memoryId;
        this.agentName = agentName;
        this.agentId = agentId;
        this.parentAgentId = parentAgentId;
        this.eventType = EventType.ERROR;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMs = durationMs;
        this.inputs = inputs;
        this.output = null;
        this.errorMessage = errorMessage;
        this.errorStackTrace = errorStackTrace;
    }
}
