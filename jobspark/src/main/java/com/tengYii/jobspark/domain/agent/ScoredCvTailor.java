package com.tengYii.jobspark.domain.agent;

import com.tengYii.jobspark.model.bo.CvBO;
import com.tengYii.jobspark.model.llm.CvReview;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ScoredCvTailor {

    @Agent("根据具体要求定制简历")
    @SystemMessage("""
            这是一份需要根据特定职位描述、反馈或其他要求进行调整的简历。
            您可以优化简历以满足要求，但请勿虚构事实。
            若删除无关内容能使简历更符合要求，可酌情删减。
            目标是让应聘者获得面试机会，并能在面试中展现与简历相符的能力。
            
            重要注意事项：
            1. 返回的JSON格式必须与CvBO类结构完全匹配
            2. LocaleConfigBO.sectionLabels字段必须是JSON字符串格式，而不是对象
            3. 确保所有日期格式为"yyyy-MM-dd"
            4. 保持原有字段结构和命名
            
            当前简历：{{cv}}
            """)
    @UserMessage("""
            以下是修改简历的指导原则与反馈意见：
            （再次强调，请勿添加原始简历中不存在的事实。
            若申请人并不完全匹配，请突出其现有特质中
            最接近要求的方面，但切勿捏造事实）
            审核意见：{{cvReview}}
            
            请返回格式正确的JSON对象，确保：
            - meta.localeConfig.sectionLabels字段是字符串格式："{\"education\":\"教育经历\",\"experience\":\"工作经历\"}"
            - 不要使用对象格式
            - 严格按照CvBO类结构返回
            """)
    CvBO tailorCv(@V("cv") CvBO cv, @V("cvReview") CvReview cvReview);
}
