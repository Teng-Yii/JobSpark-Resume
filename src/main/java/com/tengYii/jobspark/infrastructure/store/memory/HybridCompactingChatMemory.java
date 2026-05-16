package com.tengYii.jobspark.infrastructure.store.memory;

import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 混合压缩聊天记忆（Hybrid Compaction with Importance Scoring）
 * <p>
 * 核心思想：引入消息重要性评分机制，对高重要性消息保留原文，
 * 对低重要性消息进行批量压缩，同时维护一个轻量级的关键决策索引。
 * <p>
 * 压缩策略：
 * <ul>
 *   <li>HIGH importance → 保留原文（Pinned Messages）</li>
 *   <li>LOW importance → 批量摘要压缩</li>
 *   <li>MEDIUM → 保留最近的，旧的参与压缩</li>
 *   <li>最近 {@code recentMessageCount} 条消息始终保留</li>
 * </ul>
 * <p>
 * 与 LangChain4j 现有 {@link ChatMemory} 接口完全兼容，
 * 可无缝替换 {@code MessageWindowChatMemory}。
 *
 * @see ImportanceScorer
 * @see DecisionIndex
 * @see MessageImportance
 */
@Slf4j
public class HybridCompactingChatMemory implements ChatMemory {

    /**
     * 记忆ID标识
     */
    private final Object id;

    /**
     * 用于摘要压缩的ChatModel
     */
    private final ChatModel chatModel;

    /**
     * 底层消息持久化存储
     */
    private final ChatMemoryStore store;

    /**
     * 消息重要性评分器
     */
    private final ImportanceScorer scorer;

    /**
     * 触发压缩的最大消息数阈值
     */
    private final int compactThreshold;

    /**
     * 始终保留的最近消息条数
     */
    private final int recentMessageCount;

    /**
     * 决策索引（跨Agent共享）
     */
    private final DecisionIndex decisionIndex;

    @Builder
    public HybridCompactingChatMemory(Object id,
                                      ChatModel chatModel,
                                      ChatMemoryStore store,
                                      ImportanceScorer scorer,
                                      Integer compactThreshold,
                                      Integer recentMessageCount,
                                      DecisionIndex decisionIndex) {
        this.id = id;
        this.chatModel = chatModel;
        this.store = store;
        this.scorer = scorer != null ? scorer : new InterviewRuleBasedScorer();
        this.compactThreshold = compactThreshold != null ? compactThreshold : 30;
        this.recentMessageCount = recentMessageCount != null ? recentMessageCount : 6;
        this.decisionIndex = decisionIndex != null ? decisionIndex : new DecisionIndex();
    }

    @Override
    public Object id() {
        return id;
    }

    /**
     * 添加消息到记忆中
     * <p>
     * 流程：
     * 1. 追加消息到存储
     * 2. 对消息评分，高重要性消息记录到决策索引
     * 3. 检查是否超过压缩阈值，若超过则触发压缩
     *
     * @param message 新的聊天消息
     */
    @Override
    public void add(ChatMessage message) {
        List<ChatMessage> messages = new LinkedList<>(store.getMessages(id));
        messages.add(message);

        // 对新消息评分并记录到决策索引
        MessageImportance importance = scorer.score(message, messages);
        if (importance == MessageImportance.HIGH) {
            String summary = extractDecisionSummary(message);
            decisionIndex.record(id, messages.size() - 1, summary);
            log.debug("高重要性消息记录到决策索引: memoryId={}, summary={}", id, summary);
        }

        // 检查是否需要压缩
        if (messages.size() > compactThreshold) {
            log.info("触发记忆压缩: memoryId={}, 当前消息数={}, 阈值={}",
                    id, messages.size(), compactThreshold);
            messages = compact(messages);
        }

        store.updateMessages(id, messages);
    }

    /**
     * 获取当前记忆中的所有消息
     *
     * @return 消息列表
     */
    @Override
    public List<ChatMessage> messages() {
        return new ArrayList<>(store.getMessages(id));
    }

    /**
     * 清除记忆
     */
    @Override
    public void clear() {
        store.deleteMessages(id);
        decisionIndex.clear(id);
    }

