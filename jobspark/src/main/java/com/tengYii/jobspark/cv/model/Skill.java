package com.tengYii.jobspark.cv.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 技能/亮点
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Skill {
    private String name;   // 技能名称，如 Java/Spring/Redis
    private String level;  // 熟练度（可选）：熟练/良好/了解
}