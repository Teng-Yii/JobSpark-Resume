package com.tengYii.jobspark.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.time.LocalDateTime;
import java.io.Serializable;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 简历处理任务表
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-12-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@TableName("resume_task")
public class ResumeTaskPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务唯一标识符
     */
    private String taskId;

    /**
     * 用户ID（如果有用户体系）
     */
    private Long userId;

    /**
     * 存储的文件名
     */
    private String fileName;

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件MIME类型
     */
    private String contentType;

    /**
     * 文件存储路径
     */
    private String filePath;

    /**
     * 任务状态：PROCESSING-处理中，ANALYZING-解析中，SAVING-存储中，COMPLETED-完成，FAILED-失败
     *
     * @see com.tengYii.jobspark.common.enums.TaskStatusEnum
     */
    private String status;

    /**
     * 关联的简历ID（完成后填充）
     */
    private Long resumeId;

    /**
     * 错误代码
     */
    private String errorCode;

    /**
     * 错误详细信息
     */
    private String errorMessage;

    /**
     * 开始处理时间
     */
    private LocalDateTime startTime;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0-未删除 1-已删除
     *
     * @see com.tengYii.jobspark.common.enums.DeleteFlagEnum
     */
    private Integer deleteFlag;
}
