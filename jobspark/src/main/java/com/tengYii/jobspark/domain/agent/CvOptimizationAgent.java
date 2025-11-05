package com.tengYii.jobspark.domain.agent;

import com.tengYii.jobspark.model.CvReview;
import dev.langchain4j.agentic.declarative.ExitCondition;
import dev.langchain4j.agentic.declarative.LoopAgent;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SubAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.service.V;

public interface CvOptimizationAgent {

    /**
     * 一个循环代理，它接收简历和职位描述作为输入，并返回：
     * 基于职位描述优化的简历。
     * <p>
     * 循环代理将持续征求人类反馈，直至人类对优化后的简历感到满意。
     *
     * @param cv             待优化的简历。
     * @param jobDescription 用于优化简历的职位描述。
     * @return 优化后的简历。
     */
    @LoopAgent(outputName = "cv", maxIterations = 3, subAgents = {
            @SubAgent(type = CvReviewer.class, outputName = "cvReview"),
            @SubAgent(type = ScoredCvTailor.class, outputName = "cv")
    })
    String optimizeCv(String cv, String jobDescription);


    @ExitCondition(testExitAtLoopEnd = true)
    private boolean exitCondition(AgenticScope agenticScope) {
        CvReview review = (CvReview) agenticScope.readState("cvReview");
        System.out.println("检查退出条件与得分=" + review.score); // 我们记录中间分数
        return review.score > 0.8;
    }

    @Output
    private static String outputOptimizedCv(@V("cv") String cv) {
        System.out.println("=== 优化后的简历如下： ===");
        return cv;
    }
}
