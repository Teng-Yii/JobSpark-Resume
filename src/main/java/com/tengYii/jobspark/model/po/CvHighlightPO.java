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
 * 统一亮点表（工作经历/项目经历的亮点）
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-11-16
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("cv_highlight")
public class CvHighlightPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 亮点类型：1-工作经历亮点，2-项目经历亮点，3-专业技能亮点
     *
     * @see com.tengYii.jobspark.common.enums.CvHighLightTypeEnum
     */
    private Integer type;

    /**
     * 关联ID：根据type对应工作经历/项目经历/专业技能ID
     */
    private Long relatedId;

    /**
     * 亮点内容（职责/业绩/贡献，Markdown格式）
     */
    private String highlight;

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
