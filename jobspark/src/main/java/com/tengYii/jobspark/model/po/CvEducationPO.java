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
 * 简历教育经历表
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-11-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("cv_education")
public class CvEducationPO implements Serializable {

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
     * 学校名称
     */
    private String school;

    /**
     * 专业名称
     */
    private String major;

    /**
     * 学历（如：本科/硕士/博士）
     *
     * @see com.tengYii.jobspark.common.enums.EducationDegreeEnum
     */
    private String degree;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期（可为空表示在读）
     */
    private LocalDate endDate;

    /**
     * 描述（如：GPA/荣誉等，Markdown格式）
     */
    private String description;

    /**
     * 排序顺序（升序）
     */
    private Integer sortOrder;

    /**
     * 逻辑删除：0-未删除 1-已删除
     *
     * @see com.tengYii.jobspark.common.enums.DeleteFlagEnum
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
