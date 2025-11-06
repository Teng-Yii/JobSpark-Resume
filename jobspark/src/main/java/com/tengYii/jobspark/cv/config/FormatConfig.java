package com.tengYii.jobspark.cv.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用版式配置（与具体输出无关），供模板、HTML/CSS 使用。
 * 具体到 Markdown/HTML/PDF/DOCX 的差异，放在对应 *Config 中。
 *
 * 编码：UTF-8；Java 17。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormatConfig {

    @Builder.Default
    private String fontFamilyStack = "\"Noto Sans SC\", \"PingFang SC\", \"Microsoft YaHei\", \"SimSun\", sans-serif";

    @Builder.Default
    private String datePattern = "yyyy.MM";

    @Builder.Default
    private String alignment = "left"; // left/center/right

    @Builder.Default
    private double lineSpacing = 1.4;

    @Builder.Default
    private String hyperlinkStyle = "underline"; // underline / none; color由CSS控制

    @Builder.Default
    private boolean showAvatar = false;

    @Builder.Default
    private boolean showSocial = true;

    @Builder.Default
    private boolean twoColumnLayout = false;

    @Builder.Default
    private String locale = "zh-CN";

    public static FormatConfig defaults() {
        return FormatConfig.builder().build();
    }
}