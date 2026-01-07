package com.tengYii.jobspark.domain.render;

import com.tengYii.jobspark.config.cv.DocxConfig;
import com.tengYii.jobspark.config.cv.HtmlConfig;
import com.tengYii.jobspark.config.cv.MarkdownConfig;
import com.tengYii.jobspark.config.cv.PdfConfig;
import com.tengYii.jobspark.domain.render.doc.DocxService;
import com.tengYii.jobspark.domain.render.markdown.MarkdownService;
import com.tengYii.jobspark.domain.render.markdown.TemplateFieldMapper;
import com.tengYii.jobspark.domain.render.markdown.TemplateService;
import com.tengYii.jobspark.domain.render.pdf.PdfService;
import com.tengYii.jobspark.model.bo.CvBO;
import org.commonmark.parser.Parser;
import org.commonmark.node.Node;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 渲染门面：负责串联简历的完整离线渲染管线
 * FreeMarker(.ftl) -> Markdown -> CommonMark HTML -> openhtmltopdf PDF / docx4j ImportXHTML Docx
 * <p>
 * 重要：不要将渲染方法硬编码到模型类里，本门面聚合各服务。
 */
public class CvRendererFacade {

    private static final Logger log = LoggerFactory.getLogger(CvRendererFacade.class);

    private final TemplateService templateService;
    private final MarkdownService markdownService;
    private final PdfService pdfService;
    // DocxService 在后续文件中提供
    private final DocxService docxService;

    public CvRendererFacade() {
        this.templateService = new TemplateService();
        this.markdownService = new MarkdownService();
        this.pdfService = new PdfService();
        this.docxService = new DocxService();
    }

    /**
     * CvBO -> Markdown
     */
    public String toMarkdown(CvBO cv, MarkdownConfig markdownConfig, TemplateFieldMapper mapper) {
        String md = templateService.renderMarkdown(cv, markdownConfig, mapper);
        log.info("toMarkdown 完成，字数={}", md.length());
        return md;
    }

    /**
     * Markdown -> HTML（嵌入 CSS）
     */
    public String toHtmlFromMarkdown(String markdown, HtmlConfig htmlConfig) {
        String html = markdownService.toHtmlFromMarkdown(markdown, htmlConfig);
        log.info("toHtmlFromMarkdown 完成，字数={}", html.length());
        return html;
    }

    /**
     * Markdown -> HTML（嵌入 CSS）
     */
    public String toHtmlFromMarkdown(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return "<html><body></body></html>"; // 空输入返回空HTML，避免解析报错
        }

        // 1. 原有逻辑不变：Markdown → HTML 内容片段
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder()
                .escapeHtml(true) // 保留防XSS转义
                .softbreak("<br/>") // 保留软换行转<br/>
                .build();
        String contentHtml = renderer.render(document); // 原有片段（如<h1>张三</h1>...）

        // 2. 关键修改：给片段包裹标准 HTML 框架
        String fullHtml = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <!-- 方案1：XML 完整闭合标签（推荐，兼容性最好） -->
                <meta charset="UTF-8"></meta>
                <!-- 方案2：XML 自闭合标签（也支持，语法更简洁）-->
                <!-- <meta charset="UTF-8"/> -->
                
                <!-- 样式部分保持不变 -->
                <style>
                    body {
                        font-family: "Noto Sans SC", "微软雅黑", sans-serif;
                        line-height: 1.6;
                        margin: 0;
                        padding: 20px;
                    }
                    h1, h2, h3 {
                        margin: 16px 0 8px 0;
                        font-weight: bold;
                    }
                    ul {
                        margin: 8px 0;
                        padding-left: 24px;
                    }
                    a {
                        color: #0066cc;
                        text-decoration: underline;
                    }
                </style>
            </head>
            <body>
                %s
            </body>
            </html>
            """.formatted(contentHtml);

        return fullHtml;
    }

    /**
     * HTML -> PDF（out 目录输出）
     */
    public File toPdf(String html, PdfConfig pdfConfig, File outDir, String baseUri) {
        File pdf = pdfService.toPdf(html, pdfConfig, outDir, baseUri);
        log.info("toPdf 完成：{}", pdf.getAbsolutePath());
        return pdf;
    }

    private File getFile(String html, PdfConfig pdfConfig, File outDir, String baseUri) {
        File pdf = pdfService.toPdf(html, pdfConfig, outDir, baseUri);
        return pdf;
    }


    /**
     * HTML -> Docx（out 目录输出）
     */
    public File toDocx(String html, DocxConfig docxConfig, File outDir, String baseUri) {
        File docx = docxService.toDocx(html, docxConfig, outDir, baseUri);
        log.info("toDocx 完成：{}", docx.getAbsolutePath());
        return docx;
    }
}