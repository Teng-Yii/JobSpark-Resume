package com.tengYii.jobspark.common.utils.llm;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.regex.Pattern;

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

    /**
     * JSON代码块开始标记的正则表达式
     * 匹配 ```json 或 ``` 开头的标记（忽略大小写）
     */
    private static final Pattern JSON_CODE_BLOCK_START = Pattern.compile("^\\s*```(?:json)?\\s*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    /**
     * JSON代码块结束标记的正则表达式
     * 匹配行末的 ``` 标记
     */
    private static final Pattern JSON_CODE_BLOCK_END = Pattern.compile("\\s*```\\s*$", Pattern.MULTILINE);

    /**
     * Markdown代码块标记的正则表达式
     * 用于检测是否包含Markdown标记
     */
    private static final Pattern MARKDOWN_CODE_BLOCK = Pattern.compile("```(?:json)?", Pattern.CASE_INSENSITIVE);

    /**
     * 私有构造函数，防止实例化工具类
     */
    private JsonResponseCleaner() {
        throw new UnsupportedOperationException("JsonResponseCleaner是工具类，不应该被实例化");
    }

    /**
     * 清理JSON响应中的Markdown代码块标记
     * <p>
     * 移除```json和```标记，保留纯净的JSON内容。
     * 支持以下格式的清理：
     * <ul>
     *   <li>```json { ... } ```</li>
     *   <li>``` { ... } ```</li>
     *   <li>前后有空白字符的情况</li>
     *   <li>多行格式</li>
     * </ul>
     * </p>
     *
     * @param jsonResponse 原始JSON响应字符串
     * @return 清理后的纯JSON字符串，如果输入为null或空则返回空字符串
     */
    public static String cleanJsonResponse(String jsonResponse) {
        // 检查输入参数
        if (StringUtils.isEmpty(jsonResponse)) {
            return StringUtils.EMPTY;
        }

        // 去除首尾空白字符
        String cleaned = StringUtils.trim(jsonResponse);

        // 如果不包含Markdown标记，直接返回
        if (!containsMarkdownCodeBlock(cleaned)) {
            return cleaned;
        }

        // 移除开始标记 ```json 或 ```
        cleaned = JSON_CODE_BLOCK_START.matcher(cleaned).replaceFirst(StringUtils.EMPTY);

        // 移除结束标记 ```
        cleaned = JSON_CODE_BLOCK_END.matcher(cleaned).replaceFirst(StringUtils.EMPTY);

        // 再次去除首尾空白字符
        return StringUtils.trim(cleaned);
    }

    /**
     * 检查字符串是否包含Markdown代码块标记
     *
     * @param content 待检查的字符串
     * @return 如果包含Markdown标记返回true，否则返回false
     */
    public static boolean containsMarkdownCodeBlock(String content) {
        if (StringUtils.isEmpty(content)) {
            return false;
        }
        return MARKDOWN_CODE_BLOCK.matcher(content).find();
    }

    /**
     * 安全的JSON响应清理方法
     * <p>
     * 在清理过程中捕获异常，确保即使出现异常也能返回可用的结果。
     * 如果清理失败，返回原始输入。
     * </p>
     *
     * @param jsonResponse 原始JSON响应字符串
     * @return 清理后的JSON字符串，清理失败时返回原始输入
     */
    public static String safeCleanJsonResponse(String jsonResponse) {
        if (Objects.isNull(jsonResponse)) {
            return StringUtils.EMPTY;
        }

        try {
            return cleanJsonResponse(jsonResponse);
        } catch (Exception e) {
            // 记录异常但不抛出，返回原始输入
            // 在实际应用中可以添加日志记录
            return jsonResponse;
        }
    }

    /**
     * 验证清理后的字符串是否为有效的JSON格式
     * <p>
     * 简单验证JSON格式的基本特征：
     * <ul>
     *   <li>以 { 开头，以 } 结尾（对象格式）</li>
     *   <li>以 [ 开头，以 ] 结尾（数组格式）</li>
     * </ul>
     * </p>
     *
     * @param jsonString 待验证的JSON字符串
     * @return 如果符合基本JSON格式返回true，否则返回false
     */
    public static boolean isValidJsonFormat(String jsonString) {
        if (StringUtils.isEmpty(jsonString)) {
            return false;
        }

        String trimmed = StringUtils.trim(jsonString);

        // 检查对象格式 { ... }
        boolean isObject = StringUtils.startsWith(trimmed, "{") && StringUtils.endsWith(trimmed, "}");

        // 检查数组格式 [ ... ]
        boolean isArray = StringUtils.startsWith(trimmed, "[") && StringUtils.endsWith(trimmed, "]");

        return isObject || isArray;
    }

    /**
     * 清理并验证JSON响应
     * <p>
     * 组合清理和验证功能，返回清理后的有效JSON字符串。
     * 如果清理后的结果不是有效的JSON格式，返回null。
     * </p>
     *
     * @param jsonResponse 原始JSON响应字符串
     * @return 清理并验证后的JSON字符串，如果无效返回null
     */
    public static String cleanAndValidateJson(String jsonResponse) {
        String cleaned = cleanJsonResponse(jsonResponse);

        if (isValidJsonFormat(cleaned)) {
            return cleaned;
        }

        return null;
    }

    /**
     * 批量清理JSON响应数组
     * <p>
     * 对多个JSON响应进行批量清理处理。
     * </p>
     *
     * @param jsonResponses JSON响应字符串数组
     * @return 清理后的JSON字符串数组，如果输入为null返回空数组
     */
    public static String[] batchCleanJsonResponses(String... jsonResponses) {
        if (Objects.isNull(jsonResponses)) {
            return new String[0];
        }

        String[] cleanedResponses = new String[jsonResponses.length];
        for (int i = 0; i < jsonResponses.length; i++) {
            cleanedResponses[i] = cleanJsonResponse(jsonResponses[i]);
        }

        return cleanedResponses;
    }
}