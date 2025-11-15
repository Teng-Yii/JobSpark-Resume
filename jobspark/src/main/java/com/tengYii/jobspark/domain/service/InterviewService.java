package com.tengYii.jobspark.domain.service;

import com.tengYii.jobspark.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 模拟面试服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {
    
    private final RagRetrievalService ragRetrievalService;
    private final ChatService chatService;
    
    /**
     * 创建新的面试会话
     */
    public InterviewSession createInterviewSession(String resumeId, String interviewType, int questionCount) {
        try {
            // 生成唯一会话ID
            String sessionId = UUID.randomUUID().toString();
            
            // 创建面试会话
            InterviewSession session = new InterviewSession(sessionId, resumeId, interviewType);
            
            // 这里需要获取简历信息，暂时使用模拟数据
            Resume resume = getResumeById(resumeId); // 需要实现简历获取逻辑
            
            // 使用RAG检索相关问题
            List<InterviewQuestion> questions = ragRetrievalService.hybridRetrieval(
                resume, interviewType, questionCount
            );
            
            // 添加问题到会话
            for (InterviewQuestion question : questions) {
                session.addQuestion(question);
            }
            
            log.info("创建面试会话成功: {}", sessionId);
            return session;
            
        } catch (Exception e) {
            log.error("创建面试会话失败", e);
            throw new RuntimeException("创建面试会话失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 提交面试回答并进行评估
     */
    public InterviewEvaluation evaluateAnswer(InterviewSession session, String answer) {
        try {
            InterviewQuestion currentQuestion = session.getCurrentQuestion();
            if (currentQuestion == null) {
                throw new IllegalArgumentException("当前没有可评估的问题");
            }
            
            // 使用AI评估回答质量
            String evaluationResult = chatService.evaluateInterviewAnswer(
                currentQuestion.getContent(),
                answer,
                currentQuestion.getReferenceAnswer(),
                currentQuestion.getEvaluationCriteria()
            );
            
            // 解析评估结果并生成评分
            InterviewEvaluation evaluation = parseEvaluationResult(evaluationResult, currentQuestion);
            
            log.info("面试回答评估完成: {}", session.getSessionId());
            return evaluation;
            
        } catch (Exception e) {
            log.error("面试回答评估失败", e);
            throw new RuntimeException("面试回答评估失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 完成整个面试并生成综合评估
     */
    public InterviewEvaluation completeInterview(InterviewSession session, List<String> allAnswers) {
        try {
            // 使用AI生成综合评估
            String comprehensiveEvaluation = chatService.generateComprehensiveEvaluation(
                session, allAnswers
            );
            
            // 解析综合评估结果
            InterviewEvaluation finalEvaluation = parseComprehensiveEvaluation(comprehensiveEvaluation);
            
            // 完成面试
            session.completeInterview(finalEvaluation);
            
            log.info("面试完成: {}", session.getSessionId());
            return finalEvaluation;
            
        } catch (Exception e) {
            log.error("面试完成失败", e);
            throw new RuntimeException("面试完成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成个性化面试建议
     */
    public String generateInterviewSuggestions(Resume resume, String targetPosition) {
        try {
            return chatService.generateInterviewSuggestions(resume, targetPosition);
        } catch (Exception e) {
            log.error("生成面试建议失败", e);
            throw new RuntimeException("生成面试建议失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取下一个面试问题
     */
    public InterviewQuestion getNextQuestion(InterviewSession session) {
        if (session.nextQuestion()) {
            return session.getCurrentQuestion();
        }
        return null;
    }
    
    /**
     * 获取当前面试问题
     */
    public InterviewQuestion getCurrentQuestion(InterviewSession session) {
        return session.getCurrentQuestion();
    }
    
    /**
     * 检查面试是否完成
     */
    public boolean isInterviewCompleted(InterviewSession session) {
        return "已完成".equals(session.getStatus()) || 
               "已终止".equals(session.getStatus());
    }
    
    /**
     * 终止面试
     */
    public void terminateInterview(InterviewSession session) {
        session.terminateInterview();
        log.info("面试已终止: {}", session.getSessionId());
    }
    
    // 私有方法
    
    /**
     * 根据ID获取简历（需要实现）
     */
    private Resume getResumeById(String resumeId) {
        // TODO: 实现简历获取逻辑
        // 这里返回模拟数据
        ResumeBasicInfo basicInfo = new ResumeBasicInfo(
                "张三", "zhangsan@email.com", "13800138000", "北京",
                "资深Java开发工程师", "Java开发工程师", "5年", "阿里巴巴"
        );

        List<Skill> skills = new ArrayList<>();
        skills.add(new Skill("Java", "精通", "编程语言", "5年Java开发经验"));
        skills.add(new Skill("Spring Boot", "熟练", "框架", "熟悉Spring全家桶"));

        return new Resume(resumeId, "sample_resume.pdf", "简历内容...");
    }
    
    /**
     * 解析AI评估结果
     */
    private InterviewEvaluation parseEvaluationResult(String evaluationResult, InterviewQuestion question) {
        // 简化处理，实际应该解析AI返回的结构化数据
        return new InterviewEvaluation(
            8.5,  // 总体评分
            9.0,  // 技术能力
            8.0,  // 沟通能力
            8.5,  // 问题解决
            8.0,  // 团队协作
            "技术理解深入，表达清晰",
            "可以加强实际案例的详细描述"
        );
    }
    
    /**
     * 解析综合评估结果
     */
    private InterviewEvaluation parseComprehensiveEvaluation(String comprehensiveEvaluation) {
        // 简化处理，实际应该解析AI返回的结构化数据
        return new InterviewEvaluation(
            8.2,  // 总体评分
            8.8,  // 技术能力
            7.8,  // 沟通能力
            8.3,  // 问题解决
            8.0,  // 团队协作
            "技术基础扎实，项目经验丰富",
            "建议加强沟通表达和团队协作能力的展示"
        );
    }
}