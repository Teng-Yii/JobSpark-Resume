package com.tengYii.jobspark.domain.agent;

import com.tengYii.jobspark.model.CvReview;
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
            当前简历：{{cv}}
            """)
    @UserMessage("""
            以下是修改简历的指导原则与反馈意见：
            （再次强调，请勿添加原始简历中不存在的事实。
            若申请人并不完全匹配，请突出其现有特质中
            最接近要求的方面，但切勿捏造事实）
            审核意见：{{cvReview}}
            """)
    String tailorCv(@V("cv") String cv, @V("cvReview") CvReview cvReview);
}
