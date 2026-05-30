package com.tengYii.jobspark.common.utils.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON响应清理工具类
 * <p>
 * 用于清理大模型返回的JSON响应中的Markdown标记和其他非JSON文本，确保JSON能够正确解析。
 * 处理场景包括：
 * <ul>
 *   <li>响应被 ```json 和 ``` 包裹</li>
 *   <li>响应前后包含解释性文本（如中文说明、Markdown 表格等）</li>
 *   <li>JSON 嵌入在长文本中间的任意位置</li>
 *   <li>响应中包含未转义的控制字符（换行、制表符等）</li>
 * </ul>
 * </p>
 *
 * @author tengYii
 */
public class JsonResponseCleaner {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** 匹配 ```json 或 ``` 代码块，捕获块内内容（非贪婪） */
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?[ \\t]*\\r?\\n?([\\s\\S]*?)```");

    /**
     * 从可能包含非标准格式的 LLM 响应中提取并修复 JSON。
     * <p>
     * 提取策略（按优先级依次尝试）：
     * <ol>
     *   <li>直接解析原始响应（纯 JSON）</li>
     *   <li>从 ```json ... ``` 代码块中提取（正则 + indexOf 双保险）</li>
     *   <li>通过定位最外层的 { } 或 [ ] 提取 JSON</li>
     *   <li>按行查找以 { 或 [ 开头的 JSON 结构</li>
     *   <li>从文本末尾反向查找 JSON</li>
     *   <li>逐字符剥离非 JSON 前缀后解析</li>
     *   <li>兜底：对原始文本执行控制字符修复后再解析</li>
     * </ol>
     * </p>
     */
    public static String cleanJsonResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            throw new IllegalArgumentException("Input response is null or empty");
        }

        String cleaned = rawResponse.trim();
        List<String> failures = new ArrayList<>();

        // 策略1：尝试直接解析（已经是纯 JSON）
        if (tryParseRaw(cleaned) != null) {
            return cleaned;
        }
        failures.add("S1: not pure JSON");

        // 策略2a：正则匹配 ```json ... ``` 代码块
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(cleaned);
        boolean foundBlock = false;
        while (matcher.find()) {
            foundBlock = true;
            String result = tryParseRobust(matcher.group(1).trim());
            if (result != null) {
                return result;
            }
        }
        if (foundBlock) {
            failures.add("S2a: regex found code blocks but content failed to parse");
        } else {
            failures.add("S2a: no code block matched by regex");
        }

        // 策略2b：indexOf 手动查找 ``` 标记
        String codeBlockResult = extractFromCodeBlocksManual(cleaned);
        if (codeBlockResult != null) {
            return codeBlockResult;
        }
        failures.add("S2b: manual code block extraction found nothing parseable");

        // 策略3：通过定位最外层的 JSON 结构提取（正向：第一个 { 或 [）
        String extracted = extractOutermostJson(cleaned);
        if (extracted != null) {
            String result = tryParseRobust(extracted);
            if (result != null) {
                return result;
            }
        }
        failures.add("S3: outermost JSON extraction failed");

        // 策略4：按行查找以 { 或 [ 开头的 JSON 结构
        String lineResult = extractFromJsonLine(cleaned);
        if (lineResult != null) {
            return lineResult;
        }
        failures.add("S4: line-based JSON extraction failed");

        // 策略5：从文本末尾反向查找 JSON
        String tailResult = extractFromTail(cleaned);
        if (tailResult != null) {
            return tailResult;
        }
        failures.add("S5: tail-based JSON extraction failed");

        // 策略6：逐字符剥离前缀——从每个 { 或 [ 位置尝试解析
        String stripResult = extractByStrippingPrefix(cleaned);
        if (stripResult != null) {
            return stripResult;
        }
        failures.add("S6: prefix stripping failed");

        // 策略7：最终兜底——对原始文本执行控制字符修复后再解析
        String fixedJson = fixControlChars(cleaned);
        if (tryParseRaw(fixedJson) != null) {
            return fixedJson;
        }
        failures.add("S7: fixControlChars on full text failed");

