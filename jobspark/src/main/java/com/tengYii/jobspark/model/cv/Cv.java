package com.tengYii.jobspark.model.cv;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 简历BO对象，简历解析后的结构化结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cv {

    // 基本信息
    /**
     * 姓名（必填）
     */
    private String name;
    /**
     * 年龄（可选）
     */
    private Integer age;                     //
    private String title;                    // 期望岗位/头衔（可选）
    private String avatarUrl;                // 头像URL（可选）
    private RichText summary;                // 个人摘要（Markdown）

    // 联系方式与社交
    private Contact contact;                 // 联系方式（至少一种）
    private List<Link> socialLinks;          // 社交链接（GitHub、博客等）

    // 教育/经历/项目/技能/证书
    private List<Education> educations;      // 教育经历（至少一项）
    private List<Experience> experiences;    // 工作/实习经历（至少一项）
    private List<Project> projects;          // 项目经验
    private List<Skill> skills;              // 技能或亮点
    private List<Certificate> certificates;  // 证书/获奖

    // 版式与国际化元数据
    private FormatMeta meta;                 // 对齐、列表、间距、字体、日期格式、链接样式、图标占位、locale 等


    private static FormatMeta buildFormatMeta() {
        String datePattern = "yyyy.MM";
        return FormatMeta.builder()
                .alignment("left")
                .lineSpacing(1.4)
                .fontFamily("\"Noto Sans SC\", \"PingFang SC\", \"Microsoft YaHei\", \"SimSun\", sans-serif")
                .datePattern(datePattern)
                .hyperlinkStyle("underline")
                .showAvatar(false)
                .showSocial(true)
                .twoColumnLayout(false)
                .localeConfig(LocaleConfig.builder()
                        .locale("zh-CN")
                        .datePattern(datePattern) // 复用日期格式，避免不一致
                        .build())
                .build();
    }
}