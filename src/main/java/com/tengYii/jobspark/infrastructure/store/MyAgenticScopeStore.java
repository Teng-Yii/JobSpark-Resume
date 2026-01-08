package com.tengYii.jobspark.infrastructure.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import com.tengYii.jobspark.common.utils.RedisUtil;
import dev.langchain4j.agentic.scope.*;
import dev.langchain4j.internal.Json;
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

        log.info("保存或更新AgenticScope，key:{}", key);
        try {
            String scopeData = AgenticScopeSerializer.toJson(agenticScope);
//            String redisKey = buildRedisKey(key.agentId(), key.memoryId());
            String redisKey = Json.toJson(key);

            // 保存数据
            boolean isSet = redisUtil.set(redisKey, scopeData);

            // 更新索引
            if (isSet) {
                // 将key序列化后保存到索引集合中，方便getAllKeys使用
                redisUtil.sSet(REDIS_INDEX_KEY, redisKey);
            }
            return isSet;
        } catch (Exception e) {
            log.error("保存或更新AgenticScope失败", e);
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

//        String redisKey = buildRedisKey(key.agentId(), key.memoryId());
        String redisKey = Json.toJson(key);
        Object value = redisUtil.get(redisKey);
        if (Objects.isNull(value) || !(value instanceof String valueStr)) {
            return Optional.empty();
        }

        try {
            DefaultAgenticScope agenticScope = AgenticScopeSerializer.fromJson(valueStr);
            return Optional.ofNullable(agenticScope);
        } catch (Exception e) {
            log.error("加载AgenticScope失败", e);
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

        // 删除数据
//        String redisKey = buildRedisKey(key.agentId(), key.memoryId());
        String redisKey = Json.toJson(key);
        redisUtil.del(redisKey);
        // 删除索引
        try {
            redisUtil.setRemove(REDIS_INDEX_KEY, redisKey);
        } catch (Exception e) {
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
                        return Json.fromJson(obj.toString(), AgenticScopeKey.class);
                    } catch (Exception e) {
                        log.error("Failed to deserialize AgenticScopeKey from index", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public String buildRedisKey(String agentId, Object memoryId) {
        if (memoryId instanceof String) {
            return REDIS_KEY_PREFIX + agentId + ":" + memoryId;
        } else {
            log.info("memoryId 不是 String 类型，需重新toString()方法");
            return REDIS_KEY_PREFIX + agentId + ":" + memoryId.toString();
        }
    }

    /**
     * 编程方式设置为自定义持久化层
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        AgenticScopePersister.setStore(this);
    }
}
