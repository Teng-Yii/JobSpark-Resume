package com.tengYii.jobspark.domain.service.cv;

import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.rerank.TextReRank;
import com.alibaba.dashscope.rerank.TextReRankOutput;
import com.alibaba.dashscope.rerank.TextReRankParam;
import com.alibaba.dashscope.rerank.TextReRankResult;
import com.tengYii.jobspark.model.bo.cv.CvBO;
import com.tengYii.jobspark.model.bo.cv.HighlightBO;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import com.tengYii.jobspark.domain.service.qdrant.QdrantEmbeddingStore;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 简历RAG服务
 * <p>
 * 提供简历模板的存储、检索和排序功能
 * 使用 LangChain4j 的 EmbeddingModel 和 EmbeddingStore 进行向量操作
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeRagService {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private QdrantEmbeddingStore embeddingStore;

    @Resource(name = "hyDEModel")
    private ChatModel hyDEModel;

    @Resource(name = "chatModel")
    private ChatModel chatModel;

    /**
     * 将简历模板存储到向量数据库
     *
     * @param cv 简历业务对象
     */
    public void storeCvBO(CvBO cv) {
        if (Objects.isNull(cv)) {
            log.info("简历对象为空，无法存储模板");
            return;
        }

        try {
            StringBuilder contentBuilder = new StringBuilder();

            // 1. 处理个人摘要
            if (StringUtils.isNotEmpty(cv.getSummary())) {
                contentBuilder.append("Summary:\n").append(cv.getSummary()).append("\n\n");
            }
            // 2. 处理专业技能
            processSkills(cv, contentBuilder);
            // 3. 处理工作经历
            processExperiences(cv, contentBuilder);
            // 4. 处理项目经验
            processProjects(cv, contentBuilder);
            // 5. 处理教育经历
            processEducations(cv, contentBuilder);

            String templateContent = contentBuilder.toString();
            if (StringUtils.isEmpty(templateContent)) {
                log.info("简历内容为空，跳过存储");
                return;
            }

            // 创建文本段
            TextSegment textSegment = TextSegment.from(templateContent);
            // 使用 EmbeddingModel 生成向量
            Embedding embedding = embeddingModel.embed(textSegment).content();
            // 存储到 EmbeddingStore
            embeddingStore.add(embedding, textSegment);
            log.info("简历模板已存储至向量库");
        } catch (Exception e) {
            log.error("存储简历模板失败", e);
        }
    }

    /**
     * 处理教育经历
     *
     * @param cv             简历业务对象
     * @param contentBuilder 内容构建器
     */
    private void processEducations(CvBO cv, StringBuilder contentBuilder) {
        if (CollectionUtils.isNotEmpty(cv.getEducations())) {
            contentBuilder.append("Education:\n");
            cv.getEducations().forEach(edu -> {
                if (Objects.nonNull(edu)) {
                    contentBuilder.append("- ").append(StringUtils.defaultString(edu.getSchool())).append(", ")
                            .append(StringUtils.defaultString(edu.getMajor())).append(", ")
                            .append(StringUtils.defaultString(edu.getDegree())).append("\n");
                }
            });
            contentBuilder.append("\n");
        }
    }

    /**
     * 处理项目经验
     *
     * @param cv             简历业务对象
     * @param contentBuilder 内容构建器
     */
    private void processProjects(CvBO cv, StringBuilder contentBuilder) {
        if (CollectionUtils.isNotEmpty(cv.getProjects())) {
            contentBuilder.append("Projects:\n");
            cv.getProjects().forEach(proj -> {
                if (Objects.nonNull(proj)) {
                    contentBuilder.append("- ").append(StringUtils.defaultString(proj.getName())).append("\n");
                    if (StringUtils.isNotEmpty(proj.getDescription())) {
                        contentBuilder.append("  Description: ").append(proj.getDescription()).append("\n");
                    }
                    if (CollectionUtils.isNotEmpty(proj.getHighlights())) {
                        proj.getHighlights().forEach(highlight -> {
                            if (Objects.nonNull(highlight) && StringUtils.isNotEmpty(highlight.getHighlight())) {
                                contentBuilder.append("  Highlight: ").append(highlight.getHighlight()).append("\n");
                            }
                        });
                    }
                }
            });
            contentBuilder.append("\n");
        }
    }

    /**
     * 处理工作经历
     *
     * @param cv             简历业务对象
     * @param contentBuilder 内容构建器
     */
    private void processExperiences(CvBO cv, StringBuilder contentBuilder) {
        if (CollectionUtils.isNotEmpty(cv.getExperiences())) {
            contentBuilder.append("Work Experience:\n");
            cv.getExperiences().forEach(exp -> {
                if (Objects.nonNull(exp)) {
                    contentBuilder.append("- ")
                            .append(StringUtils.defaultString(exp.getIndustry())).append(" at ")
                            .append(StringUtils.defaultString(exp.getCompany())).append("\n");
                    if (StringUtils.isNotEmpty(exp.getDescription())) {
                        contentBuilder.append("  Description: ").append(exp.getDescription()).append("\n");
                    }
                    if (CollectionUtils.isNotEmpty(exp.getHighlights())) {
                        exp.getHighlights().forEach(highlight -> {
                            if (Objects.nonNull(highlight) && StringUtils.isNotEmpty(highlight.getHighlight())) {
                                contentBuilder.append("  Highlight: ").append(highlight.getHighlight()).append("\n");
                            }
                        });
                    }
                }
            });
            contentBuilder.append("\n");
        }
    }

    /**
     * 处理专业技能
     *
     * @param cv             简历业务对象
     * @param contentBuilder 内容构建器
     */
    private void processSkills(CvBO cv, StringBuilder contentBuilder) {
        if (CollectionUtils.isNotEmpty(cv.getSkills())) {
            contentBuilder.append("Skills:\n")
                    .append(cv.getSkills().stream()
                            .filter(Objects::nonNull)
                            .map(skill -> {
                                String skillName = skill.getName();
                                if (StringUtils.isEmpty(skillName)) {
                                    return null;
                                }
                                if (CollectionUtils.isNotEmpty(skill.getHighlights())) {
                                    String highlights = skill.getHighlights().stream()
                                            .filter(Objects::nonNull)
                                            .map(HighlightBO::getHighlight)
                                            .filter(StringUtils::isNotEmpty)
                                            .collect(Collectors.joining("; "));
                                    if (StringUtils.isNotEmpty(highlights)) {
                                        return skillName + " (" + highlights + ")";
                                    }
                                }
                                return skillName;
                            })
                            .filter(StringUtils::isNotEmpty)
                            .collect(Collectors.joining(", ")))
                    .append("\n\n");
        }
    }

    /**
     * 根据职位描述检索相似的简历模板
     *
     * @param query 查询文本（通常是职位描述）
     * @param limit 最大返回数量
     * @return 匹配的模板内容列表
     */
    public List<String> retrieveTemplates(String query, int limit) {
        // 使用 StringUtils 检查查询文本
        if (StringUtils.isEmpty(query)) {
            return Collections.emptyList();
        }

        try {
            // HyDE (Hypothetical Document Embeddings) 增强检索
            // 第一步：生成假设性简历
            String prompt = "请根据以下职位描述，生成一份高度匹配的候选人简历摘要和核心技能列表。主要包含Summary和Skills部分即可，不要包含虚构的联系方式。\n\n职位描述：\n" + query;
            String hypotheticalResume = hyDEModel.chat(prompt);

            if (StringUtils.isNotEmpty(hypotheticalResume)) {
                log.info("HyDE生成的假设简历内容: {}", hypotheticalResume);
            } else {
                log.warn("HyDE生成内容为空，将回退使用原始查询");
                hypotheticalResume = query;
            }

            // 第二步：将假设性简历转换为向量
            Embedding queryEmbedding = embeddingModel.embed(hypotheticalResume).content();

            // 构建检索请求
            // 设置minScore为0.7以过滤低相关性结果
            // 扩大召回范围，为Rerank做准备 (limit * 3)
            int recallLimit = limit * 3;
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .minScore(0.7)
                    .maxResults(recallLimit)
                    .build();

            // 执行检索
            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(request);

            // 提取粗排结果
            List<String> candidates = searchResult.matches().stream()
                    .map(EmbeddingMatch::embedded)
                    .filter(Objects::nonNull)
                    .map(TextSegment::text)
                    .collect(Collectors.toList());

            if (CollectionUtils.isEmpty(candidates)) {
                return Collections.emptyList();
            }

            // LLM Reranking (重排序)
            return rerank(query, candidates, limit);

        } catch (Exception e) {
            log.error("检索简历模板失败: {}", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * 使用 DashScope Rerank 模型对候选简历片段进行重排序
     * <p>
     * rerank模型会根据查询与文档的相关性进行打分排序
     * </p>
     *
     * @param query      职位描述
     * @param candidates 候选简历片段列表
     * @param limit      最终返回数量
     * @return 排序后的简历片段列表
     */
    private List<String> rerank(String query, List<String> candidates, int limit) {
        log.info("开始使用 DashScope Rerank 模型对 {} 个候选结果进行重排序", candidates.size());
        long startTime = System.currentTimeMillis();

        if (CollectionUtils.isEmpty(candidates)) {
            log.warn("候选列表为空，直接返回空列表");
            return Collections.emptyList();
        }

        try {
            // 创建 TextReRank 对象并构建参数
            TextReRank textReRank = new TextReRank();
            TextReRankParam param = TextReRankParam.builder()
                    .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                    .model("qwen3-vl-rerank")
                    .query(query)
                    .documents(candidates)
                    .topN(limit)
                    // 返回原始文档
                    .returnDocuments(true)
                    .build();

            // 调用 rerank 模型
            TextReRankResult rerankResult = textReRank.call(param);
            TextReRankOutput resultOutput = rerankResult.getOutput();

            // 提取排序后的结果
            List<String> rankedResults = resultOutput.getResults().stream()
                    .map(TextReRankOutput.Result::getDocument)
                    .map(TextReRankOutput.Document::getText)
                    .collect(Collectors.toList());


            long costTime = System.currentTimeMillis() - startTime;
            log.info("DashScope Rerank 重排序完成，返回 {} 个结果，耗时: {} ms", rankedResults.size(), costTime);
            return rankedResults;

        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            log.error("调用 DashScope Rerank 模型失败: {}", e.getMessage(), e);
            // 降级处理：返回原始列表的前 limit 个元素
            log.warn("Rerank 失败，降级返回原始候选列表的前 {} 个结果", limit);
            return candidates.stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 解析 LLM 返回的分数
     *
     * @param response LLM 响应
     * @return 分数
     */
    private int parseScore(String response) {
        if (StringUtils.isEmpty(response)) {
            return 0;
        }
        try {
            // 尝试提取数字
            Pattern pattern = Pattern.compile("(\\d+)");
            Matcher matcher = pattern.matcher(response);
            if (matcher.find()) {
                int score = Integer.parseInt(matcher.group(1));
                return Math.min(Math.max(score, 0), 100); // 限制在 0-100 之间
            }
        } catch (Exception e) {
            log.warn("解析分数失败: {}", response);
        }
        return 0;
    }
}