package com.tengYii.jobspark.cv;

import com.tengYii.jobspark.model.bo.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * CvBO对象Mock数据示例
 */
public class CvBOMock {

    public static CvBO createMockCvBO() {
        // 创建联系方式
        ContactBO contact = ContactBO.builder()
                .phone("13800138000")
                .email("zhangsan@example.com")
                .wechat("zhangsan_wx")
                .location("北京市海淀区")
                .build();

        // 创建社交链接
        List<SocialLinkBO> socialLinks = Arrays.asList(
                SocialLinkBO.builder()
                        .label("GitHub")
                        .url("https://github.com/zhangsan")
                        .build(),
                SocialLinkBO.builder()
                        .label("LinkedIn")
                        .url("https://linkedin.com/in/zhangsan")
                        .build()
        );

        // 创建教育经历
        List<EducationBO> educations = Arrays.asList(
                EducationBO.builder()
                        .school("北京大学")
                        .major("计算机科学与技术")
                        .degree("本科")
                        .startDate(LocalDate.of(2015, 9, 1))
                        .endDate(LocalDate.of(2019, 6, 30))
                        .description("GPA: 3.8/4.0\n\n获得荣誉：\n- 优秀毕业生\n- 国家奖学金")
                        .build(),
                EducationBO.builder()
                        .school("清华大学")
                        .major("软件工程")
                        .degree("硕士")
                        .startDate(LocalDate.of(2019, 9, 1))
                        .endDate(LocalDate.of(2022, 6, 30))
                        .description("研究方向：人工智能与机器学习\n\n发表论文3篇，其中SCI收录2篇")
                        .build()
        );

        // 创建工作经历
        List<HighlightBO> experienceHighlights1 = Arrays.asList(
                HighlightBO.builder()
                        .highlight("负责后端核心业务模块的设计与开发，使用Spring Boot框架")
                        .sortOrder(1)
                        .build(),
                HighlightBO.builder()
                        .highlight("优化数据库查询性能，将关键接口响应时间从500ms降低到100ms")
                        .sortOrder(2)
                        .build(),
                HighlightBO.builder()
                        .highlight("参与系统架构设计，引入微服务架构提升系统可维护性")
                        .sortOrder(3)
                        .build()
        );

        List<HighlightBO> experienceHighlights2 = Arrays.asList(
                HighlightBO.builder()
                        .highlight("负责推荐算法的设计与实现，提升推荐准确率15%")
                        .sortOrder(1)
                        .build(),
                HighlightBO.builder()
                        .highlight("使用TensorFlow搭建深度学习模型，处理大规模数据")
                        .sortOrder(2)
                        .build()
        );

        List<ExperienceBO> experiences = Arrays.asList(
                ExperienceBO.builder()
                        .type("全职")
                        .company("阿里巴巴集团")
                        .industry("互联网")
                        .role("高级Java工程师")
                        .startDate(LocalDate.of(2022, 7, 1))
                        .endDate(LocalDate.of(2023, 12, 31))
                        .description("负责电商平台核心交易系统的开发与维护")
                        .sortOrder(2)
                        .highlights(experienceHighlights1)
                        .build(),
                ExperienceBO.builder()
                        .type("全职")
                        .company("字节跳动")
                        .industry("互联网")
                        .role("算法工程师")
                        .startDate(LocalDate.of(2024, 1, 1))
                        .endDate(null) // 当前在职
                        .description("负责推荐系统算法的研发与优化")
                        .sortOrder(1)
                        .highlights(experienceHighlights2)
                        .build()
        );

        // 创建项目经验
        List<HighlightBO> projectHighlights1 = Arrays.asList(
                HighlightBO.builder()
                        .highlight("设计并实现分布式任务调度系统，支持每日千万级任务调度")
                        .sortOrder(1)
                        .build(),
                HighlightBO.builder()
                        .highlight("引入Redis缓存提升系统性能，QPS提升3倍")
                        .sortOrder(2)
                        .build()
        );

        List<HighlightBO> projectHighlights2 = Arrays.asList(
                HighlightBO.builder()
                        .highlight("使用协同过滤和深度学习模型构建推荐系统")
                        .sortOrder(1)
                        .build(),
                HighlightBO.builder()
                        .highlight("实现实时特征工程，支持模型动态更新")
                        .sortOrder(2)
                        .build()
        );

        List<ProjectBO> projects = Arrays.asList(
                ProjectBO.builder()
                        .name("电商分布式任务调度平台")
                        .startDate(LocalDate.of(2022, 9, 1))
                        .endDate(LocalDate.of(2023, 6, 30))
                        .role("技术负责人")
                        .description("基于Spring Cloud和Quartz构建的企业级任务调度平台，支持分布式环境下的任务执行与监控")
                        .sortOrder(2)
                        .highlights(projectHighlights1)
                        .build(),
                ProjectBO.builder()
                        .name("个性化推荐系统")
                        .startDate(LocalDate.of(2024, 1, 15))
                        .endDate(LocalDate.of(2024, 6, 30))
                        .role("算法工程师")
                        .description("基于深度学习的个性化推荐系统，支持多种推荐场景，包括商品推荐、内容推荐等")
                        .sortOrder(1)
                        .highlights(projectHighlights2)
                        .build()
        );

        // 创建技能列表
        List<SkillBO> skills = Arrays.asList(
                SkillBO.builder()
                        .category("编程语言")
                        .name("Java")
                        .level("精通")
                        .sortOrder(1)
                        .build(),
                SkillBO.builder()
                        .category("编程语言")
                        .name("Python")
                        .level("熟练")
                        .sortOrder(2)
                        .build(),
                SkillBO.builder()
                        .category("框架")
                        .name("Spring Boot")
                        .level("精通")
                        .sortOrder(3)
                        .build(),
                SkillBO.builder()
                        .category("框架")
                        .name("Spring Cloud")
                        .level("熟练")
                        .sortOrder(4)
                        .build(),
                SkillBO.builder()
                        .category("数据库")
                        .name("MySQL")
                        .level("熟练")
                        .sortOrder(5)
                        .build(),
                SkillBO.builder()
                        .category("数据库")
                        .name("Redis")
                        .level("熟练")
                        .sortOrder(6)
                        .build(),
                SkillBO.builder()
                        .category("AI/机器学习")
                        .name("TensorFlow")
                        .level("良好")
                        .sortOrder(7)
                        .build(),
                SkillBO.builder()
                        .category("AI/机器学习")
                        .name("PyTorch")
                        .level("了解")
                        .sortOrder(8)
                        .build()
        );

        // 创建证书列表
        List<CertificateBO> certificates = Arrays.asList(
                CertificateBO.builder()
                        .name("Oracle Certified Professional Java SE Programmer")
                        .issuer("Oracle")
                        .date(LocalDate.of(2021, 5, 15))
                        .description("分数：95%\n\n等级：优秀")
                        .sortOrder(2)
                        .build(),
                CertificateBO.builder()
                        .name("AWS Certified Solutions Architect")
                        .issuer("Amazon Web Services")
                        .date(LocalDate.of(2022, 8, 20))
                        .description("分数：88%\n\n有效期：3年")
                        .sortOrder(1)
                        .build()
        );

        // 创建国际化配置
        LocaleConfigBO localeConfig = LocaleConfigBO.builder()
                .locale("zh-CN")
                .datePattern("yyyy.MM")
                .sectionLabels("{\"education\":\"教育经历\",\"experience\":\"工作经历\",\"project\":\"项目经验\",\"skill\":\"技能特长\",\"certificate\":\"证书奖项\"}")
                .build();

        // 创建格式元数据
        FormatMetaBO formatMeta = FormatMetaBO.builder()
                .theme("简约")
                .alignment("left")
                .lineSpacing(1.4)
                .fontFamily("\"Noto Sans SC\", \"PingFang SC\", \"Microsoft YaHei\", \"SimSun\", sans-serif")
                .datePattern("yyyy.MM")
                .hyperlinkStyle("underline")
                .showAvatar(true)
                .showSocial(true)
                .twoColumnLayout(false)
                .localeConfig(localeConfig)
                .build();

        // 创建并返回CvBO对象
        return CvBO.builder()
                .name("张三")
                .birthDate(LocalDate.of(1997, 5, 15))
                .title("高级Java开发工程师")
                .avatarUrl("https://example.com/avatar/zhangsan.jpg")
                .summary("5年Java开发经验，精通Spring Boot、Spring Cloud等微服务框架，熟悉分布式系统设计。有大型电商平台和推荐系统开发经验，对技术有浓厚兴趣，善于解决复杂技术问题。")
                .contact(contact)
                .socialLinks(socialLinks)
                .educations(educations)
                .experiences(experiences)
                .projects(projects)
                .skills(skills)
                .certificates(certificates)
                .meta(formatMeta)
                .build();
    }

    public static void main(String[] args) {
        // 创建并打印Mock对象
        CvBO mockCv = createMockCvBO();
        System.out.println("Mock CvBO对象创建成功:");
        System.out.println("姓名: " + mockCv.getName());
        System.out.println("出生日期: " + mockCv.getBirthDate());
        System.out.println("职位: " + mockCv.getTitle());
        System.out.println("联系方式: " + mockCv.getContact().getPhone() + ", " + mockCv.getContact().getEmail());
        System.out.println("教育经历数量: " + mockCv.getEducations().size());
        System.out.println("工作经历数量: " + mockCv.getExperiences().size());
        System.out.println("项目经验数量: " + mockCv.getProjects().size());
        System.out.println("技能数量: " + mockCv.getSkills().size());
        System.out.println("证书数量: " + mockCv.getCertificates().size());
    }
}