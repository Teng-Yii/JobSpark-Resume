package com.tengYii.jobspark.model.bo.interview;

import com.tengYii.jobspark.common.enums.InterviewDecisionEnum;
import lombok.Data;

/**
 * 面试反思结果业务对象
 * 用于存储面试过程中对候选人回答的评估结果，包括评分、决策和反馈
 */
@Data
public class ReflectionResultBO {

    /**
     * 面试评估分数
     * 表示对候选人当前回答的综合评分
     */
    private int score;

    /**
     * 面试流程决策
     *
     * @see InterviewDecisionEnum
     */
    private InterviewDecisionEnum decision;

    /**
     * 对候选人回答的反馈评价
     * 包含对回答质量、表现等方面的具体评语
     */
    private String feedback;
}
