package com.tengYii.jobspark.application.service;

import com.tengYii.jobspark.model.InterviewEvaluation;
import com.tengYii.jobspark.model.InterviewQuestion;
import com.tengYii.jobspark.model.InterviewSession;
import com.tengYii.jobspark.model.Resume;
import com.tengYii.jobspark.domain.service.InterviewService;
import com.tengYii.jobspark.domain.service.ResumeAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 面试应用服务 - 协调领域服务和基础设施
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewApplicationService {
    
    private final InterviewService interviewService;
    private final ResumeAnalysisService resumeAnalysisService;
    
    // 内存存储面试会话（实际项目中应该使用数据库）
    // private final Map<String, InterviewSession> sessionStore = new ConcurrentHashMap<>();
    
    /**
     * 创建面试会话
     */
    public InterviewSession createInterviewSession(String resumeId, String interviewType, int questionCount) {
        try {
            // 验证简历是否存在
            Resume resume = getResumeById(resumeId);
            if (resume == null) {
                throw new IllegalArgumentException("简历不存在: " + resumeId);
            }
            
            // 创建面试会话
            InterviewSession session = interviewService.createInterviewSession(
                resumeId, interviewType, questionCount
            );
            
            // 存储会话（实际项目中应该持久化到数据库）
            // sessionStore.put(session.getSessionId(), session);
            
            log.info("面试会话创建成功: {}", session.getSessionId());
            return session;
            
        } catch (Exception e) {
            log.error("创建面试会话失败", e);
            throw new RuntimeException("创建面试会话失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 评估面试回答
     */
    public InterviewEvaluation evaluateAnswer(String sessionId, String answer) {
        try {
            InterviewSession session = getSessionById(sessionId);
            if (session == null) {
                throw new IllegalArgumentException("面试会话不存在: " + sessionId);
            }
            
            return interviewService.evaluateAnswer(session, answer);
            
        } catch (Exception e) {
            log.error("评估面试回答失败", e);
            throw new RuntimeException("评估面试回答失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 完成面试
     */
    public InterviewEvaluation completeInterview(String sessionId, List<String> allAnswers) {
        try {
            InterviewSession session = getSessionById(sessionId);
            if (session == null) {
                throw new IllegalArgumentException("面试会话不存在: " + sessionId);
            }
            
            return interviewService.completeInterview(session, allAnswers);
            
        } catch (Exception e) {
            log.error("完成面试失败", e);
            throw new RuntimeException("完成面试失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成面试建议
     */
    public String generateInterviewSuggestions(String resumeId, String targetPosition) {
        try {
            Resume resume = getResumeById(resumeId);
            if (resume == null) {
                throw new IllegalArgumentException("简历不存在: " + resumeId);
            }
            
            return interviewService.generateInterviewSuggestions(resume, targetPosition);
            
        } catch (Exception e) {
            log.error("生成面试建议失败", e);
            throw new RuntimeException("生成面试建议失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取当前问题
     */
    public InterviewQuestion getCurrentQuestion(String sessionId) {
        try {
            InterviewSession session = getSessionById(sessionId);
            if (session == null) {
                throw new IllegalArgumentException("面试会话不存在: " + sessionId);
            }
            
            return interviewService.getCurrentQuestion(session);
            
        } catch (Exception e) {
            log.error("获取当前问题失败", e);
            throw new RuntimeException("获取当前问题失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查面试是否完成
     */
    public boolean isInterviewCompleted(String sessionId) {
        try {
            InterviewSession session = getSessionById(sessionId);
            if (session == null) {
                throw new IllegalArgumentException("面试会话不存在: " + sessionId);
            }
            
            return interviewService.isInterviewCompleted(session);
            
        } catch (Exception e) {
            log.error("检查面试状态失败", e);
            throw new RuntimeException("检查面试状态失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 终止面试
     */
    public void terminateInterview(String sessionId) {
        try {
            InterviewSession session = getSessionById(sessionId);
            if (session == null) {
                throw new IllegalArgumentException("面试会话不存在: " + sessionId);
            }
            
            interviewService.terminateInterview(session);
            
        } catch (Exception e) {
            log.error("终止面试失败", e);
            throw new RuntimeException("终止面试失败: " + e.getMessage(), e);
        }
    }
    
    // 私有方法
    
    /**
     * 根据ID获取简历（需要实现持久化逻辑）
     */
    private Resume getResumeById(String resumeId) {
        // TODO: 实现简历获取逻辑
        // 这里返回模拟数据
        return (Resume)resumeAnalysisService.getResumeAnalysis(resumeId);
    }
    
    /**
     * 根据ID获取会话（需要实现持久化逻辑）
     */
    private InterviewSession getSessionById(String sessionId) {
        // TODO: 实现会话获取逻辑
        // 这里返回模拟数据
        return interviewService.createInterviewSession("sample_resume_id", "技术面试", 5);
    }
}