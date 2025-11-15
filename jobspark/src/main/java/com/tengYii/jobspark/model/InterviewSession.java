package com.tengYii.jobspark.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 面试会话聚合根
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSession {
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 简历ID
     */
    private String resumeId;
    
    /**
     * 面试类型（技术面试、行为面试、综合面试等）
     */
    private String interviewType;
    
    /**
     * 面试问题列表
     */
    private List<InterviewQuestion> questions;
    
    /**
     * 当前问题索引
     */
    private int currentQuestionIndex;
    
    /**
     * 面试状态（进行中、已完成、已终止）
     */
    private String status;
    
    /**
     * 开始时间
     */
    private Long startTime;
    
    /**
     * 结束时间
     */
    private Long endTime;
    
    /**
     * 面试评估结果
     */
    private InterviewEvaluation evaluation;
    
    /**
     * 创建新的面试会话
     */
    public InterviewSession(String sessionId, String resumeId, String interviewType) {
        this.sessionId = sessionId;
        this.resumeId = resumeId;
        this.interviewType = interviewType;
        this.questions = new ArrayList<>();
        this.currentQuestionIndex = 0;
        this.status = "进行中";
        this.startTime = System.currentTimeMillis();
    }
    
    /**
     * 添加面试问题
     */
    public void addQuestion(InterviewQuestion question) {
        if (questions == null) {
            questions = new ArrayList<>();
        }
        questions.add(question);
    }
    
    /**
     * 获取当前问题
     */
    public InterviewQuestion getCurrentQuestion() {
        if (questions == null || questions.isEmpty() || currentQuestionIndex >= questions.size()) {
            return null;
        }
        return questions.get(currentQuestionIndex);
    }
    
    /**
     * 移动到下一个问题
     */
    public boolean nextQuestion() {
        if (currentQuestionIndex < questions.size() - 1) {
            currentQuestionIndex++;
            return true;
        }
        return false;
    }
    
    /**
     * 完成面试
     */
    public void completeInterview(InterviewEvaluation evaluation) {
        this.status = "已完成";
        this.endTime = System.currentTimeMillis();
        this.evaluation = evaluation;
    }
    
    /**
     * 终止面试
     */
    public void terminateInterview() {
        this.status = "已终止";
        this.endTime = System.currentTimeMillis();
    }
    
    /**
     * 验证会话有效性
     */
    public boolean isValid() {
        return sessionId != null && !sessionId.trim().isEmpty() && 
               resumeId != null && !resumeId.trim().isEmpty() && 
               interviewType != null && !interviewType.trim().isEmpty();
    }
}