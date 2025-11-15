package com.tengYii.jobspark.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作经历实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkExperience {
    
    /**
     * 公司名称
     */
    private String company;
    
    /**
     * 职位
     */
    private String position;
    
    /**
     * 时间
     */
    private String period;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 成就
     */
    private String achievements;
    
    /**
     * 验证工作经历有效性
     */
    public boolean isValid() {
        return company != null && !company.trim().isEmpty() &&
               position != null && !position.trim().isEmpty();
    }
}