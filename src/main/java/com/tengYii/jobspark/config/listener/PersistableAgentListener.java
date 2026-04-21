package com.tengYii.jobspark.config.listener;

import com.tengYii.jobspark.common.enums.AgentTypeEnum;
import com.tengYii.jobspark.config.listener.event.AgentInvocationEvent;
import com.tengYii.jobspark.config.listener.event.AgentToolExecutionEvent;
import dev.langchain4j.agentic.observability.*;
import dev.langchain4j.agentic.scope.AgenticScope;
import lombok.Getter;
import lombok.Setter;
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
 * <p>
 * 支持特性：
 * 1. 按Agent类型区分监听策略
 * 2. 可配置的输入输出长度限制
 * 3. 详细日志开关
 * 4. 会话ID关联追踪
 *
 * @author Teng-Yii
 * @since 2026-04-14
 */
@Slf4j
public class PersistableAgentListener implements AgentListener {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Agent类型，用于区分不同监听策略
     */
    @Getter
    private final AgentTypeEnum agentType;

    // 用于追踪调用时长和工具调用顺序
    private final Map<String, Instant> executionStartTimes = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> toolInvocationCounters = new ConcurrentHashMap<>();
    private final Map<String, String> sessionIdMap = new ConcurrentHashMap<>();
    // 用于存储 memoryId 到 traceId 的映射，供工具执行事件使用
    private final Map<String, String> memoryIdToTraceIdMap = new ConcurrentHashMap<>();

    // ==================== 可配置属性 ====================

    /**
     * 是否启用详细日志
     */
    @Setter
    @Getter
    private boolean detailedLogging = true;

    /**
     * 输入参数最大长度
     */
    @Setter
    @Getter
    private int maxInputLength = 500;

    /**
     * 输出结果最大长度
     */
    @Setter
    @Getter
    private int maxOutputLength = 500;

    /**
     * 构造函数（带Agent类型）
     *
     * @param eventPublisher Spring事件发布器
     * @param agentType      Agent类型
     */
    public PersistableAgentListener(ApplicationEventPublisher eventPublisher, AgentTypeEnum agentType) {
        this.eventPublisher = eventPublisher;
        this.agentType = agentType != null ? agentType : AgentTypeEnum.GENERIC;
    }

    /**
     * 构造函数（向后兼容）
     *
     * @param eventPublisher Spring事件发布器
     */
    public PersistableAgentListener(ApplicationEventPublisher eventPublisher) {
        this(eventPublisher, AgentTypeEnum.GENERIC);
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
        try {
            String memoryId = String.valueOf(agentRequest.inputs().get("memoryId"));
            String traceId = generateTraceId(agentRequest, memoryId);
            String sessionId = sessionIdMap.getOrDefault(memoryId, null);

            executionStartTimes.put(traceId, Instant.now());
            toolInvocationCounters.put(traceId, new AtomicInteger(0));
            // 保存 memoryId 到 traceId 的映射，供工具执行事件使用
            memoryIdToTraceIdMap.put(memoryId, traceId);

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
        } catch (Exception e) {
            log.warn("beforeAgentInvocation监听器异常: {}, 影响Agent调用", e.getMessage(), e);
        }
    }

