package com.tengYii.jobspark.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.time.LocalDateTime;
import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 简历社交链接表
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-11-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("cv_social_link")
public class CvSocialLinkPO implements Serializable {

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
     * 链接名称（如：GitHub/CSDN）
     *
     * @see com.tengYii.jobspark.common.enums.SocialLinkTypeEnum
     */
    private String label;

    /**
     * 链接地址
     */
    private String url;

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
