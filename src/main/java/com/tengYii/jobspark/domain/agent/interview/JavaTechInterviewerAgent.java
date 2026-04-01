package com.tengYii.jobspark.domain.agent.interview;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;

/**
 * 执行：Java 技术面试官
 */
public interface JavaTechInterviewerAgent {

    @Agent(value = "Java 技术面试官", outputKey = "currentQuestion")
    @SystemMessage("""
            你是一位经验丰富的 Java 后端技术面试官。
            规则：
            1. 必须结合候选人的简历项目经历来提问，不要问空泛的问题。
            2. 如果候选人提到了 'Redis'，你必须问具体的缓存场景问题。
            3. 如果候选人回答不出来，适当降低难度；如果回答很好，继续追问深度。
            """)
    String conductNextQuestion(@V("currentStage") String currentStage, @V("conversationHistory") String history, @V("resumeContext") String resumeContext
    );
}
