package com.tengYii.jobspark.domain.cv.render;

import com.tengYii.jobspark.domain.cv.config.DocxConfig;
import com.tengYii.jobspark.domain.cv.config.HtmlConfig;
import com.tengYii.jobspark.domain.cv.config.MarkdownConfig;
import com.tengYii.jobspark.domain.cv.config.PdfConfig;
import com.tengYii.jobspark.domain.cv.mapping.TemplateFieldMapper;
import com.tengYii.jobspark.domain.cv.model.Cv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 渲染门面：负责串联简历的完整离线渲染管线
 * FreeMarker(.ftl) -> Markdown -> CommonMark HTML -> openhtmltopdf PDF / docx4j ImportXHTML Docx
 *
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

    /** Cv -> Markdown */
    public String toMarkdown(Cv cv, MarkdownConfig markdownConfig, TemplateFieldMapper mapper) {
        String md = templateService.renderMarkdown(cv, markdownConfig, mapper);
        log.info("toMarkdown 完成，字数={}", md.length());
        return md;
    }

    /** Markdown -> HTML（嵌入 CSS） */
    public String toHtmlFromMarkdown(String markdown, HtmlConfig htmlConfig) {
        String html = markdownService.toHtmlFromMarkdown(markdown, htmlConfig);
        log.info("toHtmlFromMarkdown 完成，字数={}", html.length());
        return html;
    }

    /** HTML -> PDF（out 目录输出） */
    public File toPdf(String html, PdfConfig pdfConfig, File outDir, String baseUri) {
        File pdf = pdfService.toPdf(html, pdfConfig, outDir, baseUri);
        log.info("toPdf 完成：{}", pdf.getAbsolutePath());
        return pdf;
    }

    /** HTML -> Docx（out 目录输出） */
    public File toDocx(String html, DocxConfig docxConfig, File outDir, String baseUri) {
        File docx = docxService.toDocx(html, docxConfig, outDir, baseUri);
        log.info("toDocx 完成：{}", docx.getAbsolutePath());
        return docx;
    }
}