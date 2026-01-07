package com.tengYii.jobspark.common.utils.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tengYii.jobspark.model.llm.CvReview;

/**
 * JSON响应清理工具类
 * <p>
 * 用于清理大模型返回的JSON响应中的Markdown标记，确保JSON能够正确解析。
 * 主要解决大模型返回的JSON被```json和```包裹的问题。
 * </p>
 *
 * @author tengYii
 */
public class JsonResponseCleaner {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从可能包含非标准格式（如 Markdown、前后缀、未转义换行）的 LLM 响应中提取并修复 JSON。
     *
     * @param rawResponse LLM 返回的原始字符串（可能包含 ```json ... ``` 或纯文本）
     * @return 修复后的合法 JSON 字符串
     */
    public static String cleanJsonResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            throw new IllegalArgumentException("Input response is null or empty");
        }

        // 尝试直接解析
        try {
            objectMapper.readTree(rawResponse);
            return rawResponse;
        } catch (Exception ignored) {
        }

        // 移除 Markdown 代码块（保守策略：找花括号范围）
        String cleaned = rawResponse.trim();
        if (cleaned.startsWith("```")) {
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}') + 1;
            if (start != -1 && end > start) {
                cleaned = cleaned.substring(start, end);
            } else {
                cleaned = cleaned.replaceAll("(?s)^```(?:json)?\\s*", "")
                        .replaceAll("(?s)```$", "");
            }
        }

        // 状态机：修复字符串内的控制字符
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        char prevChar = 0;

        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (c == '"' && prevChar != '\\') {
                inString = !inString;
                result.append(c);
            } else if (inString && (c == '\n' || c == '\r' || c == '\t')) {
                result.append(switch (c) {
                    case '\n' -> "\\n";
                    case '\r' -> "\\r";
                    case '\t' -> "\\t";
                    default -> String.valueOf(c);
                });
            } else if (!inString && (c == '\n' || c == '\r')) {
                // 忽略结构外换行
            } else {
                result.append(c);
            }
            prevChar = c;
        }

        String fixedJson = result.toString();
        try {
            objectMapper.readTree(fixedJson);
            return fixedJson;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to clean JSON: " + e.getMessage(), e);
        }
    }
}