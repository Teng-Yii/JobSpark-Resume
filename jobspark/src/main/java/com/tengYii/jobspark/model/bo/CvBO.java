package com.tengYii.jobspark.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 简历BO对象，简历解析后的结构化结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvBO {

    /**
     * 姓名（必填）
     */
    private String name;
    /**
     * 出生日期（用于计算年龄，可选）
     */
    private LocalDate birthDate;

    /**
     * 期望岗位/头衔（可选）
     */
    private String title;

    /**
     * 头像URL（可选）
     */
    private String avatarUrl;

    /**
     * 个人摘要（Markdown格式）
     */
    private String summary;

    /**
     * 联系方式与社交
     */
    private ContactBO contact;

    /**
     * 社交链接名称（如：GitHub/CSDN）
     */
    private List<SocialLinkBO> socialLinks;

    /**
     * 教育经历
     */
    private List<EducationBO> educations;

    /**
     * 工作/实习经历
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

    // 版式与国际化元数据
    // 对齐、列表、间距、字体、日期格式、链接样式、图标占位、locale 等
    private FormatMetaBO meta;


    private static FormatMetaBO buildFormatMeta() {
        String datePattern = "yyyy.MM";
        return FormatMetaBO.builder()
                .alignment("left")
                .lineSpacing(1.4)
                .fontFamily("\"Noto Sans SC\", \"PingFang SC\", \"Microsoft YaHei\", \"SimSun\", sans-serif")
                .datePattern(datePattern)
                .hyperlinkStyle("underline")
                .showAvatar(false)
                .showSocial(true)
                .twoColumnLayout(false)
                .localeConfig(LocaleConfigBO.builder()
                        .locale("zh-CN")
                        .datePattern(datePattern) // 复用日期格式，避免不一致
                        .build())
                .build();
    }
}