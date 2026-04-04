package com.tengYii.jobspark.model.bo.interview;

import lombok.Data;

import java.util.List;

/**
 * 面试会话响应对象（首次交互返回）
 * 包含会话初始化所需的完整信息
 */
@Data
public class InterviewSessionBO {

    /**
     * 面试会话唯一标识ID
     */
    private Long sessionId;

    /**
     * 简历ID
     */
    private Long resumeId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 职位描述
     */
    private String jobDescription;

    /**
     * JD对齐结果
     * 首次返回，包含简历与职位的技能匹配分析
     */
    private JDAlignmentResultBO jdAlignmentResult;

    /**
     * 面试计划
     * 首次返回，包含完整的阶段规划和问题列表
     */
    private InterviewPlanBO interviewPlan;

    /**
     * 当前阶段索引（从0开始）
     */
    private int currentStageIndex;

    /**
     * 当前阶段名称
     */
    private String currentStageName;

    /**
     * 当前问题索引
     */
    private int currentQuestionIndex;

    /**
     * 当前问题内容
     */
    private String currentQuestion;

    /**
     * 计划的总问题数量
     */
    private int totalPlannedQuestions;

    /**
     * 阶段信息列表（用于前端展示进度）
     */
    private List<StageInfo> stageInfos;

    /**
     * 阶段信息内部类
     */
    @Data
    public static class StageInfo {

        /**
         * 阶段索引
         */
        private int stageIndex;

        /**
         * 阶段名称
         */
        private String stageName;

        /**
         * 计划问题数量
         */
        private int plannedQuestionCount;

        /**
         * 是否已完成
         */
        private boolean finished;
    }
}