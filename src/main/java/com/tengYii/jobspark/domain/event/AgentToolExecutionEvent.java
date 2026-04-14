package com.tengYii.jobspark.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Agent工具调用事件
 * 用于异步记录Agent执行过程中的工具调用
 *
 * @author Teng-Yii
 * @since 2026-04-14
 */
@Getter
public class AgentToolExecutionEvent extends ApplicationEvent {

    /**
     * 关联的Agent执行轨迹ID
     */
    private final String traceId;

    /**
     * 会话ID
     */
    private final String sessionId;

    /**
     * 工具名称
     */
    private final String toolName;

    /**
     * 工具输入参数
     */
    private final String toolInput;

    /**
     * 工具输出结果
     */
    private final String toolOutput;

    /**
     * 是否执行成功
     */
    private final boolean success;

    /**
     * 执行耗时(毫秒)
     */
    private final Long executionTimeMs;

    /**
     * 调用顺序
     */
    private final int invocationOrder;

    /**
     * 构造函数
     */
    public AgentToolExecutionEvent(Object source, String traceId, String sessionId,
                                   String toolName, String toolInput, String toolOutput,
                                   boolean success, Long executionTimeMs, int invocationOrder) {
        super(source);
        this.traceId = traceId;
        this.sessionId = sessionId;
        this.toolName = toolName;
        this.toolInput = toolInput;
        this.toolOutput = toolOutput;
        this.success = success;
        this.executionTimeMs = executionTimeMs;
        this.invocationOrder = invocationOrder;
    }
}
