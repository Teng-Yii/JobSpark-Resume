package com.tengYii.jobspark.cv.render;

import com.tengYii.jobspark.cv.config.HtmlConfig;
import com.tengYii.jobspark.cv.errors.RenderException;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Markdown -> HTML（CommonMark）
 * 同时将 CSS 嵌入到 HTML 的 head，保证 PDF 与 Docx 共用同一套样式。
 */
public class MarkdownService {

    private static final Logger log = LoggerFactory.getLogger(MarkdownService.class);

    public String toHtmlFromMarkdown(String markdown, HtmlConfig config) {
        if (markdown == null) {
            throw RenderException.markdown("Markdown 内容为空");
        }
        try {
            Parser parser = Parser.builder().build();
            Node document = parser.parse(markdown);
            HtmlRenderer renderer = HtmlRenderer.builder()
                    .escapeHtml(false)
                    .build();
            String bodyHtml = renderer.render(document);

            String css = loadCss(config.getCssResourcePath());
            String containerClass = config.isTwoColumnLayout() ? "container two-column" : "container";
            String showAvatarClass = config.isShowAvatar() ? "show-avatar" : "hide-avatar";
            String showSocialClass = config.isShowSocial() ? "show-social" : "hide-social";

            String html = """
                    <!DOCTYPE html>
                    <html lang="zh-CN">
                    <head>
                      <meta charset="UTF-8">
                      <meta name="viewport" content="width=device-width, initial-scale=1.0">
                      <style>
                      %s
                      </style>
                    </head>
                    <body class="resume-body %s %s">
                      <div class="%s">
                        %s
                      </div>
                    </body>
                    </html>
                    """.formatted(css, showAvatarClass, showSocialClass, containerClass, bodyHtml);

            log.info("Markdown converted to HTML, length={}", html.length());
            return html;
        } catch (Exception e) {
            throw RenderException.markdown("Markdown 转 HTML 失败: " + e.getMessage(), e);
        }
    }

    private String loadCss(String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                log.warn("CSS 资源未找到：{}", resourcePath);
                return defaultCss();
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                return sb.toString();
            }
        } catch (Exception e) {
            log.warn("CSS 加载失败，使用默认样式: {}", e.getMessage());
            return defaultCss();
        }
    }

    private String defaultCss() {
        return """
                :root {
                  --font-stack: "Noto Sans SC","PingFang SC","Microsoft YaHei","SimSun",sans-serif;
                }
                @page { size: A4; margin: 20mm 15mm; }
                body { font-family: var(--font-stack); color: #222; }
                .container { max-width: 800px; margin: 0 auto; }
                h1,h2,h3 { margin: 0.2em 0; }
                ul { margin: 0.4em 0 0.4em 1.2em; }
                .two-column { display: grid; grid-template-columns: 2fr 1fr; gap: 16px; }
                """;
    }
}