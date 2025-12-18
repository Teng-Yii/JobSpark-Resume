package com.tengYii.jobspark.domain.agent;

import com.tengYii.jobspark.common.utils.llm.JsonResponseCleaner;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailRequest;
import dev.langchain4j.guardrail.OutputGuardrailResult;

/**
 * 大模型输出内容会返回带有```json标记的JSON，清除标记用于解析为结构化数据
 */
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

        String aiMessageText = request.responseFromLLM().aiMessage().text();

        String cleanJsonResponse = JsonResponseCleaner.cleanJsonResponse(aiMessageText);
        return successWith(cleanJsonResponse);
//        return validate(request.responseFromLLM().aiMessage());
    }
}
