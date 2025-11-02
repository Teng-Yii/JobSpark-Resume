package com.tengYii.jobspark.model;

import dev.langchain4j.model.output.structured.Description;

public class CvReview {
    @Description("请按0到1的评分标准，评估您邀请该候选人参加面试的可能性。")
    public double score;

    @Description("简历反馈：哪些方面做得不错，哪些需要改进，哪些技能存在缺失，哪些是警示信号...")
    public String feedback;

    public CvReview() {} // 反序列化时无需无参构造函数，因为已存在其他构造函数！

    public CvReview(double score, String feedback) {
        this.score = score;
        this.feedback = feedback;
    }

    @Override
    public String toString() {
        return "\nCvReview: " +
                " - score = " + score +
                "\n- feedback = \"" + feedback + "\"\n";
    }
}
