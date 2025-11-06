package com.tengYii.jobspark.cv.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Docx 渲染配置（docx4j ImportXHTML）
 * - 页边距与标题样式偏好
 * - 字体族（Word 中可再替换为系统可用中文字体）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocxConfig {

    @Builder.Default
    private FormatConfig format = FormatConfig.defaults();

    /** 页边距（单位：twips 或说明性字符串；真实边距由 docx4j 默认/模板决定，这里仅用于策略选择） */
    @Builder.Default
    private String marginTop = "20mm";
    @Builder.Default
    private String marginRight = "15mm";
    @Builder.Default
    private String marginBottom = "20mm";
    @Builder.Default
    private String marginLeft = "15mm";

    /** 标题样式大号显示 */
    @Builder.Default
    private boolean largeHeadings = true;

    /** 是否双栏布局（ImportXHTML 对复杂布局支持有限，默认 false） */
    @Builder.Default
    private boolean twoColumnLayout = false;

    /** 是否显示头像（建议关闭以提升兼容性） */
    @Builder.Default
    private boolean showAvatar = false;

    public static DocxConfig defaults() {
        return DocxConfig.builder().build();
    }
}