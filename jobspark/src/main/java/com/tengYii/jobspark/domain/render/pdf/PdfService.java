package com.tengYii.jobspark.domain.render.pdf;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.tengYii.jobspark.common.exception.RenderException;
import com.tengYii.jobspark.config.cv.PdfConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Objects;
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
     * 增强版本：包含详细的调试日志和字体验证
     *
     * @param html HTML内容（必须是完整的HTML文档）
     * @param config PDF配置
     * @param outDir 输出目录
     * @param fileName 输出文件名（包含.pdf后缀）
     * @return 生成的PDF文件
     */
    public File toPdf(String html, PdfConfig config, File outDir, String fileName) {
        log.info("=== 开始PDF转换 ===");
        log.info("输出目录: {}", outDir.getAbsolutePath());
        log.info("文件名: {}", fileName);

        if (StringUtils.isEmpty(html)) {
            throw RenderException.pdf("HTML 内容为空");
        }

        // 检查HTML内容中是否包含中文字符
        boolean containsChinese = html.chars().anyMatch(ch ->
                Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN);
        log.info("HTML内容包含中文字符: {}", containsChinese);
        log.info("HTML内容长度: {} 字符", html.length());

        ensureDir(outDir);

        // 确保文件名有.pdf后缀
        if (StringUtils.isNotEmpty(fileName) && !fileName.toLowerCase().endsWith(".pdf")) {
            fileName = fileName + ".pdf";
        }
        File outFile = new File(outDir, fileName);
        File debugHtml = new File(outDir, "debug_pdf_input.html");

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            log.info("PDF渲染器配置: FastMode = true");

            // HTML 输入（JSoup -> W3C DOM，避免 XML/TRaX 严格性）
            log.info("开始解析HTML内容...");
            Document w3cDoc = new W3CDom().fromJsoup(Jsoup.parse(html));
            log.info("HTML解析完成，DOM节点数量: {}", w3cDoc.getChildNodes().getLength());

            // 设置基础URI为输出目录，用于解析HTML中的相对路径资源
            String baseUri = outDir.toURI().toString();
            log.info("设置基础URI: {}", baseUri);
            builder.withW3cDocument(w3cDoc, baseUri);

            // 保存调试用的HTML文件
            try {
                Files.writeString(debugHtml.toPath(), html, java.nio.charset.StandardCharsets.UTF_8);
                log.info("✓ 已保存调试HTML文件: {}", debugHtml.getAbsolutePath());

                // 验证保存的HTML文件编码
                String savedContent = Files.readString(debugHtml.toPath(), java.nio.charset.StandardCharsets.UTF_8);
                boolean savedContainsChinese = savedContent.chars().anyMatch(ch ->
                        Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN);
                log.info("保存的HTML文件包含中文字符: {}", savedContainsChinese);

            } catch (Exception writeEx) {
                log.error("✗ 保存调试HTML失败: {}", writeEx.getMessage(), writeEx);
            }

            try {
                log.info("开始注册字体...");
                // 注册字体
                registerFonts(builder, config.getFontsDirResourcePath());
                log.info("字体注册完成");

                log.info("开始PDF渲染...");
                // 渲染PDF
                builder.toStream(fos);
                builder.run();
                log.info("PDF渲染完成");

            } catch (Exception e) {
                log.error("✗ PDF 渲染过程失败", e);
                throw new RuntimeException("PDF渲染失败: " + e.getMessage(), e);
            }

            long fileSize = outFile.length();
            if (fileSize == 0) {
                throw new IllegalStateException("生成的 PDF 文件大小为 0");
            }

            log.info("✓ PDF 生成成功");
            log.info("  文件路径: {}", outFile.getAbsolutePath());
            log.info("  文件大小: {} bytes ({} KB)", fileSize, fileSize / 1024);
            log.info("=== PDF转换完成 ===");

            return outFile;

        } catch (LinkageError e) {
            log.error("✗ PDF 渲染依赖版本冲突", e);
            throw RenderException.pdf("PDF 渲染依赖版本冲突: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("✗ HTML 转 PDF 失败", e);
            throw RenderException.pdf("HTML 转 PDF 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 注册字体文件到PDF渲染器
     * 增强版本：包含详细的调试日志和完整的字体权重映射
     *
     * @param builder PDF渲染器构建器
     * @param fontsDirResourcePath 字体目录资源路径
     */
    private void registerFonts(PdfRendererBuilder builder, String fontsDirResourcePath) {
        try {
            if (StringUtils.isEmpty(fontsDirResourcePath)) {
                log.warn("字体目录路径为空，将使用系统默认字体");
                return;
            }

            log.info("开始注册字体，字体目录: {}", fontsDirResourcePath);
            URL fontsDirUrl = getClass().getClassLoader().getResource(fontsDirResourcePath);

            if (Objects.isNull(fontsDirUrl)) {
                log.error("字体目录资源未找到: {}", fontsDirResourcePath);
                return;
            }

            File fontsDir = new File(fontsDirUrl.toURI());
            log.info("字体目录绝对路径: {}", fontsDir.getAbsolutePath());

            if (!fontsDir.exists() || !fontsDir.isDirectory()) {
                log.error("字体目录不存在或不是目录: {}", fontsDir.getAbsolutePath());
                return;
            }

            // 统计注册成功的字体数量
            final int[] registeredCount = {0};
            final int[] totalCount = {0};

            Files.list(fontsDir.toPath())
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".ttf"))
                    .forEach(p -> {
                        totalCount[0]++;
                        try {
                            String fileName = p.getFileName().toString();
                            String fileNameLower = fileName.toLowerCase();

                            // 解析字体family和权重信息
                            FontInfo fontInfo = parseFontInfo(fileName);

                            log.info("解析字体文件: {} -> Family: {}, Weight: {}, Style: {}",
                                    fileName, fontInfo.family, fontInfo.weight, fontInfo.style);

                            // 注册字体到PDF渲染器，启用字体子集化以减小PDF文件大小
                            // 第五个参数设为true表示启用字体子集化，只嵌入实际使用的字符
                            builder.useFont(p.toFile(), fontInfo.family, fontInfo.weight, fontInfo.style, true);

                            registeredCount[0]++;
                            log.info("✓ 字体注册成功: {} -> {} (weight={}, style={}, subset=true)",
                                    fileName, fontInfo.family, fontInfo.weight, fontInfo.style);

                            // 验证字体文件是否包含中文字符支持
                            validateChineseFontSupport(p.toFile(), fontInfo);

                        } catch (Exception ex) {
                            log.error("✗ 字体注册失败: {} - {}", p.getFileName(), ex.getMessage(), ex);
                        }
                    });

            log.info("字体注册完成，成功注册 {}/{} 个字体文件", registeredCount[0], totalCount[0]);

            // 验证字体目录中的所有文件
            logFontDirectoryContents(fontsDir);

        } catch (Exception e) {
            log.error("字体目录扫描失败，将使用系统字体栈: {}", e.getMessage(), e);
        }
    }

    /**
     * 解析字体文件信息，包括family、weight和style
     *
     * @param fileName 字体文件名
     * @return 字体信息对象
     */
    private FontInfo parseFontInfo(String fileName) {
        String fileNameLower = fileName.toLowerCase();

        // 确定字体family - 统一使用 "Noto Sans SC"
        String fontFamily = "Noto Sans SC";

        // 解析字体权重
        int fontWeight = parseFontWeight(fileNameLower);

        // 解析字体样式
        BaseRendererBuilder.FontStyle fontStyle = fileNameLower.contains("italic")
                ? BaseRendererBuilder.FontStyle.ITALIC
                : BaseRendererBuilder.FontStyle.NORMAL;

        return new FontInfo(fontFamily, fontWeight, fontStyle);
    }

    /**
     * 解析字体权重
     *
     * @param fileNameLower 小写的字体文件名
     * @return 字体权重值
     */
    private int parseFontWeight(String fileNameLower) {
        if (fileNameLower.contains("thin")) {
            return 100;
        } else if (fileNameLower.contains("extralight") || fileNameLower.contains("ultra-light")) {
            return 200;
        } else if (fileNameLower.contains("light")) {
            return 300;
        } else if (fileNameLower.contains("regular") || fileNameLower.contains("normal")) {
            return 400;
        } else if (fileNameLower.contains("medium")) {
            return 500;
        } else if (fileNameLower.contains("semibold") || fileNameLower.contains("semi-bold")) {
            return 600;
        } else if (fileNameLower.contains("bold")) {
            return 700;
        } else if (fileNameLower.contains("extrabold") || fileNameLower.contains("ultra-bold")) {
            return 800;
        } else if (fileNameLower.contains("black") || fileNameLower.contains("heavy")) {
            return 900;
        } else {
            // 默认权重
            return 400;
        }
    }

    /**
     * 记录字体目录中的所有内容，用于调试
     *
     * @param fontsDir 字体目录
     */
    private void logFontDirectoryContents(File fontsDir) {
        try {
            log.info("字体目录内容列表:");
            Files.list(fontsDir.toPath())
                    .forEach(p -> {
                        String fileName = p.getFileName().toString();
                        boolean isTtf = fileName.toLowerCase().endsWith(".ttf");
                        log.info("  {} {} (TTF: {})",
                                isTtf ? "✓" : "✗", fileName, isTtf);
                    });
        } catch (Exception e) {
            log.warn("无法列出字体目录内容: {}", e.getMessage());
        }
    }

    /**
     * 验证字体文件是否支持中文字符
     * 通过检查字体文件大小和名称来进行基本验证
     *
     * @param fontFile 字体文件
     * @param fontInfo 字体信息
     */
    private void validateChineseFontSupport(File fontFile, FontInfo fontInfo) {
        try {
            long fileSize = fontFile.length();

            // Noto Sans SC字体文件通常较大（包含完整中文字符集）
            // 基本验证：文件大小应该大于5MB（简单的启发式检查）
            if (fileSize < 5 * 1024 * 1024) {
                log.warn("⚠️ 字体文件可能不包含完整中文字符集: {} (大小: {} KB)",
                        fontFile.getName(), fileSize / 1024);
            } else {
                log.info("✓ 字体文件大小验证通过: {} (大小: {} MB)",
                        fontFile.getName(), fileSize / (1024 * 1024));
            }

            // 验证字体名称是否符合预期
            if (StringUtils.contains(fontFile.getName().toLowerCase(), "noto") &&
                    StringUtils.contains(fontFile.getName().toLowerCase(), "sc")) {
                log.info("✓ 字体名称验证通过: {} 包含中文字符支持标识", fontFile.getName());
            } else {
                log.warn("⚠️ 字体名称可能不支持中文: {}", fontFile.getName());
            }

            // 验证字体文件是否可读
            if (!fontFile.canRead()) {
                log.error("✗ 字体文件不可读: {}", fontFile.getAbsolutePath());
            } else {
                log.debug("✓ 字体文件可读性验证通过: {}", fontFile.getName());
            }

        } catch (Exception e) {
            log.error("字体验证失败: {} - {}", fontFile.getName(), e.getMessage(), e);
        }
    }

    /**
     * 字体信息内部类
     */
    private static class FontInfo {
        final String family;
        final int weight;
        final BaseRendererBuilder.FontStyle style;

        FontInfo(String family, int weight, BaseRendererBuilder.FontStyle style) {
            this.family = family;
            this.weight = weight;
            this.style = style;
        }
    }

    private void ensureDir(File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            throw RenderException.io("创建输出目录失败: " + dir.getAbsolutePath());
        }
    }
}
