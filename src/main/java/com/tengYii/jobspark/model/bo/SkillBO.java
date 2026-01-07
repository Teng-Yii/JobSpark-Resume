package com.tengYii.jobspark.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 技能/亮点
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillBO {

    /**
     * 技能分类（如：技术/语言/软技能）
     *
     * @see com.tengYii.jobspark.common.enums.SkillCategoryEnum
     */
    private String category;

    /**
     * 技能名称
     */
    private String name;

    /**
     * 熟练度（熟练/良好/了解/精通）
     *
     * @see com.tengYii.jobspark.common.enums.SkillLevelEnum
     */
    private String level;

    /**
     * 排序顺序（升序）
     */
    private Integer sortOrder;

    /**
     * 专业技能亮点（Markdown格式）
     */
    private List<HighlightBO> highlights;
}