package com.tengYii.jobspark.infrastructure.store.memory;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * 消息重要性评分器接口
 * <p>
 * 用于对聊天消息进行重要性评分，支持混合压缩策略中的分级决策。
 * 实现可以基于规则（零成本）或结合LLM辅助评分（更精准）。
 * <p>
 * 设计原则：
 * <ul>
 *   <li>接口简洁，单一职责</li>
 *   <li>支持上下文感知评分（可查看消息列表中的上下文）</li>
 *   <li>实现可插拔，适配不同业务场景</li>
 * </ul>
 *
 * @see MessageImportance
 * @see InterviewRuleBasedScorer
 */
public interface ImportanceScorer {

    /**
     * 对指定消息进行重要性评分
     *
     * @param message 待评分的消息
     * @param context 消息所在的上下文列表（可用于上下文感知评分）
     * @return 消息的重要性等级
     */
    MessageImportance score(ChatMessage message, List<ChatMessage> context);
}
