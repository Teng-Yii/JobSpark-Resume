package com.tengYii.jobspark.config.listener;

import dev.langchain4j.agentic.observability.*;
import dev.langchain4j.agentic.scope.AgenticScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 可观测性监听器
 * 监控 Agent 的调用、工具执行、错误等信息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MyAgentListener implements AgentListener {

    // 用于追踪调用时长
    private final Map<String, Instant> executionStartTimes = new ConcurrentHashMap<>();

    // ==================== Agent 级别监控 ====================

    @Override
    public void beforeAgentInvocation(AgentRequest agentRequest) {
        String traceId = generateTraceId(agentRequest);
        executionStartTimes.put(traceId, Instant.now());

        log.info("[Agent调用] 开始 | Agent名称: {} | AgentID: {} | 输入参数: {}", agentRequest.agentName(), agentRequest.agentId(), agentRequest.inputs());

    }

    @Override
    public void afterAgentInvocation(AgentResponse agentResponse) {
        String traceId = generateTraceId(agentResponse);
        Instant startTime = executionStartTimes.remove(traceId);
        long durationMs = startTime != null ? Duration.between(startTime, Instant.now()).toMillis() : 0;

        log.info("[Agent调用] 完成 | Agent名称: {} | 耗时: {}ms | 输出: {}", agentResponse.agentName(), durationMs, truncateOutput(agentResponse.output()));
    }

    @Override
    public void onAgentInvocationError(AgentInvocationError error) {
        String traceId = generateTraceId(error);
        Instant startTime = executionStartTimes.remove(traceId);
        long durationMs = startTime != null ? Duration.between(startTime, Instant.now()).toMillis() : 0;

        log.error("[Agent调用] 异常 | Agent名称: {} | 耗时: {}ms | 错误: {}", error.agentName(), durationMs, error.error().getMessage(), error.error());


    }

    // ==================== 工具执行监控 ====================

    @Override
    public void beforeAgentToolExecution(BeforeAgentToolExecution beforeAgentToolExecution) {
        String toolName = beforeAgentToolExecution.toolExecution().request().name();
        log.info("[工具执行] 开始 | 工具名称: {}", toolName);


    }

    @Override
    public void afterAgentToolExecution(AfterAgentToolExecution afterAgentToolExecution) {
        String toolName = afterAgentToolExecution.toolExecution().request().name();
        Object toolResult = afterAgentToolExecution.toolExecution().result();

        log.info("[工具执行] 完成 | 工具名称: {} | 结果: {}", toolName, truncateOutput(toolResult));

    }

    // ==================== AgenticScope 生命周期监控 ====================

    @Override
    public void afterAgenticScopeCreated(AgenticScope agenticScope) {
    }

    @Override
    public void beforeAgenticScopeDestroyed(AgenticScope agenticScope) {
    }

    private String generateTraceId(AgentRequest request) {
        return request.agentId() + "_" + System.currentTimeMillis();
    }

    private String generateTraceId(AgentResponse response) {
        return response.agentId() + "_" + System.currentTimeMillis();
    }

    private String generateTraceId(AgentInvocationError error) {
        return error.agentId() + "_" + System.currentTimeMillis();
    }

    private String truncateOutput(Object output) {
        if (output == null) return "null";
        String str = output.toString();
        return str.length() > 200 ? str.substring(0, 200) + "..." : str;
    }
}