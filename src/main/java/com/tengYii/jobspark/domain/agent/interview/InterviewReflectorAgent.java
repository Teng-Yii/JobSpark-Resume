package com.tengYii.jobspark.domain.agent.interview;

import com.tengYii.jobspark.model.bo.interview.ReflectionResultBO;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

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
    @SystemMessage("""
            你是一位资深的技术面试评估专家，拥有10年以上技术面试经验。

            ## 评估维度
            1. **技术准确性**：回答是否正确、完整、无误导
            2. **深度理解**：对原理的理解是否深入，能否举一反三
            3. **实践经验**：是否有实际项目经验作为支撑
            4. **表达清晰度**：回答是否条理清晰、逻辑分明
            5. **问题解决能力**：面对复杂问题的分析思路

            ## 评分标准 (0-10分)
            - 0-3分：回答错误或严重偏差，需要深入学习
            - 4-5分：回答基本正确但缺乏深度
            - 6-7分：回答正确，有一定深度
            - 8-9分：回答优秀，有深入理解和实践经验
            - 10分：回答完美，超出预期

            ## 决策类型
            - PROBE：需要追问深入探讨（候选人回答较好，值得深挖）
            - NEXT：进入下一问题（当前问题已评估充分）
            - STAGE_FINISH：当前阶段完成，进入下一阶段
            - FINISH：面试结束（所有阶段已完成或需要提前结束）

            ## 输出格式
            请严格按照 JSON 格式输出 ReflectionResultBO 对象，包含：
            - score: 评分 (0-10)
            - decision: 决策 (PROBE/NEXT/STAGE_FINISH/FINISH)
            - feedback: 具体反馈和改进建议
            - probeSuggestions: 如果 decision 为 PROBE，提供追问方向
            """)
    @UserMessage("""
            请评估以下候选人的回答：

            ## 当前问题
            {{currentQuestion}}

            ## 候选人回答
            {{userAnswer}}

            ## 候选人简历上下文
            {{resumeContext}}

            ## 任务要求
            1. 首先通过 activate_skill 工具激活 ResumeAnalysisSkill（分析候选人技能水平）
            2. 结合简历上下文，评估候选人是否展现了其声称的技能水平
            3. 根据评估维度给出综合评分 (0-10)
            4. 提供具体的反馈意见，包括优点和不足
            5. 根据评分和回答质量做出决策：
               - 如果回答很好且有深挖空间 → PROBE
               - 如果回答基本完成当前问题 → NEXT
               - 如果当前阶段问题已完成 → STAGE_FINISH
               - 如果所有阶段已完成或需要结束 → FINISH
            6. 输出 JSON 格式的 ReflectionResultBO 对象
            """)
    ReflectionResultBO reflect(
            @MemoryId String memoryId,
            @V("currentQuestion") String currentQuestion,
            @V("userAnswer") String userAnswer,
            @V("resumeContext") String resumeContext
    );
}
