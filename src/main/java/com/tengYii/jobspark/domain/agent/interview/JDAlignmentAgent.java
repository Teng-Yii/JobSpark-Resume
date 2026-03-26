package com.tengYii.jobspark.domain.agent.interview;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface JDAlignmentAgent {

    @Agent("简历-JD匹配与重点考察方向分析")
    @UserMessage("""
                调用 JDAlignmentSkill 分析简历与JD的匹配度，
                输出：匹配技能、缺失技能、重点考察领域。
            """)
    String align(@V("jdText") String jdText, @V("cvId") Long cvId);
}
