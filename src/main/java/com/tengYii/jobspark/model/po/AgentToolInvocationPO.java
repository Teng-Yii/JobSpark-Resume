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
 * Agent工具调用记录表
 * 记录Agent执行过程中调用的工具信息
 *
 * @author Teng-Yii
 * @since 2026-04-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent_tool_invocation")
public class AgentToolInvocationPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联的Agent执行轨迹ID
     */
    private String traceId;

    /**
     * 调用顺序
     */
    private Integer invocationOrder;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具输入参数(JSON格式)
     */
    private String toolInput;

    /**
     * 工具输出结果
     */
    private String toolOutput;

    /**
     * 是否执行成功: 1-成功 0-失败
     */
    private Integer success;

    /**
     * 执行耗时(毫秒)
     */
    private Long executionTimeMs;

    /**
     * 删除标记: 0-未删除 1-已删除
     */
    private Integer deleteFlag;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
