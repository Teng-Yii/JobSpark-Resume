# JobSpark Resume - 智能简历优化与模拟面试系统

## 🎯 项目概述

JobSpark Resume 是一个基于DDD（领域驱动设计）架构的智能简历优化和模拟面试系统。系统通过AI技术帮助用户解析简历内容、提供优化建议，并基于RAG（检索增强生成）技术进行个性化模拟面试。

## ✨ 核心功能

### 1. 简历智能解析
- 支持PDF、Word、TXT等多种格式文件上传
- AI驱动的简历内容自动提取和结构化
- 智能识别基本信息、教育经历、工作经历、技能等关键信息

### 2. 简历优化建议
- 基于AI分析简历内容质量
- 提供多维度优化建议（内容结构、关键词、技能展示等）
- 个性化改进方案生成

### 3. RAG检索引擎
- 基于向量相似度的智能问题检索
- 结合技能匹配和语义相似度的混合检索策略
- 支持多种面试类型（技术面试、行为面试等）

### 4. 模拟面试
- 个性化面试问题生成
- 实时回答评估和反馈
- 综合面试表现分析
- 面试准备建议生成

## 🏗️ 系统架构

### DDD架构设计

```
src/main/java/com/tengYii/jobspark/
├── application/           # 应用层
│   ├── controller/        # REST API控制器
│   ├── service/          # 应用服务
│   └── dto/              # 数据传输对象
├── domain/               # 领域层
│   ├── model/            # 领域模型（聚合根、实体、值对象）
│   └── service/          # 领域服务
├── infrastructure/       # 基础设施层
│   ├── ai/               # AI服务集成
│   ├── file/             # 文件处理
│   └── vector/           # 向量数据库
└── shared/              # 共享内核
    └── utils/            # 通用工具类
```

### 核心领域模型

- **Resume（简历聚合根）**: 管理简历的完整生命周期
- **InterviewSession（面试会话聚合根）**: 管理面试流程和状态
- **InterviewQuestion（面试问题实体）**: 面试问题及其向量表示
- **InterviewEvaluation（面试评估值对象）**: 评估结果和评分

## 🚀 技术栈

### 后端技术
- **框架**: Spring Boot 3.3.8
- **AI集成**: LangChain4j + OpenAI API
- **架构**: DDD领域驱动设计
- **文件处理**: Multipart文件上传
- **向量检索**: 自定义向量数据库（可扩展至Milvus/Pinecone）

### 核心依赖
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.31.0</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>0.31.0</version>
</dependency>
```

## 📡 API接口

### 简历相关接口

#### 1. 上传简历
```http
POST /api/v1/resumes/upload
Content-Type: multipart/form-data

{
    "file": "简历文件",
    "jobTitle": "目标职位",
    "industry": "行业"
}
```

#### 2. 获取简历解析结果
```http
GET /api/v1/resumes/{resumeId}/analysis
```

#### 3. 获取优化建议
```http
GET /api/v1/resumes/{resumeId}/suggestions
```

### 面试相关接口

#### 1. 创建面试会话
```http
POST /api/v1/interviews/sessions
Content-Type: application/json

{
    "resumeId": "简历ID",
    "interviewType": "技术面试",
    "questionCount": 10
}
```

#### 2. 获取当前问题
```http
GET /api/v1/interviews/sessions/{sessionId}/current-question
```

#### 3. 提交回答
```http
POST /api/v1/interviews/sessions/{sessionId}/answer
Content-Type: application/json

{
    "answer": "用户回答内容"
}
```

#### 4. 完成面试
```http
POST /api/v1/interviews/sessions/{sessionId}/complete
Content-Type: application/json

{
    "allAnswers": ["回答1", "回答2", ...]
}
```

## 🔧 部署运行

### 环境要求
- Java 17+
- Maven 3.6+
- OpenAI API Key

### 配置步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd JobSpark-Resume
```

2. **配置环境变量**
```bash
export OPENAI_API_KEY=your_openai_api_key
export SPRING_PROFILES_ACTIVE=dev
```

3. **运行应用**
```bash
mvn spring-boot:run
```

4. **访问应用**
```
http://localhost:8080
```

### 配置文件

`src/main/resources/application.yml`
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY}
      model-name: gpt-3.5-turbo
      temperature: 0.7
      timeout: 60s
```

## 🧪 功能演示

### 简历解析流程
1. 用户上传简历文件
2. 系统自动解析简历内容
3. 提取结构化信息（基本信息、经历、技能等）
4. 生成解析报告和优化建议

### 模拟面试流程
1. 基于简历内容生成个性化面试问题
2. 用户逐题回答，系统实时评估
3. 生成综合面试评估报告
4. 提供面试准备建议

## 🔍 RAG检索原理

### 向量检索流程
1. **文本嵌入**: 使用嵌入模型将文本转换为向量
2. **相似度计算**: 计算查询向量与问题向量的余弦相似度
3. **结果排序**: 按相似度降序排列检索结果
4. **混合检索**: 结合技能匹配和语义相似度

### 检索策略
- **向量检索**: 基于语义相似度的精准匹配
- **技能匹配**: 根据简历技能生成相关问题
- **混合策略**: 结合两种方法提高检索质量

## 🚀 扩展性设计

### 向量数据库扩展
当前使用内存向量数据库，可轻松扩展至：
- **Milvus**: 开源向量数据库
- **Pinecone**: 云原生向量数据库
- **Chroma**: 轻量级向量数据库

### AI模型扩展
支持多种AI模型集成：
- OpenAI GPT系列
- 本地部署的LLM模型
- 多模态模型支持

## 📊 性能优化

### 缓存策略
- 简历解析结果缓存
- 向量检索结果缓存
- 面试会话状态缓存

### 异步处理
- 文件上传异步处理
- AI推理异步执行
- 批量操作支持

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

- 感谢 LangChain4j 提供的优秀AI集成框架
- 感谢 Spring Boot 团队提供的强大开发框架
- 感谢 OpenAI 提供的先进AI能力

## 📞 联系我们

如有问题或建议，请通过以下方式联系：
- 项目地址: [GitHub Repository]
- 邮箱: contact@jobspark.com
- 文档: [项目Wiki]

---

**JobSpark Resume** - 让每一次求职都更加智能和高效！ 🚀