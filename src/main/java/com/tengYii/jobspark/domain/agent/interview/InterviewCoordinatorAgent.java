package com.tengYii.jobspark.domain.agent.interview;

import com.tengYii.jobspark.dto.response.ResumeDetailResponse;
import com.tengYii.jobspark.model.bo.interview.InterviewPlanBO;
import com.tengYii.jobspark.model.bo.interview.JDAlignmentResultBO;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 面试计划制定Agent：Plan-and-Execute 模式中的计划制定者。
 * <p>
 * 该Agent负责：
 * <ul>
 *   <li>分析候选人简历中的技能栈和工作经历</li>
 *   <li>结合JD对齐结果制定个性化的面试计划</li>
 *   <li>设计合理的面试阶段和问题优先级</li>
 * </ul>
 * <p>
 * 输入：
 * <ul>
 *   <li>resumeContext - 简历上下文信息</li>
 *   <li>j dAlignResult - JD对齐分析结果（可选）</li>
 * </ul>
 * <p>
 * 输出：
 * <ul>
 *   <li>interviewPlan - 包含面试阶段和问题列表的计划</li>
 * </ul>
 *
 * @see InterviewPlanBO
 */
public interface InterviewCoordinatorAgent {

    /**
     * 根据候选人的简历上下文生成个性化的面试计划。
     * <p>
     * 计划生成步骤：
     * <ol>
     *   <li>分析简历中的技能栈，确定考察重点</li>
     *   <li>分析项目经验，提取可深入讨论的技术点</li>
     *   <li>结合JD对齐结果，调整问题优先级</li>
     *   <li>设计面试阶段顺序</li>
     *   <li>为每个阶段分配问题和时间</li>
     * </ol>
     *
     * @param resumeContext 简历上下文信息，包含技能列表和项目经验
     * @param jdAlignResult JD对齐分析结果，用于调整考察重点
     * @return 个性化的面试计划，包含阶段划分和问题列表
     */
    @Agent(value = "面试计划制定Agent：根据候选人简历和JD对齐结果，制定个性化面试计划，设计合理的面试阶段和问题优先级", outputKey = "interviewPlan")
    @SystemMessage("""
            你是一位资深的Java技术面试官，拥有10年以上开发经验和面试经验。
            
            ## 核心职责
            你需要根据候选人的简历和JD对齐结果，制定一个结构化、有针对性的面试计划。
            
            ## 面试计划结构要求
            面试计划应包含以下阶段（可根据实际情况调整顺序和重点）：
            
            1. **自我介绍与项目破冰** (1-3分钟)
               - 针对简历中最有代表性的项目进行深入了解
               - 考察候选人的表达能力和项目深度
            
            2. **Java基础与进阶** (8-15分钟)
               - 根据简历技能栈选择合适的知识点
               - 涵盖：集合、多线程、并发、JVM、设计模式等
            
            3. **系统设计与架构** (10-15分钟)
               - 如果简历有架构经验，深入讨论
               - 考察系统设计能力和解决方案思路
            
            4. **技术深度挖掘** (10-15分钟)
               - 针对简历中的技术亮点进行深度追问
               - 考察技术的深度理解和实际应用能力
            
            5. **综合能力评估** (2-5分钟)
               - 团队协作、问题解决能力、职业规划等
            
            ## 输出格式
            请严格按照 JSON 格式输出 InterviewPlanBO 对象，包含：
            - stages: 面试阶段列表
            - estimatedDuration: 预计面试时长
            - focusAreas: 本次面试的重点考察领域
            - questionPriorities: 问题优先级排序
            """)
    @UserMessage("""
            请根据以下信息生成面试计划：
            
            ## 候选人简历上下文
            {{resumeContext}}
            
            ## JD对齐分析结果
            {{jdAlignResult}}
            
            ## 任务要求
            1. 首先通过 activate_skill 工具激活 ResumeAnalysisSkill
            2. 调用 ResumeAnalysisSkill 进一步分析候选人技能
            3. 结合JD对齐结果，识别需要重点考察的技能领域
            4. 根据候选人技能栈设计合适的问题难度和深度
            5. 生成结构化的面试计划，包含每个阶段的问题列表
            6. 输出JSON格式的 InterviewPlanBO 对象
            """)
    InterviewPlanBO generateInterviewPlan(@MemoryId String memoryId, @V("resumeContext") ResumeDetailResponse resumeContext, @V("jdAlignResult") JDAlignmentResultBO jdAlignResult);
}
