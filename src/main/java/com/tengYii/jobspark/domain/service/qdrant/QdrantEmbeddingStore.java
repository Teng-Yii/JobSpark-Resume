package com.tengYii.jobspark.domain.service.qdrant;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.CosineSimilarity;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.RelevanceScore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.WithVectorsSelectorFactory;
import io.qdrant.client.grpc.Common.Filter;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.DeletePoints;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Collections.CollectionInfo;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points.PointsSelector;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static dev.langchain4j.internal.Utils.randomUUID;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;
import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;
import static io.qdrant.client.WithPayloadSelectorFactory.enable;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * 将 <a href="https://qdrant.tech/">Qdrant</a> 集合表示为嵌入存储库。
 * 支持存储 {@link dev.langchain4j.data.document.Metadata}。
 */
@Component
public class QdrantEmbeddingStore implements EmbeddingStore<TextSegment> {
    private static final Logger log = LoggerFactory.getLogger(QdrantEmbeddingStore.class);

    /**
     * Qdrant 客户端实例
     */
    private final QdrantClient client;
    /**
     * Qdrant 负载中存储文本片段的字段名称
     */
    private final String payloadTextKey;
    /**
     * Qdrant 集合名称
     */
    private final String collectionName;

    /**
     * 构造函数，使用 Spring 注入配置属性。
     *
     * @param collectionName Qdrant 集合名称，默认为 "resumes"
     * @param host           Qdrant 实例的主机地址，默认为 "localhost"
     * @param port           Qdrant 实例的 GRPC 端口，默认为 6334
     * @param useTls         是否使用 TLS (HTTPS)，默认为 false
     * @param payloadTextKey Qdrant 负载中存储文本片段的字段名称，默认为 "content"
     * @param apiKey         用于身份验证的 Qdrant API 密钥，可选
     */
    @Autowired
    public QdrantEmbeddingStore(
            @Value("${langchain4j.qdrant.collection-name:resumes}") String collectionName,
            @Value("${langchain4j.qdrant.host:localhost}") String host,
            @Value("${langchain4j.qdrant.port:6334}") int port,
            @Value("${langchain4j.qdrant.use-tls:false}") boolean useTls,
            @Value("${langchain4j.qdrant.payload-text-key:content}") String payloadTextKey,
            @Value("${langchain4j.qdrant.api-key:}") @Nullable String apiKey,
            @Value("${langchain4j.qdrant.vector-size:1536}") int vectorSize) {

        QdrantGrpcClient.Builder grpcClientBuilder = QdrantGrpcClient.newBuilder(host, port, useTls);

        if (StringUtils.isNotEmpty(apiKey)) {
            grpcClientBuilder.withApiKey(apiKey);
        }

        this.client = new QdrantClient(grpcClientBuilder.build());
        this.collectionName = collectionName;
        this.payloadTextKey = payloadTextKey;

        ensureCollectionExists(vectorSize);
    }

    /**
     * 确保 Qdrant 集合存在，如果不存在则创建。
     *
     * @param vectorSize 向量维度
     */
    private void ensureCollectionExists(int vectorSize) {
        List<String> collections = getUnchecked(client.listCollectionsAsync());
        boolean exists = collections.stream()
                .anyMatch(c -> StringUtils.equals(c, collectionName));

        if (exists) {
            // 检查现有集合的配置是否与当前 vectorSize 匹配
            CollectionInfo collectionInfo = getUnchecked(client.getCollectionInfoAsync(collectionName));
            // 假设默认使用名为 "" (空字符串) 的向量配置，或者只有一个未命名向量配置
            // Qdrant 的 vectors_config 可能包含单个 vector_params 或者 map 形式的 params
            // 这里简化处理：如果能获取到 vectors_count > 0 且 config 里的 size 不一致，则重建
            // 注意：Qdrant Java 客户端获取 params 结构可能比较复杂，需根据实际情况判断
            // 简单策略：如果获取到的 size 不匹配，则删除重建
            boolean sizeMismatch = false;
            if (collectionInfo.hasConfig() && collectionInfo.getConfig().hasParams()) {
                // 检查 vector params
                if (collectionInfo.getConfig().getParams().hasVectorsConfig()) {
                    if (collectionInfo.getConfig().getParams().getVectorsConfig().hasParams()) {
                        long existingSize = collectionInfo.getConfig().getParams().getVectorsConfig().getParams().getSize();
                        if (existingSize != vectorSize) {
                            sizeMismatch = true;
                            log.warn("检测到 Qdrant 集合 {} 维度不匹配: 期望 {}, 实际 {}", collectionName, vectorSize, existingSize);
                        }
                    }
                }
            }

            if (sizeMismatch) {
                log.info("正在删除维度不匹配的 Qdrant 集合: {}", collectionName);
                getUnchecked(client.deleteCollectionAsync(collectionName));
                // 标记为不存在，以便后续重建
                exists = false;
            }
        }

        if (!exists) {
            getUnchecked(client.createCollectionAsync(collectionName,
                    VectorParams.newBuilder()
                            .setSize(vectorSize)
                            .setDistance(Distance.Cosine)
                            .build()
            ));
            log.info("已自动创建 Qdrant 集合: {}, 向量维度: {}", collectionName, vectorSize);
        }
    }