        throw new RuntimeException(
                "Failed to clean JSON after all strategies. Diagnostics: " + String.join("; ", failures));
    }

    // ==================== 解析辅助方法 ====================

    /** 尝试直接解析（不做任何修改） */
    private static String tryParseRaw(String candidate) {
        try {
            objectMapper.readTree(candidate);
            return candidate;
        } catch (Exception e) {
            return null;
        }
    }

    /** 先尝试原始解析，失败后再修复控制字符重试 */
    private static String tryParseRobust(String candidate) {
        // 先尝试原始解析（不经过 fixControlChars）
        String result = tryParseRaw(candidate);
        if (result != null) {
            return result;
        }
        // 再尝试修复控制字符后解析
        String fixed = fixControlChars(candidate);
        return tryParseRaw(fixed);
    }

    // ==================== 策略实现 ====================

    /**
     * 手动查找 ``` 标记并提取其中的 JSON 内容。
     */
    private static String extractFromCodeBlocksManual(String text) {
        int searchFrom = 0;
        while (true) {
            int fenceStart = text.indexOf("```", searchFrom);
            if (fenceStart == -1) {
                break;
            }

            // 跳过开头的 ``` 和可选的 "json" 标识符及后续空白
            int contentStart = fenceStart + 3;
            if (contentStart < text.length()) {
                String afterFence = text.substring(contentStart);
                // 跳过可选的 "json"、水平空白、一个换行
                String trimmed = afterFence.replaceFirst("^json[ \\t]*\\r?\\n?", "");
                contentStart = text.length() - trimmed.length();
            }

            // 查找闭合的 ```
            int fenceEnd = text.indexOf("```", contentStart);
            if (fenceEnd == -1) {
                fenceEnd = text.length();
            }

            String content = text.substring(contentStart, fenceEnd).trim();
            if (!content.isEmpty()) {
                String result = tryParseRobust(content);
                if (result != null) {
                    return result;
                }
            }

            searchFrom = Math.max(fenceStart + 1, fenceEnd > 0 ? fenceEnd + 3 : contentStart);
            if (searchFrom >= text.length()) {
                break;
            }
        }
        return null;
    }

    /**
     * 按行查找 JSON 结构：找到第一个以 { 或 [ 开头的行，从该位置提取。
     */
    private static String extractFromJsonLine(String text) {
        String[] lines = text.split("\\R", -1);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                int lineStart = text.indexOf(trimmed);
                if (lineStart == -1) {
                    lineStart = text.indexOf(line.trim());
                }
                if (lineStart != -1) {
                    char openBrace = trimmed.charAt(0);
                    char closeBrace = openBrace == '{' ? '}' : ']';
                    String extracted = extractBracedStructure(text, openBrace, closeBrace, lineStart);
                    if (extracted != null) {
                        String result = tryParseRobust(extracted);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 从文本末尾反向查找 JSON 结构。
     */
    private static String extractFromTail(String text) {
        int lastBrace = text.lastIndexOf('}');
        int lastBracket = text.lastIndexOf(']');

        int closePos = Math.max(lastBrace, lastBracket);
        if (closePos == -1) {
            return null;
        }

        char closeChar = (lastBrace > lastBracket) ? '}' : ']';
        char openChar = (closeChar == '}') ? '{' : '[';

        int depth = 0;
        boolean inString = false;

        for (int i = closePos; i >= 0; i--) {
            char c = text.charAt(i);

            // 反向扫描时的字符串状态追踪
            if (c == '"' && i > 0 && text.charAt(i - 1) != '\\') {
                inString = !inString;
            }

            if (!inString) {
                if (c == closeChar) {
                    depth++;
                } else if (c == openChar) {
                    depth--;
                    if (depth == 0) {
                        String extracted = text.substring(i, closePos + 1);
                        String result = tryParseRobust(extracted);
                        if (result != null) {
                            return result;
                        }
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 逐字符剥离前缀：从每个 { 或 [ 的位置开始尝试解析为 JSON。
     * 这是兜底策略，能处理几乎任何"JSON 嵌在文本中"的场景。
     */
    private static String extractByStrippingPrefix(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{' || c == '[') {
                // 尝试从这里解析剩余部分
                String candidate = text.substring(i);
                String result = tryParseRobust(candidate);
                if (result != null) {
                    return result;
                }
                // 如果全文解析失败，尝试只提取括号匹配的部分
                char closeChar = (c == '{') ? '}' : ']';
                String extracted = extractBracedStructure(text, c, closeChar, i);
                if (extracted != null) {
                    result = tryParseRobust(extracted);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 从文本中提取最外层的 JSON 对象或数组。
     */
    private static String extractOutermostJson(String text) {
        int objStart = text.indexOf('{');
        int arrStart = text.indexOf('[');

        if (objStart == -1 && arrStart == -1) {
            return null;
        }

        if (objStart == -1) {
            return extractBracedStructure(text, '[', ']', arrStart);
        }
        if (arrStart == -1) {
            return extractBracedStructure(text, '{', '}', objStart);
        }

        if (arrStart < objStart) {
            String arrResult = extractBracedStructure(text, '[', ']', arrStart);
            if (arrResult != null) return arrResult;
            return extractBracedStructure(text, '{', '}', objStart);
        } else {
            String objResult = extractBracedStructure(text, '{', '}', objStart);
            if (objResult != null) return objResult;
            return extractBracedStructure(text, '[', ']', arrStart);
        }
    }

    private static String extractBracedStructure(String text, char openBrace, char closeBrace) {
        int start = text.indexOf(openBrace);
        if (start == -1) return null;
        return extractBracedStructure(text, openBrace, closeBrace, start);
    }

    /**
     * 通过括号匹配从文本的指定位置开始提取最外层的括号结构。
     */
    private static String extractBracedStructure(String text, char openBrace, char closeBrace, int start) {
        if (start < 0 || start >= text.length()) {
            return null;
        }

        int depth = 0;
        boolean inString = false;
        char prevChar = 0;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '"' && prevChar != '\\') {
                inString = !inString;
            }

            if (!inString) {
                if (c == openBrace) {
                    depth++;
                } else if (c == closeBrace) {
                    depth--;
                    if (depth == 0) {
                        return text.substring(start, i + 1);
                    }
                }
            }
            prevChar = c;
        }

        // 括号未闭合——回退：取第一个 openBrace 和最后一个 closeBrace
        int end = text.lastIndexOf(closeBrace);
        if (end > start) {
            return text.substring(start, end + 1);
        }

        return null;
    }

    /**
     * 状态机：修复 JSON 字符串内的未转义控制字符。
     * <p>
     * 在 JSON 字符串值内部，将实际的 \n、\r、\t 字符替换为对应的转义序列，
     * 同时去除 JSON 结构外（字符串外）的换行符。
     * </p>
     */
    private static String fixControlChars(String input) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        char prevChar = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
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
                // 忽略 JSON 结构外的换行符
            } else {
                result.append(c);
            }
            prevChar = c;
        }

        return result.toString();
    }
}
