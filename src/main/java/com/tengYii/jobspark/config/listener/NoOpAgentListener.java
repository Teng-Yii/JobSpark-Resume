package com.tengYii.jobspark.config.listener;

import dev.langchain4j.agentic.observability.*;
import dev.langchain4j.agentic.scope.AgenticScope;

/**
 * 空Agent监听器
 * 当监听被禁用时使用，不执行任何操作
 * <p>
 * 使用场景：
 * 1. 全局监听被禁用时
 * 2. 特定Agent类型的监听被禁用时
 * 3. 性能敏感场景下临时禁用监听
 *
 * @author Teng-Yii
 * @since 2026-04-15
 */
public class NoOpAgentListener implements AgentListener {

    @Override
    public void beforeAgentInvocation(AgentRequest agentRequest) {
        // 空实现，不执行任何操作
    }

    @Override
    public void afterAgentInvocation(AgentResponse agentResponse) {
        // 空实现，不执行任何操作
    }

    @Override
    public void onAgentInvocationError(AgentInvocationError error) {
        // 空实现，不执行任何操作
    }

    @Override
    public void beforeAgentToolExecution(BeforeAgentToolExecution beforeAgentToolExecution) {
        // 空实现，不执行任何操作
    }

    @Override
    public void afterAgentToolExecution(AfterAgentToolExecution afterAgentToolExecution) {
        // 空实现，不执行任何操作
    }

    @Override
    public void afterAgenticScopeCreated(AgenticScope agenticScope) {
        // 空实现，不执行任何操作
    }

    @Override
    public void beforeAgenticScopeDestroyed(AgenticScope agenticScope) {
        // 空实现，不执行任何操作
    }

    @Override
    public boolean inheritedBySubagents() {
        return true;
    }
}