    /**
     * 等待 Future 完成并返回结果，将检查型异常转换为运行时异常。
     *
     * @param future 要等待的 Future
     * @param <T>    返回结果的类型
     * @return Future 的结果
     */
    private static <T> T getUnchecked(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("操作被中断", e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 构造函数。
     *
     * @param client         Qdrant 客户端实例
     * @param collectionName Qdrant 集合名称
     * @param payloadTextKey Qdrant 负载中存储文本片段的字段名称
     */
    public QdrantEmbeddingStore(QdrantClient client, String collectionName, String payloadTextKey) {
        this.client = client;
        this.collectionName = collectionName;
        this.payloadTextKey = payloadTextKey;
    }

    @Override
    public String add(Embedding embedding) {
        String id = randomUUID();
        add(id, embedding);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        addInternal(id, embedding, null);
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = randomUUID();
        addInternal(id, embedding, textSegment);
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = embeddings.stream().map(ignored -> randomUUID()).toList();
        addAll(ids, embeddings, null);
        return ids;
    }

    /**
     * 内部添加方法，处理单个嵌入向量的添加。
     *
     * @param id          唯一标识符
     * @param embedding   嵌入向量
     * @param textSegment 文本片段（可选）
     */
    private void addInternal(String id, Embedding embedding, TextSegment textSegment) {
        addAll(singletonList(id), singletonList(embedding), Objects.isNull(textSegment) ? null : singletonList(textSegment));
    }

    @Override
    public void addAll(List<String> ids, List<Embedding> embeddings, List<TextSegment> textSegments) {
        if (CollectionUtils.isEmpty(ids) || CollectionUtils.isEmpty(embeddings)) {
            log.info("嵌入向量列表为空 - 不执行任何操作");
            return;
        }

        List<PointStruct> points = new ArrayList<>(embeddings.size());

        for (int i = 0; i < embeddings.size(); i++) {
            String id = ids.get(i);
            UUID uuid = UUID.fromString(id);
            Embedding embedding = embeddings.get(i);

            PointStruct.Builder pointBuilder =
                    PointStruct.newBuilder().setId(id(uuid)).setVectors(vectors(embedding.vector()));

            if (CollectionUtils.isNotEmpty(textSegments)) {
                Map<String, Object> metadata = textSegments.get(i).metadata().toMap();

                // 使用 HashMap 包装以确保 Map 可变，避免后续 put 操作抛出异常
                Map<String, JsonWithInt.Value> payload = new HashMap<>(ValueMapFactory.valueMap(metadata));
                payload.put(payloadTextKey, value(textSegments.get(i).text()));
                pointBuilder.putAllPayload(payload);
            }

            points.add(pointBuilder.build());
        }

        getUnchecked(client.upsertAsync(collectionName, points));
    }

    @Override
    public void remove(String id) {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("id 不能为空或空白字符串");
        }
        removeAll(Collections.singleton(id));
    }

    @Override
    public void removeAll(Collection<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new IllegalArgumentException("ids 集合不能为空");
        }

        Points.PointsIdsList pointsIdsList = Points.PointsIdsList.newBuilder()
                .addAllIds(ids.stream().map(id -> id(UUID.fromString(id))).toList())
                .build();
        PointsSelector pointsSelector =
                PointsSelector.newBuilder().setPoints(pointsIdsList).build();

        getUnchecked(client.deleteAsync(DeletePoints.newBuilder()
                .setCollectionName(collectionName)
                .setPoints(pointsSelector)
                .build()));
    }

