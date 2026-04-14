package com.tengYii.jobspark.model.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Agent执行轨迹表
 * 记录Agent调用的完整生命周期信息
 *
 * @author Teng-Yii
 * @since 2026-04-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent_execution_trace")
public class AgentExecutionTracePO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 唯一追踪ID
     * 格式: sessionId_memoryId_agentId_timestamp
     */
    private String traceId;

    /**
     * 面试会话ID
     */
    private String sessionId;

    /**
     * Memory ID
     * 格式: userId_resumeId
     */
    private String memoryId;

    /**
     * Agent名称
     */
    private String agentName;

    /**
     * Agent实例ID
     */
    private String agentId;

    /**
     * 父Agent ID（用于嵌套调用）
     */
    private String parentAgentId;

    /**
     * 执行状态: RUNNING/SUCCESS/FAILED
     */
    private String status;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 执行耗时(毫秒)
     */
    private Long durationMs;

    /**
     * 输入参数摘要(JSON格式)
     */
    private String inputSummary;

    /**
     * 输出结果摘要
     */
    private String outputSummary;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误堆栈
     */
    private String errorStackTrace;

    /**
     * 删除标记: 0-未删除 1-已删除
     */
    private Integer deleteFlag;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}