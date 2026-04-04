---
name: jd-alignment
description: 对比岗位 JD 与候选人简历，识别匹配度、缺失技能和重点考察方向
---
# 指令内容

当接收到 `jobDescription`（岗位描述）、`resumeContext`（简历上下文）时，按以下步骤执行：

1. **JD 解析**
   - 提取硬性要求：学历、工作年限、必备技能（如 Java 8+、Spring Boot、MySQL）
   - 提取软性要求：沟通能力、团队协作、学习能力
   - 提取优先条件：大厂经验、特定行业经验、开源贡献

2. **简历匹配**
   - 使用传入的 `resumeContext`（即 `ResumeDetailResponse` 简历结构化数据）进行对比
   - 对比必备技能：标记简历中已具备的技能，标记缺失的技能
   - 对比项目经验：标记与 JD 业务场景相关的项目
   - 对比工作年限：计算候选人工作年限是否满足要求

3. **输出格式（重要！）**
   必须输出**纯 JSON 格式**，不使用任何 Markdown 代码块包裹，使用 camelCase 命名：
   ```json
   {
     "matchScore": 85,
     "matchedSkills": ["Java", "Spring Boot", "MySQL"],
     "missingSkills": ["Redis Cluster", "Kubernetes"],
     "relatedProjects": ["秒杀系统重构项目"],
     "focusAreas": ["Redis 集群经验", "云原生相关技术"],
     "suggestion": "候选人基础扎实，但缺少云原生经验，建议重点考察学习能力和Redis使用经验"
   }
