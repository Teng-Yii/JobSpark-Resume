package com.tengYii.jobspark.application.controller;

import com.tengYii.jobspark.application.service.InterviewApplicationService;
import com.tengYii.jobspark.common.utils.login.UserContext;
import com.tengYii.jobspark.domain.service.interview.InterviewOrchestratorService;
import com.tengYii.jobspark.dto.request.InterviewSimulationRequest;
import com.tengYii.jobspark.dto.response.*;
import com.tengYii.jobspark.model.InterviewSession;
import com.tengYii.jobspark.model.bo.interview.JavaInterviewResultBO;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 模拟面试API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    @Resource
    private InterviewApplicationService interviewApplicationServiceImpl;

    @Resource
    private InterviewOrchestratorService interviewOrchestratorService;

    /**
     * 创建新的面试会话
     */
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<InterviewSessionResponse>> startInterview(@RequestBody InterviewSimulationRequest request) {

        Long userId = getLoginUserId();
        request.setUserId(userId);
        try {

            InterviewSession session = interviewApplicationServiceImpl.startInterview(request);

            InterviewSessionResponse response = new InterviewSessionResponse(
                    session.getSessionId(),
                    session.getResumeId(),
                    session.getInterviewType(),
                    session.getQuestions().size(),
                    session.getStatus()
            );

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("创建面试会话失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("创建面试会话失败: " + e.getMessage()));
        }
    }


    /**
     * 测试面试
     */
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<JavaInterviewResultBO>> testInterview(@RequestBody InterviewSimulationRequest request) {
        try {

            Long userId = getLoginUserId();
            request.setUserId(userId);
            JavaInterviewResultBO interviewResult = interviewOrchestratorService.startInterview(request, new ResumeDetailResponse());
            return ResponseEntity.ok(ApiResponse.success(interviewResult, "面试已终止"));

        } catch (Exception e) {
            log.error("终止面试失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("终止面试失败: " + e.getMessage()));
        }
    }

    /**
     * 获取当前登录用户的ID。
     *
     * @return 当前登录用户的ID。
     */
    private Long getLoginUserId() {
        // 直接从ThreadLocal获取当前用户ID，无需手动传递
        return UserContext.getCurrentUserId();
    }
}