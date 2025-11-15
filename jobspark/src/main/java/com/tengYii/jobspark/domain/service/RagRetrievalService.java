package com.tengYii.jobspark.domain.service;

import com.tengYii.jobspark.model.InterviewQuestion;
import com.tengYii.jobspark.model.Resume;
import com.tengYii.jobspark.model.Skill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG检索服务 - 基于向量相似度的面试问题检索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagRetrievalService {
    
    private final EmbeddingService embeddingService;
    private final VectorDatabaseService vectorDatabaseService;
    
    /**
     * 根据简历检索相关面试问题
     */
    public List<InterviewQuestion> retrieveInterviewQuestions(Resume resume, String interviewType, int maxQuestions) {
        try {
            // 1. 从简历中提取关键信息用于检索
            List<String> searchKeywords = extractSearchKeywords(resume);
            
            // 2. 为每个关键词生成向量嵌入
            List<float[]> keywordEmbeddings = new ArrayList<>();
            for (String keyword : searchKeywords) {
                float[] embedding = embeddingService.generateEmbedding(keyword);
                if (embedding != null) {
                    keywordEmbeddings.add(embedding);
                }
            }
            
            // 3. 在向量数据库中检索相似问题
            List<InterviewQuestion> retrievedQuestions = new ArrayList<>();
            for (float[] embedding : keywordEmbeddings) {
                List<InterviewQuestion> similarQuestions = vectorDatabaseService.searchSimilarQuestions(
                    embedding, interviewType, maxQuestions / searchKeywords.size()
                );
                retrievedQuestions.addAll(similarQuestions);
            }
            
            // 4. 去重和排序
            return retrievedQuestions.stream()
                .distinct()
                .limit(maxQuestions)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("RAG检索失败", e);
            throw new RuntimeException("RAG检索失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 基于技能匹配度检索问题
     */
    public List<InterviewQuestion> retrieveQuestionsBySkillMatch(Resume resume, String interviewType, int maxQuestions) {
        try {
            // 获取简历中的技能列表
            List<Skill> resumeSkills = resume.getSkills();
            if (resumeSkills == null || resumeSkills.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 为每个技能生成问题
            List<InterviewQuestion> skillBasedQuestions = new ArrayList<>();
            for (Skill skill : resumeSkills) {
                // 根据技能名称和熟练程度生成相关问题
                List<InterviewQuestion> questions = generateQuestionsForSkill(skill, interviewType);
                skillBasedQuestions.addAll(questions);
            }
            
            // 限制问题数量
            return skillBasedQuestions.stream()
                .limit(maxQuestions)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("技能匹配检索失败", e);
            throw new RuntimeException("技能匹配检索失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 混合检索策略 - 结合向量检索和技能匹配
     */
    public List<InterviewQuestion> hybridRetrieval(Resume resume, String interviewType, int maxQuestions) {
        try {
            // 向量检索获取相关问题
            List<InterviewQuestion> vectorQuestions = retrieveInterviewQuestions(resume, interviewType, maxQuestions / 2);
            
            // 技能匹配获取基础问题
            List<InterviewQuestion> skillQuestions = retrieveQuestionsBySkillMatch(resume, interviewType, maxQuestions / 2);
            
            // 合并结果
            List<InterviewQuestion> allQuestions = new ArrayList<>();
            allQuestions.addAll(vectorQuestions);
            allQuestions.addAll(skillQuestions);
            
            // 去重和限制数量
            return allQuestions.stream()
                .distinct()
                .limit(maxQuestions)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("混合检索失败", e);
            throw new RuntimeException("混合检索失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 提取简历中的搜索关键词
     */
    private List<String> extractSearchKeywords(Resume resume) {
        List<String> keywords = new ArrayList<>();
        
        // 从基本信息中提取
        if (resume.getBasicInfo() != null) {
            if (resume.getBasicInfo().getTargetPosition() != null) {
                keywords.add(resume.getBasicInfo().getTargetPosition());
            }
            if (resume.getBasicInfo().getCurrentPosition() != null) {
                keywords.add(resume.getBasicInfo().getCurrentPosition());
            }
        }
        
        // 从技能中提取
        if (resume.getSkills() != null) {
            for (Skill skill : resume.getSkills()) {
                if (skill.getSkillName() != null) {
                    keywords.add(skill.getSkillName());
                }
            }
        }
        
        // 从工作经历中提取公司和技术关键词
        if (resume.getWorkExperiences() != null) {
            for (var experience : resume.getWorkExperiences()) {
                if (experience.getCompany() != null) {
                    keywords.add(experience.getCompany());
                }
                if (experience.getPosition() != null) {
                    keywords.add(experience.getPosition());
                }
            }
        }
        
        return keywords.stream()
            .filter(keyword -> keyword != null && !keyword.trim().isEmpty())
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * 为特定技能生成面试问题
     */
    private List<InterviewQuestion> generateQuestionsForSkill(Skill skill, String interviewType) {
        List<InterviewQuestion> questions = new ArrayList<>();
        
        // 根据技能熟练程度和类型生成不同难度的问题
        String skillName = skill.getSkillName();
        String proficiency = skill.getProficiency();
        
        // 基础技术问题
        if ("技术面试".equals(interviewType)) {
            questions.add(createTechnicalQuestion(skillName, "基础", proficiency));
            
            if ("精通".equals(proficiency) || "熟练".equals(proficiency)) {
                questions.add(createTechnicalQuestion(skillName, "进阶", proficiency));
                questions.add(createScenarioQuestion(skillName, proficiency));
            }
        }
        
        // 行为面试问题
        if ("行为面试".equals(interviewType)) {
            questions.add(createBehavioralQuestion(skillName, proficiency));
        }
        
        return questions;
    }
    
    private InterviewQuestion createTechnicalQuestion(String skill, String difficulty, String proficiency) {
        String questionId = "tech_" + skill.toLowerCase().replace(" ", "_") + "_" + difficulty;
        String content = String.format("请介绍一下你对%s的理解和应用经验？", skill);
        
        return new InterviewQuestion(questionId, content, "技术问题", skill, difficulty, 
                                   "参考答案待生成", "根据回答的深度和准确性评分", null ,null);
    }
    
    private InterviewQuestion createScenarioQuestion(String skill, String proficiency) {
        String questionId = "scenario_" + skill.toLowerCase().replace(" ", "_");
        String content = String.format("请描述一个你在实际项目中应用%s解决问题的具体案例？", skill);
        
        return new InterviewQuestion(questionId, content, "情景问题", skill, "高级", 
                                   "参考答案待生成", "根据案例的真实性和解决方案的有效性评分", null, null);
    }
    
    private InterviewQuestion createBehavioralQuestion(String skill, String proficiency) {
        String questionId = "behavioral_" + skill.toLowerCase().replace(" ", "_");
        String content = String.format("在工作中遇到%s相关的挑战时，你是如何应对的？", skill);
        
        return new InterviewQuestion(questionId, content, "行为问题", skill, "中级", 
                                   "参考答案待生成", "根据应对策略和反思深度评分", null, null);
    }
}