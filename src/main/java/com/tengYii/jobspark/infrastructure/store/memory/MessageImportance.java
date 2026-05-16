package com.tengYii.jobspark.infrastructure.store.memory;

/**
 * 消息重要性等级枚举
 * <p>
 * 用于混合压缩策略中对消息进行分级管理：
 * <ul>
 *   <li>HIGH - 高重要性：工具调用结果、关键决策等，保留原文不压缩</li>
 *   <li>MEDIUM - 中等重要性：较长的AI回复、一般性用户输入</li>
 *   <li>LOW - 低重要性：确认性回复、简短闲聊等，优先参与批量压缩</li>
 * </ul>
 */
public enum MessageImportance {

    /**
     * 高重要性 - 保留原文（Pinned Messages）
     */
    HIGH,

    /**
     * 中等重要性 - 保留最近的，旧的参与压缩
     */
    MEDIUM,

    /**
     * 低重要性 - 优先批量摘要压缩
     */
    LOW
}
