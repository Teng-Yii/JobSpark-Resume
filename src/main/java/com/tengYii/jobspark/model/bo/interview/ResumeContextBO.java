package com.tengYii.jobspark.model.bo.interview;

import com.tengYii.jobspark.model.bo.cv.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 简历上下文精简版业务对象
 * <p>
 * 用于在Agent调用时传递简历关键信息，避免传递完整Resume对象导致token过多
 * 该对象从Resume对象精简而来，只保留面试过程中需要了解的必要信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeContextBO {
    /**
     * 简历ID
     */
    private String resumeId;

    /**
     * 候选人姓名
     */
    private String name;

    /**
     * 期望岗位/头衔
     */
    private String title;

    /**
     * 个人摘要
     */
    private String summary;

    /**
     * 专业技能
     */
    private List<SkillBO> skills;

    /**
     * 项目经验列表
     */
    private List<ProjectBO> projects;

    /**
     * 工作经历列表
     */
    private List<ExperienceBO> experiences;

    /**
     * 教育经历列表
     */
    private List<EducationBO> educations;

    /**
     * 证书/获奖
     */
    private List<CertificateBO> certificates;
}