package com.tengYii.jobspark.domain.render.doc;

import com.tengYii.jobspark.common.exception.RenderException;
import com.tengYii.jobspark.config.cv.DocxConfig;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * HTML -> DOCX (docx4j ImportXHTML)
 * - 使用 ImportXHTML 将中间 HTML 转为 Word 文档
 * - 输出到项目根 out/resume_YYYYMMDD_HHMM.docx
 *
 * 说明：
 * - ImportXHTML 对复杂两栏/表格兼容较弱，建议在 CSS 中尽量使用块级布局
 * - 字体由 Word 客户端最终替换为系统可用中文字体（例如 Noto Sans SC/PingFang SC/YaHei）
 */
public class DocxService {

    private static final Logger log = LoggerFactory.getLogger(DocxService.class);

    public File toDocx(String html, DocxConfig config, File outDir, String baseUri) {
        if (html == null) {
            throw RenderException.docx("HTML 内容为空");
        }
        ensureDir(outDir);

        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        File outFile = new File(outDir, "resume_" + ts + ".docx");

        try {
            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
            XHTMLImporterImpl importer = new XHTMLImporterImpl(wordMLPackage);
            // 基于 HTML 的导入；对于分页/标题大小等，通过 CSS/HTML 调整
            wordMLPackage.getMainDocumentPart().getContent().addAll(importer.convert(html, baseUri == null ? "" : baseUri));
            wordMLPackage.save(outFile);

            if (outFile.length() == 0) {
                throw new IllegalStateException("生成的 DOCX 文件大小为 0");
            }
            log.info("DOCX 生成成功：{}", outFile.getAbsolutePath());
            return outFile;
        } catch (NoClassDefFoundError e) {
            // 依赖缺失的友好提示
            throw RenderException.docx("缺少 docx4j ImportXHTML 相关依赖，请在联网后执行 Maven 依赖解析", e);
        } catch (Docx4JException e) {
            throw RenderException.docx("HTML 转 DOCX 失败: " + e.getMessage(), e);
        } catch (Exception e) {
            throw RenderException.docx("DOCX 生成过程中出现异常: " + e.getMessage(), e);
        }
    }

    private void ensureDir(File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            throw RenderException.io("创建输出目录失败: " + dir.getAbsolutePath());
        }
    }
}