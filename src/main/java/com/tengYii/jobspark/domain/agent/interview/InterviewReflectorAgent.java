package com.tengYii.jobspark.domain.agent.interview;

import com.tengYii.jobspark.domain.agent.cv.JsonResponseCleanGuard;
import com.tengYii.jobspark.model.bo.interview.ReflectionResultBO;
import com.tengYii.jobspark.model.bo.interview.ResumeContextBO;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.guardrail.OutputGuardrails;

/**
 * 面试反思评估Agent：Plan-and-Execute 模式中的反思者。
 * <p>
 * 该Agent负责：
 * <ul>
 *   <li>评估候选人对技术问题的回答质量</li>
 *   <li>给出具体的评分和反馈</li>
 *   <li>根据评估结果决策下一步动作</li>
 *   <li>为后续问题提供调整建议</li>
 * </ul>
 * <p>
 * 输入：
 * <ul>
 *   <li>currentQuestion - 当前提出的问题</li>
 *   <li>userAnswer - 候选人的回答</li>
 *   <li>resumeContext - 简历上下文</li>
 * </ul>
 * <p>
 * 输出：
 * <ul>
 *   <li>reflection - 包含评分、反馈和决策的反思结果</li>
 * </ul>
 *
 * @see ReflectionResultBO
 */
public interface InterviewReflectorAgent {

    /**
     * 评估候选人的回答并生成反思结果。
     * <p>
     * 评估步骤：
     * <ol>
     *   <li>分析回答的技术准确性和完整性</li>
     *   <li>评估回答的深度和广度</li>
     *   <li>给出0-10的评分</li>
     *   <li>生成具体的改进建议</li>
     *   <li>根据评估结果做出决策</li>
     * </ol>
     *
     * @param currentQuestion 当前提出的技术问题
     * @param userAnswer 候选人的回答内容
     * @param resumeContext 简历上下文，用于评估候选人的技能匹配度
     * @return 反思结果，包含评分、反馈和下一步决策
     */
    @Agent(value = "面试反思评估Agent：评估候选人回答质量，给出评分和具体反馈，决策后续面试流程（追问/下一题/下一阶段/结束）", outputKey = "reflection")
    @OutputGuardrails(value = {JsonResponseCleanGuard.class}, maxRetries = 0)
    @SystemMessage("""
            你是技术面试评估引擎。根据评估维度和评分标准，对候选人回答进行评分和决策。

            ## 评估维度
            1. 技术准确性：回答是否正确、完整、无误导
            2. 深度理解：对原理的理解是否深入，能否举一反三
            3. 实践经验：是否有实际项目经验作为支撑
            4. 表达清晰度：回答是否条理清晰、逻辑分明
            5. 问题解决能力：面对复杂问题的分析思路

            ## 评分标准 (0-10分)
            - 0-3分：回答错误或严重偏差
            - 4-5分：基本正确但缺乏深度
            - 6-7分：正确，有一定深度
            - 8-9分：优秀，有深入理解和实践经验
            - 10分：完美，超出预期

            ## 决策类型
            - PROBE：追问深入探讨
            - NEXT：进入下一问题
            - STAGE_FINISH：当前阶段完成
            - FINISH：面试结束

            ## 输出字段
            - score: 评分 (0-10整数)
            - decision: 决策 (PROBE/NEXT/STAGE_FINISH/FINISH)
            - feedback: 具体反馈和改进建议
            - probeSuggestions: 如果decision为PROBE，提供追问方向（字符串数组）
            """)
    @UserMessage("""
            评估以下候选人的回答：

            ## 当前问题
            {{currentQuestion}}

            ## 候选人回答
            {{userAnswer}}

            ## 候选人简历上下文
            {{resumeContext}}

            ## 任务
            1. 结合简历上下文评估回答质量
            2. 根据评估维度给出综合评分(0-10)
            3. 提供具体反馈（优点和不足）
            4. 做出决策 (PROBE/NEXT/STAGE_FINISH/FINISH)

            ## CRITICAL OUTPUT RULES - 必须严格遵守：
            - 只输出一个纯 JSON 对象，不要任何其他文字
            - 禁止输出 Markdown 标题、代码块、解释性文字
            - 禁止用 ```json 代码块包裹
            - 禁止在 JSON 前后添加任何前缀或后缀文本
            - 你的整个回复必须是一个可直接解析的 JSON 对象，以 { 开头，以 } 结尾
            - 字符串值中的换行使用 \\n 转义，不要输出真实的换行符
            """)
    ReflectionResultBO reflect(
            @MemoryId String memoryId,
            @V("currentQuestion") String currentQuestion,
            @V("userAnswer") String userAnswer,
            @V("resumeContext") ResumeContextBO resumeContext
    );
}
