package com.tengYii.jobspark.domain.cv.demo;

import com.tengYii.jobspark.domain.cv.config.DocxConfig;
import com.tengYii.jobspark.domain.cv.config.HtmlConfig;
import com.tengYii.jobspark.domain.cv.config.MarkdownConfig;
import com.tengYii.jobspark.domain.cv.config.PdfConfig;
import com.tengYii.jobspark.domain.cv.errors.RenderException;
import com.tengYii.jobspark.domain.cv.mapping.TemplateFieldMapper;
import com.tengYii.jobspark.domain.cv.model.*;
import com.tengYii.jobspark.domain.cv.render.CvRendererFacade;
import com.tengYii.jobspark.domain.cv.validation.CvValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 最小可运行示例（main 方法）
 * 渲染管线：FreeMarker(.ftl模板) → Markdown → CommonMark HTML → openhtmltopdf PDF → docx4j ImportXHTML Docx
 * 输出路径：项目根 out/ 目录，文件名包含时间戳（resume_YYYYMMDD_HHMM）
 *
 * 注意：
 * - 首次运行需联网下载依赖（docx4j/openhtmltopdf 等）。若当前环境暂不联网，PDF/Docx 步骤会抛出 RenderException（已捕获并提示）。
 * - 字体：resources/templates/fonts 目录可放置中文字体（如 NotoSansSC-Regular.ttf）；若缺失则回退系统字体栈。
 */
public class CvDemoMain {

    private static final Logger log = LoggerFactory.getLogger(CvDemoMain.class);

    public static void main(String[] args) {
        try {
            // 1) 构建示例 Cv（中文内容，贴近样例：教育、实习经历、项目、技能/亮点）
            Cv cv = buildSampleCv();

            // 2) 校验必填字段
            new CvValidator().validateOrThrow(cv);

            // 3) 渲染配置与门面
            MarkdownConfig mdCfg = MarkdownConfig.defaults();
            HtmlConfig htmlCfg = HtmlConfig.defaults();
            PdfConfig pdfCfg = PdfConfig.defaults();
            DocxConfig docxCfg = DocxConfig.defaults();
            TemplateFieldMapper mapper = TemplateFieldMapper.builder()
                    .aliases(java.util.Map.of()) // 可在此提供模板字段别名映射
                    .build();

            CvRendererFacade facade = new CvRendererFacade();

            // 4) 输出目录与时间戳
            File outDir = new File("out");
            if (!outDir.exists() && !outDir.mkdirs()) {
                throw new RuntimeException("创建输出目录失败: " + outDir.getAbsolutePath());
            }
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
            String baseName = "resume_" + ts;

            // 5) 生成 Markdown
            String markdown = facade.toMarkdown(cv, mdCfg, mapper);
            Path mdPath = outPath(outDir, baseName + ".md");
            Files.writeString(mdPath, markdown);
            log.info("Markdown 已输出：{}", mdPath.toAbsolutePath());

            // 6) Markdown → HTML（作为中间格式）
            String html = facade.toHtmlFromMarkdown(markdown, htmlCfg);
            // 可选：保存 HTML 以便预览（不是必须交付项）
            // Files.writeString(outPath(outDir, baseName + ".html"), html);

            // 7) HTML → PDF
            try {
                File pdfFile = facade.toPdf(html, pdfCfg, outDir, "");
                log.info("PDF 已输出：{}", pdfFile.getAbsolutePath());
            } catch (RenderException e) {
                log.warn("PDF 生成失败（可能缺少依赖或字体）：{}", e.getMessage());
            }

            // 8) HTML → Docx
            try {
                File docxFile = facade.toDocx(html, docxCfg, outDir, "");
                log.info("Docx 已输出：{}", docxFile.getAbsolutePath());
            } catch (RenderException e) {
                log.warn("Docx 生成失败（可能缺少依赖）：{}", e.getMessage());
            }

            // 9) 打印最终输出文件绝对路径（至少 Markdown 一定存在）
            log.info("输出目录：{}", outDir.getAbsolutePath());
            log.info("Markdown：{}", mdPath.toAbsolutePath());
            // 其余两个文件路径将在各服务成功后打印

        } catch (Exception e) {
            log.error("简历渲染流程失败：{}", e.getMessage(), e);
            System.err.println("渲染失败：" + e.getMessage());
        }
    }

