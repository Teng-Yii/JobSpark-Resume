package com.tengYii.jobspark.utils;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class ChatModelProvider {
    public static ChatModel createChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .modelName("qwen-flash")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
//                .logRequests(true)
//                .logResponses(true)
                .build();
    }
}
