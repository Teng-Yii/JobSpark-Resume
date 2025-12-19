package com.tengYii.jobspark.model.llm;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;


/**
 * 简历评审结果模型类
 * <p>
 * 用于封装大模型对简历的评审结果，包括评分和反馈意见。
 * </p>
 *
 * @author tengYii
 */
@Data
public class CvReview {

    /**
     * 简历评分
     * 范围：0.0-1.0，表示邀请候选人参加面试的可能性
     */
    @Description("请按0到1的评分标准，评估您邀请该候选人参加面试的可能性。")
    public double score;

    /**
     * 简历反馈意见
     * 包含优势、不足、改进建议等详细信息
     */
    @Description("简历反馈：哪些方面做得不错，哪些需要改进，哪些技能存在缺失，哪些是警示信号...")
    public String feedback;

    @Override
    public String toString() {
        return "\nCvReview: " +
                " - score = " + score +
                "\n- feedback = \"" + feedback + "\"\n";
    }
}
