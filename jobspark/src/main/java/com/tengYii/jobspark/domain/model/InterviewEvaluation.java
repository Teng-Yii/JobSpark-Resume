package com.tengYii.jobspark.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 面试评估结果值对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewEvaluation {
    
    /**
     * 总体评分（1-10分）
     */
    private double overallScore;
    
    /**
     * 技术能力评分
     */
    private double technicalScore;
    
    /**
     * 沟通能力评分
     */
    private double communicationScore;
    
    /**
     * 问题解决能力评分
     */
    private double problemSolvingScore;
    
    /**
     * 团队协作评分
     */
    private double teamworkScore;
    
    /**
     * 优势分析
     */
    private String strengths;
    
    /**
     * 改进建议
     */
    private String improvementSuggestions;
    
    /**
     * 详细评估结果
     */
    private Map<String, Object> detailedEvaluation;
    
    /**
     * 创建评估结果
     */
    public InterviewEvaluation(double overallScore, double technicalScore, 
                              double communicationScore, double problemSolvingScore,
                              double teamworkScore, String strengths, 
                              String improvementSuggestions) {
        this.overallScore = overallScore;
        this.technicalScore = technicalScore;
        this.communicationScore = communicationScore;
        this.problemSolvingScore = problemSolvingScore;
        this.teamworkScore = teamworkScore;
        this.strengths = strengths;
        this.improvementSuggestions = improvementSuggestions;
        this.detailedEvaluation = new HashMap<>();
    }
    
    /**
     * 添加详细评估项
     */
    public void addDetailedEvaluation(String key, Object value) {
        if (detailedEvaluation == null) {
            detailedEvaluation = new HashMap<>();
        }
        detailedEvaluation.put(key, value);
    }
    
    /**
     * 验证评估结果有效性
     */
    public boolean isValid() {
        return overallScore >= 0 && overallScore <= 10 &&
               technicalScore >= 0 && technicalScore <= 10 &&
               communicationScore >= 0 && communicationScore <= 10 &&
               problemSolvingScore >= 0 && problemSolvingScore <= 10 &&
               teamworkScore >= 0 && teamworkScore <= 10;
    }
}