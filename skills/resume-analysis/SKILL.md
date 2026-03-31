---
name: resume-analysis
description: 深度挖掘简历结构化数据，提取技能、项目、工作经历等关键信息，为面试提供高价值上下文
---
# 指令内容

当接收到 `resumeId` 和 `userId` 参数时，按以下步骤执行：

1. **数据查询**
   - 调用 ResumeApplicationService.getResumeDetail(resumeId, userId) 获取简历详情
   - 从返回的 ResumeDetailResponse 中解析以下信息：
      - 基本信息：姓名、期望岗位、个人摘要、头像URL
      - 联系方式：邮箱、电话、地址等（ContactBO）
      - 社交链接：GitHub、LinkedIn等（SocialLinkBO列表）
      - 教育经历：学校、学位、专业、时间（EducationBO列表）
      - 工作经历：公司、行业、职位、工作概述、时间（ExperienceBO列表）
      - 项目经验：项目名称、角色、描述、技术栈、时间（ProjectBO列表）
      - 专业技能：技能名称、熟练度、分类（SkillBO列表）
      - 证书/获奖：证书名称、颁发机构、时间（CertificateBO列表）

2. **信息组织**
   - 技能部分：标注熟练度（精通/熟练/良好/了解），优先展示"精通"和"熟练"的技能
   - 项目部分：提取项目名称、角色、描述，重点标记包含高并发、分布式、微服务等关键词的项目
   - 工作经历部分：提取公司、行业、职位、工作概述
   - 教育经历部分：提取学校、学位、专业、入学时间

3. **输出格式**
   以结构化 Markdown 格式输出，包含以下章节：
   - 候选人概览
   - 核心技能图谱
   - 教育背景
   - 重点项目经历
   - 职业背景
   - 证书与荣誉