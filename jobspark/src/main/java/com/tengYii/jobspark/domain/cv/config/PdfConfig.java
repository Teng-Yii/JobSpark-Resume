package com.tengYii.jobspark.domain.cv.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * PDF 渲染配置（openhtmltopdf）
 * - 页面大小/边距
 * - 字体注册目录
 * - 是否严格分页（长列表分页控制）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfConfig {

    @Builder.Default
    private FormatConfig format = FormatConfig.defaults();

    /** A4 页面尺寸（仅说明，真正尺寸由 CSS @page 设置） */
    @Builder.Default
    private String pageSize = "A4";

    /** 页边距（CSS 中通过 @page 使用，但此处保留供模板/样式选择） */
    @Builder.Default
    private String marginTop = "20mm";
    @Builder.Default
    private String marginRight = "15mm";
    @Builder.Default
    private String marginBottom = "20mm";
    @Builder.Default
    private String marginLeft = "15mm";

    /** 字体目录（classpath 相对路径），默认 templates/fonts */
    @Builder.Default
    private String fontsDirResourcePath = "templates/fonts";

    /** 是否在 PDF 中更严格的分页控制（列表避免内部分页） */
    @Builder.Default
    private boolean strictPageBreak = true;

    /** 输出文件名前缀（out/resume_YYYYMMDD_HHMM） */
    @Builder.Default
    private String outputNamePrefix = "resume";

    public static PdfConfig defaults() {
        return PdfConfig.builder().build();
    }
}