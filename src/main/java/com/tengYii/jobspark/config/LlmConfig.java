package com.tengYii.jobspark.config;

import dev.langchain4j.http.client.spring.restclient.SpringRestClientBuilder;
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

    private SpringRestClientBuilder dashscopeHttpClientBuilder() {
        return new SpringRestClientBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30));
    }

    private SpringRestClientBuilder deepseekHttpClientBuilder() {
        return new SpringRestClientBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(150));
    }

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
                // 超时时间设置为150秒（默认60秒），防止大简历解析/优化超时
                .timeout(Duration.ofSeconds(150))
//                .logRequests(true)
//                .logResponses(true)
                // 结构化输出
                .strictJsonSchema(true)
                .httpClientBuilder(deepseekHttpClientBuilder())
                .maxRetries(3)
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
                .timeout(Duration.ofSeconds(30))
                .httpClientBuilder(dashscopeHttpClientBuilder())
                .maxRetries(3)
                .build();
    }

    @Bean
    public ChatModel hyDEModel() {
        return OpenAiChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .modelName("qwen-flash")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                // 结构化输出
                .strictJsonSchema(true)
                .httpClientBuilder(deepseekHttpClientBuilder())
                .maxRetries(3)
                .build();
    }
}