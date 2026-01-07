package com.tengYii.jobspark.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 面试问题实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestion {
    
    /**
     * 问题ID
     */
    private String questionId;
    
    /**
     * 问题内容
     */
    private String content;
    
    /**
     * 问题类型（技术问题、行为问题、情景问题等）
     */
    private String type;
    
    /**
     * 技能标签
     */
    private String skillTag;
    
    /**
     * 难度级别（初级、中级、高级）
     */
    private String difficulty;
    
    /**
     * 参考答案
     */
    private String referenceAnswer;
    
    /**
     * 评分标准
     */
    private String evaluationCriteria;
    
    /**
     * 向量嵌入（用于相似度检索）
     */
    private float[] embedding;
    
    /**
     * 创建时间
     */
    private Long createTime;
    
    /**
     * 更新问题向量嵌入
     */
    public void updateEmbedding(float[] newEmbedding) {
        this.embedding = newEmbedding;
    }
    
    /**
     * 验证问题有效性
     */
    public boolean isValid() {
        return content != null && !content.trim().isEmpty() && 
               type != null && !type.trim().isEmpty();
    }
}