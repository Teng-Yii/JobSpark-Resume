# 简历数据库表结构参考

## 核心表字段说明
- `cv_skill.level`: 熟练度枚举（精通/熟练/良好/了解）
- `cv_project.description`: 项目描述（Markdown 格式）
- `cv_experience.type`: 经历类型（全职/实习/兼职/freelance）
- `cv_highlight.type`: 亮点类型（1-工作经历，2-项目经历，3-专业技能）

## 关联查询逻辑
- 通过 `cv_id` 关联所有子表
- `cv_highlight.related_id` 需根据 `type` 关联到对应表的主键