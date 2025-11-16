package com.tengYii.jobspark.cv;

import com.tengYii.jobspark.common.exception.RenderException;
import com.tengYii.jobspark.config.cv.DocxConfig;
import com.tengYii.jobspark.config.cv.HtmlConfig;
import com.tengYii.jobspark.config.cv.MarkdownConfig;
import com.tengYii.jobspark.config.cv.PdfConfig;
import com.tengYii.jobspark.domain.cv.render.markdown.TemplateFieldMapper;
import com.tengYii.jobspark.domain.cv.render.CvRendererFacade;
import com.tengYii.jobspark.application.validate.CvValidator;
import com.tengYii.jobspark.model.bo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 简历多格式渲染示例（优化版）
 * 核心管线：FreeMarker(.ftl) → Markdown → CommonMark HTML → PDF/Docx/HTML
 * 优化点：
 * 1. 结构模块化：拆分长方法，职责单一化
 * 2. 配置集中化：提取常量与配置初始化方法
 * 3. 可读性提升：拆分CV构建逻辑，补充清晰注释
 * 4. 灵活性增强：支持开关控制HTML输出，统一文件命名规则
 * 5. 异常精细化：细分异常类型，日志更具排查性
 * 6. API优化：使用Java NIO2规范，简化文件操作
 */
public class CvDemoMain {
    // ==================== 常量定义（集中管理，便于修改）====================
    private static final Logger log = LoggerFactory.getLogger(CvDemoMain.class);
    private static final String OUTPUT_DIR = "out";
    private static final String FILE_PREFIX = "一腾简历_";
    private static final String TIMESTAMP_PATTERN = "yyyyMMdd_HHmm";
    private static final boolean SAVE_HTML = true; // 开关：是否保存中间HTML文件
    private static final CvValidator CV_VALIDATOR = new CvValidator(); // 单例复用，避免重复创建

    // ==================== 配置初始化（集中封装，降低耦合）====================

    /**
     * 初始化所有渲染配置（统一管理，便于后续扩展配置参数）
     */
    private record RenderConfigs(
            MarkdownConfig mdConfig,
            HtmlConfig htmlConfig,
            PdfConfig pdfConfig,
            DocxConfig docxConfig,
            TemplateFieldMapper fieldMapper
    ) {
    }

    /**
     * 主方法，用于执行简历渲染流程。
     *
     * @param args 命令行参数，当前未使用。
     */
    public static void main(String[] args) {
        // 初始化上下文（时间戳、输出路径、配置）
        String timeStamp = generateTimeStamp();
        Path outputDir = createOutputDir();
        RenderConfigs configs = initRenderConfigs();

        try {
            // 1. 构建并校验CV数据
            CvBO cv = buildSampleCv();
            validateCv(cv);

            // 2. 生成基础文件名（统一命名规则）
            String baseFileName = FILE_PREFIX + timeStamp;

            // 3. 执行渲染管线（按依赖顺序执行）
            String markdown = generateMarkdown(cv, configs, outputDir, baseFileName);
            String html = generateHtmlFromMarkdown(markdown, configs, outputDir, baseFileName);
            generatePdf(html, configs, outputDir, baseFileName);
            generateDocx(html, configs, outputDir, baseFileName);

            // 4. 打印最终结果（汇总成功输出的文件）
            printOutputSummary(outputDir, baseFileName);

        } catch (Exception e) {
            log.error("简历渲染流程整体失败", e);
            System.err.printf("渲染失败：%s（详情见日志）%n", e.getMessage());
        }
    }

    // ==================== 核心流程方法（职责单一，便于调试）====================

