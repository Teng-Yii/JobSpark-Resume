package com.tengYii.jobspark.infrastructure.store.memory;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 决策索引 —— 轻量级关键决策快速回查
 * <p>
 * 记录面试流程中的关键决策点，支持：
 * <ul>
 *   <li>跨Agent决策共享：任意Agent可查询历史决策</li>
 *   <li>审计追溯：记录决策时间点和摘要信息</li>
 *   <li>快速回查：基于memoryId的O(1)检索</li>
 * </ul>
 * <p>
 * 存储在内存中（ConcurrentHashMap），适合单实例场景。
 * 生产环境可扩展为Redis持久化实现。
 *
 * @see HybridCompactingChatMemory
 */
@Slf4j
public class DecisionIndex {

    /**
     * 决策索引存储：memoryId → 决策条目列表
     */
    private final Map<Object, List<DecisionEntry>> index = new ConcurrentHashMap<>();

    /**
     * 记录一条关键决策
     *
     * @param memoryId     会话记忆ID
     * @param messageIndex 触发决策的消息在列表中的索引位置
     * @param summary      决策摘要描述
     */
    public void record(Object memoryId, int messageIndex, String summary) {
        index.computeIfAbsent(memoryId, k -> new ArrayList<>())
                .add(new DecisionEntry(messageIndex, summary, Instant.now()));
        log.debug("记录决策: memoryId={}, messageIndex={}, summary={}", memoryId, messageIndex, summary);
    }

    /**
     * 获取指定memoryId的所有决策记录
     *
     * @param memoryId 会话记忆ID
     * @return 决策条目列表，不存在则返回空列表
     */
    public List<DecisionEntry> getDecisions(Object memoryId) {
        return index.getOrDefault(memoryId, List.of());
    }

    /**
     * 获取最近N条决策记录
     *
     * @param memoryId 会话记忆ID
     * @param limit    最大返回条数
     * @return 最近的决策条目列表
     */
    public List<DecisionEntry> getRecentDecisions(Object memoryId, int limit) {
        List<DecisionEntry> all = getDecisions(memoryId);
        if (all.size() <= limit) {
            return all;
        }
        return all.subList(all.size() - limit, all.size());
    }

    /**
     * 格式化决策上下文，用于注入到系统消息中
     *
     * @param memoryId 会话记忆ID
     * @param limit    最大返回条数
     * @return 格式化的决策上下文字符串，无决策时返回空字符串
     */
    public String formatDecisionContext(Object memoryId, int limit) {
        List<DecisionEntry> decisions = getRecentDecisions(memoryId, limit);
        if (decisions.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("[Historical Key Decisions]\n");
        for (int i = 0; i < decisions.size(); i++) {
            DecisionEntry entry = decisions.get(i);
            sb.append(String.format("- [%d] %s\n", i + 1, entry.summary()));
        }
        return sb.toString();
    }

    /**
     * 清除指定memoryId的决策记录
     *
     * @param memoryId 会话记忆ID
     */
    public void clear(Object memoryId) {
        index.remove(memoryId);
    }

    /**
     * 决策条目记录
     *
     * @param messageIndex 触发决策的消息索引
     * @param summary      决策摘要
     * @param timestamp    记录时间
     */
    public record DecisionEntry(int messageIndex, String summary, Instant timestamp) {
    }
}
