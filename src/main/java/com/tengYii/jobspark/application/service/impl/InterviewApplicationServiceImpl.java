package com.tengYii.jobspark.application.service.impl;

import com.tengYii.jobspark.application.service.InterviewApplicationService;
import com.tengYii.jobspark.common.exception.BusinessException;
import com.tengYii.jobspark.domain.service.interview.InterviewOrchestratorService;
import com.tengYii.jobspark.dto.request.InterviewSimulationRequest;
import com.tengYii.jobspark.dto.response.ResumeDetailResponse;
import com.tengYii.jobspark.model.InterviewSession;
import com.tengYii.jobspark.domain.service.cv.ResumeAnalysisService;
import com.tengYii.jobspark.model.bo.cv.CvBO;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 面试应用服务 - 协调领域服务和基础设施
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewApplicationServiceImpl implements InterviewApplicationService {

    @Resource
    private InterviewOrchestratorService interviewOrchestratorService;

    @Resource
    private ResumeApplicationServiceImpl resumeApplicationService;

    /**
     * 创建面试会话
     *
     * @param request 模拟面试请求对象
     * @return 新创建的面试会话对象
     */
    public InterviewSession startInterview(InterviewSimulationRequest request) {
        try {
            // 验证简历是否存在
            Long userId = request.getUserId();
            Long resumeId = Long.parseLong(request.getResumeId());
            ResumeDetailResponse resumeDetail = resumeApplicationService.getResumeDetail(resumeId, userId);


            // 创建面试会话
            interviewOrchestratorService.startInterview(request, resumeDetail);

            // 存储会话（实际项目中应该持久化到数据库）
            // sessionStore.put(session.getSessionId(), session);

            log.info("面试会话创建成功: {}", 1);
            return new InterviewSession();

        } catch (BusinessException be) {
            log.error("创建面试会话失败", e);
        } catch (Exception e) {
            log.error("创建面试会话失败", e);
            throw new RuntimeException("创建面试会话失败: " + e.getMessage(), e);
        }
    }
}