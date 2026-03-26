package com.tengYii.jobspark.common.enums;

/**
 * 面试流程决策枚举
 * 定义面试过程中对候选人回答后的流程控制决策
 */
public enum InterviewDecisionEnum {
    /**
     * 继续探查
     * 深入了解候选人的具体能力或经验细节
     */
    PROBE,

    /**
     * 进入下一个问题
     * 当前问题评估完成，准备切换到下一问题
     */
    NEXT,

    /**
     * 当前阶段完成
     * 当前面试阶段的所有问题已结束，准备进入下一阶段
     */
    STAGE_FINISH,

    /**
     * 面试结束
     * 所有面试流程已完成
     */
    FINISH
}