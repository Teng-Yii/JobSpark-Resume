package com.tengYii.jobspark.infrastructure.store;

import com.tengYii.jobspark.common.utils.RedisUtil;
import com.tengYii.jobspark.model.po.AgentExecutionTracePO;
import com.tengYii.jobspark.model.po.AgentToolInvocationPO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Agent可观测性Redis存储服务
 * 负责存储Agent执行轨迹的热数据
 *
 * @author Teng-Yii
 * @since 2026-04-14
 */
@Slf4j
@Component
public class AgentTraceStoreService {

    /**
     * Redis Key前缀
     */
    private static final String TRACE_KEY_PREFIX = "agent:trace:";
    private static final String SESSION_TRACES_KEY_PREFIX = "agent:session:";
    private static final String TOOL_INVOCATIONS_KEY_PREFIX = "agent:tools:";

    /**
     * 默认过期时间：7天
     */
    private static final long DEFAULT_EXPIRE_DAYS = 7;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private RedisUtil redisUtil;

    /**
     * 保存Agent执行开始状态
     *
     * @param trace 执行轨迹
     */
    public void saveStartTrace(AgentExecutionTracePO trace) {
        String key = buildTraceKey(trace.getTraceId());
        Map<String, Object> traceMap = convertToMap(trace);
        traceMap.put("status", "RUNNING");
        redisUtil.hmset(key, traceMap, DEFAULT_EXPIRE_DAYS, TimeUnit.DAYS);
        
        // 添加到会话关联列表
        if (trace.getSessionId() != null) {
            String sessionKey = buildSessionTracesKey(trace.getSessionId());
            redisUtil.sSetAndTime(sessionKey, DEFAULT_EXPIRE_DAYS, TimeUnit.DAYS, trace.getTraceId());
        }
        
        log.debug("保存Agent开始轨迹到Redis: traceId={}", trace.getTraceId());
    }

    /**
     * 更新Agent执行结束状态
     *
     * @param traceId    追踪ID
     * @param status     状态
     * @param endTime    结束时间
     * @param durationMs 耗时
     * @param output     输出摘要
     */
    public void updateEndTrace(String traceId, String status, LocalDateTime endTime, 
                               Long durationMs, String output) {
        String key = buildTraceKey(traceId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        if (endTime != null) {
            updates.put("endTime", endTime.format(DATE_TIME_FORMATTER));
        }
        if (durationMs != null) {
            updates.put("durationMs", String.valueOf(durationMs));
        }
        if (output != null) {
            updates.put("outputSummary", truncate(output, 500));
        }
        redisUtil.hmset(key, updates);
        log.debug("更新Agent结束状态到Redis: traceId={}, status={}", traceId, status);
    }

    /**
     * 更新Agent执行错误状态
     *
     * @param traceId        追踪ID
     * @param errorMessage   错误信息
     * @param errorStackTrace 错误堆栈
     */
    public void updateErrorTrace(String traceId, String errorMessage, String errorStackTrace) {
        String key = buildTraceKey(traceId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "FAILED");
        updates.put("errorMessage", truncate(errorMessage, 500));
        if (errorStackTrace != null) {
            updates.put("errorStackTrace", truncate(errorStackTrace, 2000));
        }
        redisUtil.hmset(key, updates);
        log.debug("更新Agent错误状态到Redis: traceId={}", traceId);
    }

    /**
     * 保存工具调用记录
     *
     * @param invocation 工具调用记录
     */
    public void saveToolInvocation(AgentToolInvocationPO invocation) {
        String key = buildToolInvocationsKey(invocation.getTraceId());
        String field = String.valueOf(invocation.getInvocationOrder());
        Map<String, String> toolMap = new HashMap<>();
        toolMap.put("toolName", invocation.getToolName());
        toolMap.put("toolInput", truncate(invocation.getToolInput(), 500));
        toolMap.put("toolOutput", truncate(invocation.getToolOutput(), 500));
        toolMap.put("success", String.valueOf(invocation.getSuccess()));
        if (invocation.getExecutionTimeMs() != null) {
            toolMap.put("executionTimeMs", String.valueOf(invocation.getExecutionTimeMs()));
        }
        redisUtil.hset(key, field, toolMap.toString(), DEFAULT_EXPIRE_DAYS, TimeUnit.DAYS);
        log.debug("保存工具调用记录到Redis: traceId={}, toolName={}", 
                invocation.getTraceId(), invocation.getToolName());
    }

    /**
     * 获取Agent执行轨迹
     *
     * @param traceId 追踪ID
     * @return 执行轨迹Map
     */
    public Map<Object, Object> getTrace(String traceId) {
        String key = buildTraceKey(traceId);
        return redisUtil.hmget(key);
    }

    /**
     * 获取会话关联的所有traceId
     *
     * @param sessionId 会话ID
     * @return traceId集合
     */
    public java.util.Set<Object> getSessionTraceIds(String sessionId) {
        String key = buildSessionTracesKey(sessionId);
        return redisUtil.sGet(key);
    }

    /**
     * 删除执行轨迹
     *
     * @param traceId 追踪ID
     */
    public void deleteTrace(String traceId) {
        String key = buildTraceKey(traceId);
        redisUtil.del(key);
    }

    private String buildTraceKey(String traceId) {
        return TRACE_KEY_PREFIX + traceId;
    }

    private String buildSessionTracesKey(String sessionId) {
        return SESSION_TRACES_KEY_PREFIX + sessionId + ":traces";
    }

    private String buildToolInvocationsKey(String traceId) {
        return TOOL_INVOCATIONS_KEY_PREFIX + traceId;
    }

    private Map<String, Object> convertToMap(AgentExecutionTracePO trace) {
        Map<String, Object> map = new HashMap<>();
        map.put("traceId", trace.getTraceId());
        if (trace.getSessionId() != null) {
            map.put("sessionId", trace.getSessionId());
        }
        if (trace.getMemoryId() != null) {
            map.put("memoryId", trace.getMemoryId());
        }
        map.put("agentName", trace.getAgentName());
        map.put("agentId", trace.getAgentId());
        if (trace.getParentAgentId() != null) {
            map.put("parentAgentId", trace.getParentAgentId());
        }
        if (trace.getStartTime() != null) {
            map.put("startTime", trace.getStartTime().format(DATE_TIME_FORMATTER));
        }
        if (trace.getInputSummary() != null) {
            map.put("inputSummary", truncate(trace.getInputSummary(), 500));
        }
        return map;
    }

    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
}
