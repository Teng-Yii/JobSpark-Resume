package com.tengYii.jobspark.infrastructure.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.langchain4j.agentic.scope.AgenticScopeJsonCodec;
import dev.langchain4j.agentic.scope.AgentInvocation;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;
import dev.langchain4j.agentic.scope.DefaultAgenticScope.AgentMessage;
import dev.langchain4j.agentic.scope.DefaultAgenticScope.Kind;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.JacksonChatMessageJsonCodec;

import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * 自定义 AgenticScopeJsonCodec，注册 JavaTimeModule 以支持 Java 8 date/time 类型序列化。
 * 在 JacksonAgenticScopeJsonCodec 基础上补充 JSR-310 模块支持。
 */
public class Jsr310AgenticScopeJsonCodec implements AgenticScopeJsonCodec {

    private static final ObjectMapper MAPPER = buildMapper();

    private static ObjectMapper buildMapper() {
        ObjectMapper mapper = JacksonChatMessageJsonCodec.chatMessageJsonMapperBuilder()
                // 补充 AgenticScope 相关的 Mix-in（与 JacksonAgenticScopeJsonCodec 保持一致）
                .addMixIn(DefaultAgenticScope.class, AgenticScopeMixin.class)
                .addMixIn(AgentMessage.class, AgentMessageMixin.class)
                .addMixIn(AgentInvocation.class, AgentInvocationMixin.class)
                // 注册 JavaTimeModule 以支持 Java 8 date/time 类型
                .addModule(new JavaTimeModule())
                .build();
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator());
        // 默认将 LocalDate 序列化为数组 [2024, 1, 15]，改为序列化为字符串 "2024-01-15"
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Override
    public DefaultAgenticScope fromJson(String json) {
        try {
            return MAPPER.readValue(json, DefaultAgenticScope.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize AgenticScope from JSON", e);
        }
    }

    @Override
    public String toJson(DefaultAgenticScope agenticScope) {
        try {
            return MAPPER.writeValueAsString(agenticScope);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize AgenticScope to JSON", e);
        }
    }

    // ===== Mix-in 定义（与 JacksonAgenticScopeJsonCodec 中的 Mix-in 保持一致）=====

    @JsonInclude(NON_NULL)
    private static abstract class AgenticScopeMixin {
        @JsonCreator
        public AgenticScopeMixin(
                @JsonProperty("memoryId") Object memoryId,
                @JsonProperty("kind") Kind kind) {
        }
    }

    @JsonInclude(NON_NULL)
    private static abstract class AgentMessageMixin {
        @JsonCreator
        public AgentMessageMixin(
                @JsonProperty("agentName") String agentName,
                @JsonProperty("agentId") String agentId,
                @JsonProperty("message") ChatMessage message) {
        }
    }

    @JsonInclude(NON_NULL)
    private static abstract class AgentInvocationMixin {
        @JsonCreator
        public AgentInvocationMixin(
                @JsonProperty("agentType") Class<?> agentType,
                @JsonProperty("agentName") String agentName,
                @JsonProperty("agentId") String agentId,
                @JsonProperty("input") Map<String, Object> input,
                @JsonProperty("output") Object output) {
        }
    }
}