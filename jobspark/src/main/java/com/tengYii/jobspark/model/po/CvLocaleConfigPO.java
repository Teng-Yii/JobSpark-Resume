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
 * 简历国际化配置表
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-11-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("cv_locale_config")
public class CvLocaleConfigPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 格式元数据ID
     */
    private Long formatMetaId;

    /**
     * 语言标识（如：zh-CN, en-US, ja-JP）
     */
    private String locale;

    /**
     * 本地化日期格式
     */
    private String datePattern;

    /**
     * 区块名称本地化（如：{"education":"教育经历","experience":"工作经历"}）
     */
    private String sectionLabels;

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
