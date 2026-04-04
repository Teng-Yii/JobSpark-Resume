package com.tengYii.jobspark.model.bo.interview;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 结构化面试问题业务对象
 * 包含面试官提问、出题意图分析、追问预案等完整的问题元数据
 */
@Data
public class InterviewQuestionBO {

    /**
     * 当前阶段名称
     * 例如："阶段1（自我介绍与项目破冰）"
     */
    private String stageName;

    /**
     * 当前阶段序号
     */
    private int stageOrder;

    /**
     * 考察主题名称
     * 例如："对公预约开户系统整体架构与核心流程"
     */
    private String topicName;

    /**
     * 面试官提问内容
     * 包含完整的问题文本
     */
    private String questionContent;

    /**
     * 出题意图分析列表
     */
    private List<IntentAnalysis> intentAnalyses = new ArrayList<>();

    /**
     * 追问预案列表
     */
    private List<FollowUpPlan> followUpPlans = new ArrayList<>();

    /**
     * 备选简化问题
     * 当候选人回答困难时使用的降级问题
     */
    private String simplifiedQuestion;

    /**
     * 出题意图分析内部类
     * 包含考察维度和评估目标
     */
    @Data
    public static class IntentAnalysis {

        /**
         * 考察维度
         * 例如："业务理解深度"、"全局架构视角"等
         */
        private String dimension;

        /**
         * 评估目标
         * 描述该维度要验证的具体能力
         */
        private String evaluationTarget;
    }

    /**
     * 追问预案内部类
     * 包含追问方向和具体问题
     */
    @Data
    public static class FollowUpPlan {

        /**
         * 追问方向标识
         * 例如："追问方向1"、"追问方向2"等
         */
        private String direction;

        /**
         * 适用场景
         * 描述在什么情况下使用该追问
         * 例如："若候选人架构描述偏笼统/偏执行层面"
         */
        private String applicableScenario;

        /**
         * 追问问题内容
         */
        private String followUpQuestion;
    }
}