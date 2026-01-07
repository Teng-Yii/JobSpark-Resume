package com.tengYii.jobspark.application.controller;

import com.tengYii.jobspark.application.service.InterviewApplicationService;
import com.tengYii.jobspark.dto.request.CompleteInterviewRequest;
import com.tengYii.jobspark.dto.request.CreateInterviewRequest;
import com.tengYii.jobspark.dto.request.SubmitAnswerRequest;
import com.tengYii.jobspark.dto.response.*;
import com.tengYii.jobspark.model.InterviewEvaluation;
import com.tengYii.jobspark.model.InterviewQuestion;
import com.tengYii.jobspark.model.InterviewSession;
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

    private final InterviewApplicationService interviewApplicationServiceImpl;

    /**
     * 创建新的面试会话
     */
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<InterviewSessionResponse>> createInterviewSession(
            @RequestBody CreateInterviewRequest request) {
        try {
            InterviewSession session = interviewApplicationServiceImpl.createInterviewSession(
                    request.getResumeId(),
                    request.getInterviewType(),
                    request.getQuestionCount()
            );

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
     * 获取当前面试问题
     */
    @GetMapping("/sessions/{sessionId}/current-question")
    public ResponseEntity<ApiResponse<InterviewQuestionResponse>> getCurrentQuestion(
            @PathVariable String sessionId) {
        try {
            InterviewQuestion question = interviewApplicationServiceImpl.getCurrentQuestion(sessionId);

            if (question == null) {
                return ResponseEntity.ok(ApiResponse.success(null, "没有更多问题"));
            }

            InterviewQuestionResponse response = new InterviewQuestionResponse(
                    question.getQuestionId(),
                    question.getContent(),
                    question.getType(),
                    question.getSkillTag(),
                    question.getDifficulty()
            );

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("获取当前问题失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取当前问题失败: " + e.getMessage()));
        }
    }

    /**
     * 提交面试回答
     */
    @PostMapping("/sessions/{sessionId}/answer")
    public ResponseEntity<ApiResponse<InterviewEvaluationResponse>> submitAnswer(
            @PathVariable String sessionId,
            @RequestBody SubmitAnswerRequest request) {
        try {
            InterviewEvaluation evaluation = interviewApplicationServiceImpl.evaluateAnswer(
                    sessionId, request.getAnswer()
            );

            InterviewEvaluationResponse response = new InterviewEvaluationResponse(
                    evaluation.getOverallScore(),
                    evaluation.getTechnicalScore(),
                    evaluation.getCommunicationScore(),
                    evaluation.getProblemSolvingScore(),
                    evaluation.getTeamworkScore(),
                    evaluation.getStrengths(),
                    evaluation.getImprovementSuggestions()
            );

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("提交回答失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("提交回答失败: " + e.getMessage()));
        }
    }

    /**
     * 完成面试
     */
    @PostMapping("/sessions/{sessionId}/complete")
    public ResponseEntity<ApiResponse<InterviewEvaluationResponse>> completeInterview(
            @PathVariable String sessionId,
            @RequestBody CompleteInterviewRequest request) {
        try {
            InterviewEvaluation evaluation = interviewApplicationServiceImpl.completeInterview(
                    sessionId, request.getAllAnswers()
            );

            InterviewEvaluationResponse response = new InterviewEvaluationResponse(
                    evaluation.getOverallScore(),
                    evaluation.getTechnicalScore(),
                    evaluation.getCommunicationScore(),
                    evaluation.getProblemSolvingScore(),
                    evaluation.getTeamworkScore(),
                    evaluation.getStrengths(),
                    evaluation.getImprovementSuggestions()
            );

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("完成面试失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("完成面试失败: " + e.getMessage()));
        }
    }

    /**
     * 获取面试建议
     */
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<String>> getInterviewSuggestions(
            @RequestParam String resumeId,
            @RequestParam String targetPosition) {
        try {
            String suggestions = interviewApplicationServiceImpl.generateInterviewSuggestions(
                    Long.parseLong(resumeId), targetPosition
            );

            return ResponseEntity.ok(ApiResponse.success(suggestions));

        } catch (Exception e) {
            log.error("获取面试建议失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取面试建议失败: " + e.getMessage()));
        }
    }

    /**
     * 检查面试状态
     */
    @GetMapping("/sessions/{sessionId}/status")
    public ResponseEntity<ApiResponse<InterviewStatusResponse>> getInterviewStatus(
            @PathVariable String sessionId) {
        try {
            boolean isCompleted = interviewApplicationServiceImpl.isInterviewCompleted(sessionId);
            InterviewQuestion currentQuestion = interviewApplicationServiceImpl.getCurrentQuestion(sessionId);

            InterviewStatusResponse response = new InterviewStatusResponse(
                    sessionId,
                    isCompleted ? "已完成" : "进行中",
                    currentQuestion != null ? currentQuestion.getQuestionId() : null,
                    currentQuestion != null ? currentQuestion.getContent() : null
            );

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("获取面试状态失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取面试状态失败: " + e.getMessage()));
        }
    }

    /**
     * 终止面试
     */
    @PostMapping("/sessions/{sessionId}/terminate")
    public ResponseEntity<ApiResponse<Void>> terminateInterview(@PathVariable String sessionId) {
        try {
            interviewApplicationServiceImpl.terminateInterview(sessionId);
            return ResponseEntity.ok(ApiResponse.success(null, "面试已终止"));

        } catch (Exception e) {
            log.error("终止面试失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("终止面试失败: " + e.getMessage()));
        }
    }
}