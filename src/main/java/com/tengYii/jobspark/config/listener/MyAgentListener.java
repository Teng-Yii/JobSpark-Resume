package com.tengYii.jobspark.config.listener;

import dev.langchain4j.agentic.observability.AfterAgentToolExecution;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyAgentListener implements AgentListener {
    @Override
    public void afterAgentToolExecution(AfterAgentToolExecution afterAgentToolExecution) {

        log.info("调用工具的name:{}", afterAgentToolExecution.toolExecution().request().name());
        AgentListener.super.afterAgentToolExecution(afterAgentToolExecution);
    }

    @Override
    public void beforeAgentInvocation(AgentRequest agentRequest) {
        AgentListener.super.beforeAgentInvocation(agentRequest);
    }
}
