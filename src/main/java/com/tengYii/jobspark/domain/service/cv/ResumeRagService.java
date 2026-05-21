package com.tengYii.jobspark.domain.service.cv;

import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.rerank.TextReRank;
import com.alibaba.dashscope.rerank.TextReRankOutput;
import com.alibaba.dashscope.rerank.TextReRankParam;
import com.alibaba.dashscope.rerank.TextReRankResult;
import com.tengYii.jobspark.common.enums.ResultCodeEnum;
import com.tengYii.jobspark.common.exception.BusinessException;
import com.tengYii.jobspark.common.utils.RedisUtil;
import com.tengYii.jobspark.domain.service.qdrant.QdrantEmbeddingStore;
import com.tengYii.jobspark.model.bo.cv.CvBO;
import com.tengYii.jobspark.model.bo.cv.EducationBO;
import com.tengYii.jobspark.model.bo.cv.ExperienceBO;
import com.tengYii.jobspark.model.bo.cv.HighlightBO;
import com.tengYii.jobspark.model.bo.cv.ProjectBO;
import com.tengYii.jobspark.model.bo.cv.SkillBO;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsIn;
import dev.langchain4j.store.embedding.filter.logical.Or;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tengYii.jobspark.common.constants.ResumeRagConstants.*;

/**
 * 简历RAG服务（增强版）
 * <p>
 * 基于RAG全链路技术增强，提供：
 * <ul>
 *   <li><b>结构化语义分块</b>：按简历章节边界拆分，多粒度存储</li>
 *   <li><b>批量向量化</b>：embedAll批量处理，降低API调用开销</li>
 *   <li><b>HyDE假设文档嵌入</b>：生成假设简历提升语义召回（带缓存）</li>
 *   <li><b>多查询生成</b>：技术栈/行业经验/同义词多角度扩展查询（带缓存）</li>
 *   <li><b>层次化检索</b>：粗筛(overview/skills) -> 精检(全chunk)</li>
 *   <li><b>混合检索</b>：向量召回 + BM25重排 + RRF融合</li>
 *   <li><b>上下文增强</b>：同简历多chunk自动合并为完整上下文</li>
 *   <li><b>Rerank重排序</b>：DashScope Rerank模型精排</li>
 * </ul>
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

    @Autowired
    private RedisUtil redisUtil;

    // ==================== 公共API：存储 ====================

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
            // 1. 结构化语义分块
            List<ResumeChunk> chunks = chunkCvBO(cv);
            if (CollectionUtils.isEmpty(chunks)) {
                log.info("简历分块结果为空，跳过存储");
                return;
            }

            // 2. 转换为TextSegment（携带元数据）
            List<TextSegment> segments = chunks.stream()
                    .map(this::toTextSegment)
                    .collect(Collectors.toList());

            // 3. 批量向量化（性能优化：单次API调用处理所有chunk）
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

            // 4. 批量存储到EmbeddingStore
            embeddingStore.addAll(embeddings, segments);
            log.info("简历模板已分块存储至向量库，共 {} 个chunk，resumeId={}", chunks.size(), resolveResumeId(cv));

        } catch (Exception e) {
            log.error("存储简历模板失败", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "存储简历模板失败");
        }
    }

    // ==================== 公共API：检索 ====================

    /**
     * 根据职位描述检索相似的简历模板（增强版）
     * <p>
     * 检索链路：
     * <ol>
     *   <li><b>HyDE</b>：生成假设简历，扩展语义空间（带Redis缓存）</li>
     *   <li><b>多查询生成</b>：技术栈/行业经验/同义词 3个角度扩展（带Redis缓存）</li>
     *   <li><b>层次化粗筛</b>：先检索 overview/skills chunk 锁定候选简历集合</li>
     *   <li><b>混合精检</b>：多路向量召回 + BM25关键词重排 + RRF融合</li>
     *   <li><b>上下文增强</b>：同一简历的多chunk自动合并为完整上下文</li>
     *   <li><b>Rerank精排</b>：DashScope Rerank模型最终排序</li>
     * </ol>
     * </p>
     *
     * @param query 查询文本（通常是职位描述）
     * @param limit 最大返回数量
     * @return 匹配的模板内容列表
     */
    public List<String> retrieveTemplates(String query, int limit) {
        if (StringUtils.isEmpty(query)) {
            return Collections.emptyList();
        }

        long totalStart = System.currentTimeMillis();
        try {
            // Step 1: HyDE 假设文档嵌入（带缓存）
            String hydeResume = generateHyDE(query);

            // Step 2: 多查询生成（带缓存）
            List<String> allQueries = new ArrayList<>();
            allQueries.add(query); // 原始查询
            if (StringUtils.isNotEmpty(hydeResume)) {
                allQueries.add(hydeResume); // HyDE假设简历
            }
            List<String> multiQueries = generateMultiQueries(query);
            allQueries.addAll(multiQueries);

            log.info("检索查询集合（原始+HyDE+多查询共{}条）：{}", allQueries.size(), allQueries);

            // Step 3: 层次化检索 - 粗筛层（基于overview/skills快速锁定候选简历）
            Set<String> candidateResumeIds = hierarchicalCoarseSearch(allQueries, COARSE_RECALL_LIMIT);
            if (CollectionUtils.isNotEmpty(candidateResumeIds)) {
                log.info("层次化粗筛命中 {} 份候选简历", candidateResumeIds.size());
            } else {
                log.warn("层次化粗筛无结果，将退化为全库精检");
            }

            // Step 4: 混合检索 - 精检层（多路向量召回 + BM25重排 + RRF融合）
            List<RetrievedChunk> fusedChunks = hybridFineSearch(allQueries, candidateResumeIds, limit * FINE_RECALL_MULTIPLIER);
            log.info("混合检索精检召回 {} 个chunk", fusedChunks.size());

            // Step 5: 上下文增强（同简历chunk合并为完整上下文，按结构化顺序拼接）
            List<String> augmentedResults = contextAugment(fusedChunks, limit * 2);
            log.info("上下文增强后 {} 份完整简历", augmentedResults.size());

            // Step 6: LLM Reranking（DashScope Rerank模型精排）
            List<String> finalResults = rerank(query, augmentedResults, limit);

            log.info("完整RAG检索完成，query=[{}]，最终返回 {} 个结果，总耗时 {} ms",
                    StringUtils.abbreviate(query, 50), finalResults.size(), System.currentTimeMillis() - totalStart);
            return finalResults;

        } catch (Exception e) {
            log.error("检索简历模板失败: {}", query, e);
            return Collections.emptyList();
        }
    }

    // ==================== 结构化分块 ====================

    /**
     * 将简历按语义边界拆分为多个chunk
     * <p>
     * 分块策略：
     * <ul>
     *   <li><b>overview</b>：全简历摘要，聚合姓名、头衔、摘要、技能、行业、公司等关键信息（粗粒度索引）</li>
     *   <li><b>summary</b>：个人摘要独立块</li>
     *   <li><b>skills</b>：所有专业技能聚合为一个块</li>
     *   <li><b>experience</b>：每条工作经历独立成块，保留完整语义</li>
     *   <li><b>project</b>：每个项目经验独立成块</li>
     *   <li><b>education</b>：每条教育经历独立成块</li>
     * </ul>
     * </p>
     */
    private List<ResumeChunk> chunkCvBO(CvBO cv) {
        List<ResumeChunk> chunks = new ArrayList<>();
        String resumeId = resolveResumeId(cv);
        String cvType = StringUtils.defaultString(cv.getCvType(), "unknown");

        // 1. 全简历摘要块（粗粒度，用于层次化检索第一层）
        ResumeChunk overview = buildOverviewChunk(cv, resumeId, cvType);
        if (overview != null) {
            chunks.add(overview);
        }

        // 2. 个人摘要块
        if (StringUtils.isNotEmpty(cv.getSummary())) {
            chunks.add(ResumeChunk.builder()
                    .chunkType(CHUNK_TYPE_SUMMARY)
                    .content("Summary:\n" + cv.getSummary())
                    .metadata(buildBaseMetadata(resumeId, cvType, CHUNK_TYPE_SUMMARY, 0))
                    .build());
        }

        // 3. 专业技能块
        ResumeChunk skills = buildSkillsChunk(cv, resumeId, cvType);
        if (skills != null) {
            chunks.add(skills);
        }

        // 4. 工作经历块（每条独立，保留语义完整性）
        if (CollectionUtils.isNotEmpty(cv.getExperiences())) {
            int idx = 0;
            for (ExperienceBO exp : cv.getExperiences()) {
                if (exp == null) continue;
                ResumeChunk chunk = buildExperienceChunk(exp, resumeId, cvType, idx++);
                if (chunk != null) {
                    chunks.add(chunk);
                }
            }
        }

        // 5. 项目经验块（每个独立）
        if (CollectionUtils.isNotEmpty(cv.getProjects())) {
            int idx = 0;
            for (ProjectBO proj : cv.getProjects()) {
                if (proj == null) continue;
                ResumeChunk chunk = buildProjectChunk(proj, resumeId, cvType, idx++);
                if (chunk != null) {
                    chunks.add(chunk);
                }
            }
        }

        // 6. 教育经历块
        if (CollectionUtils.isNotEmpty(cv.getEducations())) {
            int idx = 0;
            for (EducationBO edu : cv.getEducations()) {
                if (edu == null) continue;
                ResumeChunk chunk = buildEducationChunk(edu, resumeId, cvType, idx++);
                if (chunk != null) {
                    chunks.add(chunk);
                }
            }
        }

        return chunks;
    }

    private String resolveResumeId(CvBO cv) {
        if (cv.getUserId() != null) {
            return String.valueOf(cv.getUserId());
        }
        // 对无userId的模板简历，使用name+title的hash作为稳定id
        return "tmpl_" + Math.abs(Objects.hash(cv.getName(), cv.getTitle()));
    }

    private Map<String, String> buildBaseMetadata(String resumeId, String cvType, String chunkType, int index) {
        Map<String, String> meta = new HashMap<>();
        meta.put(META_RESUME_ID, resumeId);
        meta.put(META_CV_TYPE, cvType);
        meta.put(META_CHUNK_TYPE, chunkType);
        meta.put(META_CHUNK_INDEX, String.valueOf(index));
        return meta;
    }

    private ResumeChunk buildOverviewChunk(CvBO cv, String resumeId, String cvType) {
        StringBuilder sb = new StringBuilder();
        sb.append("Resume Overview\n");
        if (StringUtils.isNotEmpty(cv.getName())) {
            sb.append("Name: ").append(cv.getName()).append("\n");
        }
        if (StringUtils.isNotEmpty(cv.getTitle())) {
            sb.append("Title: ").append(cv.getTitle()).append("\n");
        }
        if (StringUtils.isNotEmpty(cv.getSummary())) {
            sb.append("Summary: ").append(StringUtils.abbreviate(cv.getSummary(), 300)).append("\n");
        }

        List<String> skillNames = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(cv.getSkills())) {
            for (SkillBO s : cv.getSkills()) {
                if (s != null && StringUtils.isNotEmpty(s.getName())) {
                    skillNames.add(s.getName());
                }
            }
        }
        if (!skillNames.isEmpty()) {
            sb.append("Key Skills: ").append(String.join(", ", skillNames)).append("\n");
        }

        List<String> industries = new ArrayList<>();
        List<String> companies = new ArrayList<>();
        List<String> roles = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(cv.getExperiences())) {
            for (ExperienceBO e : cv.getExperiences()) {
                if (e == null) continue;
                if (StringUtils.isNotEmpty(e.getIndustry())) industries.add(e.getIndustry());
                if (StringUtils.isNotEmpty(e.getCompany())) companies.add(e.getCompany());
                if (StringUtils.isNotEmpty(e.getRole())) roles.add(e.getRole());
            }
        }
        if (!industries.isEmpty()) {
            sb.append("Industries: ").append(String.join(", ", industries)).append("\n");
        }
        if (!companies.isEmpty()) {
            sb.append("Companies: ").append(String.join(", ", companies)).append("\n");
        }
        if (!roles.isEmpty()) {
            sb.append("Roles: ").append(String.join(", ", roles)).append("\n");
        }

        String content = sb.toString().trim();
        if (StringUtils.isEmpty(content)) {
            return null;
        }

        Map<String, String> meta = buildBaseMetadata(resumeId, cvType, CHUNK_TYPE_OVERVIEW, 0);
        meta.put(META_SKILL_NAMES, String.join(", ", skillNames));
        meta.put(META_INDUSTRIES, String.join(", ", industries));
        meta.put(META_COMPANIES, String.join(", ", companies));
        meta.put(META_ROLES, String.join(", ", roles));

        return ResumeChunk.builder()
                .chunkType(CHUNK_TYPE_OVERVIEW)
                .content(content)
                .metadata(meta)
                .build();
    }

    private ResumeChunk buildSkillsChunk(CvBO cv, String resumeId, String cvType) {
        if (CollectionUtils.isEmpty(cv.getSkills())) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Skills:\n");
        List<String> skillNames = new ArrayList<>();
        for (SkillBO skill : cv.getSkills()) {
            if (skill == null) continue;
            String name = skill.getName();
            if (StringUtils.isEmpty(name)) continue;
            skillNames.add(name);
            sb.append("- ").append(name);
            if (StringUtils.isNotEmpty(skill.getLevel())) {
                sb.append(" (").append(skill.getLevel()).append(")");
            }
            if (CollectionUtils.isNotEmpty(skill.getHighlights())) {
                String highlights = skill.getHighlights().stream()
                        .filter(h -> h != null && StringUtils.isNotEmpty(h.getHighlight()))
                        .map(HighlightBO::getHighlight)
                        .collect(Collectors.joining("; "));
                if (StringUtils.isNotEmpty(highlights)) {
                    sb.append(": ").append(highlights);
                }
            }
            sb.append("\n");
        }
        if (skillNames.isEmpty()) {
            return null;
        }
        Map<String, String> meta = buildBaseMetadata(resumeId, cvType, CHUNK_TYPE_SKILLS, 0);
        meta.put(META_SKILL_NAMES, String.join(", ", skillNames));
        return ResumeChunk.builder()
                .chunkType(CHUNK_TYPE_SKILLS)
                .content(sb.toString().trim())
                .metadata(meta)
                .build();
    }

    private ResumeChunk buildExperienceChunk(ExperienceBO exp, String resumeId, String cvType, int index) {
        StringBuilder sb = new StringBuilder();
        sb.append("Work Experience:\n");
        sb.append("- ").append(StringUtils.defaultString(exp.getIndustry())).append(" at ")
                .append(StringUtils.defaultString(exp.getCompany()));
        if (StringUtils.isNotEmpty(exp.getRole())) {
            sb.append(" (").append(exp.getRole()).append(")");
        }
        sb.append("\n");
        if (StringUtils.isNotEmpty(exp.getDescription())) {
            sb.append("  Description: ").append(exp.getDescription()).append("\n");
        }
        if (CollectionUtils.isNotEmpty(exp.getHighlights())) {
            for (HighlightBO h : exp.getHighlights()) {
                if (h != null && StringUtils.isNotEmpty(h.getHighlight())) {
                    sb.append("  Highlight: ").append(h.getHighlight()).append("\n");
                }
            }
        }
        String content = sb.toString().trim();
        Map<String, String> meta = buildBaseMetadata(resumeId, cvType, CHUNK_TYPE_EXPERIENCE, index);
        meta.put(META_INDUSTRIES, StringUtils.defaultString(exp.getIndustry()));
        meta.put(META_COMPANIES, StringUtils.defaultString(exp.getCompany()));
        meta.put(META_ROLES, StringUtils.defaultString(exp.getRole()));
        return ResumeChunk.builder()
                .chunkType(CHUNK_TYPE_EXPERIENCE)
                .content(content)
                .metadata(meta)
                .build();
    }

    private ResumeChunk buildProjectChunk(ProjectBO proj, String resumeId, String cvType, int index) {
        StringBuilder sb = new StringBuilder();
        sb.append("Project:\n");
        sb.append("- ").append(StringUtils.defaultString(proj.getName()));
        if (StringUtils.isNotEmpty(proj.getRole())) {
            sb.append(" (Role: ").append(proj.getRole()).append(")");
        }
        sb.append("\n");
        if (StringUtils.isNotEmpty(proj.getDescription())) {
            sb.append("  Description: ").append(proj.getDescription()).append("\n");
        }
        if (CollectionUtils.isNotEmpty(proj.getHighlights())) {
            for (HighlightBO h : proj.getHighlights()) {
                if (h != null && StringUtils.isNotEmpty(h.getHighlight())) {
                    sb.append("  Highlight: ").append(h.getHighlight()).append("\n");
                }
            }
        }
        String content = sb.toString().trim();
        Map<String, String> meta = buildBaseMetadata(resumeId, cvType, CHUNK_TYPE_PROJECT, index);
        meta.put(META_ROLES, StringUtils.defaultString(proj.getRole()));
        return ResumeChunk.builder()
                .chunkType(CHUNK_TYPE_PROJECT)
                .content(content)
                .metadata(meta)
                .build();
    }

    private ResumeChunk buildEducationChunk(EducationBO edu, String resumeId, String cvType, int index) {
        StringBuilder sb = new StringBuilder();
        sb.append("Education:\n");
        sb.append("- ").append(StringUtils.defaultString(edu.getSchool())).append(", ")
                .append(StringUtils.defaultString(edu.getMajor())).append(", ")
                .append(StringUtils.defaultString(edu.getDegree())).append("\n");
        if (StringUtils.isNotEmpty(edu.getDescription())) {
            sb.append("  Description: ").append(edu.getDescription()).append("\n");
        }
        String content = sb.toString().trim();
        Map<String, String> meta = buildBaseMetadata(resumeId, cvType, CHUNK_TYPE_EDUCATION, index);
        return ResumeChunk.builder()
                .chunkType(CHUNK_TYPE_EDUCATION)
                .content(content)
                .metadata(meta)
                .build();
    }

    private TextSegment toTextSegment(ResumeChunk chunk) {
        Metadata metadata = new Metadata();
        if (chunk.getMetadata() != null) {
            chunk.getMetadata().forEach(metadata::put);
        }
        return TextSegment.from(chunk.getContent(), metadata);
    }

    // ==================== 检索增强策略 ====================

    /**
     * HyDE（假设文档嵌入）- 带Redis缓存
     */
    private String generateHyDE(String query) {
        String cacheKey = CACHE_KEY_HYDE + DigestUtils.md5DigestAsHex(query.getBytes(StandardCharsets.UTF_8));
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof String cachedStr && StringUtils.isNotEmpty(cachedStr)) {
            log.info("HyDE结果命中缓存");
            return cachedStr;
        }

        try {
            String prompt = "请根据以下职位描述，生成一份高度匹配的候选人简历摘要和核心技能列表。主要包含Summary和Skills部分即可，不要包含虚构的联系方式。\n\n职位描述：\n" + query;
            String hypotheticalResume = hyDEModel.chat(prompt);
            if (StringUtils.isNotEmpty(hypotheticalResume)) {
                redisUtil.set(cacheKey, hypotheticalResume, CACHE_TTL_HOUR, TimeUnit.HOURS);
                log.info("HyDE生成成功并已缓存，长度={}", hypotheticalResume.length());
                return hypotheticalResume;
            }
        } catch (Exception e) {
            log.warn("HyDE生成失败，将回退使用原始查询", e);
        }
        return null;
    }

    /**
     * 多查询生成（Multi-query generation）- 带Redis缓存
     * <p>
     * 生成多个不同角度的查询变体，提升召回覆盖率：
     * 1. 技术栈和硬技能角度
     * 2. 行业经验和业务领域角度
     * 3. 通用表达与同义词角度
     * </p>
     */
    private List<String> generateMultiQueries(String query) {
        String cacheKey = CACHE_KEY_MQ + DigestUtils.md5DigestAsHex(query.getBytes(StandardCharsets.UTF_8));
        Object cached = redisUtil.get(cacheKey);
        if (cached instanceof List<?> list) {
            log.info("多查询结果命中缓存");
            return list.stream()
                    .map(Object::toString)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toList());
        }

        try {
            String prompt = "你是一位招聘专家。请针对以下职位描述，生成" + MULTI_QUERY_COUNT
                    + "个不同角度但语义相关的查询语句，用于在简历库中检索最匹配的候选人。要求：\n"
                    + "1. 第一个查询侧重技术栈和硬技能\n"
                    + "2. 第二个查询侧重行业经验和业务领域\n"
                    + "3. 第三个查询使用更通用的表达，覆盖同义词和相近概念\n"
                    + "每个查询单独一行，不要编号，不要解释，只输出查询文本。\n\n职位描述：\n" + query;
            String response = chatModel.chat(prompt);
            if (StringUtils.isNotEmpty(response)) {
                List<String> queries = Arrays.stream(response.split("\\n"))
                        .map(String::trim)
                        .filter(s -> StringUtils.isNotEmpty(s) && s.length() > 5)
                        .limit(MULTI_QUERY_COUNT)
                        .collect(Collectors.toList());
                if (!queries.isEmpty()) {
                    redisUtil.set(cacheKey, queries, CACHE_TTL_HOUR, TimeUnit.HOURS);
                    log.info("多查询生成成功：{}", queries);
                    return queries;
                }
            }
        } catch (Exception e) {
            log.warn("多查询生成失败", e);
        }
        return Collections.emptyList();
    }

    /**
     * 层次化检索 - 粗筛层
     * <p>
     * 仅检索粗粒度chunk（overview + skills），快速锁定相关候选简历集合。
     * 利用Qdrant的payload过滤功能，只搜索chunk_type为overview或skills的数据。
     * </p>
     */
    private Set<String> hierarchicalCoarseSearch(List<String> queries, int targetLimit) {
        Set<String> resumeIds = new LinkedHashSet<>();
        // 构建OR过滤条件：chunk_type = overview OR chunk_type = skills
        Or typeFilter = new Or(
                new IsEqualTo(META_CHUNK_TYPE, CHUNK_TYPE_OVERVIEW),
                new IsEqualTo(META_CHUNK_TYPE, CHUNK_TYPE_SKILLS)
        );

        for (String q : queries) {
            if (StringUtils.isEmpty(q)) continue;
            try {
                Embedding emb = embeddingModel.embed(q).content();
                EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                        .queryEmbedding(emb)
                        .minScore(VECTOR_MIN_SCORE_COARSE)
                        .maxResults(targetLimit * 2)
                        .filter(typeFilter)
                        .build();
                EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
                for (EmbeddingMatch<TextSegment> match : result.matches()) {
                    TextSegment seg = match.embedded();
                    if (seg == null || seg.metadata() == null) {
                        continue;
                    }
                    String rid = seg.metadata().getString(META_RESUME_ID);
                    if (StringUtils.isNotEmpty(rid)) {
                        resumeIds.add(rid);
                        if (resumeIds.size() >= targetLimit) {
                            return resumeIds;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("层次化粗筛检索失败, query={}", StringUtils.abbreviate(q, 30), e);
            }
        }
        return resumeIds;
    }

    /**
     * 混合检索 - 精检层
     * <p>
     * 1. 多路向量召回：每个查询（原始+HyDE+多查询）独立做向量检索
     * 2. BM25重排：对每路召回的候选chunk计算BM25关键词相关性分数并重新排序
     * 3. RRF融合：使用Reciprocal Rank Fusion算法融合多路排序结果
     * </p>
     */
    private List<RetrievedChunk> hybridFineSearch(List<String> queries, Set<String> candidateResumeIds, int limit) {
        List<List<RetrievedChunk>> rankedLists = new ArrayList<>();

        // 构建精检filter：限制在候选简历范围内（如果粗筛有结果）
        Filter resumeFilter = null;
        if (CollectionUtils.isNotEmpty(candidateResumeIds)) {
            resumeFilter = new IsIn(META_RESUME_ID, new ArrayList<>(candidateResumeIds));
        }

        for (String q : queries) {
            if (StringUtils.isEmpty(q)) {
                continue;
            }
            try {
                // 向量召回
                List<RetrievedChunk> chunks = vectorSearchChunks(q, resumeFilter, limit);
                if (chunks.isEmpty()) continue;

                // 提取文本用于BM25
                List<String> texts = chunks.stream()
                        .map(c -> c.text)
                        .distinct()
                        .collect(Collectors.toList());

                // BM25重排
                Map<String, Double> bm25Scores = Bm25Scorer.score(texts, q);
                List<RetrievedChunk> bm25Ranked = chunks.stream()
                        .sorted(Comparator.comparingDouble(
                                (RetrievedChunk c) -> bm25Scores.getOrDefault(c.text, 0.0)).reversed())
                        .distinct()
                        .collect(Collectors.toList());

                rankedLists.add(bm25Ranked);
                log.info("查询 [{}] 向量召回{}个chunk，BM25重排完成", StringUtils.abbreviate(q, 30), bm25Ranked.size());

            } catch (Exception e) {
                log.warn("混合精检检索失败, query={}", StringUtils.abbreviate(q, 30), e);
            }
        }

        if (rankedLists.isEmpty()) {
            return Collections.emptyList();
        }

        // RRF融合多路BM25排序结果
        return rrfFusionChunks(rankedLists, RRF_K, limit);
    }

    /**
     * 向量检索chunk（保留metadata）
     */
    private List<RetrievedChunk> vectorSearchChunks(String query, Filter filter, int limit) {
        Embedding emb = embeddingModel.embed(query).content();
        var builder = EmbeddingSearchRequest.builder()
                .queryEmbedding(emb)
                .minScore(VECTOR_MIN_SCORE_FINE)
                .maxResults(limit);
        if (filter != null) {
            builder.filter(filter);
        }
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(builder.build());

        List<RetrievedChunk> chunks = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : result.matches()) {
            TextSegment seg = match.embedded();
            if (seg == null) continue;
            Metadata meta = seg.metadata();
            String resumeId = (meta != null) ? StringUtils.defaultString(meta.getString(META_RESUME_ID)) : "";
            String chunkType = (meta != null) ? StringUtils.defaultString(meta.getString(META_CHUNK_TYPE)) : "";
            int chunkIndex = 0;
            if (meta != null) {
                String idxStr = meta.getString(META_CHUNK_INDEX);
                if (StringUtils.isNotEmpty(idxStr)) {
                    try {
                        chunkIndex = Integer.parseInt(idxStr);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            chunks.add(new RetrievedChunk(seg.text(), resumeId, chunkType, chunkIndex));
        }
        return chunks;
    }

    /**
     * RRF（Reciprocal Rank Fusion）融合多路chunk排序列表
     */
    private List<RetrievedChunk> rrfFusionChunks(List<List<RetrievedChunk>> rankedLists, int k, int limit) {
        // 使用 resumeId + chunkType + chunkIndex 作为唯一键
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, RetrievedChunk> chunkMap = new HashMap<>();

        for (List<RetrievedChunk> list : rankedLists) {
            for (int i = 0; i < list.size(); i++) {
                RetrievedChunk c = list.get(i);
                String key = c.resumeId + "|" + c.chunkType + "|" + c.chunkIndex;
                double score = 1.0 / (k + i + 1);
                rrfScores.merge(key, score, Double::sum);
                chunkMap.putIfAbsent(key, c);
            }
        }

        // 将RRF分数写回chunk对象
        List<RetrievedChunk> fused = new ArrayList<>();
        for (Map.Entry<String, Double> entry : rrfScores.entrySet()) {
            RetrievedChunk c = chunkMap.get(entry.getKey());
            if (c != null) {
                c.rrfScore = entry.getValue();
                fused.add(c);
            }
        }

        return fused.stream()
                .sorted(Comparator.comparingDouble((RetrievedChunk c) -> c.rrfScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 上下文增强：按resume_id合并同一简历的多个chunk，形成完整上下文
     * <p>
     * 当同一简历的多个chunk被命中时，按结构化顺序（overview -> summary -> skills -> experience -> project -> education）
     * 拼接为一份完整简历，提供更丰富的上下文信息供下游Rerank和生成使用。
     * </p>
     */
    private List<String> contextAugment(List<RetrievedChunk> chunks, int limit) {
        if (CollectionUtils.isEmpty(chunks)) {
            return Collections.emptyList();
        }

        // 按 resumeId 分组，记录每份简历的最佳RRF分数
        Map<String, List<RetrievedChunk>> grouped = new HashMap<>();
        Map<String, Double> resumeBestScore = new HashMap<>();

        for (RetrievedChunk c : chunks) {
            grouped.computeIfAbsent(c.resumeId, k -> new ArrayList<>()).add(c);
            resumeBestScore.merge(c.resumeId, c.rrfScore, Math::max);
        }

        // 按简历最佳RRF分数降序排序
        List<String> sortedResumeIds = resumeBestScore.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<String> results = new ArrayList<>();
        for (String resumeId : sortedResumeIds) {
            List<RetrievedChunk> resumeChunks = grouped.get(resumeId);
            String merged = mergeResumeChunks(resumeChunks);
            if (StringUtils.isNotEmpty(merged)) {
                results.add(merged);
            }
        }
        return results.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * 将同一简历的多个chunk按预定类型顺序拼接为完整简历文本
     */
    private String mergeResumeChunks(List<RetrievedChunk> chunks) {
        if (CollectionUtils.isEmpty(chunks)) {
            return "";
        }
        // 去重并按类型顺序排序
        Map<String, RetrievedChunk> dedup = new LinkedHashMap<>();
        for (RetrievedChunk c : chunks) {
            String key = c.chunkType + "|" + c.chunkIndex;
            dedup.putIfAbsent(key, c);
        }
        List<RetrievedChunk> sorted = new ArrayList<>(dedup.values());
        sorted.sort(Comparator.comparingInt(c -> chunkTypeOrder(c.chunkType)));

        StringBuilder sb = new StringBuilder();
        for (RetrievedChunk c : sorted) {
            sb.append(c.text).append("\n\n");
        }
        return sb.toString().trim();
    }

    private int chunkTypeOrder(String type) {
        return switch (StringUtils.defaultString(type)) {
            case CHUNK_TYPE_OVERVIEW -> 0;
            case CHUNK_TYPE_SUMMARY -> 1;
            case CHUNK_TYPE_SKILLS -> 2;
            case CHUNK_TYPE_EXPERIENCE -> 3;
            case CHUNK_TYPE_PROJECT -> 4;
            case CHUNK_TYPE_EDUCATION -> 5;
            default -> 99;
        };
    }

    // ==================== Rerank ====================

    /**
     * 使用 DashScope Rerank 模型对候选简历进行重排序
     * <p>
     * rerank模型会根据查询与文档的相关性进行打分排序
     * </p>
     *
     * @param query      职位描述
     * @param candidates 候选简历列表
     * @param limit      最终返回数量
     * @return 排序后的简历列表
     */
    private List<String> rerank(String query, List<String> candidates, int limit) {
        log.info("开始使用 DashScope Rerank 模型对 {} 个候选结果进行重排序", candidates.size());
        long startTime = System.currentTimeMillis();

        if (CollectionUtils.isEmpty(candidates)) {
            log.warn("候选列表为空，直接返回空列表");
            return Collections.emptyList();
        }

        try {
            TextReRank textReRank = new TextReRank();
            TextReRankParam param = TextReRankParam.builder()
                    .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                    .model("qwen3-vl-rerank")
                    .query(query)
                    .documents(candidates)
                    .topN(limit)
                    .returnDocuments(true)
                    .build();

            TextReRankResult rerankResult = textReRank.call(param);
            TextReRankOutput resultOutput = rerankResult.getOutput();

            List<String> rankedResults = resultOutput.getResults().stream()
                    .map(TextReRankOutput.Result::getDocument)
                    .map(TextReRankOutput.Document::getText)
                    .collect(Collectors.toList());

            long costTime = System.currentTimeMillis() - startTime;
            log.info("DashScope Rerank 重排序完成，返回 {} 个结果，耗时 {} ms", rankedResults.size(), costTime);
            return rankedResults;

        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            log.error("调用 DashScope Rerank 模型失败: {}", e.getMessage(), e);
            log.warn("Rerank 失败，降级返回原始候选列表的前 {} 个结果", limit);
            return candidates.stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        }
    }

    // ==================== 内部数据类 ====================

    /**
     * 检索到的chunk内部对象，保留metadata用于上下文增强和RRF融合
     */
    private static class RetrievedChunk {
        final String text;
        final String resumeId;
        final String chunkType;
        final int chunkIndex;
        double rrfScore;

        RetrievedChunk(String text, String resumeId, String chunkType, int chunkIndex) {
            this.text = text;
            this.resumeId = resumeId;
            this.chunkType = chunkType;
            this.chunkIndex = chunkIndex;
            this.rrfScore = 0.0;
        }
    }
}
