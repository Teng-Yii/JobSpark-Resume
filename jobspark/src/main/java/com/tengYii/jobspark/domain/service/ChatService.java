package com.tengYii.jobspark.domain.service;

import com.tengYii.jobspark.model.InterviewSession;
import com.tengYii.jobspark.model.Resume;
import com.tengYii.jobspark.common.utils.llm.ChatModelProvider;
import com.tengYii.jobspark.model.bo.CvBO;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AI聊天服务 - 处理面试评估和建议生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    
    private final ChatModel chatModel = ChatModelProvider.createChatModel();
    
    /**
     * 评估面试回答
     */
    public String evaluateInterviewAnswer(String question, String answer, String referenceAnswer, String criteria) {
        try {
            String prompt = buildEvaluationPrompt(question, answer, referenceAnswer, criteria);
            return chatModel.chat(prompt);
            
        } catch (Exception e) {
            log.error("AI评估面试回答失败", e);
            throw new RuntimeException("AI评估失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成综合评估
     */
    public String generateComprehensiveEvaluation(InterviewSession session, java.util.List<String> allAnswers) {
        try {
            String prompt = buildComprehensiveEvaluationPrompt(session, allAnswers);
            return chatModel.chat(prompt);
            
        } catch (Exception e) {
            log.error("AI生成综合评估失败", e);
            throw new RuntimeException("综合评估生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成面试建议
     */
    public String generateInterviewSuggestions(CvBO cvBO, String targetPosition) {
        try {
            String prompt = buildSuggestionPrompt(cvBO, targetPosition);
            return chatModel.chat(prompt);
            
        } catch (Exception e) {
            log.error("AI生成面试建议失败", e);
            throw new RuntimeException("面试建议生成失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建评估提示词
     */
    private String buildEvaluationPrompt(String question, String answer, String referenceAnswer, String criteria) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请评估以下面试回答的质量：\n\n");
        prompt.append("问题：").append(question).append("\n\n");
        prompt.append("回答：").append(answer).append("\n\n");
        
        if (referenceAnswer != null && !referenceAnswer.isEmpty()) {
            prompt.append("参考答案：").append(referenceAnswer).append("\n\n");
        }
        
        prompt.append("评分标准：").append(criteria).append("\n\n");
        prompt.append("请从以下维度进行评估：\n");
        prompt.append("1. 技术准确性（0-10分）\n");
        prompt.append("2. 回答完整性（0-10分）\n");
        prompt.append("3. 表达清晰度（0-10分）\n");
        prompt.append("4. 实际案例相关性（0-10分）\n");
        prompt.append("5. 改进建议\n\n");
        prompt.append("请以JSON格式返回评估结果。");
        
        return prompt.toString();
    }
    
    /**
     * 构建综合评估提示词
     */
    private String buildComprehensiveEvaluationPrompt(InterviewSession session, java.util.List<String> allAnswers) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请基于以下面试会话生成综合评估：\n\n");
        prompt.append("面试类型：").append(session.getInterviewType()).append("\n");
        prompt.append("问题数量：").append(session.getQuestions().size()).append("\n\n");
        
        prompt.append("所有回答：\n");
        for (int i = 0; i < allAnswers.size(); i++) {
            prompt.append("问题").append(i + 1).append("：")
                  .append(session.getQuestions().get(i).getContent()).append("\n")
                  .append("回答：").append(allAnswers.get(i)).append("\n\n");
        }
        
        prompt.append("请从以下维度进行综合评估：\n");
        prompt.append("1. 总体表现评分（0-10分）\n");
        prompt.append("2. 技术能力评估\n");
        prompt.append("3. 沟通表达能力评估\n");
        prompt.append("4. 问题解决能力评估\n");
        prompt.append("5. 团队协作能力评估\n");
        prompt.append("6. 主要优势\n");
        prompt.append("7. 改进建议\n\n");
        prompt.append("请以JSON格式返回综合评估结果。");
        
        return prompt.toString();
    }
    
    /**
     * 构建建议提示词
     */
    private String buildSuggestionPrompt(CvBO cvBO, String targetPosition) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请基于以下简历信息为目标职位生成面试准备建议：\n\n");
        
        /*if (cvBO.getBasicInfo() != null) {
            prompt.append("基本信息：\n");
            prompt.append("- 姓名：").append(cvBO.getBasicInfo().getName()).append("\n");
            prompt.append("- 当前职位：").append(cvBO.getBasicInfo().getCurrentPosition()).append("\n");
            prompt.append("- 目标职位：").append(cvBO.getBasicInfo().getTargetPosition()).append("\n");
            prompt.append("- 工作年限：").append(cvBO.getBasicInfo().getWorkExperience()).append("\n\n");
        }
        
        if (cvBO.getSkills() != null && !cvBO.getSkills().isEmpty()) {
            prompt.append("技能列表：\n");
            for (var skill : cvBO.getSkills()) {
                prompt.append("- ").append(skill.getSkillName()).append(" (").append(skill.getProficiency()).append(")\n");
            }
            prompt.append("\n");
        }
        */
        prompt.append("目标职位：").append(targetPosition).append("\n\n");
        
        prompt.append("请从以下方面提供建议：\n");
        prompt.append("1. 技术准备重点\n");
        prompt.append("2. 常见面试问题预测\n");
        prompt.append("3. 简历亮点展示策略\n");
        prompt.append("4. 沟通表达技巧\n");
        prompt.append("5. 薪资谈判建议\n\n");
        prompt.append("请以清晰的结构化格式返回建议。");
        
        return prompt.toString();
    }
}