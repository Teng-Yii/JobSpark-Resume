package com.tengYii.jobspark.model.bo.interview;

import com.tengYii.jobspark.common.enums.InterviewDecisionEnum;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Java面试流程返回对象
 * 包含当前阶段信息和可恢复执行的上下文，支持面试会话的断点续传
 */
@Data
public class JavaInterviewResultBO {

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
     */
    private String jdAlignmentResult;

    /**
     * 面试计划
     */
    private InterviewPlanBO interviewPlan;

    /**
     * 当前阶段索引
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
     * 上一轮回答（用于恢复时继续对话）
     */
    private String lastUserAnswer;

    /**
     * 问答历史记录
     */
    private List<QARecord> qaHistory = new ArrayList<>();

    /**
     * 当前决策状态
     */
    private InterviewDecisionEnum currentDecision;

    /**
     * 面试是否已结束
     */
    private boolean finished;

    /**
     * 面试阶段完成状态
     * key: 阶段索引, value: 是否完成
     */
    private List<Boolean> stageCompletionStatus = new ArrayList<>();

    /**
     * 整体面试评估分数（面试结束后有值）
     */
    private Integer totalScore;

    /**
     * 面试反馈（面试结束后有值）
     */
    private String finalFeedback;

    /**
     * 问答记录内部类
     */
    @Data
    public static class QARecord {

        /**
         * 所属阶段索引
         */
        private int stageIndex;

        /**
         * 阶段名称
         */
        private String stageName;

        /**
         * 问题内容
         */
        private String question;

        /**
         * 回答内容
         */
        private String answer;

        /**
         * 评估分数
         */
        private int score;

        /**
         * 决策结果
         */
        private InterviewDecisionEnum decision;

        /**
         * 反馈评语
         */
        private String feedback;

        /**
         * 是否为追问
         */
        private boolean isProbe;
    }
}