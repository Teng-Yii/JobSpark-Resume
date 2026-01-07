package com.tengYii.jobspark.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简历格式元数据BO
 * 版式与渲染元数据：用于模板与 CSS 控制版式一致性。
 * HTML 为中间格式，PDF/Docx 共用同一套 CSS。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormatMetaBO {

    /**
     * 简历主题（如：简约/商务）
     */
    private String theme;

    /**
     * 对齐方式：left/center/right
     */
    private String alignment;

    /**
     * 行间距倍数
     */
    private Double lineSpacing;

    /**
     * 字体栈
     */
    private String fontFamily;

    /**
     * 日期格式
     */
    private String datePattern;

    /**
     * 超链接样式：underline/none
     */
    private String hyperlinkStyle;

    /**
     * 是否显示头像
     */
    private Boolean showAvatar;

    /**
     * 是否显示社交链接
     */
    private Boolean showSocial;

    /**
     * 是否双栏布局
     */
    private Boolean twoColumnLayout;

    /**
     * 国际化配置bo对象
     */
    private LocaleConfigBO localeConfig;
}