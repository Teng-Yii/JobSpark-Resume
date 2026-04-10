package com.tengYii.jobspark.model.bo.interview;

import lombok.Data;

import java.util.List;

/**
 * 面试会话响应对象（首次交互返回）
 * 仅返回给候选人必要的信息，保护面试评估逻辑
 */
@Data
public class InterviewSessionBO {

    /**
     * 面试会话唯一标识ID
     */
    private String sessionId;

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
     * 阶段信息列表（仅包含阶段名称和进度，不含具体问题）
     */
    private List<StageInfo> stageInfos;

    /**
     * 欢迎提示信息
     */
    private String welcomeMessage;

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