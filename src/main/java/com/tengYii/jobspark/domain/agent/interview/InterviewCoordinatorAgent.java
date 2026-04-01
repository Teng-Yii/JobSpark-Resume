package com.tengYii.jobspark.domain.agent.interview;

import com.tengYii.jobspark.model.bo.interview.InterviewPlanBO;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;

/**
 * 核心：Plan-and-Execute 协调者
 */
public interface InterviewCoordinatorAgent {

    @Agent(value = "面试计划制定专家", outputKey = "interviewPlan")
    @SystemMessage("""
            你是一个专业的 Java 面试主考官。你的任务是根据候选人的简历制定面试计划。
            请严格按照 JSON 格式输出计划。
            阶段建议：
            1. 自我介绍与项目破冰 (针对简历中的项目描述)
            2. Java 基础与进阶 (根据简历技能栈调整)
            3. 系统设计与架构 (如果简历有提到架构经验)
            """)
    InterviewPlanBO generateInterviewPlan(@V("resumeContext") String resumeContext);
}