    @Override
    public void afterAgentInvocation(AgentResponse agentResponse) {
        try {
            String memoryId = String.valueOf(agentResponse.inputs().get("memoryId"));
            String traceId = generateTraceId(agentResponse, memoryId);
            Instant startTime = executionStartTimes.remove(traceId);
            long durationMs = startTime != null ? Duration.between(startTime, Instant.now()).toMillis() : 0;

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
        } catch (Exception e) {
            log.warn("afterAgentInvocation监听器异常: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onAgentInvocationError(AgentInvocationError error) {
        try {
            String memoryId = String.valueOf(error.inputs().get("memoryId"));
            String traceId = generateTraceId(error, memoryId);
            Instant startTime = executionStartTimes.remove(traceId);
            long durationMs = startTime != null ? Duration.between(startTime, Instant.now()).toMillis() : 0;

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
            log.error("[Agent调用] 异常 | traceId={}, agentName={}, error={}"
                    , traceId, error.agentName(), error.error().getMessage());
        } catch (Exception e) {
            log.warn("onAgentInvocationError监听器异常: {}", e.getMessage(), e);
        }
    }

    @Override
    public void beforeAgentToolExecution(BeforeAgentToolExecution beforeAgentToolExecution) {
        try {
            String memoryId = String.valueOf(beforeAgentToolExecution.toolExecution().invocationContext().chatMemoryId());

            // 如果memoryIdToTraceIdMap中没有对应的traceId，则初始化
            // 这样可以处理直接从工具调用开始的场景（绕过Agent调用）
            if (!memoryIdToTraceIdMap.containsKey(memoryId)) {
                String traceId = "tool_" + memoryId + "_" + System.currentTimeMillis();
                memoryIdToTraceIdMap.put(memoryId, traceId);
                executionStartTimes.put(traceId, Instant.now());
                toolInvocationCounters.put(traceId, new AtomicInteger(0));

                log.info("[工具执行] 初始化追踪 | memoryId={}, traceId={}", memoryId, traceId);
            }

            log.info("[工具执行] 开始 | traceId={}, toolName={}", memoryIdToTraceIdMap.get(memoryId), beforeAgentToolExecution.toolExecution().request().name());
        } catch (Exception e) {
            log.warn("beforeAgentToolExecution监听器异常: {}", e.getMessage(), e);
        }
    }

    @Override
    public void afterAgentToolExecution(AfterAgentToolExecution afterAgentToolExecution) {
        try {
            String traceId = generateTraceId(afterAgentToolExecution);
            AtomicInteger counter = toolInvocationCounters.get(traceId);
            int order = counter != null ? counter.incrementAndGet() : 0;

            String memoryId = String.valueOf(afterAgentToolExecution.toolExecution().invocationContext().chatMemoryId());
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
        } catch (Exception e) {
            log.warn("afterAgentToolExecution监听器异常: {}", e.getMessage(), e);
        }
    }

    @Override
    public void afterAgenticScopeCreated(AgenticScope agenticScope) {
        // 作用域创建时不处理
    }

    @Override
    public void beforeAgenticScopeDestroyed(AgenticScope agenticScope) {
    }

    @Override
    public boolean inheritedBySubagents() {
        return true;
    }

    // ==================== 辅助方法 ====================

    private String generateTraceId(AgentRequest request, String memoryId) {
        return request.agentId() + "_" + memoryId;
    }

    private String generateTraceId(AgentResponse response, String memoryId) {
        return response.agentId() + "_" + memoryId;
    }

    private String generateTraceId(AgentInvocationError error, String memoryId) {
        return error.agentId() + "_" + memoryId;
    }

    private String generateTraceId(BeforeAgentToolExecution beforeAgentToolExecution) {
        String memoryId = String.valueOf(beforeAgentToolExecution.toolExecution().invocationContext().chatMemoryId());
        return memoryIdToTraceIdMap.getOrDefault(memoryId, "unknown_" + System.currentTimeMillis());
    }

    private String generateTraceId(AfterAgentToolExecution afterAgentToolExecution) {
        // 从 memoryId 获取对应的 traceId，确保与 Agent 调用事件使用相同的 traceId
        String memoryId = String.valueOf(afterAgentToolExecution.toolExecution().invocationContext().chatMemoryId());
        return memoryIdToTraceIdMap.getOrDefault(memoryId, "unknown_" + System.currentTimeMillis());
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

    /**
     * 截断输出内容
     *
     * @param output 输出内容
     * @return 截断后的字符串
     */
    private String truncateOutput(Object output) {
        if (output == null) return null;
        String str = output.toString();
        return str.length() > maxOutputLength ? str.substring(0, maxOutputLength) + "..." : str;
    }

    /**
     * 截断输入内容
     *
     * @param inputs 输入参数
     * @return 截断后的字符串
     */
    private String truncateInput(Map<String, Object> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return null;
        }
        String str = inputs.toString();
        return str.length() > maxInputLength ? str.substring(0, maxInputLength) + "..." : str;
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