package com.tengYii.jobspark.model.bo.interview;

import com.tengYii.jobspark.common.enums.InterviewDecisionEnum;
import lombok.Data;

import java.util.List;

/**
 * 面试完成响应对象
 * 面试结束时返回，包含完整的结果汇总
 */
@Data
public class InterviewCompleteBO {

    /**
     * 面试会话唯一标识ID
     */
    private Long sessionId;

    /**
     * 面试是否已完成
     */
    private boolean finished;

    /**
     * 整体面试评估分数（0-100）
     */
    private Integer totalScore;

    /**
     * 面试反馈（综合评语）
     */
    private String finalFeedback;

    /**
     * 问答历史记录
     * 仅在面试结束时返回完整历史
     */
    private List<QARecord> qaHistory;

    /**
     * 阶段结果汇总
     */
    private List<StageResult> stageResults;

    /**
     * 面试统计信息
     */
    private Statistics statistics;

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

    /**
     * 阶段结果内部类
     */
    @Data
    public static class StageResult {

        /**
         * 阶段索引
         */
        private int stageIndex;

        /**
         * 阶段名称
         */
        private String stageName;

        /**
         * 阶段得分（该阶段所有问题平均分）
         */
        private Double stageScore;

        /**
         * 该阶段问题数
         */
        private int questionCount;

        /**
         * 该阶段追问数
         */
        private int probeCount;

        /**
         * 阶段综合评价
         */
        private String stageFeedback;
    }

    /**
     * 统计信息内部类
     */
    @Data
    public static class Statistics {

        /**
         * 总问题数
         */
        private int totalQuestions;

        /**
         * 总追问数
         */
        private int totalProbes;

        /**
         * 回答正确数
         */
        private int correctAnswers;

        /**
         * 回答错误数
         */
        private int wrongAnswers;

        /**
         * 需要加强的知识点
         */
        private List<String> weakAreas;

        /**
         * 表现优秀的知识点
         */
        private List<String> strongAreas;

        /**
         * 面试耗时（分钟）
         */
        private int durationMinutes;
    }
}