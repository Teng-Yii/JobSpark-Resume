package com.tengYii.jobspark.domain.agent.interview;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

public interface JavaInterviewPlanAndExecuteWorkflow {

    @Agent("Java面试全流程：JD对齐 → 计划 → 执行 → 反思 → 循环")
    Object startInterview(
            @V("userId") Long userId,
            @V("cvId") Long cvId,
            @V("jdText") String jdText,
            @V("userAnswer") String userAnswer
    );
}