package com.tengYii.jobspark.domain.service;

import com.tengYii.jobspark.model.InterviewQuestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 向量数据库服务 - 模拟向量数据库操作
 * 实际项目中可以集成Milvus、Pinecone、Chroma等向量数据库
 */
@Slf4j
@Service
public class VectorDatabaseService {
    
    // 模拟问题数据库
    private final List<InterviewQuestion> questionDatabase = new ArrayList<>();
    
    public VectorDatabaseService() {
        // 初始化一些示例问题
        initializeSampleQuestions();
    }
    
    /**
     * 搜索相似问题
     */
    public List<InterviewQuestion> searchSimilarQuestions(float[] queryEmbedding, String interviewType, int maxResults) {
        try {
            // 过滤指定类型的问题
            List<InterviewQuestion> filteredQuestions = questionDatabase.stream()
                .filter(q -> interviewType == null || interviewType.equals(q.getType()))
                .collect(Collectors.toList());
            
            // 计算相似度并排序
            return filteredQuestions.stream()
                .sorted((q1, q2) -> {
                    double sim1 = calculateSimilarity(queryEmbedding, q1.getEmbedding());
                    double sim2 = calculateSimilarity(queryEmbedding, q2.getEmbedding());
                    return Double.compare(sim2, sim1); // 降序排序
                })
                .limit(maxResults)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("向量数据库搜索失败", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 添加问题到数据库
     */
    public void addQuestion(InterviewQuestion question) {
        if (question != null && question.isValid()) {
            questionDatabase.add(question);
            log.info("问题已添加到向量数据库: {}", question.getQuestionId());
        }
    }
    
    /**
     * 批量添加问题
     */
    public void addQuestions(List<InterviewQuestion> questions) {
        if (questions != null) {
            questions.stream()
                .filter(InterviewQuestion::isValid)
                .forEach(this::addQuestion);
        }
    }
    
    /**
     * 根据ID获取问题
     */
    public InterviewQuestion getQuestionById(String questionId) {
        return questionDatabase.stream()
            .filter(q -> q.getQuestionId().equals(questionId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 计算向量相似度（余弦相似度）
     */
    private double calculateSimilarity(float[] vector1, float[] vector2) {
        if (vector1 == null || vector2 == null || vector1.length != vector2.length) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += Math.pow(vector1[i], 2);
            norm2 += Math.pow(vector2[i], 2);
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * 初始化示例问题
     */
    private void initializeSampleQuestions() {
        // Java技术问题
        addQuestion(createSampleQuestion(
            "java_basic_1", 
            "请解释Java中的多态性及其实现方式？", 
            "技术问题", "Java", "基础",
            "多态性是指同一个行为具有多个不同表现形式的能力。在Java中主要通过方法重载和方法重写实现...",
            "根据对多态性概念的理解深度和实际应用举例评分"
        ));
        
        addQuestion(createSampleQuestion(
            "java_spring_1", 
            "Spring框架的核心特性有哪些？", 
            "技术问题", "Spring", "基础",
            "Spring框架的核心特性包括IoC（控制反转）、AOP（面向切面编程）、事务管理、MVC框架等...",
            "根据对Spring框架特性的全面性和理解深度评分"
        ));
        
        addQuestion(createSampleQuestion(
            "java_database_1", 
            "请解释数据库事务的ACID特性？", 
            "技术问题", "数据库", "基础",
            "ACID特性包括原子性（Atomicity）、一致性（Consistency）、隔离性（Isolation）、持久性（Durability）...",
            "根据对ACID特性的准确理解和实际应用举例评分"
        ));
        
        // 行为面试问题
        addQuestion(createSampleQuestion(
            "behavioral_teamwork_1", 
            "请描述一个你在团队合作中遇到的挑战及如何解决的？", 
            "行为问题", "团队协作", "中级",
            "参考答案应包含具体情境、采取的行动、取得的成果和反思...",
            "根据案例的真实性、解决策略的有效性和反思深度评分"
        ));
        
        addQuestion(createSampleQuestion(
            "behavioral_problem_1", 
            "请描述一个你解决过的复杂技术问题？", 
            "行为问题", "问题解决", "高级",
            "参考答案应包含问题背景、分析过程、解决方案和实施结果...",
            "根据问题的复杂性、解决方法的创新性和实际效果评分"
        ));
        
        log.info("向量数据库初始化完成，共加载{}个问题", questionDatabase.size());
    }
    
    private InterviewQuestion createSampleQuestion(String id, String content, String type, 
                                                  String skillTag, String difficulty, 
                                                  String referenceAnswer, String criteria) {
        InterviewQuestion question = new InterviewQuestion();
        question.setQuestionId(id);
        question.setContent(content);
        question.setType(type);
        question.setSkillTag(skillTag);
        question.setDifficulty(difficulty);
        question.setReferenceAnswer(referenceAnswer);
        question.setEvaluationCriteria(criteria);
        question.setCreateTime(System.currentTimeMillis());
        
        // 生成模拟嵌入向量
        question.setEmbedding(generateMockEmbedding(content));
        
        return question;
    }
    
    private float[] generateMockEmbedding(String text) {
        // 生成384维的模拟向量
        float[] embedding = new float[384];
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] = (float) (Math.random() * 2 - 1);
        }
        
        // 归一化
        double norm = 0.0;
        for (float value : embedding) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] /= norm;
        }
        
        return embedding;
    }
}