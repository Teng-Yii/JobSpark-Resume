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
 * Agent会话聚合统计表
 * 用于快速查询会话级别的统计信息
 *
 * @author Teng-Yii
 * @since 2026-04-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("agent_session_stats")
public class AgentSessionStatsPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * Memory ID
     */
    private String memoryId;

    /**
     * Agent调用总次数
     */
    private Integer totalAgentCalls;

    /**
     * 工具调用总次数
     */
    private Integer totalToolCalls;

    /**
     * 总耗时(毫秒)
     */
    private Long totalDurationMs;

    /**
     * 成功次数
     */
    private Integer successCount;

    /**
     * 失败次数
     */
    private Integer failedCount;

    /**
     * 首次调用时间
     */
    private LocalDateTime firstCallTime;

    /**
     * 最后调用时间
     */
    private LocalDateTime lastCallTime;

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
