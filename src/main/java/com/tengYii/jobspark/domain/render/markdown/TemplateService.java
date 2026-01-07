package com.tengYii.jobspark.domain.render.markdown;

import com.tengYii.jobspark.common.exception.RenderException;
import com.tengYii.jobspark.config.cv.MarkdownConfig;
import com.tengYii.jobspark.model.bo.CvBO;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import lombok.extern.slf4j.Slf4j;

import java.io.StringWriter;
import java.net.URL;
import java.util.Map;

/**
 * FreeMarker 模板服务：加载并渲染 .ftl -> Markdown 文本
 * 模板位置：classpath:/templates/cv.md.ftl
 */
@Slf4j
public class TemplateService {

    private final Configuration cfg;

    public TemplateService() {
        try {
            cfg = new Configuration(Configuration.VERSION_2_3_32);
            cfg.setDefaultEncoding("UTF-8");
            // 模板目录：resources/templates
            ClassLoader cl = TemplateService.class.getClassLoader();
            ClassLoader tcl = Thread.currentThread().getContextClassLoader();
            TemplateLoader l1 = new ClassTemplateLoader(TemplateService.class, "/templates"); // reliable in packaged runtime
            TemplateLoader l2 = (cl != null) ? new ClassTemplateLoader(cl, "/templates") : null; // class loader of this class
            TemplateLoader l3 = (tcl != null) ? new ClassTemplateLoader(tcl, "/templates") : null; // thread context loader (IDE/tests)

            TemplateLoader[] loaders = (l2 == null && l3 == null) ? new TemplateLoader[]{l1}
                    : (l2 == null) ? new TemplateLoader[]{l1, l3}
                    : (l3 == null) ? new TemplateLoader[]{l1, l2}
                    : new TemplateLoader[]{l1, l2, l3};
            cfg.setTemplateLoader(new MultiTemplateLoader(loaders));

            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            cfg.setLogTemplateExceptions(false);
            cfg.setWrapUncheckedExceptions(true);
            log.info("Configured FreeMarker loaders: l1={}, l2={}, l3={}, tcl={}, cl={}", l1, l2, l3, tcl, cl);
        } catch (Exception e) {
            throw RenderException.template("初始化 FreeMarker 配置失败", e);
        }
    }

    /**
     * 渲染简历为 Markdown 文本
     *
     * @param cv 领域对象
     * @param markdownConfig 渲染配置（紧凑列表、标题偏移等）
     * @param fieldMapper 字段映射器（别名表与缺失检查）
     * @return Markdown 文本
     */
    public String renderMarkdown(CvBO cv, MarkdownConfig markdownConfig, TemplateFieldMapper fieldMapper) {
        try {
            Map<String, Object> dataModel = fieldMapper.toTemplateData(cv);
            // 额外的渲染配置注入（供模板条件判断）
            dataModel.put("format", markdownConfig.getFormat());
            dataModel.put("headingOffset", markdownConfig.getHeadingOffset());
            dataModel.put("compactList", markdownConfig.isCompactList());
            dataModel.put("includeHeaderBlock", markdownConfig.isIncludeHeaderBlock());

            // Diagnostics: verify classpath visibility and resource resolution before loading template
            URL tclUrl = (Thread.currentThread().getContextClassLoader() != null)
                    ? Thread.currentThread().getContextClassLoader().getResource("templates/cv.md.ftl")
                    : null;
            URL classUrl = TemplateService.class.getResource("/templates/cv.md.ftl");
            String codeSource = (TemplateService.class.getProtectionDomain() != null
                    && TemplateService.class.getProtectionDomain().getCodeSource() != null
                    && TemplateService.class.getProtectionDomain().getCodeSource().getLocation() != null)
                    ? TemplateService.class.getProtectionDomain().getCodeSource().getLocation().toString()
                    : "null";
            log.info("Diagnose FreeMarker: base=/templates, will load 'cv.md.ftl'; tclUrl={}, classUrl={}, codeSource={}, templateLoader={}",
                    tclUrl, classUrl, codeSource, cfg.getTemplateLoader());

            Template template;
            template = cfg.getTemplate("cv.md.ftl", "UTF-8"); // 基于基路径 /templates
            StringWriter writer = new StringWriter();
            template.process(dataModel, writer);

            String md = writer.toString();
            log.info("Template rendered to Markdown, length={}", md.length());
            return md;
        } catch (Exception e) {
            throw RenderException.template("模板渲染失败: " + e.getMessage(), e);
        }
    }
}