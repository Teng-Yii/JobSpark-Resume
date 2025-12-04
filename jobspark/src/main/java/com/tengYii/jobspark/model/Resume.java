package com.tengYii.jobspark.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历聚合根
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Resume {

    /**
     * 简历ID
     */
    private String resumeId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 原始内容
     */
    private String originalContent;

    /**
     * 解析后的内容
     */
    private String parsedContent;

    /**
     * 基本信息
     */
    private ResumeBasicInfo basicInfo;

    /**
     * 教育经历列表
     */
    private List<EducationExperience> educationExperiences;

    /**
     * 工作经历列表
     */
    private List<WorkExperience> workExperiences;

    /**
     * 技能列表
     */
    private List<Skill> skills;

    /**
     * 分析状态
     */
    private String analysisStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 构造函数
     */
    public Resume(String resumeId, String fileName, String originalContent) {
        this.resumeId = resumeId;
        this.fileName = fileName;
        this.originalContent = originalContent;
        this.analysisStatus = "未分析";

        LocalDateTime nowTime = LocalDateTime.now();
        this.createTime = nowTime;
        this.updateTime = nowTime;
    }

    /**
     * 标记为已分析
     */
    public void markAsAnalyzed(String parsedContent, ResumeBasicInfo basicInfo,
                               List<EducationExperience> educationExperiences,
                               List<WorkExperience> workExperiences, List<Skill> skills) {
        this.parsedContent = parsedContent;
        this.basicInfo = basicInfo;
        this.educationExperiences = educationExperiences;
        this.workExperiences = workExperiences;
        this.skills = skills;
        this.analysisStatus = "已分析";
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 验证简历有效性
     */
    public boolean isValid() {
        return resumeId != null && !resumeId.trim().isEmpty() &&
                fileName != null && !fileName.trim().isEmpty() &&
                originalContent != null && !originalContent.trim().isEmpty();
    }

    /**
     * 检查是否已分析
     */
    public boolean isAnalyzed() {
        return "已分析".equals(analysisStatus);
    }
}