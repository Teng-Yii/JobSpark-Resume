package com.tengYii.jobspark.domain.service;

import com.tengYii.jobspark.domain.model.Resume;
import com.tengYii.jobspark.utils.ChatModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeOptimizationService {

    private final ChatModel chatModel = ChatModelProvider.createChatModel();

    public Object getOptimizationSuggestions(String resumeId) {
        try {
            // 这里应该从存储中获取简历数据
            // 暂时使用模拟数据
            String resumeContent = getResumeContentFromStorage(resumeId);
            
            // 使用AI生成优化建议
            String suggestions = generateOptimizationSuggestions(resumeContent);
            
            return new OptimizationResult(resumeId, suggestions);
        } catch (Exception e) {
            log.error("获取优化建议失败", e);
            throw new RuntimeException("获取优化建议失败: " + e.getMessage(), e);
        }
    }

    private String getResumeContentFromStorage(String resumeId) {
        // 模拟从存储中获取简历内容
        // 实际应该从数据库或文件系统中获取
        return "模拟简历内容";
    }

    private String generateOptimizationSuggestions(String resumeContent) {
        String prompt = buildOptimizationPrompt(resumeContent);
        return chatModel.generate(prompt);
    }

    private String buildOptimizationPrompt(String resumeContent) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为以下简历提供优化建议：\n\n");
        prompt.append("简历内容：\n");
        prompt.append(resumeContent);
        prompt.append("\n\n");
        
        prompt.append("请从以下角度提供具体建议：\n");
        prompt.append("1. 内容结构优化\n");
        prompt.append("2. 关键词优化（针对ATS系统）\n");
        prompt.append("3. 技能展示优化\n");
        prompt.append("4. 成就量化描述\n");
        prompt.append("5. 格式和排版建议\n");
        prompt.append("6. 针对目标职位的定制建议\n");
        
        return prompt.toString();
    }

    // 优化结果封装类
    @RequiredArgsConstructor
    private static class OptimizationResult {
        private final String resumeId;
        private final String suggestions;

        public String getResumeId() { return resumeId; }
        public String getSuggestions() { return suggestions; }
    }
}