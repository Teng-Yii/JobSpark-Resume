package com.tengYii.jobspark.config.listener;

import com.tengYii.jobspark.domain.event.AgentInvocationEvent;
import com.tengYii.jobspark.domain.event.AgentToolExecutionEvent;
import dev.langchain4j.agentic.observability.*;
import dev.langchain4j.agentic.scope.AgenticScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 可持久化的Agent监听器
 * 扩展AgentListener接口，将监控数据通过Spring Event异步发布
 *
 * @author Teng-Yii
 * @since 2026-04-14
 */
@Slf4j
public class PersistableAgentListener implements AgentListener {

    private final ApplicationEventPublisher eventPublisher;
    
    // 用于追踪调用时长和工具调用顺序
    private final Map<String, Instant> executionStartTimes = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> toolInvocationCounters = new ConcurrentHashMap<>();
    private final Map<String, String> sessionIdMap = new ConcurrentHashMap<>();

    public PersistableAgentListener(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 设置会话ID，用于关联追踪
     *
     * @param memoryId  Memory ID
     * @param sessionId 会话ID
     */
    public void setSessionId(String memoryId, String sessionId) {
        sessionIdMap.put(memoryId, sessionId);
    }

    @Override
    public void beforeAgentInvocation(AgentRequest agentRequest) {
        String traceId = generateTraceId(agentRequest);
        String memoryId = agentRequest.agenticScope().memoryId().toString();
        String sessionId = sessionIdMap.getOrDefault(memoryId, null);
        
        executionStartTimes.put(traceId, Instant.now());
        toolInvocationCounters.put(traceId, new AtomicInteger(0));

        // 发布开始事件
        AgentInvocationEvent event = new AgentInvocationEvent(
                this,
                traceId,
                sessionId,
                memoryId,
                agentRequest.agentName(),
                agentRequest.agentId(),
                getParentAgentId(agentRequest),
                LocalDateTime.now(),
                agentRequest.inputs()
        );
        
        eventPublisher.publishEvent(event);
        log.debug("[Agent调用] 开始 | traceId={}, agentName={}", traceId, agentRequest.agentName());
    }

    @Override
    public void afterAgentInvocation(AgentResponse agentResponse) {
        String traceId = generateTraceId(agentResponse);
        Instant startTime = executionStartTimes.remove(traceId);
        long durationMs = startTime != null ? Duration.between(startTime, Instant.now()).toMillis() : 0;
        
        String memoryId = agentResponse.agenticScope().memoryId().toString();
        String sessionId = sessionIdMap.getOrDefault(memoryId, null);

        // 发布结束事件
        AgentInvocationEvent event = new AgentInvocationEvent(
                this,
                traceId,
                sessionId,
                memoryId,
                agentResponse.agentName(),
                agentResponse.agentId(),
                getParentAgentId(agentResponse),
                startTime != null ? LocalDateTime.ofInstant(startTime, java.time.ZoneId.systemDefault()) : LocalDateTime.now(),
                LocalDateTime.now(),
                durationMs,
                agentResponse.agent().parent() != null ? null : Map.of(), // 简化处理
                truncateOutput(agentResponse.output())
        );
        
        eventPublisher.publishEvent(event);
        toolInvocationCounters.remove(traceId);
        log.debug("[Agent调用] 完成 | traceId={}, agentName={}, duration={}ms", 
                traceId, agentResponse.agentName(), durationMs);
    }

    @Override
    public void onAgentInvocationError(AgentInvocationError error) {
        String traceId = generateTraceId(error);
        Instant startTime = executionStartTimes.remove(traceId);
        long durationMs = startTime != null ? Duration.between(startTime, Instant.now()).toMillis() : 0;
        
        String memoryId = error.agenticScope().memoryId().toString();
        String sessionId = sessionIdMap.getOrDefault(memoryId, null);

        // 发布错误事件
        AgentInvocationEvent event = new AgentInvocationEvent(
                this,
                traceId,
                sessionId,
                memoryId,
                error.agentName(),
                error.agentId(),
                getParentAgentId(error),
                startTime != null ? LocalDateTime.ofInstant(startTime, java.time.ZoneId.systemDefault()) : LocalDateTime.now(),
                LocalDateTime.now(),
                durationMs,
                error.agent().parent() != null ? null : Map.of(), // 简化处理
                error.error().getMessage(),
                getStackTraceString(error.error())
        );
        
        eventPublisher.publishEvent(event);
        toolInvocationCounters.remove(traceId);
        log.error("[Agent调用] 异常 | traceId={}, agentName={}, error={}", 
                traceId, error.agentName(), error.error().getMessage());
    }

    @Override
    public void beforeAgentToolExecution(BeforeAgentToolExecution beforeAgentToolExecution) {
        // 工具执行前不处理，在完成后统一记录
    }

    @Override
    public void afterAgentToolExecution(AfterAgentToolExecution afterAgentToolExecution) {
        String traceId = generateTraceId(afterAgentToolExecution);
        AtomicInteger counter = toolInvocationCounters.get(traceId);
        int order = counter != null ? counter.incrementAndGet() : 0;
        
        String memoryId = afterAgentToolExecution.agenticScope().memoryId().toString();
        String sessionId = sessionIdMap.getOrDefault(memoryId, null);
        
        String toolName = afterAgentToolExecution.toolExecution().request().name();
        Object toolResult = afterAgentToolExecution.toolExecution().result();
        // 简化处理，假设工具执行成功
        boolean success = true;

        // 发布工具执行事件
        AgentToolExecutionEvent event = new AgentToolExecutionEvent(
                this,
                traceId,
                sessionId,
                toolName,
                truncateOutput(afterAgentToolExecution.toolExecution().request().arguments()),
                truncateOutput(toolResult),
                success,
                null, // 工具执行耗时需要额外追踪
                order
        );
        
        eventPublisher.publishEvent(event);
        log.debug("[工具执行] 完成 | traceId={}, toolName={}, order={}", traceId, toolName, order);
    }

    @Override
    public void afterAgenticScopeCreated(AgenticScope agenticScope) {
        // 作用域创建时不处理
    }

    @Override
    public void beforeAgenticScopeDestroyed(AgenticScope agenticScope) {
        // 作用域销毁时清理相关数据
        String memoryId = agenticScope.memoryId().toString();
        sessionIdMap.remove(memoryId);
    }

    @Override
    public boolean inheritedBySubagents() {
        return true;
    }

    // ==================== 辅助方法 ====================

    private String generateTraceId(AgentRequest request) {
        return request.agentId() + "_" + System.currentTimeMillis();
    }

    private String generateTraceId(AgentResponse response) {
        return response.agentId() + "_" + System.currentTimeMillis();
    }

    private String generateTraceId(AgentInvocationError error) {
        return error.agentId() + "_" + System.currentTimeMillis();
    }

    private String generateTraceId(AfterAgentToolExecution toolExecution) {
        // 从工具执行上下文中获取traceId - 简化处理
        return "tool_" + System.currentTimeMillis();
    }

    private String getParentAgentId(AgentRequest request) {
        return request.agent().parent() != null ? request.agent().parent().agentId() : null;
    }

    private String getParentAgentId(AgentResponse response) {
        return response.agent().parent() != null ? response.agent().parent().agentId() : null;
    }

    private String getParentAgentId(AgentInvocationError error) {
        return error.agent().parent() != null ? error.agent().parent().agentId() : null;
    }

    private String truncateOutput(Object output) {
        if (output == null) return null;
        String str = output.toString();
        return str.length() > 500 ? str.substring(0, 500) + "..." : str;
    }

    private String getStackTraceString(Throwable throwable) {
        if (throwable == null) return null;
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append(element.toString()).append("\n");
            if (sb.length() > 2000) {
                sb.append("...");
                break;
            }
        }
        return sb.toString();
    }
}