    /**
     * 生成时间戳（独立方法，便于后续修改格式或时区）
     */
    private static String generateTimeStamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_PATTERN));
    }

    /**
     * 创建输出目录（使用NIO2 API，自动创建多级目录，简化异常处理）
     */
    private static Path createOutputDir() {
        Path dirPath = Paths.get(OUTPUT_DIR);
        try {
            Files.createDirectories(dirPath);
            log.info("输出目录已准备就绪：{}", dirPath.toAbsolutePath());
            return dirPath;
        } catch (Exception e) {
            throw new RuntimeException("创建输出目录失败：" + dirPath.toAbsolutePath(), e);
        }
    }

    /**
     * 初始化渲染配置（集中封装，后续扩展配置时无需修改main方法）
     */
    private static RenderConfigs initRenderConfigs() {
        // 模板字段别名示例：若模板中用"userName"而非"name"，可添加映射 .aliases(Map.of("userName", "name"))
        TemplateFieldMapper fieldMapper = TemplateFieldMapper.builder()
//                .aliases(new HashMap<>())
                .build();

        return new RenderConfigs(
                MarkdownConfig.defaults(),
                HtmlConfig.defaults(),
                PdfConfig.defaults(),
                DocxConfig.defaults(),
                fieldMapper
        );
    }

    /**
     * 校验CV数据（独立方法，便于后续扩展校验规则）
     */
    private static void validateCv(CvBO cv) {
        try {
            CV_VALIDATOR.validateOrThrow(cv);
            log.info("CV数据校验通过");
        } catch (Exception e) {
            throw new RuntimeException("CV数据校验失败：" + e.getMessage(), e);
        }
    }

    /**
     * 生成Markdown文件
     */
    private static String generateMarkdown(CvBO cv, RenderConfigs configs, Path outputDir, String baseFileName) {
        try {
            CvRendererFacade facade = new CvRendererFacade();
            String markdownContent = facade.toMarkdown(cv, configs.mdConfig(), configs.fieldMapper());

            Path mdPath = outputDir.resolve(baseFileName + ".md");
            Files.writeString(mdPath, markdownContent);
            log.info("✅ Markdown文件生成成功：{}", mdPath);
            return markdownContent;
        } catch (Exception e) {
            throw new RuntimeException("Markdown生成失败", e);
        }
    }

    /**
     * 从Markdown生成HTML（支持保存中间文件）
     */
    private static String generateHtmlFromMarkdown(String markdown, RenderConfigs configs, Path outputDir, String baseFileName) {
        try {
            CvRendererFacade facade = new CvRendererFacade();
//            String htmlContent = facade.toHtmlFromMarkdown(markdown, configs.htmlConfig);
            String htmlContent = facade.toHtmlFromMarkdown(markdown);

            // 按需保存HTML文件（通过开关控制）
            if (SAVE_HTML) {
                Path htmlPath = outputDir.resolve(baseFileName + ".html");
                Files.writeString(htmlPath, htmlContent);
                log.info("✅ HTML文件生成成功：{}", htmlPath);
            } else {
                log.info("ℹ️ HTML作为中间格式，未保存到本地");
            }
            return htmlContent;
        } catch (Exception e) {
            throw new RuntimeException("HTML生成失败", e);
        }
    }

    /**
     * 生成PDF文件（单独捕获渲染异常，不影响整体流程）
     */
    private static void generatePdf(String html, RenderConfigs configs, Path outputDir, String baseFileName) {
        try {
            CvRendererFacade facade = new CvRendererFacade();
            String pdfFileName = baseFileName + ".pdf";
            // 统一传入完整文件名，避免路径拼接混乱
            Path pdfPath = outputDir.resolve(pdfFileName);
            File pdfFile = facade.toPdf(html, configs.pdfConfig(), outputDir.toFile(), pdfFileName);
            log.info("✅ PDF文件生成成功：{}", pdfFile.getAbsolutePath());
        } catch (RenderException e) {
            log.warn("⚠️ PDF生成失败（常见原因：缺少中文字体/依赖未下载）：{}", e.getMessage());
        } catch (Exception e) {
            log.error("⚠️ PDF生成异常", e);
        }
    }

    /**
     * 生成Docx文件（单独捕获渲染异常，不影响整体流程）
     */
    private static void generateDocx(String html, RenderConfigs configs, Path outputDir, String baseFileName) {
        try {
            CvRendererFacade facade = new CvRendererFacade();
            String docxFileName = baseFileName + ".docx";
            File docxFile = facade.toDocx(html, configs.docxConfig(), outputDir.toFile(), docxFileName);
            log.info("✅ Docx文件生成成功：{}", docxFile.getAbsolutePath());
        } catch (RenderException e) {
            log.warn("⚠️ Docx生成失败（常见原因：依赖未下载/HTML标签不兼容）：{}", e.getMessage());
        } catch (Exception e) {
            log.error("⚠️ Docx生成异常", e);
        }
    }

    /**
     * 打印输出汇总（清晰展示所有生成的文件）
     */
    private static void printOutputSummary(Path outputDir, String baseFileName) {
        List<String> generatedFiles = List.of(
                        baseFileName + ".md",
                        SAVE_HTML ? baseFileName + ".html" : null,
                        baseFileName + ".pdf",
                        baseFileName + ".docx"
                ).stream()
                .filter(fileName -> fileName != null)
                .map(fileName -> outputDir.resolve(fileName).toAbsolutePath().toString())
                .collect(Collectors.toList());

        log.info("========================================");
        log.info("📁 简历渲染完成，输出文件汇总：");
        generatedFiles.forEach(file -> log.info("   - {}", file));
        log.info("========================================");
    }

    // ==================== CV数据构建（拆分方法，提升可读性）====================

    /**
     * 构建示例CV数据（拆分多个子方法，便于维护单个模块数据）
     */
    private static CvBO buildSampleCv() {
        return CvBO.builder()
                .name("张三")
                .birthDate(LocalDate.parse("2004-01-01"))
                .title("Java后端开发实习生")
                .avatarUrl("") // 可选：resources目录下的头像路径，需配合渲染器配置
                .summary(buildSummary())
                .contact(buildContact())
                .socialLinks(buildSocialLinks())
                .educations(buildEducations())
                .experiences(buildExperiences())
                .projects(buildProjects())
                .skills(buildSkills())
                .certificates(buildCertificates())
                .meta(buildFormatMeta())
                .build();
    }

    /**
     * 构建个人简介
     */
    private static String buildSummary() {
        return """
                - 热爱后端开发，具备良好的编码习惯与团队协作能力
                - 熟悉 Java / SpringBoot / MySQL / Redis / RocketMQ / MyBatis-Plus
                - 关注性能与可靠性，重视日志、监控与故障排查
                """;
    }

    /**
     * 构建联系方式
     */
    private static ContactBO buildContact() {
        return ContactBO.builder()
                .phone("138****0000")
                .email("z***@example.com")
                .location("西安")
                .build();
    }

    /**
     * 构建社交链接
     */
    private static List<LinkBO> buildSocialLinks() {
        return List.of(
                LinkBO.builder().label("GitHub").url("https://github.com/example").build(),
                LinkBO.builder().label("Blog").url("https://blog.example.com").build()
        );
    }

    /**
     * 构建教育经历
     */
    private static List<EducationBO> buildEducations() {
        return List.of(
                EducationBO.builder()
                        .school("西安某大学")
                        .major("人工智能专业")
                        .startDate(LocalDate.of(2022, 9, 1))
                        .endDate(LocalDate.of(2026, 7, 1))
                        .description("""
                                - 连续获得奖学金，担任学院技术社团负责人
                                - 课程涉猎：数据结构、操作系统、数据库系统、计算机网络、算法设计
                                """)
                        .build()
        );
    }

    /**
     * 构建实习经历
     */
    private static List<ExperienceBO> buildExperiences() {

        return List.of(
                ExperienceBO.builder()
                        .company("上海某金融科技公司（实习）")
                        .role("Java后端实习生")
                        .startDate(LocalDate.of(2025, 4, 1))
                        .endDate(LocalDate.of(2025, 8, 1))
                        .highlights(List.of(
                                HighlightBO.builder().highlightMarkdown("负责核心接口的改造与性能优化，关键接口响应延迟降低约20%").sortOrder(0).build(),
                                HighlightBO.builder().highlightMarkdown("参与风控规则引擎开发，基于 Redis 结构与多维索引提升命中效率").sortOrder(1).build(),
                                HighlightBO.builder().highlightMarkdown("推进 MyBatis-Plus 版本升级与分页查询优化，减少冗余SQL").sortOrder(2).build()
                        ))
                        .build()
        );
    }

    /**
     * 构建项目经验
     */
    private static List<ProjectBO> buildProjects() {
        return List.of(
                ProjectBO.builder()
                        .name("乐谱主题服务平台")
                        .role("后端开发")
                        .descriptionMarkdown("""
                                - 平台提供高并发消息通知与用户信息查询，支持秒级延迟
                                - 技术栈：SpringBoot + MySQL + Redis + RocketMQ + Caffeine + MyBatis-Plus
                                """)
                        .highlights(List.of(
                                HighlightBO.builder().highlightMarkdown("使用 Redis 与 Lua 实现幂等与限流，保障接口稳定性").sortOrder(0).build(),
                                HighlightBO.builder().highlightMarkdown("构建消息投递重试与死信队列，提升消息可靠性").sortOrder(1).build(),
                                HighlightBO.builder().highlightMarkdown("设计缓存淘汰与预热策略，降低数据库压力").sortOrder(2).build()
                        ))
                        .build()
        );
    }

    /**
     * 构建技能列表
     */
    private static List<SkillBO> buildSkills() {
        return List.of(
                SkillBO.builder().name("Java / SpringBoot").level("熟练").build(),
                SkillBO.builder().name("MySQL / MyBatis-Plus").level("熟练").build(),
                SkillBO.builder().name("Redis / RocketMQ").level("良好").build(),
                SkillBO.builder().name("HTTP / TCP / 负载均衡").level("了解").build()
        );
    }

    /**
     * 构建证书列表
     */
    private static List<CertificateBO> buildCertificates() {
        return List.of(
                CertificateBO.builder()
                        .name("ACM 校内竞赛奖项")
                        .issuer("校方")
                        .date(LocalDate.of(2024, 6, 1))
                        .build()
        );
    }

    /**
     * 构建版式配置（统一日期格式，避免冗余）
     */
    private static FormatMetaBO buildFormatMeta() {
        String datePattern = "yyyy.MM";
        return FormatMetaBO.builder()
                .alignment("left")
                .lineSpacing(1.4)
                .fontFamily("\"Noto Sans SC\", \"PingFang SC\", \"Microsoft YaHei\", \"SimSun\", sans-serif")
                .datePattern(datePattern)
                .hyperlinkStyle("underline")
                .showAvatar(false)
                .showSocial(true)
                .twoColumnLayout(false)
                .localeConfig(LocaleConfigBO.builder()
                        .locale("zh-CN")
                        .datePattern(datePattern) // 复用日期格式，避免不一致
                        .build())
                .build();
    }
}