package com.tengYii.jobspark.model.bo.interview;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 面试状态业务对象
 * 用于存储面试过程中的状态信息，包括当前阶段、问题进度和问答历史
 */
@Data
public class InterviewStateBO {

    /**
     * 面试会话唯一标识ID
     */
    private Long sessionId;

    /**
     * 关联的简历ID
     */
    private Long cvId;

    /**
     * 面试者用户ID
     */
    private Long userId;

    /**
     * 面试计划信息
     */
    private InterviewPlanBO plan;

    /**
     * 当前面试阶段的索引位置
     */
    private int currentStageIndex;

    /**
     * 当前阶段内问题的索引位置
     */
    private int currentQuestionIndex;

    /**
     * 问答历史记录列表
     */
    private List<QA> qaHistory = new ArrayList<>();

    /**
     * 问答记录内部类
     * 用于存储单个问答对的信息
     */
    @Data
    public static class QA {
        /**
         * 问题内容
         */
        private String question;

        /**
         * 回答内容
         */
        private String answer;

        /**
         * 是否为探查性问题
         * 探查性问题用于深入了解候选人的具体能力或经验
         */
        private boolean isProbe;
    }
}
