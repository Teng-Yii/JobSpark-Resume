---
name: resume-analysis
description: 深度挖掘简历结构化数据，提取技能、项目、工作经历等关键信息，为面试提供高价值上下文
---
# 指令内容

当接收到 `resumeId` 和 `userId` 参数时，按以下步骤执行：

1. **数据查询**
    - 调用 ResumeApplicationService.getResumeDetail(resumeId, userId) 获取简历详情
    - 直接返回 ResumeDetailResponse 对象，不要进行任何转换

2. **ResumeDetailResponse 字段说明**
    - resumeId: 简历ID
    - userId: 用户ID
    - name: 姓名
    - title: 期望岗位
    - summary: 个人摘要
    - avatarUrl: 头像URL
    - contact: 联系方式（ContactBO）
    - socialLinks: 社交链接列表（SocialLinkBO列表）
    - educations: 教育经历列表（EducationBO列表）
    - experiences: 工作经历列表（ExperienceBO列表）
    - projects: 项目经验列表（ProjectBO列表）
    - skills: 专业技能列表（SkillBO列表）
    - certificates: 证书/获奖列表（CertificateBO列表）

3. **输出要求**
    - 直接返回 ResumeDetailResponse 对象
    - 不要转换为 Markdown 或其他格式
    - 保持所有字段的原始类型和数据