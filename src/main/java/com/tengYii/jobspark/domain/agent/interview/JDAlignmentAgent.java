package com.tengYii.jobspark.domain.agent.interview;

import com.tengYii.jobspark.domain.agent.cv.JsonResponseCleanGuard;
import com.tengYii.jobspark.model.bo.interview.JDAlignmentResultBO;
import com.tengYii.jobspark.model.bo.interview.ResumeContextBO;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.guardrail.OutputGuardrails;

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
            你是JD与简历技能匹配分析引擎。根据以下信息进行深度分析：

            ## 职位描述 (JD)
            {{jobDescription}}

            ## 简历上下文
            {{resumeContext}}

            ## 分析要求（在内部思考，不要输出分析过程）
            1. 首先通过 activate_skill 工具激活 JDAlignmentSkill
            2. 调用 JDAlignmentSkill 分析简历与JD的匹配度
            3. 从JD中提取所有关键技能要求（硬性技能、软性技能、加分项）
            4. 逐项对比候选人简历中的技能，计算匹配度(0-100%)
            5. 识别匹配的技能、缺失的技能、相关项目经验
            6. 确定面试重点考察方向
            7. 给出综合评分(0-100)和建议

            ## 输出字段说明
            - matchScore: 综合匹配度评分(整数,0-100)
            - matchedSkills: 已匹配技能及匹配度描述列表（字符串数组）
            - missingSkills: 缺失的关键技能列表（字符串数组）
            - relatedProjects: 相关项目经验列表（字符串数组）
            - focusAreas: 面试重点考察方向列表（字符串数组）
            - suggestion: 综合评价与面试建议（字符串）

            ## CRITICAL OUTPUT RULES - 必须严格遵守：
            - 只输出一个纯 JSON 对象，不要任何其他文字
            - 禁止输出 Markdown 标题、表格、分隔线、分析报告
            - 禁止用 ```json 代码块包裹
            - 禁止输出"综合分析"、"详细评估"等解释性文字
            - 禁止在 JSON 前后添加任何前缀或后缀文本
            - 你的整个回复必须是一个可直接解析的 JSON 对象，以 { 开头，以 } 结尾
            - 字符串值中的换行使用 \\n 转义，不要输出真实的换行符

            错误示例（禁止）：
            下面是我的分析... ```json {...} ```

            正确示例（必须）：
            {"matchScore":72,"matchedSkills":["Java(95%)"],"missingSkills":["Spring Cloud"],"relatedProjects":["项目A"],"focusAreas":["考察点1"],"suggestion":"建议内容"}
            """)
    @OutputGuardrails(value = {JsonResponseCleanGuard.class}, maxRetries = 0)
    JDAlignmentResultBO align(@MemoryId String memoryId, @V("jobDescription") String jobDescription, @V("resumeContext") ResumeContextBO resumeContext);

}
