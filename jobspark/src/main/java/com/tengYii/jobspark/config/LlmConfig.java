package com.tengYii.jobspark.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import org.springframework.context.annotation.Configuration;

/**
 * LLM 配置类
 * 用于配置和注册大语言模型相关的 Bean
 */
@Configuration
public class LlmConfig {

    /**
     * 注册 ChatModel Bean
     *
     * @return ChatModel
     */
    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
//                .modelName("qwen-flash")
                .modelName("qwen3-vl-plus")
//                .modelName("qwen-plus")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                // 超时时间设置为80秒（默认60秒），防止大简历解析超时
                .timeout(Duration.ofSeconds(80))
//                .logRequests(true)
//                .logResponses(true)
                // 结构化输出
                .strictJsonSchema(true)
                .build();
    }

    /**
     * 注册 EmbeddingModel Bean
     *
     * @return EmbeddingModel
     */
    @Bean
    public OpenAiEmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .modelName("text-embedding-v4")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .build();
    }
}