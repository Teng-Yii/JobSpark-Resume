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

            log.info("对LLM响应进行JSON格式化，LLM响应原文本: {}", aiMessageText);
            String cleanJsonResponse = JsonResponseCleaner.cleanJsonResponse(aiMessageText);
            return successWith(cleanJsonResponse);
        } catch (Exception e) {
            log.error("JSON清理失败: {}", e.getMessage(), e);
            // 返回失败结果而不是抛出异常
            return failure("JSON清理失败: %s".formatted(e.getMessage()), e);
        }
    }
}
