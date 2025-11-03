package com.tengYii.jobspark.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 教育经历实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EducationExperience {
    
    /**
     * 学校名称
     */
    private String school;
    
    /**
     * 学位
     */
    private String degree;
    
    /**
     * 专业
     */
    private String major;
    
    /**
     * 时间
     */
    private String period;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 验证教育经历有效性
     */
    public boolean isValid() {
        return school != null && !school.trim().isEmpty() &&
               degree != null && !degree.trim().isEmpty();
    }
}