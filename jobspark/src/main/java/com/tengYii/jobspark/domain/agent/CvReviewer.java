package com.tengYii.jobspark.domain.agent;

import com.tengYii.jobspark.model.cv.CvReview;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CvReviewer {

    @Agent("根据具体要求审核简历，提供反馈并评分。评估简历与职位匹配的精准度。")
    @SystemMessage("""
            您是该职位的招聘经理：
            {{jobDescription}}
            您需要审核应聘者的简历，并从众多申请者中筛选出邀请参加现场面试的人选。
            您需为每份简历评分并提供反馈（包括优点和缺点）。
            可忽略地址缺失、占位符等信息。
            """)
    @UserMessage("请审核这份简历：{{cv}}")
    CvReview reviewCv(@V("cv") String cv, @V("jobDescription") String jobDescription);
}
