package com.tengYii.jobspark.config.listener;

import com.tengYii.jobspark.common.enums.AgentTypeEnum;
import dev.langchain4j.agentic.observability.AgentListener;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent监听器工厂
 * 负责为不同类型的Agent创建对应的监听器实例
 * <p>
 * 支持特性：
 * 1. 按Agent类型创建专用监听器
 * 2. 支持全局启用/禁用监听
 * 3. 支持按Agent类型细粒度控制
 * 4. 监听器实例复用（每个类型一个实例）
 *
 * @author Teng-Yii
 * @since 2026-04-15
 */
@Slf4j
@Component
public class AgentListenerFactory {

    @Resource
    private ApplicationEventPublisher eventPublisher;

    /**
     * 全局是否启用Agent监听
     */
    @Value("${agent.observability.enabled:true}")
    private boolean globalEnabled;

    /**
     * 监听器实例缓存，每个Agent类型一个实例
     */
    private final Map<AgentTypeEnum, PersistableAgentListener> listenerCache = new ConcurrentHashMap<>();

    /**
     * 空监听器（当监听禁用时返回）
     */
    private volatile NoOpAgentListener noOpListener;

    /**
     * 获取指定Agent类型的监听器
     *
     * @param agentType Agent类型
     * @return 对应的Agent监听器
     */
    public AgentListener getListener(AgentTypeEnum agentType) {
        if (!globalEnabled) {
            log.info("Agent监听已全局禁用，返回空监听器");
            return getNoOpListener();
        }

        if (agentType == null || !agentType.isPersistenceEnabled()) {
            log.info("Agent类型为空或持久化已禁用，返回空监听器");
            return getNoOpListener();
        }

        return listenerCache.computeIfAbsent(agentType, this::createListener);
    }

    /**
     * 根据Agent类获取对应的监听器
     *
     * @param agentClass Agent接口类
     * @return 对应的Agent监听器
     */
    public AgentListener getListener(Class<?> agentClass) {
        AgentTypeEnum agentType = AgentTypeEnum.fromAgentClass(agentClass);
        return getListener(agentType);
    }

    /**
     * 创建指定类型的监听器
     *
     * @param agentType Agent类型
     * @return 新创建的监听器实例
     */
    private PersistableAgentListener createListener(AgentTypeEnum agentType) {
        log.info("创建Agent监听器: agentType={}, displayName={}", 
                agentType.name(), agentType.getDisplayName());
        
        PersistableAgentListener listener = new PersistableAgentListener(eventPublisher, agentType);
        
        // 根据Agent类型配置不同的监听策略
        configureListener(listener, agentType);
        
        return listener;
    }

    /**
     * 根据Agent类型配置监听器特性
     *
     * @param listener  监听器实例
     * @param agentType Agent类型
     */
    private void configureListener(PersistableAgentListener listener, AgentTypeEnum agentType) {
        // 配置详细日志
        listener.setDetailedLogging(agentType.isDetailedLogging());
        
        // 可以在这里添加更多配置：
        // - 采样率配置
        // - 特定字段脱敏规则
        // - 告警阈值等
        
        switch (agentType) {
            case JD_ALIGNMENT -> {
                // JD对齐Agent：记录完整的输入输出用于调试
                listener.setMaxInputLength(2000);
                listener.setMaxOutputLength(2000);
            }
            case INTERVIEW_COORDINATOR -> {
                // 面试协调Agent：中等详细程度
                listener.setMaxInputLength(1500);
                listener.setMaxOutputLength(1500);
            }
            case JAVA_TECH_INTERVIEWER -> {
                // 技术面试官Agent：关注问题生成质量
                listener.setMaxInputLength(1000);
                listener.setMaxOutputLength(2000);
            }
            case INTERVIEW_REFLECTOR -> {
                // 反思Agent：关注评分和决策
                listener.setMaxInputLength(1000);
                listener.setMaxOutputLength(1000);
            }
            default -> {
                // 默认配置
                listener.setMaxInputLength(500);
                listener.setMaxOutputLength(500);
            }
        }
    }

    /**
     * 获取空监听器实例
     *
     * @return 空监听器
     */
    private synchronized NoOpAgentListener getNoOpListener() {
        if (noOpListener == null) {
            noOpListener = new NoOpAgentListener();
        }
        return noOpListener;
    }

    /**
     * 清空监听器缓存（用于动态刷新配置）
     */
    public void clearCache() {
        log.info("清空Agent监听器缓存");
        listenerCache.clear();
    }

    /**
     * 获取当前缓存的监听器数量
     *
     * @return 缓存的监听器数量
     */
    public int getCacheSize() {
        return listenerCache.size();
    }
    
    /**
     * 设置会话ID到所有监听器，用于Trace ID关联
     *
     * @param memoryId  Memory ID
     * @param sessionId 会话ID
     */
    public void setSessionId(String memoryId, String sessionId) {
        listenerCache.values().forEach(listener -> listener.setSessionId(memoryId, sessionId));
    }
}
