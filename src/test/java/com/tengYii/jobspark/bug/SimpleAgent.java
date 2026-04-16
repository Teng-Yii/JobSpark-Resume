package com.tengYii.jobspark.bug;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SimpleAgent {
    @Agent(value = "简单Agent用于复现bug", outputKey = "result")
    @UserMessage("你是一个对话助手，回答下面的问题：{{input}}")
    String execute(@V("input") String input);
}
