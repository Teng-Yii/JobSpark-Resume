package com.tengYii.jobspark.domain.service;

import com.tengYii.jobspark.model.bo.CvBO;
import com.tengYii.jobspark.model.bo.HighlightBO;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.CosineSimilarity;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import com.tengYii.jobspark.domain.service.qdrant.QdrantEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    /**
     * 将简历模板存储到向量数据库
     *
     * @param cv 简历业务对象
     */
    public void storeTemplate(CvBO cv) {
        // 使用 Objects 和 StringUtils 进行判空检查
        if (Objects.isNull(cv)) {
            log.warn("简历对象为空，无法存储模板");
            return;
        }

        try {
            StringBuilder contentBuilder = new StringBuilder();

            // 1. 处理个人摘要
            if (StringUtils.isNotEmpty(cv.getSummary())) {
                contentBuilder.append("Summary:\n").append(cv.getSummary()).append("\n\n");
            }

            // 2. 处理专业技能
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

            // 3. 处理工作经历
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

            // 4. 处理项目经验
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

            // 5. 处理教育经历
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

            String templateContent = contentBuilder.toString();
            if (StringUtils.isEmpty(templateContent)) {
                log.warn("简历内容为空，跳过存储");
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
            // 1. 初筛：使用 EmbeddingStore 检索相似文本
            // 为了更好的重排序效果，这里检索的数量比 limit 多一些，比如 2 倍
            int searchLimit = limit * 2;

            // 生成查询向量
            Embedding queryEmbedding = embeddingModel.embed(query).content();

            // 构建检索请求
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(searchLimit)
                    .build();

            // 执行检索
            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(request);

            // 提取候选文本
            List<String> candidates = searchResult.matches().stream()
                    .map(EmbeddingMatch::embedded)
                    .filter(Objects::nonNull)
                    .map(TextSegment::text)
                    .collect(Collectors.toList());

            // 使用 CollectionUtils 检查候选列表
            if (CollectionUtils.isEmpty(candidates)) {
                return Collections.emptyList();
            }

            // 2. 重排序 (Rerank)
            // 对初步检索的结果进行二次排序以提高准确性
            return rerankTemplates(query, candidates, limit);

        } catch (Exception e) {
            log.error("检索简历模板失败: {}", query, e);
            return Collections.emptyList();
        }
    }

    /**
     * 对候选模板进行重排序
     *
     * @param query      查询文本
     * @param candidates 候选模板列表
     * @param limit      最终返回数量
     * @return 排序后的模板列表
     */
    private List<String> rerankTemplates(String query, List<String> candidates, int limit) {
        // 使用 CollectionUtils 检查候选列表
        if (CollectionUtils.isEmpty(candidates)) {
            return Collections.emptyList();
        }

        // 重新生成查询文本的向量（如果上层方法已生成，实际可传递 Embedding 对象以优化性能，此处按要求重新生成或保持接口独立）
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        // 计算相似度并排序
        List<Map.Entry<String, Double>> scoredCandidates = new ArrayList<>();

        for (String candidate : candidates) {
            // 使用 StringUtils 检查候选文本
            if (StringUtils.isEmpty(candidate)) {
                continue;
            }

            // 生成候选文本的向量
            // 注意：这步操作在数据量大时会比较耗时，实际生产中通常直接从向量库获取向量或使用专门的轻量级 Rerank 模型
            Embedding candidateEmbedding = embeddingModel.embed(candidate).content();

            // 计算余弦相似度
            double similarity = CosineSimilarity.between(queryEmbedding, candidateEmbedding);
            scoredCandidates.add(Map.entry(candidate, similarity));
        }

        // 按相似度降序排序
        scoredCandidates.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // 取前 limit 个结果并返回文本内容
        return scoredCandidates.stream()
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}