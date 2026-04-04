package com.tengYii.jobspark.application.controller;

import com.tengYii.jobspark.application.service.InterviewApplicationService;
import com.tengYii.jobspark.common.utils.login.UserContext;
import com.tengYii.jobspark.domain.service.interview.InterviewOrchestratorService;
import com.tengYii.jobspark.dto.request.InterviewContinueRequest;
import com.tengYii.jobspark.dto.request.InterviewSimulationRequest;
import com.tengYii.jobspark.dto.response.*;
import com.tengYii.jobspark.model.InterviewSession;
import com.tengYii.jobspark.model.bo.interview.InterviewResponseBO;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 模拟面试API控制器
 * 支持多轮交互式面试会话
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    @Resource
    private InterviewApplicationService interviewApplicationService;

    @Resource
    private InterviewOrchestratorService interviewOrchestratorService;

    /**
     * 启动新的面试会话（首次交互）
     * 执行JD对齐、计划制定，返回首个问题
     */
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<InterviewResponseBO>> startInterview(@RequestBody InterviewSimulationRequest request) {
        Long userId = getLoginUserId();
        request.setUserId(userId);

        try {
            InterviewResponseBO response = interviewApplicationService.startInterview(request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("启动面试会话失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("启动面试会话失败: " + e.getMessage()));
        }
    }

    /**
     * 继续面试会话（后续交互）
     * 接收用户回答，评估后返回下一问题或结束面试
     */
    @PostMapping("/sessions/{sessionId}/continue")
    public ResponseEntity<ApiResponse<InterviewResponseBO>> continueInterview(
            @PathVariable Long sessionId,
            @RequestBody InterviewContinueRequest request) {

        try {
            InterviewResponseBO response = interviewApplicationService.continueInterview(sessionId, request.getUserAnswer());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("继续面试会话失败, sessionId={}", sessionId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("继续面试会话失败: " + e.getMessage()));
        }
    }

    /**
     * 获取面试会话当前状态
     */
    @GetMapping("/sessions/{sessionId}/status")
    public ResponseEntity<ApiResponse<InterviewResponseBO>> getSessionStatus(@PathVariable Long sessionId) {
        try {
            InterviewResponseBO response = interviewApplicationService.getSessionStatus(sessionId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("获取面试状态失败, sessionId={}", sessionId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取面试状态失败: " + e.getMessage()));
        }
    }

    /**
     * 结束面试会话
     */
    @PostMapping("/sessions/{sessionId}/finish")
    public ResponseEntity<ApiResponse<InterviewResponseBO>> finishInterview(@PathVariable Long sessionId) {
        try {
            InterviewResponseBO response = interviewApplicationService.finishInterview(sessionId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("结束面试会话失败, sessionId={}", sessionId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("结束面试会话失败: " + e.getMessage()));
        }
    }


    /**
     * 获取当前登录用户的ID
     */
    private Long getLoginUserId() {
        return UserContext.getCurrentUserId();
    }
}