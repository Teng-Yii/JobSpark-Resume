package com.tengYii.jobspark.model.bo.interview;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.List;

/**
 * JD对齐结果业务对象
 * 用于存储岗位JD与候选人简历的匹配分析结果
 */
@Data
public class JDAlignmentResultBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 整体匹配度 0-100
     */
    private Integer matchScore;

    /**
     * 已匹配技能列表
     */
    private List<String> matchedSkills;

    /**
     * 缺失技能列表
     */
    private List<String> missingSkills;

    /**
     * 相关项目列表
     */
    private List<String> relatedProjects;

    /**
     * 面试重点考察方向列表
     */
    private List<String> focusAreas;

    /**
     * 综合建议
     */
    private String suggestion;

    /**
     * 获取匹配技能数量
     */
    public Integer getMatchedSkillCount() {
        return CollectionUtils.isEmpty(matchedSkills) ? 0 : matchedSkills.size();
    }

    /**
     * 获取缺失技能数量
     */
    public Integer getMissingSkillCount() {
        return CollectionUtils.isEmpty(missingSkills) ? 0 : missingSkills.size();
    }

    /**
     * 判断是否有缺失技能
     */
    public boolean hasMissingSkills() {
        return CollectionUtils.isNotEmpty(missingSkills);
    }

    /**
     * 判断匹配度是否达到阈值
     *
     * @param threshold 阈值
     * @return 是否达到阈值
     */
    public boolean matchScoreReaches(Integer threshold) {
        return matchScore != null && matchScore >= threshold;
    }
}