    @Override
    public void removeAll(dev.langchain4j.store.embedding.filter.Filter filter) {
        ensureNotNull(filter, "filter");
        Filter qdrantFilter = QdrantFilterConverter.convertExpression(filter);
        PointsSelector pointsSelector =
                PointsSelector.newBuilder().setFilter(qdrantFilter).build();

        getUnchecked(client.deleteAsync(DeletePoints.newBuilder()
                .setCollectionName(collectionName)
                .setPoints(pointsSelector)
                .build()));
    }

    @Override
    public void removeAll() {
        clearStore();
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        SearchPoints.Builder searchBuilder = SearchPoints.newBuilder()
                .setCollectionName(collectionName)
                .addAllVector(request.queryEmbedding().vectorAsList())
                .setWithVectors(WithVectorsSelectorFactory.enable(true))
                .setWithPayload(enable(true))
                .setLimit(request.maxResults());

        if (Objects.nonNull(request.filter())) {
            Filter filter = QdrantFilterConverter.convertExpression(request.filter());
            searchBuilder.setFilter(filter);
        }

        List<ScoredPoint> results = getUnchecked(client.searchAsync(searchBuilder.build()));

        if (CollectionUtils.isEmpty(results)) {
            return new EmbeddingSearchResult<>(emptyList());
        }

        List<EmbeddingMatch<TextSegment>> matches = results.stream()
                .map(vector -> toEmbeddingMatch(vector, request.queryEmbedding()))
                .filter(match -> match.score() >= request.minScore())
                .sorted(comparingDouble(EmbeddingMatch::score))
                .collect(toList());

        Collections.reverse(matches);

        return new EmbeddingSearchResult<>(matches);
    }

    /**
     * 从 Qdrant 集合中删除所有数据点。
     */
    public void clearStore() {
        Filter emptyFilter = Filter.newBuilder().build();
        PointsSelector allPointsSelector =
                PointsSelector.newBuilder().setFilter(emptyFilter).build();

        getUnchecked(client.deleteAsync(DeletePoints.newBuilder()
                .setCollectionName(collectionName)
                .setPoints(allPointsSelector)
                .build()));
    }

    /**
     * 关闭底层的 GRPC 客户端。
     */
    public void close() {
        client.close();
    }

    /**
     * 将 ScoredPoint 转换为 EmbeddingMatch。
     *
     * @param scoredPoint        Qdrant 返回的评分点
     * @param referenceEmbedding 参考嵌入向量
     * @return 嵌入匹配对象
     */
    private EmbeddingMatch<TextSegment> toEmbeddingMatch(ScoredPoint scoredPoint, Embedding referenceEmbedding) {
        Map<String, JsonWithInt.Value> payload = scoredPoint.getPayloadMap();

        JsonWithInt.Value textSegmentValue = MapUtils.getObject(payload, payloadTextKey);

        Map<String, Object> metadata = payload.entrySet().stream()
                .filter(entry -> !StringUtils.equals(entry.getKey(), payloadTextKey))
                .collect(toMap(Map.Entry::getKey, entry -> ObjectFactory.object(entry.getValue())));

        Embedding embedding =
                Embedding.from(scoredPoint.getVectors().getVector().getDataList());
        double cosineSimilarity = CosineSimilarity.between(embedding, referenceEmbedding);

        return new EmbeddingMatch<>(
                RelevanceScore.fromCosineSimilarity(cosineSimilarity),
                scoredPoint.getId().getUuid(),
                embedding,
                Objects.isNull(textSegmentValue)
                        ? null
                        : TextSegment.from(textSegmentValue.getStringValue(), new Metadata(metadata)));
    }

}
