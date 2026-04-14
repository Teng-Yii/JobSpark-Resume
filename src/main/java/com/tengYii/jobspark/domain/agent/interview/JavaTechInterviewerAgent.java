package com.tengYii.jobspark.domain.agent.interview;

import com.tengYii.jobspark.domain.agent.cv.JsonResponseCleanGuard;
import com.tengYii.jobspark.model.bo.interview.InterviewPlanBO;
import com.tengYii.jobspark.model.bo.interview.InterviewQuestionBO;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.guardrail.OutputGuardrails;

/**
 * Java技术面试执行Agent：交互式面试中的问题执行者。
 * <p>
 * 该Agent负责：
 * <ul>
 *   <li>根据面试计划执行技术问题</li>
 *   <li>结合候选人简历进行个性化提问</li>
 *   <li>根据用户回答动态判断是否需要追问</li>
 *   <li>支持追问功能深入考察技术深度</li>
 * </ul>
 * <p>
 * 输入（服务层组装）：
 * <ul>
 *   <li>interviewPlan - 面试计划，包含阶段和问题列表</li>
 *   <li>currentStageIndex - 当前阶段索引</li>
 *   <li>currentQuestionIndex - 当前问题索引</li>
 *   <li>isProbe - 是否为追问模式</li>
 * </ul>
 * <p>
 * 输出：
 * <ul>
 *   <li>currentQuestion - 当前要提出的技术问题（InterviewQuestionBO对象）</li>
 * </ul>
 */
public interface JavaTechInterviewerAgent {

    /**
     * 生成下一个技术面试问题。
     * <p>
     * 执行步骤：
     * <ol>
     *   <li>根据阶段和问题索引从面试计划获取对应的考察主题</li>
     *   <li>结合会话上下文中的候选人信息定制问题</li>
     *   <li>如果是追问模式，基于上一轮回答深挖</li>
     *   <li>输出结构化的面试问题对象</li>
     * </ol>
     *
     * @param interviewPlan       面试计划，包含问题列表和阶段安排
     * @param currentStageIndex   当前阶段索引
     * @param currentQuestionIndex 当前问题索引
     * @param isProbe             是否为追问模式
     * @return 包含问题内容、出题意图、追问预案等完整元数据的面试问题对象
     */
    @Agent(value = "Java技术面试执行Agent：根据面试计划和会话上下文，执行定制化的技术问题，动态调整问题难度和深度", outputKey = "questionBO")
    @UserMessage("""
            请根据以下信息生成技术面试问题：

            ## 面试计划
            {{interviewPlan}}

            ## 当前阶段索引
            {{currentStageIndex}}

            ## 当前问题索引
            {{currentQuestionIndex}}

            ## 是否为追问模式
            {{isProbe}}

            ## 任务要求
            1. 首先通过 activate_skill 工具激活 QuestionProbingSkill（追问能力）
            2. 根据当前阶段索引从面试计划获取对应阶段，根据问题索引获取考察主题
            3. 结合会话上下文中的候选人项目经历，定制化生成技术问题
            4. 如果是追问模式（isProbe=true），基于上一轮回答内容进行深入追问
            5. 输出 JSON 格式的 InterviewQuestionBO 对象
            """)
    @OutputGuardrails(value = {JsonResponseCleanGuard.class}, maxRetries = 0)
    InterviewQuestionBO generateQuestion(
            @MemoryId String memoryId,
            @V("interviewPlan") InterviewPlanBO interviewPlan,
            @V("currentStageIndex") int currentStageIndex,
            @V("currentQuestionIndex") int currentQuestionIndex,
            @V("isProbe") boolean isProbe
    );
}