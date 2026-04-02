package com.tengYii.jobspark.domain.agent.interview;

import com.tengYii.jobspark.model.bo.interview.InterviewPlanBO;
import com.tengYii.jobspark.model.bo.interview.ResumeContextBO;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Java技术面试执行Agent：Plan-and-Execute 模式中的执行者。
 * <p>
 * 该Agent负责：
 * <ul>
 *   <li>根据面试计划执行技术问题</li>
 *   <li>结合候选人简历进行个性化提问</li>
 *   <li>根据候选人回答动态调整问题难度</li>
 *   <li>支持追问功能深入考察技术深度</li>
 * </ul>
 * <p>
 * 输入：
 * <ul>
 *   <li>currentStage - 当前面试阶段</li>
 *   <li>conversationHistory - 对话历史</li>
 *   <li>resumeContext - 简历上下文</li>
 *   <li>interviewPlan - 面试计划</li>
 * </ul>
 * <p>
 * 输出：
 * <ul>
 *   <li>currentQuestion - 当前要提出的技术问题</li>
 * </ul>
 */
public interface JavaTechInterviewerAgent {

    /**
     * 执行下一个技术面试问题。
     * <p>
     * 执行步骤：
     * <ol>
     *   <li>根据当前阶段从面试计划中获取对应的问题</li>
     *   <li>结合候选人的项目经历定制化问题</li>
     *   <li>参考对话历史，避免重复提问</li>
     *   <li>输出定制化的问题文本</li>
     * </ol>
     *
     * @param currentStage        当前面试阶段
     * @param conversationHistory 对话历史，包含之前的问答内容
     * @param resumeContext       简历上下文，包含候选人技能和项目经验
     * @param interviewPlan       面试计划，包含问题列表和阶段安排
     * @return 当前阶段需要提出的技术问题
     */
    @Agent(value = "Java技术面试执行Agent：根据面试计划和候选人简历，执行定制化的技术问题，动态调整问题难度和深度", outputKey = "currentQuestion")
    @SystemMessage("""
            你是一位拥有10年以上开发经验的资深Java技术面试官。
            
            ## 核心原则
            1. **个性化提问**：必须结合候选人的简历项目经历来定制问题，不要问空泛的理论问题
            2. **深度追问**：根据候选人的回答质量动态调整，对深入的回答进行追问
            3. **难度适配**：候选人回答困难时适当降低难度，回答优秀时继续深挖
            4. **场景化问题**：技术问题要结合实际业务场景，特别是候选人简历中提到的技术栈
            
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
            请根据以下信息生成下一个技术面试问题：
            
            ## 当前面试阶段
            {{currentStage}}
            
            ## 对话历史
            {{conversationHistory}}
            
            ## 候选人简历上下文
            {{resumeContext}}
            
            ## 面试计划
            {{interviewPlan}}
            
            ## 任务要求
            1. 首先通过 activate_skill 工具激活 ResumeAnalysisSkill（分析候选人技能）和 QuestionProbingSkill（追问能力）
            2. 根据当前阶段从面试计划中获取对应的问题列表
            3. 结合候选人的项目经历和技能栈，定制化生成技术问题
            4. 考虑对话历史，避免重复提问相同主题
            5. 如果需要深入追问，可以调用 QuestionProbingSkill
            6. 输出当前阶段的技术问题文本
            """)
    String conductNextQuestion(
            @V("currentStage") InterviewPlanBO.Stage currentStage,
            @V("conversationHistory") String conversationHistory,
            @V("resumeContext") ResumeContextBO resumeContext,
            @V("interviewPlan") InterviewPlanBO interviewPlan
    );
}
