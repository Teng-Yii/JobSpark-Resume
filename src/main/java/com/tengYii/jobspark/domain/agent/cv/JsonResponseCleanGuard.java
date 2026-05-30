package com.tengYii.jobspark.domain.agent.cv;

import com.tengYii.jobspark.common.utils.llm.JsonResponseCleaner;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailRequest;
import dev.langchain4j.guardrail.OutputGuardrailResult;
import lombok.extern.slf4j.Slf4j;

/**
 * 大模型输出内容会返回带有```json标记的JSON，清除标记用于解析为结构化数据
 */
@Slf4j
public class JsonResponseCleanGuard implements OutputGuardrail {

    /**
     * 验证 LLM 的响应。
     * 与 validate(AiMessage) 不同，此方法允许访问内存和增强结果（在 RAG 的情况下）。
     * 实现不得尝试写入内存或增强结果。
     * 参数：
     *
     * @param request – 参数，包括 LLM 的响应、内存和增强结果。
     */
    @Override
    public OutputGuardrailResult validate(OutputGuardrailRequest request) {
        try {
            String aiMessageText = request.responseFromLLM().aiMessage().text();

            if (aiMessageText == null || aiMessageText.trim().isEmpty()) {
                log.warn("LLM响应为空，跳过JSON清理");
                return successWith("");
            }

            // 检测是否为 Agent tool call 响应（如 activate_skill 等）
            // tool call 需要原样返回给 Agent 框架处理，不能做 JSON 清理
            if (isToolCallResponse(aiMessageText)) {
                log.info("检测到 tool call 响应，跳过JSON清理直接返回");
                return successWith(aiMessageText);
            }

            log.info("对LLM响应进行JSON格式化，LLM响应原文本: {}", aiMessageText);
            String cleanJsonResponse = JsonResponseCleaner.cleanJsonResponse(aiMessageText);
            return successWith(cleanJsonResponse);
        } catch (Exception e) {
            log.error("JSON清理失败: {}", e.getMessage(), e);
            // 返回失败结果而不是抛出异常
            return failure("JSON清理失败: %s".formatted(e.getMessage()), e);
        }
    }

    /**
     * 判断响应是否为 Agent tool call（如 activate_skill）。
     * <p>
     * tool call 响应包含工具调用标记，需要原样传递给 Agent 框架，
     * 不能走 JSON 清理逻辑。
     * </p>
     */
    private boolean isToolCallResponse(String text) {
        String trimmed = text.trim();
        return trimmed.startsWith("<tool_calls>")
                || trimmed.startsWith("<invoke")
                || trimmed.startsWith("<parameter");
    }
}
