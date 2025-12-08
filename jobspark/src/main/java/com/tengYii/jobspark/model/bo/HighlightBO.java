package com.tengYii.jobspark.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作经历要点
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HighlightBO {

    /**
     * 主键ID
     */
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
}
