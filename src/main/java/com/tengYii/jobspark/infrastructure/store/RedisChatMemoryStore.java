package com.tengYii.jobspark.infrastructure.store;

import com.tengYii.jobspark.common.utils.RedisUtil;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static dev.langchain4j.data.message.ChatMessageDeserializer.messagesFromJson;
import static dev.langchain4j.data.message.ChatMessageSerializer.messagesToJson;

/**
 * 基于Redis的ChatMemory持久化存储实现类。
 * <p>
 * 该类实现了LangChain4j的ChatMemoryStore接口，通过Redis作为中间件
 * 存储会话记忆，支持多用户、多会话的记忆持久化。
 * <p>
 * 存储结构：
 * <ul>
 *   <li>Key格式：chat_memory:{memoryId}</li>
 *   <li>Value格式：JSON序列化的聊天消息列表</li>
 *   <li>过期时间：24小时（可配置）</li>
 * </ul>
 *
 * @see ChatMemoryStore
 */
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final String KEY_PREFIX = "chat_memory:";

    private static final long DEFAULT_EXPIRE_HOURS = 24;

    @Resource
    private RedisUtil redisUtil;

    /**
     * 获取指定memoryId对应的聊天消息列表。
     *
     * @param memoryId 会话标识符
     * @return 聊天消息列表，如果不存在则返回空列表
     */
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = buildKey(memoryId);
        Object value = redisUtil.get(key);
        if (value == null) {
            return List.of();
        }
        String json = value.toString();
        return messagesFromJson(json);
    }

    /**
     * 更新指定memoryId对应的聊天消息列表。
     *
     * @param memoryId 会话标识符
     * @param messages 聊天消息列表
     */
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = buildKey(memoryId);
        String json = messagesToJson(messages);
        redisUtil.set(key, json, DEFAULT_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    /**
     * 删除指定memoryId对应的聊天消息。
     *
     * @param memoryId 会话标识符
     */
    @Override
    public void deleteMessages(Object memoryId) {
        String key = buildKey(memoryId);
        redisUtil.del(key);
    }

    /**
     * 构建Redis键名。
     *
     * @param memoryId 会话标识符
     * @return 格式化的Redis键名
     */
    private String buildKey(Object memoryId) {
        return KEY_PREFIX + memoryId.toString();
    }
}