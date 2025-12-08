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
 * 简历格式元数据表
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-11-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("cv_format_meta")
public class CvFormatMetaPO implements Serializable {

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
     * 简历主题（如：简约/商务）
     */
    private String theme;

    /**
     * 对齐方式：left/center/right
     */
    private String alignment;

    /**
     * 行间距倍数
     */
    private Double lineSpacing;

    /**
     * 字体栈
     */
    private String fontFamily;

    /**
     * 日期格式
     */
    private String datePattern;

    /**
     * 超链接样式：underline/none
     */
    private String hyperlinkStyle;

    /**
     * 是否显示头像
     */
    private Boolean showAvatar;

    /**
     * 是否显示社交链接
     */
    private Boolean showSocial;

    /**
     * 是否双栏布局
     */
    private Boolean twoColumnLayout;

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
