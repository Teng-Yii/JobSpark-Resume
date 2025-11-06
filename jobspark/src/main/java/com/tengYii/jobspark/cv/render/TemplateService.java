package com.tengYii.jobspark.cv.render;

import com.tengYii.jobspark.cv.config.MarkdownConfig;
import com.tengYii.jobspark.cv.errors.RenderException;
import com.tengYii.jobspark.cv.mapping.TemplateFieldMapper;
import com.tengYii.jobspark.cv.model.Cv;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.Map;

/**
 * FreeMarker 模板服务：加载并渲染 .ftl -> Markdown 文本
 * 模板位置：classpath:/templates/cv.md.ftl
 */
public class TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);

    private final Configuration cfg;

    public TemplateService() {
        try {
            cfg = new Configuration(Configuration.VERSION_2_3_32);
            cfg.setDefaultEncoding("UTF-8");
            // 模板目录：resources/templates
            cfg.setClassForTemplateLoading(TemplateService.class, "/templates");
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            cfg.setLogTemplateExceptions(false);
            cfg.setWrapUncheckedExceptions(true);
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
    public String renderMarkdown(Cv cv, MarkdownConfig markdownConfig, TemplateFieldMapper fieldMapper) {
        try {
            Map<String, Object> dataModel = fieldMapper.toTemplateData(cv);
            // 额外的渲染配置注入（供模板条件判断）
            dataModel.put("format", markdownConfig.getFormat());
            dataModel.put("headingOffset", markdownConfig.getHeadingOffset());
            dataModel.put("compactList", markdownConfig.isCompactList());
            dataModel.put("includeHeaderBlock", markdownConfig.isIncludeHeaderBlock());

            Template template = cfg.getTemplate("cv.md.ftl", "UTF-8");
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