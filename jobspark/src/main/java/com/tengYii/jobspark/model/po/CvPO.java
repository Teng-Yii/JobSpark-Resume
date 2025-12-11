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
 * 简历基本信息表
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-11-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("cv")
public class CvPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 简历ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 姓名（必填）
     */
    private String name;

    /**
     * 出生日期（用于计算年龄，可选）
     */
    private LocalDate birthDate;

    /**
     * 期望岗位/头衔（可选）
     */
    private String title;

    /**
     * 头像URL（可选）
     */
    private String avatarUrl;

    /**
     * 个人摘要（Markdown格式）
     */
    private String summary;

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
