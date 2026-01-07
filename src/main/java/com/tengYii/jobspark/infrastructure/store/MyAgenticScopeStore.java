package com.tengYii.jobspark.infrastructure.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tengYii.jobspark.common.utils.RedisUtil;
import dev.langchain4j.agentic.scope.AgenticScopeKey;
import dev.langchain4j.agentic.scope.AgenticScopePersister;
import dev.langchain4j.agentic.scope.AgenticScopeStore;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 将AgentScope持久化到Redis中
 */
@Slf4j
@Component
public class MyAgenticScopeStore implements AgenticScopeStore, InitializingBean {

    private static final String REDIS_KEY_PREFIX = "agentic:scope:";
    private static final String REDIS_INDEX_KEY = "agentic:scope:index";

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 保存或更新 AgenticScope
     *
     * @param key          AgenticScopeKey
     * @param agenticScope DefaultAgenticScope
     * @return 是否成功
     */
    @Override
    public boolean save(AgenticScopeKey key, DefaultAgenticScope agenticScope) {
        if (Objects.isNull(key) || Objects.isNull(agenticScope)) {
            return false;
        }

        String agentId = key.agentId();
        String memoryId = (String) key.memoryId();

        if (StringUtils.isEmpty(agentId) || StringUtils.isEmpty(memoryId)) {
            log.warn("AgentId or MemoryId is empty, save skipped.");
            return false;
        }

        try {
            String scopeData = objectMapper.writeValueAsString(agenticScope);
            String redisKey = REDIS_KEY_PREFIX + key;

            // 保存数据
            boolean isSet = redisUtil.set(redisKey, scopeData);

            // 更新索引
            if (isSet) {
                // 将key序列化后保存到索引集合中，方便getAllKeys使用
                String keyJson = objectMapper.writeValueAsString(key);
                redisUtil.sSet(REDIS_INDEX_KEY, keyJson);
            }

            return isSet;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize DefaultAgenticScope or AgenticScopeKey", e);
            return false;
        }
    }

    /**
     * 加载 AgenticScope
     *
     * @param key AgenticScopeKey
     * @return Optional<DefaultAgenticScope>
     */
    @Override
    public Optional<DefaultAgenticScope> load(AgenticScopeKey key) {
        if (Objects.isNull(key)) {
            return Optional.empty();
        }

        String agentId = key.agentId();
        String memoryId = (String) key.memoryId();

        if (StringUtils.isEmpty(agentId) || StringUtils.isEmpty(memoryId)) {
            return Optional.empty();
        }

        String redisKey = REDIS_KEY_PREFIX + key;
        Object value = redisUtil.get(redisKey);

        if (Objects.isNull(value) || !(value instanceof String jsonStr)) {
            return Optional.empty();
        }

        if (StringUtils.isEmpty(jsonStr)) {
            return Optional.empty();
        }

        try {
            DefaultAgenticScope scope = objectMapper.readValue(jsonStr, DefaultAgenticScope.class);
            return Optional.ofNullable(scope);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize DefaultAgenticScope", e);
            return Optional.empty();
        }
    }

    /**
     * 删除 AgenticScope
     *
     * @param key AgenticScopeKey
     * @return 是否成功
     */
    @Override
    public boolean delete(AgenticScopeKey key) {
        if (Objects.isNull(key)) {
            return false;
        }

        String agentId = key.agentId();
        String memoryId = (String) key.memoryId();

        if (StringUtils.isEmpty(agentId) || StringUtils.isEmpty(memoryId)) {
            return false;
        }

        String redisKey = REDIS_KEY_PREFIX + key;

        // 删除数据
        redisUtil.del(redisKey);
        // 删除索引
        try {
            String keyJson = objectMapper.writeValueAsString(key);
            redisUtil.setRemove(REDIS_INDEX_KEY, keyJson);
        } catch (JsonProcessingException e) {
            // 索引删除失败，记录错误日志
            log.error("Failed to serialize AgenticScopeKey for deletion from index", e);
        }
        return true;
    }

    /**
     * 获取所有 Key
     *
     * @return Set<AgenticScopeKey>
     */
    @Override
    public Set<AgenticScopeKey> getAllKeys() {
        Set<Object> keys = redisUtil.sGet(REDIS_INDEX_KEY);

        if (CollectionUtils.isEmpty(keys)) {
            return Collections.emptySet();
        }

        return keys.stream()
                .filter(Objects::nonNull)
                .map(obj -> {
                    try {
                        return objectMapper.readValue(obj.toString(), AgenticScopeKey.class);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize AgenticScopeKey from index", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 编程方式设置为自定义持久化层
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        AgenticScopePersister.setStore(this);
    }
}
