package com.tengYii.jobspark.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 简历项目经验表
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-11-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("cv_project")
public class CvProjectPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 简历ID
     */
    private Long cvId;

    /**
     * 项目名称
     */
    private String name;

    /**
     * 项目开始日期
     */
    private LocalDate startDate;

    /**
     * 项目结束日期
     */
    private LocalDate endDate;

    /**
     * 角色/职责
     */
    private String role;

    /**
     * 项目描述（Markdown格式）
     */
    private String descriptionMarkdown;

    /**
     * 排序顺序（升序）
     */
    private Integer sortOrder;

    /**
     * 逻辑删除：0-未删除 1-已删除
     */
    private Boolean isDeleted;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;


}
