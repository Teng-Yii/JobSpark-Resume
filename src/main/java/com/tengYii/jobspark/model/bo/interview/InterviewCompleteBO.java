package com.tengYii.jobspark.model.bo.interview;

import lombok.Data;

import java.util.List;

/**
 * 面试完成响应对象
 * 候选人视角：返回综合评估结果，不暴露评估细节
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
     * 需要加强的知识点（作为成长建议）
     */
    private List<String> improvementAreas;

    /**
     * 表现优秀的知识点
     */
    private List<String> strongAreas;

    /**
     * 面试统计信息
     */
    private Statistics statistics;

    /**
     * 问答历史记录（仅包含问题和回答，不含评估信息）
     */
    private List<QARecord> qaHistory;

    /**
     * 问答记录内部类（候选人视角）
     */
    @Data
    public static class QARecord {

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
         * 面试耗时（分钟）
         */
        private int durationMinutes;
    }
}