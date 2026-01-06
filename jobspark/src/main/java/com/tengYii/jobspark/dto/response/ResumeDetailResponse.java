package com.tengYii.jobspark.dto.response;

import com.tengYii.jobspark.model.bo.CertificateBO;
import com.tengYii.jobspark.model.bo.ContactBO;
import com.tengYii.jobspark.model.bo.CvBO;
import com.tengYii.jobspark.model.bo.EducationBO;
import com.tengYii.jobspark.model.bo.ExperienceBO;
import com.tengYii.jobspark.model.bo.FormatMetaBO;
import com.tengYii.jobspark.model.bo.ProjectBO;
import com.tengYii.jobspark.model.bo.SkillBO;
import com.tengYii.jobspark.model.bo.SocialLinkBO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历详情响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 简历ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 姓名
     */
    private String name;

    /**
     * 出生日期
     */
    private LocalDate birthDate;

    /**
     * 期望岗位/头衔
     */
    private String title;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 个人摘要
     */
    private String summary;

    /**
     * 联系方式
     */
    private ContactBO contact;

    /**
     * 社交链接
     */
    private List<SocialLinkBO> socialLinks;

    /**
     * 教育经历
     */
    private List<EducationBO> educations;

    /**
     * 工作经历
     */
    private List<ExperienceBO> experiences;

    /**
     * 项目经验
     */
    private List<ProjectBO> projects;

    /**
     * 专业技能
     */
    private List<SkillBO> skills;

    /**
     * 证书/获奖
     */
    private List<CertificateBO> certificates;
}