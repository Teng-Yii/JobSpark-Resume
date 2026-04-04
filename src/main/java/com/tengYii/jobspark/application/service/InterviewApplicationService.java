package com.tengYii.jobspark.application.service;

import com.tengYii.jobspark.dto.request.InterviewSimulationRequest;
import com.tengYii.jobspark.model.bo.interview.InterviewResponseBO;

/**
 * 面试应用服务接口
 * 负责面试会话的业务逻辑管理，包括会话上下文、状态流转等
 */
public interface InterviewApplicationService {

    /**
     * 开始面试会话
     *
     * @param request 模拟面试请求对象
     * @return 面试响应结果
     */
    InterviewResponseBO startInterview(InterviewSimulationRequest request);

    /**
     * 继续面试会话
     *
     * @param sessionId  会话ID
     * @param userAnswer 用户回答
     * @return 面试响应结果
     */
    InterviewResponseBO continueInterview(Long sessionId, String userAnswer);

    /**
     * 获取会话状态
     *
     * @param sessionId 会话ID
     * @return 面试响应结果
     */
    InterviewResponseBO getSessionStatus(Long sessionId);

    /**
     * 结束面试会话
     *
     * @param sessionId 会话ID
     * @return 面试响应结果
     */
    InterviewResponseBO finishInterview(Long sessionId);
}