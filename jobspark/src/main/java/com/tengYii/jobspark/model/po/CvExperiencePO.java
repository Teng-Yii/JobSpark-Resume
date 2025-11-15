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
 * 简历工作经历表
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-11-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("cv_experience")
public class CvExperiencePO implements Serializable {

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
     * 经历类型（全职/实习/兼职/ freelance）
     */
    private String type;

    /**
     * 公司名称
     */
    private String company;

    /**
     * 行业（如：互联网/金融）
     */
    private String industry;

    /**
     * 职位名称
     */
    private String role;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期（可为空表示在职）
     */
    private LocalDate endDate;

    /**
     * 工作概述（Markdown格式）
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
