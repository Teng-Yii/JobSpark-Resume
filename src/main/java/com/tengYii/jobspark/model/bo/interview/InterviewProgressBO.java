package com.tengYii.jobspark.model.bo.interview;

import com.tengYii.jobspark.common.enums.InterviewDecisionEnum;
import lombok.Data;

import java.util.List;

/**
 * 面试进行中响应对象
 * 用户每次回答问题后返回，包含当前状态和下一个问题
 */
@Data
public class InterviewProgressBO {

    /**
     * 面试会话唯一标识ID
     */
    private Long sessionId;

    /**
     * 当前问题索引
     */
    private int currentQuestionIndex;

    /**
     * 当前阶段索引
     */
    private int currentStageIndex;

    /**
     * 当前阶段名称
     */
    private String currentStageName;

    /**
     * 当前问题内容
     */
    private String currentQuestion;

    /**
     * 上一轮用户回答（用于确认）
     */
    private String lastUserAnswer;

    /**
     * 上一轮评估结果
     */
    private ReflectionResultBO lastReflection;

    /**
     * 当前决策状态
     * 决定下一轮的行为
     */
    private InterviewDecisionEnum currentDecision;

    /**
     * 进度信息
     */
    private ProgressInfo progress;

    /**
     * 进度信息内部类
     */
    @Data
    public static class ProgressInfo {

        /**
         * 已完成问题数
         */
        private int completedQuestions;

        /**
         * 计划总问题数
         */
        private int totalQuestions;

        /**
         * 当前阶段已完成问题数
         */
        private int currentStageCompleted;

        /**
         * 当前阶段计划问题数
         */
        private int currentStageTotal;

        /**
         * 已完成阶段数
         */
        private int completedStages;

        /**
         * 总阶段数
         */
        private int totalStages;
    }

    /**
     * 快速返回的便捷方法 - 仅包含最小信息
     * 适用于前后端约定好不需要完整信息的场景
     */
    public static InterviewProgressBO minimal(Long sessionId, String question, InterviewDecisionEnum decision) {
        InterviewProgressBO result = new InterviewProgressBO();
        result.setSessionId(sessionId);
        result.setCurrentQuestion(question);
        result.setCurrentDecision(decision);
        return result;
    }
}