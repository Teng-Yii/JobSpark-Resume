package com.tengYii.jobspark.domain.cv.render;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.tengYii.jobspark.domain.cv.config.PdfConfig;
import com.tengYii.jobspark.domain.cv.errors.RenderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.lang.reflect.Method;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.w3c.dom.Document;

/**
 * HTML -> PDF (openhtmltopdf)
 * 说明：
 * - HTML/CSS 由 MarkdownService 生成。
 * - 字体：若 classpath:templates/fonts 存在有效 ttf/otf，则进行注册；否则使用系统字体栈。
 * - 输出：项目根 out/resume_YYYYMMDD_HHMM.pdf
 */
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    public File toPdf(String html, PdfConfig config, File outDir, String baseUri) {
        if (html == null) {
            throw RenderException.pdf("HTML 内容为空");
        }
        ensureDir(outDir);

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        File outFile = new File(outDir, config.getOutputNamePrefix() + "_" + ts + ".pdf");
        File debugHtml = new File(outDir, "debug_pdf_input.html");

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            // HTML 输入（JSoup -> W3C DOM，避免 XML/TRaX 严格性）
            Document w3cDoc = new W3CDom().fromJsoup(Jsoup.parse(html));
            builder.withW3cDocument(w3cDoc, baseUri == null ? "" : baseUri);
            // Dump HTML for diagnostics to inspect mismatched tags (e.g., SAXParseException line/col)
            try {
                java.nio.file.Files.writeString(debugHtml.toPath(), html, java.nio.charset.StandardCharsets.UTF_8);
                log.info("Wrote PDF input HTML to: {}", debugHtml.getAbsolutePath());
            } catch (Exception writeEx) {
                log.warn("Failed to write PDF input HTML: {}", writeEx.toString());
            }

            // 字体注册（可选）
            registerFonts(builder, config.getFontsDirResourcePath());

            // 依赖诊断（记录关键类来源与方法签名，用于定位 NoSuchMethodError/版本冲突）
            try {
                Class<?> ttfParserClz = Class.forName("org.apache.fontbox.ttf.TTFParser");
                String ttfSrc = (ttfParserClz.getProtectionDomain() != null
                        && ttfParserClz.getProtectionDomain().getCodeSource() != null
                        && ttfParserClz.getProtectionDomain().getCodeSource().getLocation() != null)
                        ? ttfParserClz.getProtectionDomain().getCodeSource().getLocation().toString()
                        : "null";
                boolean hasParseIS = false;
                for (Method m : ttfParserClz.getDeclaredMethods()) {
                    if (m.getName().equals("parse")) {
                        Class<?>[] ps = m.getParameterTypes();
                        if (ps.length == 1 && "java.io.InputStream".equals(ps[0].getName())) {
                            hasParseIS = true;
                        }
                    }
                }
                String pdfbxSrc = (PdfRendererBuilder.class.getProtectionDomain() != null
                        && PdfRendererBuilder.class.getProtectionDomain().getCodeSource() != null
                        && PdfRendererBuilder.class.getProtectionDomain().getCodeSource().getLocation() != null)
                        ? PdfRendererBuilder.class.getProtectionDomain().getCodeSource().getLocation().toString()
                        : "null";
                log.info("Diagnose PDF deps: TTFParser@{}, hasParse(InputStream)={}, PdfRendererBuilder@{}",
                        ttfSrc, hasParseIS, pdfbxSrc);
            } catch (Throwable diag) {
                log.debug("PDF deps diagnosis failed: {}", diag.toString());
            }

            // 渲染
            builder.toStream(fos);
            builder.run();

            if (outFile.length() == 0) {
                throw new IllegalStateException("生成的 PDF 文件大小为 0");
            }
            log.info("PDF 生成成功：{}", outFile.getAbsolutePath());
            return outFile;
        } catch (LinkageError e) {
            // 版本冲突或链接错误（如 pdfbox/fontbox 与 openhtmltopdf 版本不匹配）
            throw RenderException.pdf("PDF 渲染依赖版本冲突或链接错误: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        } catch (Exception e) {
            throw RenderException.pdf("HTML 转 PDF 失败: " + e.getMessage(), e);
        }
    }

    private void registerFonts(PdfRendererBuilder builder, String fontsDirResourcePath) {
        try {
            if (fontsDirResourcePath == null || fontsDirResourcePath.isBlank()) return;
            URL fontsDirUrl = getClass().getClassLoader().getResource(fontsDirResourcePath);
            if (fontsDirUrl == null) {
                log.info("未找到字体目录（可忽略）：{}", fontsDirResourcePath);
                return;
            }
            File fontsDir = new File(fontsDirUrl.toURI());
            if (!fontsDir.exists() || !fontsDir.isDirectory()) {
                log.info("字体目录不可用（可忽略）：{}", fontsDir.getAbsolutePath());
                return;
            }
            Files.list(fontsDir.toPath())
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase();
                        return n.endsWith(".ttf") || n.endsWith(".otf");
                    })
                    .forEach(p -> {
                        try {
                            String path = p.toFile().getAbsolutePath();
                            builder.useFont(p.toFile(), "custom", 400, BaseRendererBuilder.FontStyle.NORMAL, true);
                            log.info("注册字体: {}", path);
                        } catch (Exception ex) {
                            log.warn("字体注册失败（跳过）: {} - {}", p, ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.warn("字体目录扫描失败（将使用系统字体栈）：{}", e.getMessage());
        }
    }

    private void ensureDir(File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            throw RenderException.io("创建输出目录失败: " + dir.getAbsolutePath());
        }
    }
}