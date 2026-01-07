package com.tengYii.jobspark.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 技能实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Skill {
    
    /**
     * 技能名称
     */
    private String skillName;
    
    /**
     * 熟练程度
     */
    private String proficiency;
    
    /**
     * 类别
     */
    private String category;
    
    /**
     * 描述
     */
    private String description;
    
    /**
     * 验证技能有效性
     */
    public boolean isValid() {
        return skillName != null && !skillName.trim().isEmpty();
    }
}