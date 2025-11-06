package com.tengYii.jobspark.cv.render;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.tengYii.jobspark.cv.config.PdfConfig;
import com.tengYii.jobspark.cv.errors.RenderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.usePdfUaAccessbility(false);


            // HTML 输入
            builder.withHtmlContent(html, baseUri == null ? "" : baseUri);

            // 字体注册（可选）
            registerFonts(builder, config.getFontsDirResourcePath());

            // 渲染
            builder.toStream(fos);
            builder.run();

            if (outFile.length() == 0) {
                throw new IllegalStateException("生成的 PDF 文件大小为 0");
            }
            log.info("PDF 生成成功：{}", outFile.getAbsolutePath());
            return outFile;
        } catch (NoClassDefFoundError e) {
            // 依赖缺失时的友好错误
            throw RenderException.pdf("缺少 openhtmltopdf 相关依赖，请在联网后执行 Maven 依赖解析", e);
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