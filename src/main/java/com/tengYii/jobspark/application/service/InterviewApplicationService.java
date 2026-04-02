package com.tengYii.jobspark.application.service;

import com.tengYii.jobspark.dto.request.InterviewSimulationRequest;
import com.tengYii.jobspark.model.InterviewSession;

public interface InterviewApplicationService {

    /**
     * 开始面试
     *
     * @param request 模拟面试请求对象
     * @return 新创建的面试会话对象
     */
    InterviewSession startInterview(InterviewSimulationRequest request);
}
