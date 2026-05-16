package com.tengYii.jobspark.infrastructure.store.memory;

import dev.langchain4j.data.message.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 面试场景规则化重要性评分器
 * <p>
 * 基于纯规则实现，零LLM调用成本。针对面试多Agent协作场景定制评分规则：
 * <ul>
 *   <li>工具调用结果（ToolExecutionResultMessage）→ HIGH</li>
 *   <li>含工具调用请求的AI消息 → HIGH</li>
 *   <li>包含决策性关键词的用户消息 → HIGH</li>
 *   <li>较长的AI回复（>200字符）→ MEDIUM</li>
 *   <li>简短确认性回复（<30字符）→ LOW</li>
 *   <li>其余消息 → MEDIUM</li>
 * </ul>
 *
 * @see ImportanceScorer
 * @see MessageImportance
 */
@Slf4j
public class InterviewRuleBasedScorer implements ImportanceScorer {

    /**
     * 决策性关键词模式（面试场景）
     * 匹配包含面试评估、技术方案决策、阶段推进相关的关键词
     */
    private static final Pattern DECISION_PATTERN = Pattern.compile(
            "(?i).*(评分|决策|结论|推荐|通过|不通过|PROBE|NEXT|STAGE_FINISH|FINISH|" +
                    "面试计划|技能匹配|对齐结果|追问|评估|总结|" +
                    "matchScore|interviewPlan|reflection|decision).*",
            Pattern.DOTALL
    );

    /**
     * 高重要性关键词模式
     * 匹配结构化输出（JSON结果）、关键面试阶段标记
     */
    private static final Pattern HIGH_IMPORTANCE_CONTENT_PATTERN = Pattern.compile(
            "(?i).*(\\{.*\"(matchScore|stages|questionContent|score|decision)\".*}|" +
                    "JD对齐|面试计划|反思评估|阶段完成).*",
            Pattern.DOTALL
    );

    @Override
    public MessageImportance score(ChatMessage message, List<ChatMessage> context) {
        // 工具执行结果消息始终为高重要性
        if (message instanceof ToolExecutionResultMessage) {
            return MessageImportance.HIGH;
        }

        // AI消息评分
        if (message instanceof AiMessage aiMessage) {
            return scoreAiMessage(aiMessage);
        }

        // 用户消息评分
        if (message instanceof UserMessage userMessage) {
            return scoreUserMessage(userMessage);
        }

        // 系统消息始终保留
        if (message instanceof SystemMessage) {
            return MessageImportance.HIGH;
        }

        return MessageImportance.MEDIUM;
    }

    /**
     * AI消息评分规则
     */
    private MessageImportance scoreAiMessage(AiMessage aiMessage) {
        // 含工具调用请求 → 高重要性
        if (aiMessage.hasToolExecutionRequests()) {
            return MessageImportance.HIGH;
        }

        String text = aiMessage.text();
        if (text == null || text.isEmpty()) {
            return MessageImportance.LOW;
        }

        // 包含高重要性内容模式（结构化输出、关键决策）
        if (HIGH_IMPORTANCE_CONTENT_PATTERN.matcher(text).matches()) {
            return MessageImportance.HIGH;
        }

        // 较长的回复通常包含有价值的分析内容
        if (text.length() > 200) {
            return MessageImportance.MEDIUM;
        }

        // 短回复（确认性质）
        if (text.length() < 50) {
            return MessageImportance.LOW;
        }

        return MessageImportance.MEDIUM;
    }

    /**
     * 用户消息评分规则
     */
    private MessageImportance scoreUserMessage(UserMessage userMessage) {
        String text = userMessage.singleText();
        if (text == null || text.isEmpty()) {
            return MessageImportance.LOW;
        }

        // 包含决策性关键词
        if (DECISION_PATTERN.matcher(text).matches()) {
            return MessageImportance.HIGH;
        }

        // 较长的用户回答（通常是面试中的详细技术回答）
        if (text.length() > 150) {
            return MessageImportance.MEDIUM;
        }

        // 简短回复（如"好的"、"是的"、"继续"等）
        if (text.length() < 30) {
            return MessageImportance.LOW;
        }

        return MessageImportance.MEDIUM;
    }
}
