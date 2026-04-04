package com.tengYii.jobspark.application.service.impl;

import com.tengYii.jobspark.application.service.InterviewApplicationService;
import com.tengYii.jobspark.application.service.ResumeApplicationService;
import com.tengYii.jobspark.domain.service.interview.InterviewOrchestratorService;
import com.tengYii.jobspark.dto.request.InterviewSimulationRequest;
import com.tengYii.jobspark.dto.response.ResumeDetailResponse;
import com.tengYii.jobspark.model.bo.interview.InterviewResponseBO;
import com.tengYii.jobspark.model.bo.interview.InterviewSessionContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 面试应用服务实现类
 * 负责面试会话的业务逻辑管理
 * 将Agent调用委托给 InterviewOrchestratorService
 * 会话上下文由 InterviewSessionContext 统一管理
 */
@Slf4j
@Service
public class InterviewApplicationServiceImpl implements InterviewApplicationService {

    @Resource
    private InterviewOrchestratorService interviewOrchestratorService;

    @Resource
    private ResumeApplicationService resumeApplicationService;

    @Override
    public InterviewResponseBO startInterview(InterviewSimulationRequest request) {
        Long userId = request.getUserId();
        Long resumeId = Long.parseLong(request.getResumeId());
        String jobDescription = request.getJobDescription();

        ResumeDetailResponse resumeDetail = resumeApplicationService.getResumeDetail(resumeId, userId);
        return interviewOrchestratorService.startInterview(request, resumeDetail);
    }

    @Override
    public InterviewResponseBO continueInterview(Long sessionId, String userAnswer) {
        if (!InterviewSessionContext.exists(sessionId)) {
            throw new IllegalArgumentException("会话不存在或已过期");
        }

        return interviewOrchestratorService.continueInterview(sessionId, userAnswer);
    }

    @Override
    public InterviewResponseBO getSessionStatus(Long sessionId) {
        if (!InterviewSessionContext.exists(sessionId)) {
            throw new IllegalArgumentException("会话不存在或已过期");
        }

        return interviewOrchestratorService.getSessionStatus(sessionId);
    }

    @Override
    public InterviewResponseBO finishInterview(Long sessionId) {
        if (!InterviewSessionContext.exists(sessionId)) {
            throw new IllegalArgumentException("会话不存在或已过期");
        }

        return interviewOrchestratorService.finishInterview(sessionId);
    }
}