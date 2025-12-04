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
            - **基础信息**：姓名(必填)、出生日期、期望岗位、头像URL、个人摘要
            - **联系方式**：电话、邮箱、地址、网站等联系信息
            - **社交链接**：GitHub、LinkedIn、博客等社交平台链接
            - **教育经历**：学校、专业、学历、时间范围、相关描述
            - **工作经历**：公司、职位、行业、类型、时间范围、工作描述、关键亮点
            - **项目经验**：项目名称、角色、时间范围、项目描述、技术亮点
            - **专业技能**：技能分类、名称、熟练度等级
            - **证书获奖**：证书名称、颁发机构、获得时间、详细描述
            
            ### 解析规则与标准
            1. **数据格式规范**：
               - 日期统一使用LocalDate格式(yyyy-MM-dd)
               - 描述性内容支持Markdown格式
               - 所有字段严格按照CvBO对象属性命名
            
            2. **智能推理规则**：
               - 根据上下文推断工作类型(全职/实习/兼职/freelance)
               - 智能识别技能熟练度(精通/熟练/良好/了解)
               - 自动分类技能类型(编程语言/框架工具/数据库/软技能)
            
            3. **数据完整性保证**：
               - 姓名为必填字段，必须准确提取
               - 缺失信息设为null，空集合设为空数组
               - 保持原文描述的结构和格式
            
            4. **排序与优先级**：
               - 经历按时间倒序排列(最新在前)
               - 技能按重要性和熟练度排序
               - 使用sortOrder字段标识排序顺序
            
            ### 输出要求
            - 严格按照CvBO对象结构输出JSON格式
            - 确保所有字段名称与Java对象属性一致
            - 保持数据类型的准确性和完整性
            """)
    @UserMessage("""
            请仔细分析以下简历文本，将其转换为标准化的CvBO对象结构：
            
            简历内容：
            {{resumeText}}
            
            请按照以下要求进行解析：
            1. 准确提取所有可识别的个人信息、联系方式和社交链接
            2. 完整解析教育背景、工作经历和项目经验的时间线
            3. 智能识别和分类专业技能，评估熟练度等级
            4. 提取证书、获奖等成就信息
            5. 保持原文描述的格式和结构，支持Markdown语法
            6. 对于无法确定的信息，请设置为null或空值
            7. 确保输出的JSON结构完全符合CvBO对象定义
            
            请返回完整的CvBO对象JSON结构。
            """)
    Result<CvBO> reviewCv(@V("resumeText") String cv);
}
