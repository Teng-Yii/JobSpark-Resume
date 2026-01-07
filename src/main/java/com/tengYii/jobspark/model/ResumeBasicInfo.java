package com.tengYii.jobspark.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简历基本信息值对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeBasicInfo {
    
    /**
     * 姓名
     */
    private String name;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 电话
     */
    private String phone;
    
    /**
     * 地点
     */
    private String location;
    
    /**
     * 个人简介
     */
    private String summary;
    
    /**
     * 目标职位
     */
    private String targetPosition;
    
    /**
     * 当前职位
     */
    private String currentPosition;
    
    /**
     * 工作年限
     */
    private String workExperience;
    
    /**
     * 验证基本信息有效性
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty();
    }
}