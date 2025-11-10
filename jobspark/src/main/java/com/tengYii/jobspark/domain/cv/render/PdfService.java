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
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.w3c.dom.Document;

/**
 * HTML -> PDF (openhtmltopdf)
 * 说明：
 * - HTML/CSS 由 MarkdownService 生成。
 * - 字体：若 classpath:templates/fonts 存在有效 ttf/otf，则进行注册；否则使用系统字体栈。
 * - 输出：指定目录下的PDF文件
 */
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    /**
     * 将HTML内容转换为PDF文件
     *
     * @param html HTML内容（必须是完整的HTML文档）
     * @param config PDF配置
     * @param outDir 输出目录
     * @param fileName 输出文件名（包含.pdf后缀）
     * @return 生成的PDF文件
     */
    public File toPdf(String html, PdfConfig config, File outDir, String fileName) {
        if (html == null || html.trim().isEmpty()) {
            throw RenderException.pdf("HTML 内容为空");
        }
        ensureDir(outDir);

        File outFile = new File(outDir, fileName);
        File debugHtml = new File(outDir, "debug_pdf_input.html");

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            // HTML 输入（JSoup -> W3C DOM，避免 XML/TRaX 严格性）
            Document w3cDoc = new W3CDom().fromJsoup(Jsoup.parse(html));

            // 设置基础URI为输出目录，用于解析HTML中的相对路径资源
            String baseUri = outDir.toURI().toString();
            builder.withW3cDocument(w3cDoc, baseUri);

            // 保存调试用的HTML文件
            try {
                Files.writeString(debugHtml.toPath(), html, java.nio.charset.StandardCharsets.UTF_8);
                log.info("已保存调试HTML: {}", debugHtml.getAbsolutePath());
            } catch (Exception writeEx) {
                log.warn("保存调试HTML失败: {}", writeEx.getMessage());
            }

            // 注册字体
            registerFonts(builder, config.getFontsDirResourcePath());

            // 渲染PDF
            builder.toStream(fos);
            builder.run();

            if (outFile.length() == 0) {
                throw new IllegalStateException("生成的 PDF 文件大小为 0");
            }
            log.info("PDF 生成成功：{}", outFile.getAbsolutePath());
            return outFile;
        } catch (LinkageError e) {
            throw RenderException.pdf("PDF 渲染依赖版本冲突: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("HTML 转 PDF 失败", e);
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
                            String fileName = p.getFileName().toString();
                            // 使用 "Noto Sans SC" 作为字体族名，与CSS中的font-family保持一致
                            String fontFamily = "Noto Sans SC";
                            builder.useFont(p.toFile(), fontFamily, 400, BaseRendererBuilder.FontStyle.NORMAL, true);
                            log.info("注册字体: {} -> {}", fileName, fontFamily);
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
