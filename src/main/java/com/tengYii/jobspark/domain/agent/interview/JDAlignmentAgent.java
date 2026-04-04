package com.tengYii.jobspark.domain.agent.interview;

import com.tengYii.jobspark.dto.response.ResumeDetailResponse;
import com.tengYii.jobspark.model.bo.interview.JDAlignmentResultBO;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * JD对齐：分析职位描述与候选人简历的技能匹配度。
 */
public interface JDAlignmentAgent {
    /**
     * 执行JD与简历的技能对齐分析。
     * <p>
     * 分析步骤：
     * <ol>
     *   <li>提取JD中的硬性技能要求（技术栈、工具、框架）</li>
     *   <li>提取JD中的软性技能要求（沟通能力、团队协作等）</li>
     *   <li>从简历中提取候选人的技能列表</li>
     *   <li>计算各项技能的匹配度</li>
     *   <li>识别关键匹配点和缺失技能</li>
     *   <li>生成对齐分析报告</li>
     * </ol>
     *
     * @param jobDescription 职位描述文本，包含职位要求和技能需求
     * @param resumeContext  简历上下文信息
     * @return JD对齐分析结果，包含匹配度评分和关键差异点
     */
    @Agent(value = "JD对齐分析Agent：深入分析职位描述与候选人简历的技能匹配度，输出详细的匹配度评分和关键差异点", outputKey = "jdAlignResult")
    @UserMessage("""
            你是一位专业的HR技术招聘专家，擅长分析职位描述(JD)与候选人简历之间的技能匹配度。
            
            请根据以下信息进行深度分析：
            
            ## 职位描述 (JD)
            {{jobDescription}}
            
            ## 简历上下文
            {{resumeContext}}
            
            ## 任务要求
            1. 首先通过 activate_skill 工具激活 JDAlignmentSkill
            2. 调用 JDAlignmentSkill 分析简历与JD的匹配度
            3. 从JD中提取所有关键技能要求：
               - 硬性技能：编程语言、框架、工具、平台
               - 软性技能：沟通能力、团队协作、项目管理等
               - 加分项：额外技能或经验
            4. 识别候选人已展示的技能并计算每项技能的匹配度
            5. 输出：
               - matchedSkills: 已匹配的技能列表及匹配度(0-100%)
               - missingSkills: 缺失的关键技能列表
               - matchScore: 综合匹配度评分(0-100)
               - relatedProjects: 相关项目列表
               - focusAreas: 面试重点考察方向列表
               - suggestion: 综合建议
            6. 输出 JSON 格式的 JDAlignmentResultBO 对象
            """)
    JDAlignmentResultBO align(@MemoryId String memoryId, @V("jobDescription") String jobDescription, @V("resumeContext") ResumeDetailResponse resumeContext);

}
