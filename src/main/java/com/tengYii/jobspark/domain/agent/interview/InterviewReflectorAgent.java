package com.tengYii.jobspark.domain.agent.interview;

import com.tengYii.jobspark.model.bo.interview.ReflectionResultBO;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 反思：回答评估与策略调整
 */
public interface InterviewReflectorAgent {

    @Agent(value = "面试评审官：评分并决策下一步", outputKey = "reflection")
    @UserMessage("""
                评估回答，score:0-10。
                decision只能是：PROBE/NEXT/STAGE_FINISH/FINISH
            """)
    ReflectionResultBO reflect(@V("currentQuestion") String question, @V("userAnswer") String answer, @V("resumeContext") String resumeContext);
}
