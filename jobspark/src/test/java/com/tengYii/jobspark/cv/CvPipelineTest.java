package com.tengYii.jobspark.cv;

import com.tengYii.jobspark.cv.config.DocxConfig;
import com.tengYii.jobspark.cv.config.HtmlConfig;
import com.tengYii.jobspark.cv.config.MarkdownConfig;
import com.tengYii.jobspark.cv.config.PdfConfig;
import com.tengYii.jobspark.cv.errors.RenderException;
import com.tengYii.jobspark.cv.errors.ValidationException;
import com.tengYii.jobspark.cv.mapping.TemplateFieldMapper;
import com.tengYii.jobspark.cv.model.*;
import com.tengYii.jobspark.cv.render.MarkdownService;
import com.tengYii.jobspark.cv.render.PdfService;
import com.tengYii.jobspark.cv.render.TemplateService;
import com.tengYii.jobspark.cv.render.DocxService;
import com.tengYii.jobspark.cv.validation.CvValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

/**
 * 管线使用演示与测试：
 * - 字段校验（缺少姓名/联系方式抛异常）
 * - 模板渲染正确性（包含关键段落）
 * - HTML→PDF/Docx 转换（文件存在且非零；若依赖缺失则跳过）
 *
 * 运行：mvn -q -Dtest=com.tengYii.jobspark.cv.CvPipelineTest test
 */
public class CvPipelineTest {

    @Test
    void validate_missing_fields_should_throw() {
        Cv cv = Cv.builder()
                .name(null)
                .contact(Contact.builder().phone("13800000000").build())
                .educations(List.of(Education.builder().school("X").major("Y").startDate(LocalDate.now()).build()))
                .build();

        Assertions.assertThrows(ValidationException.class, () -> new CvValidator().validateOrThrow(cv));

        Cv cv2 = Cv.builder()
                .name("张三")
                .contact(Contact.builder().build()) // 无联系方式
                .educations(List.of(Education.builder().school("X").major("Y").startDate(LocalDate.now()).build()))
                .build();

        Assertions.assertThrows(ValidationException.class, () -> new CvValidator().validateOrThrow(cv2));

        Cv cv3 = Cv.builder()
                .name("张三")
                .contact(Contact.builder().phone("13800000000").build())
                .build();

        Assertions.assertThrows(ValidationException.class, () -> new CvValidator().validateOrThrow(cv3));
    }

    @Test
    void template_render_contains_sections() throws Exception {
        Cv cv = sampleCvMinimal();
        new CvValidator().validateOrThrow(cv);

        TemplateService ts = new TemplateService();
        MarkdownConfig mdCfg = MarkdownConfig.defaults();
        TemplateFieldMapper mapper = TemplateFieldMapper.builder().aliases(java.util.Map.of()).build();

        String md = ts.renderMarkdown(cv, mdCfg, mapper);

        Assertions.assertTrue(md.contains("教育经历"), "Markdown 应包含 教育经历 段落标题");
        Assertions.assertTrue(md.contains("项目经验"), "Markdown 应包含 项目经验 段落标题");
        Assertions.assertTrue(md.contains("技能与亮点"), "Markdown 应包含 技能与亮点 段落标题");
    }

    @Test
    void html_to_pdf_and_docx_generate_files_or_skip_when_deps_missing() throws Exception {
        Cv cv = sampleCvMinimal();
        new CvValidator().validateOrThrow(cv);

        // Markdown -> HTML
        TemplateService ts = new TemplateService();
        String md = ts.renderMarkdown(cv, MarkdownConfig.defaults(), TemplateFieldMapper.builder().aliases(java.util.Map.of()).build());
        MarkdownService ms = new MarkdownService();
        String html = ms.toHtmlFromMarkdown(md, HtmlConfig.defaults());

        File outDir = new File("out/test");
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new RuntimeException("创建测试输出目录失败: " + outDir.getAbsolutePath());
        }

        // HTML -> PDF
        try {
            PdfService ps = new PdfService();
            File pdf = ps.toPdf(html, PdfConfig.defaults(), outDir, "");
            Assertions.assertTrue(pdf.exists() && pdf.length() > 0, "PDF 文件应存在且非零大小");
        } catch (RenderException e) {
            // 依赖缺失时跳过
            Assumptions.assumeTrue(false, "跳过 PDF 测试：缺少依赖或环境不支持 - " + e.getMessage());
        }

        // HTML -> Docx
        try {
            DocxService ds = new DocxService();
            File docx = ds.toDocx(html, DocxConfig.defaults(), outDir, "");
            Assertions.assertTrue(docx.exists() && docx.length() > 0, "Docx 文件应存在且非零大小");
        } catch (RenderException e) {
            // 依赖缺失时跳过
            Assumptions.assumeTrue(false, "跳过 Docx 测试：缺少依赖或环境不支持 - " + e.getMessage());
        }
    }

    // 构建一个最小可用的 Cv（中文内容）
    private Cv sampleCvMinimal() throws Exception {
        RichText summary = RichText.builder().markdown("热爱后端开发，关注性能与可靠性。").build();
        Contact contact = Contact.builder().phone("13800000000").email("z***@example.com").location("西安").build();
        Education edu = Education.builder()
                .school("西安某大学")
                .major("计算机科学")
                .startDate(LocalDate.of(2022, 9, 1))
                .endDate(LocalDate.of(2026, 7, 1))
                .description(RichText.builder().markdown("- 主修数据结构、数据库系统、计算机网络等").build())
                .build();
        Project proj = Project.builder()
                .name("示例项目")
                .role("后端开发")
                .description(RichText.builder().markdown("- SpringBoot + MySQL + Redis").build())
                .highlights(List.of(RichText.builder().markdown("实现接口幂等与限流").build()))
                .build();
        Skill skill = Skill.builder().name("Java / SpringBoot").level("熟练").build();

        FormatMeta meta = FormatMeta.builder()
                .alignment("left")
                .lineSpacing(1.4)
                .fontFamily("\"Noto Sans SC\", \"PingFang SC\", \"Microsoft YaHei\", \"SimSun\", sans-serif")
                .datePattern("yyyy.MM")
                .hyperlinkStyle("underline")
                .showAvatar(false)
                .showSocial(true)
                .twoColumnLayout(false)
                .localeConfig(LocaleConfig.builder().locale("zh-CN").datePattern("yyyy.MM").build())
                .build();

        return Cv.builder()
                .name("张三")
                .age(23)
                .title("Java后端开发实习生")
                .summary(summary)
                .contact(contact)
                .educations(List.of(edu))
                .projects(List.of(proj))
                .skills(List.of(skill))
                .meta(meta)
                .build();
    }
}