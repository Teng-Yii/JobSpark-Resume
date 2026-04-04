package com.tengYii.jobspark.domain.agent.interview;

import com.tengYii.jobspark.model.bo.interview.InterviewPlanBO;
import com.tengYii.jobspark.model.bo.interview.InterviewQuestionBO;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

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
 *   <li>currentQuestion - 当前要提出的技术问题</li>
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
     *   <li>输出定制化的问题文本</li>
     * </ol>
     *
     * @param interviewPlan       面试计划，包含问题列表和阶段安排
     * @param currentStageIndex   当前阶段索引
     * @param currentQuestionIndex 当前问题索引
     * @param isProbe             是否为追问模式
     * @return 当前阶段需要提出的技术问题
     */
    @Agent(value = "Java技术面试执行Agent：根据面试计划和会话上下文，执行定制化的技术问题，动态调整问题难度和深度", outputKey = "currentQuestion")
    @SystemMessage("""
            你是一位拥有10年以上开发经验的资深Java技术面试官。

            ## 核心原则
            1. **个性化提问**：必须结合候选人的简历项目经历来定制问题，不要问空泛的理论问题
            2. **深度追问**：根据候选人的回答质量动态调整，对深入的回答进行追问
            3. **难度适配**：候选人回答困难时适当降低难度，回答优秀时继续深挖
            4. **场景化问题**：技术问题要结合实际业务场景，特别是候选人简历中提到的技术栈

            ## 追问模式说明
            - 当 isProbe=true 时，表示需要对上一轮回答进行深入追问
            - 追问应基于候选人的回答细节进行针对性提问
            - 追问方向：原理深挖、场景应用、解决方案对比、最佳实践等

            ## 提问技巧
            - 如果候选人提到 Redis，必须问具体的缓存使用场景、缓存策略、雪崩处理等
            - 如果候选人提到微服务，要问服务治理、熔断限流、分布式事务等
            - 如果候选人提到并发，要问具体的并发场景、线程安全解决方案等
            - 项目问题要深挖：为什么用这个技术？遇到的最大挑战？如何解决的？

            ## 输出要求
            输出格式化的技术问题，包含：
            - 问题核心要点
            - 追问方向（如果候选人回答得好）
            - 备选简化问题（如果候选人回答困难）
            """)
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
            1. 首先通过 activate_skill 工具激活 ResumeAnalysisSkill（分析候选人技能）和 QuestionProbingSkill（追问能力）
            2. 根据当前阶段索引从面试计划获取对应阶段，根据问题索引获取考察主题
            3. 结合会话上下文中的候选人项目经历，定制化生成技术问题
            4. 如果是追问模式（isProbe=true），基于上一轮回答内容进行深入追问
            5. 输出当前阶段的技术问题文本
            """)
    String generateQuestion(
            @MemoryId String memoryId,
            @V("interviewPlan") InterviewPlanBO interviewPlan,
            @V("currentStageIndex") int currentStageIndex,
            @V("currentQuestionIndex") int currentQuestionIndex,
            @V("isProbe") boolean isProbe
    );
}