package com.tengYii.jobspark.domain.service.qdrant;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * Qdrant 向量存储集成测试类
 * 用于验证向量数据的插入和查询功能是否正常
 */
@SpringBootTest
public class QdrantEmbeddingStoreTest {

    private static final Logger log = LoggerFactory.getLogger(QdrantEmbeddingStoreTest.class);

    @Autowired
    private QdrantEmbeddingStore qdrantEmbeddingStore;

    @Autowired
    private EmbeddingModel embeddingModel;

    /**
     * 测试插入并查询向量数据
     * 流程：
     * 1. 准备文本数据
     * 2. 调用模型生成向量
     * 3. 存储到 Qdrant
     * 4. 检索并验证
     *
     * @throws InterruptedException 如果线程休眠被中断
     */
    @Test
    void testInsertAndSearch() throws InterruptedException {
        // 1. 准备测试数据
        String testContent = "这是一个用于测试 Qdrant 插入和查询功能的文本，时间戳：" + System.currentTimeMillis();
        log.info("开始测试，准备插入文本: {}", testContent);
        System.out.println(">>> [Step 1] 正在准备测试数据: " + testContent);

        TextSegment textSegment = TextSegment.from(testContent);

        // 2. 将文本转换为向量
        log.info("正在调用 EmbeddingModel 生成向量...");
        System.out.println(">>> [Step 2] 正在调用 EmbeddingModel 生成向量...");
        Embedding embedding = embeddingModel.embed(textSegment).content();
        log.info("向量生成成功，维度: {}", embedding.dimension());

        // 3. 插入数据到 Qdrant
        log.info("正在将数据插入 Qdrant...");
        System.out.println(">>> [Step 3] 正在执行插入操作...");
        String id = qdrantEmbeddingStore.add(embedding, textSegment);
        log.info("数据插入成功，ID: {}", id);
        System.out.println(">>> [Step 3] 插入成功，返回 ID: " + id);

        // 4. 稍作等待，确保数据已索引 (Qdrant 通常很快，但为了保险起见等待 1 秒)
        System.out.println(">>> [Step 4] 等待 1 秒以确保索引完成...");
        Thread.sleep(1000);

        // 5. 执行查询
        log.info("正在执行查询操作...");
        System.out.println(">>> [Step 5] 正在查询刚刚插入的文本...");

        // 构建查询请求，期望匹配度大于 0.8
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(1)
                .minScore(0.8)
                .build();

        EmbeddingSearchResult<TextSegment> result = qdrantEmbeddingStore.search(request);
        List<EmbeddingMatch<TextSegment>> matches = result.matches();

        // 6. 验证结果
        System.out.println(">>> [Step 6] 查询完成，结果数量: " + matches.size());

        // 使用 Apache Commons CollectionUtils 判空规范
        Assertions.assertTrue(CollectionUtils.isNotEmpty(matches), "查询结果不应为空");

        EmbeddingMatch<TextSegment> bestMatch = matches.get(0);
        System.out.println(">>> 最佳匹配分数: " + bestMatch.score());
        System.out.println(">>> 最佳匹配内容: " + bestMatch.embedded().text());

        // 断言内容一致且分数符合预期
        Assertions.assertEquals(testContent, bestMatch.embedded().text(), "查询到的内容应与插入的内容一致");
        Assertions.assertTrue(bestMatch.score() > 0.9, "匹配分数应较高 (> 0.9)");

        log.info("测试通过！");
        System.out.println(">>> [Success] 测试全部通过！");
    }
}