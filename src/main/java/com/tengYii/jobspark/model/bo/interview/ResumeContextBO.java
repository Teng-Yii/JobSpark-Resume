package com.tengYii.jobspark.model.bo.interview;

import lombok.Data;

/**
 * 简历上下文业务对象
 * 用于在不同Agent之间传递简历分析结果和面试上下文信息
 */
@Data
public class ResumeContextBO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 简历ID
     */
    private Long resumeId;

    /**
     * 简历基本信息（姓名、期望岗位、个人摘要）
     */
    private String summary;

    /**
     * 核心技能列表（按熟练度分类）
     */
    private String skills;

    /**
     * 教育背景
     */
    private String education;

    /**
     * 重点项目经历
     */
    private String projects;

    /**
     * 职业背景
     */
    private String experience;

    /**
     * 证书与荣誉
     */
    private String certificates;

    /**
     * JD对齐结果（匹配度、缺失技能、重点考察方向）
     */
    private JDAlignmentResultBO jdAlignmentResult;

    /**
     * 当前面试阶段名称
     */
    private String currentStage;

    /**
     * 面试计划
     */
    private InterviewPlanBO interviewPlan;
}