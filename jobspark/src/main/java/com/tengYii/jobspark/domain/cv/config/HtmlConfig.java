package com.tengYii.jobspark.domain.cv.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * HTML 渲染配置：控制从 Markdown → CommonMark → HTML 阶段的输出细节。
 * HTML 作为中间格式，PDF 与 Docx 共用同一套 CSS。
 *
 * 编码：UTF-8；Java 17。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HtmlConfig {

    @Builder.Default
    private FormatConfig format = FormatConfig.defaults();

    /** CSS 资源路径（相对于 classpath）：templates/style/cv.css */
    @Builder.Default
    private String cssResourcePath = "templates/style/cv.css";

    /** 字体资源目录（相对于 classpath），PDF 阶段可用：templates/fonts */
    @Builder.Default
    private String fontsDirResourcePath = "templates/fonts";

    /** 是否双栏布局（通过 CSS grid 控制） */
    @Builder.Default
    private boolean twoColumnLayout = false;

    /** 是否显示头像 */
    @Builder.Default
    private boolean showAvatar = false;

    /** 是否显示社交链接 */
    @Builder.Default
    private boolean showSocial = true;

    /** HTML 中资源基准 URI（用于图片/字体的相对路径解析） */
    @Builder.Default
    private String baseUri = "";

    public static HtmlConfig defaults() {
        return HtmlConfig.builder().build();
    }
}