    /**
     * 执行混合压缩
     * <p>
     * 压缩逻辑：
     * 1. 将消息分为三类：pinned（高重要性）、compressible（可压缩）、recent（最近N条）
     * 2. 对可压缩消息调用LLM生成摘要
     * 3. 重组消息列表：[压缩摘要] + [pinned原文] + [最近消息]
     *
     * @param messages 原始消息列表
     * @return 压缩后的消息列表
     */
    private List<ChatMessage> compact(List<ChatMessage> messages) {
        List<ChatMessage> pinned = new ArrayList<>();
        List<ChatMessage> compressible = new ArrayList<>();
        List<ChatMessage> recent = new ArrayList<>();

        int recentBoundary = messages.size() - recentMessageCount;

        for (int i = 0; i < messages.size(); i++) {
            ChatMessage msg = messages.get(i);
            if (i >= recentBoundary) {
                // 最近N条始终保留
                recent.add(msg);
            } else if (MessageImportance.HIGH.equals(scorer.score(msg, messages))) {
                // 高重要性消息 pin 住
                pinned.add(msg);
            } else {
                // 低/中重要性消息参与压缩
                compressible.add(msg);
            }
        }

        // 对可压缩消息生成摘要
        String summary = "";
        if (!compressible.isEmpty()) {
            summary = generateCompressedSummary(compressible);
        }

        // 重组消息列表
        List<ChatMessage> result = new ArrayList<>();
        if (!summary.isEmpty()) {
            result.add(SystemMessage.from("[Compressed History]\n" + summary));
        }
        result.addAll(pinned);   // 高重要性原文保留
        result.addAll(recent);   // 最近消息保留

        log.info("压缩完成: memoryId={}, 压缩前={}, 压缩后={}, pinned={}, compressed={}",
                id, messages.size(), result.size(), pinned.size(), compressible.size());

        return result;
    }

    /**
     * 使用LLM生成可压缩消息的摘要
     *
     * @param compressible 待压缩的消息列表
     * @return 生成的摘要文本
     */
    private String generateCompressedSummary(List<ChatMessage> compressible) {
        String conversation = compressible.stream()
                .map(this::formatMessageForSummary)
                .collect(Collectors.joining("\n"));

        String prompt = """
                请将以下对话历史压缩为简洁的摘要，保留所有关键事实信息和决策要点。
                要求：
                1. 保留技术问答的核心信息
                2. 保留面试评估相关的关键结论
                3. 使用中文bullet points格式
                4. 长度不超过原文的30%%
                
                对话内容：
                %s
                """.formatted(conversation);

        try {
            return chatModel.chat(prompt);
        } catch (Exception e) {
            log.error("LLM摘要生成失败，回退为截断策略: memoryId={}", id, e);
            // 降级策略：简单截取前几条的文本
            return compressible.stream()
                    .limit(3)
                    .map(this::formatMessageForSummary)
                    .collect(Collectors.joining("\n", "[Truncated] ", ""));
        }
    }

    /**
     * 从消息中提取决策摘要
     *
     * @param message 消息
     * @return 决策摘要字符串
     */
    private String extractDecisionSummary(ChatMessage message) {
        String text = extractText(message);
        if (text == null || text.isEmpty()) {
            return message.type().name() + " message";
        }
        // 截取前100字符作为摘要
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }

    /**
     * 格式化消息用于摘要生成
     */
    private String formatMessageForSummary(ChatMessage message) {
        String type = message.type().name();
        String text = extractText(message);
        if (text != null && text.length() > 300) {
            text = text.substring(0, 300) + "...";
        }
        return type + ": " + (text != null ? text : "[empty]");
    }

    /**
     * 提取消息文本内容
     *
     * @param message 聊天消息
     * @return 文本内容
     */
    private String extractText(ChatMessage message) {
        if (message instanceof AiMessage aiMessage) {
            return aiMessage.text();
        }
        if (message instanceof UserMessage userMessage) {
            return userMessage.singleText();
        }
        if (message instanceof SystemMessage systemMessage) {
            return systemMessage.text();
        }
        if (message instanceof ToolExecutionResultMessage toolResult) {
            return toolResult.text();
        }
        return null;
    }

    /**
     * 获取关联的决策索引
     *
     * @return DecisionIndex实例
     */
    public DecisionIndex getDecisionIndex() {
        return decisionIndex;
    }
}
