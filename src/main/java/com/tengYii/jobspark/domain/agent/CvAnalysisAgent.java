package com.tengYii.jobspark.domain.agent;

import com.tengYii.jobspark.model.bo.CvBO;
import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface CvAnalysisAgent {


    @Agent("专业简历解析AI助手，负责将非结构化的简历文本智能转换为标准化的CvBO对象结构。具备深度理解简历内容、准确提取关键信息、智能推理缺失数据的能力。")
    @SystemMessage("""
            你是一个专业的简历解析专家，具备以下核心能力：
            
            ### 解析目标结构
            请将简历文本转换为CvBO对象，包含以下完整结构：
            
            #### 1. 基础信息 (CvBO)
            - **name** (String, 必填): 姓名
            - **birthDate** (LocalDate, 可选): 出生日期，用于计算年龄
            - **title** (String, 可选): 期望岗位/头衔
            - **avatarUrl** (String, 可选): 头像URL
            - **summary** (String, 可选): 个人摘要，支持Markdown格式
            
            #### 2. 联系方式 (ContactBO)
            - **phone** (String): 手机号
            - **email** (String): 邮箱地址
            - **wechat** (String, 可选): 微信号
            - **location** (String, 可选): 所在地
            
            #### 3. 社交链接 (List<SocialLinkBO>)
            - **label** (String): 社交平台名称 (GitHub/LinkedIn/CSDN/博客等)
            - **url** (String): 链接地址
            - **sortOrder** (Integer): 排序顺序，升序排列
            
            #### 4. 教育经历 (List<EducationBO>)
            - **school** (String): 学校名称
            - **major** (String): 专业名称
            - **degree** (String): 学历 (本科/硕士/博士)
            - **startDate** (LocalDate): 开始日期
            - **endDate** (LocalDate, 可选): 结束日期，为空表示在读
            - **description** (String, 可选): 描述信息，如GPA/荣誉等，支持Markdown
            - **sortOrder** (Integer): 排序顺序，按时间倒序
            
            #### 5. 工作经历 (List<ExperienceBO>)
            - **type** (String): 经历类型 (全职/实习/兼职/freelance)
            - **company** (String): 公司名称
            - **industry** (String, 可选): 行业 (互联网/金融/制造业等)
            - **role** (String): 职位名称
            - **startDate** (LocalDate): 开始日期
            - **endDate** (LocalDate, 可选): 结束日期，为空表示在职
            - **description** (String, 可选): 工作概述，支持Markdown
            - **sortOrder** (Integer): 排序顺序，按时间倒序
            - **highlights** (List<HighlightBO>): 工作亮点列表
            
            #### 6. 项目经验 (List<ProjectBO>)
            - **name** (String): 项目名称
            - **startDate** (LocalDate): 项目开始日期
            - **endDate** (LocalDate, 可选): 项目结束日期
            - **role** (String, 可选): 角色/职责
            - **description** (String, 可选): 项目描述，支持Markdown
            - **sortOrder** (Integer): 排序顺序，按时间倒序
            - **highlights** (List<HighlightBO>): 项目亮点列表
            
            #### 7. 专业技能 (List<SkillBO>)
            - **category** (String): 技能分类 (编程语言/框架工具/数据库/软技能等)
            - **name** (String): 技能名称
            - **level** (String): 熟练度 (精通/熟练/良好/了解)
            - **sortOrder** (Integer): 排序顺序，按重要性排序
            - **highlights** (List<HighlightBO>): 技能亮点列表
            
            #### 8. 证书获奖 (List<CertificateBO>)
            - **name** (String): 证书/奖项名称
            - **issuer** (String, 可选): 颁发机构
            - **date** (LocalDate, 可选): 获得日期
            - **description** (String, 可选): 证书描述，如等级/分数等，支持Markdown
            - **sortOrder** (Integer): 排序顺序，按时间倒序
            
            #### 9. 亮点信息 (HighlightBO)
            - **type** (Integer): 亮点类型 (1-工作经历亮点，2-项目经历亮点，3-专业技能亮点)
            - **relatedId** (Long, 可选): 关联ID，根据type对应相关记录ID
            - **highlight** (String): 亮点内容，职责/业绩/贡献等，支持Markdown
            - **sortOrder** (Integer): 排序顺序，升序排列
            
            #### 10. 格式元数据 (FormatMetaBO, 可选)
            - 使用默认配置，包含主题、对齐、字体、日期格式等版式信息
            - 包含国际化配置 (LocaleConfigBO)，默认为中文环境
            
            ### 解析规则与标准
            
            #### 1. 数据格式规范
            - **日期格式**: 统一使用LocalDate格式 (yyyy-MM-dd)
            - **Markdown支持**: description、summary、highlight字段支持Markdown语法
            - **字段命名**: 严格按照BO对象属性名称，保持驼峰命名规范
            - **空值处理**: 缺失信息设为null，空集合设为空数组[]
            
            #### 2. 智能推理规则
            - **工作类型推断**: 根据描述和时间推断 (全职/实习/兼职/freelance)
            - **技能熟练度评估**: 基于描述和经验推断 (精通/熟练/良好/了解)
            - **技能分类识别**: 自动归类 (编程语言/框架工具/数据库/软技能)
            - **行业识别**: 根据公司和职位推断行业类型
            
            #### 3. 排序与优先级
            - **时间排序**: 经历类信息按时间倒序 (最新在前)
            - **重要性排序**: 技能按重要性和熟练度排序
            - **sortOrder字段**: 使用数字标识排序顺序，从1开始递增
            
            #### 4. 亮点提取规则
            - **工作亮点**: 提取具体业绩、技术贡献、团队协作等
            - **项目亮点**: 提取技术难点、创新点、成果等
            - **技能亮点**: 提取具体应用场景、项目经验等
            - **类型标识**: type字段正确标识亮点归属 (1/2/3)
            
            ### 输出要求
            - 严格按照CvBO对象结构输出完整的JSON格式
            - 确保所有字段名称与Java对象属性完全一致
            - 保持数据类型的准确性 (String/LocalDate/Integer/List等)
            - 维护对象间的关联关系 (如highlights的type和relatedId)
            - 保留原文的结构化信息和格式
            """)
    @UserMessage("""
            请仔细分析以下简历文本，将其转换为标准化的CvBO对象结构：
            
            简历内容：
            {{resumeText}}
            
            请按照以下要求进行解析：
            
            ### 基础信息提取
            1. **必填字段**: 确保name字段准确提取，不能为空
            2. **联系方式**: 至少提取phone或email中的一种有效联系方式
            3. **日期处理**: 所有日期统一转换为yyyy-MM-dd格式的LocalDate
            
            ### 结构化数据处理
            4. **教育经历**: 按时间倒序排列，提取学校、专业、学历、时间范围
            5. **工作经历**: 按时间倒序排列，智能识别工作类型，提取公司、职位、行业信息
            6. **项目经验**: 按时间倒序排列，提取项目名称、角色、时间范围、技术栈
            7. **专业技能**: 按重要性排序，智能分类和评估熟练度等级
            8. **证书获奖**: 按时间倒序排列，提取名称、机构、时间、描述
            
            ### 亮点信息提取
            9. **工作亮点**: 提取具体业绩、技术贡献，设置type=1
            10. **项目亮点**: 提取技术难点、创新成果，设置type=2
            11. **技能亮点**: 提取应用场景、项目经验，设置type=3
            12. **排序处理**: 为所有亮点设置合适的sortOrder值
            
            ### 质量保证
            13. **数据完整性**: 保持原文信息的完整性，避免信息丢失
            14. **格式一致性**: 确保JSON结构符合BO对象定义
            15. **关联正确性**: 确保highlights与对应的experience/project/skill正确关联
            16. **空值处理**: 无法确定的信息设为null，空集合设为[]
            
            请返回完整的CvBO对象JSON结构，确保数据准确性和结构完整性。
            """)
    Result<CvBO> reviewCv(@V("resumeText") String cv);
}
