package com.tengYii.jobspark.model.cv;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 版式与渲染元数据：用于模板与 CSS 控制版式一致性。
 * HTML 为中间格式，PDF/Docx 共用同一套 CSS。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormatMeta {
    private String alignment;         // 对齐：left/center/right
    private Double lineSpacing;       // 行间距倍数
    private String fontFamily;        // 字体栈
    private String datePattern;       // 日期格式（如 yyyy.MM）
    private String hyperlinkStyle;    // 超链接样式（underline/none 等）
    private Boolean showAvatar;       // 是否显示头像
    private Boolean showSocial;       // 是否显示社交链接
    private Boolean twoColumnLayout;  // 是否双栏布局
    private LocaleConfig localeConfig;// 国际化配置
}