    private static Path outPath(File dir, String fileName) {
        return new File(dir, fileName).toPath();
    }

    private static Cv buildSampleCv() {
        // 个人信息与摘要
        RichText summary = RichText.builder().markdown("""
                - 热爱后端开发，具备良好的编码习惯与团队协作能力
                - 熟悉 Java / SpringBoot / MySQL / Redis / RocketMQ / MyBatis-Plus
                - 关注性能与可靠性，重视日志、监控与故障排查
                """).build();

        Contact contact = Contact.builder()
                .phone("138****0000")
                .email("z***@example.com")
                .location("西安")
                .build();

        List<Link> links = List.of(
                Link.builder().label("GitHub").url("https://github.com/example").build(),
                Link.builder().label("Blog").url("https://blog.example.com").build()
        );

        // 教育
        Education edu = Education.builder()
                .school("西安某大学")
                .major("人工智能专业")
                .startDate(LocalDate.of(2022, 9, 1))
                .endDate(LocalDate.of(2026, 7, 1))
                .description(RichText.builder().markdown("""
                        - 连续获得奖学金，担任学院技术社团负责人
                        - 课程涉猎：数据结构、操作系统、数据库系统、计算机网络、算法设计
                        """).build())
                .build();

        // 经历（实习）
        Experience exp = Experience.builder()
                .company("上海某金融科技公司（实习）")
                .role("Java后端实习生")
                .startDate(LocalDate.of(2025, 4, 1))
                .endDate(LocalDate.of(2025, 8, 1))
                .highlights(List.of(
                        RichText.builder().markdown("负责核心接口的改造与性能优化，关键接口响应延迟降低约20%").build(),
                        RichText.builder().markdown("参与风控规则引擎开发，基于 Redis 结构与多维索引提升命中效率").build(),
                        RichText.builder().markdown("推进 MyBatis-Plus 版本升级与分页查询优化，减少冗余SQL").build()
                ))
                .build();

        // 项目经验
        Project proj = Project.builder()
                .name("乐谱主题服务平台")
                .role("后端开发")
                .description(RichText.builder().markdown("""
                        - 平台提供高并发消息通知与用户信息查询，支持秒级延迟
                        - 技术栈：SpringBoot + MySQL + Redis + RocketMQ + Caffeine + MyBatis-Plus
                        """).build())
                .highlights(List.of(
                        RichText.builder().markdown("使用 Redis 与 Lua 实现幂等与限流，保障接口稳定性").build(),
                        RichText.builder().markdown("构建消息投递重试与死信队列，提升消息可靠性").build(),
                        RichText.builder().markdown("设计缓存淘汰与预热策略，降低数据库压力").build()
                ))
                .build();

        // 技能与证书
        List<Skill> skills = List.of(
                Skill.builder().name("Java / SpringBoot").level("熟练").build(),
                Skill.builder().name("MySQL / MyBatis-Plus").level("熟练").build(),
                Skill.builder().name("Redis / RocketMQ").level("良好").build(),
                Skill.builder().name("HTTP / TCP / 负载均衡").level("了解").build()
        );

        List<Certificate> certs = List.of(
                Certificate.builder().name("ACM 校内竞赛奖项").issuer("校方").date(LocalDate.of(2024, 6, 1)).build()
        );

        // 版式元数据
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
                .avatarUrl("") // 可选头像路径，默认不显示
                .summary(summary)
                .contact(contact)
                .socialLinks(links)
                .educations(List.of(edu))
                .experiences(List.of(exp))
                .projects(List.of(proj))
                .skills(skills)
                .certificates(certs)
                .meta(meta)
                .build();
    }
}