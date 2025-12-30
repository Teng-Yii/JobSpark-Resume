package com.tengYii.jobspark.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 简历BO对象，简历解析后的结构化结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CvBO {

    /**
     * 用户ID
     */
    private Long userId;

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

    /**
     * 优化建议
     */
    private String advice;

    /**
     * 优化历史记录
     */
    private List<OptimizationRecord> optimizationHistory;

    /**
     * 增加优化记录
     *
     * @param feedback 优化反馈
     * @param score    优化评分
     */
    public void addOptimizationRecord(String feedback, Double score) {
        if (Objects.isNull(this.optimizationHistory)) {
            this.optimizationHistory = new ArrayList<>();
        }
        this.optimizationHistory.add(OptimizationRecord.builder()
                .feedback(feedback)
                .score(score)
                .build());
    }

    /**
     * 优化记录内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptimizationRecord {
        /**
         * 反馈建议
         */
        private String feedback;

        /**
         * 评分
         */
        private Double score;
    }

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