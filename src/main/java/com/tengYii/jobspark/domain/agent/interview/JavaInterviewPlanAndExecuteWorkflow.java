package com.tengYii.jobspark.domain.agent.interview;

import com.tengYii.jobspark.model.bo.interview.JavaInterviewResultBO;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

/**
 * Java面试计划与执行工作流接口
 * 定义了完整的面试流程：JD对齐 → 计划制定 → 问题执行 → 反思评估 → 条件路由
 */
public interface JavaInterviewPlanAndExecuteWorkflow {

    /**
     * 启动Java面试全流程
     *
     * @param userId         用户ID
     * @param resumeId       简历ID
     * @param jobDescription 职位描述
     * @param userAnswer     用户回答（首次调用时可为空）
     * @return 包含当前阶段信息和可恢复上下文的面试结果
     */
    @Agent("Java面试全流程：JD对齐 → 计划 → 执行 → 反思 → 循环")
    JavaInterviewResultBO startInterview(
            @V("userId") Long userId,
            @V("resumeId") Long resumeId,
            @V("jobDescription") String jobDescription,
            @V("userAnswer") String userAnswer
    );
}