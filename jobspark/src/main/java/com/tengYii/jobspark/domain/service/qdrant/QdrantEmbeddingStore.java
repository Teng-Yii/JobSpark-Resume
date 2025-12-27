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
            @Value("${langchain4j.qdrant.api-key:#{null}}") @Nullable String apiKey) {

        QdrantGrpcClient.Builder grpcClientBuilder = QdrantGrpcClient.newBuilder(host, port, useTls);

        if (StringUtils.isNotEmpty(apiKey)) {
            grpcClientBuilder.withApiKey(apiKey);
        }

        this.client = new QdrantClient(grpcClientBuilder.build());
        this.collectionName = collectionName;
        this.payloadTextKey = payloadTextKey;
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

        try {
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

            client.upsertAsync(collectionName, points).get();
        } catch (InterruptedException | ExecutionException e) {
            // 恢复中断状态
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(e);
        }
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

        try {
            Points.PointsIdsList pointsIdsList = Points.PointsIdsList.newBuilder()
                    .addAllIds(ids.stream().map(id -> id(UUID.fromString(id))).toList())
                    .build();
            PointsSelector pointsSelector =
                    PointsSelector.newBuilder().setPoints(pointsIdsList).build();

            client.deleteAsync(DeletePoints.newBuilder()
                            .setCollectionName(collectionName)
                            .setPoints(pointsSelector)
                            .build())
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeAll(dev.langchain4j.store.embedding.filter.Filter filter) {
        ensureNotNull(filter, "filter");
        try {
            Filter qdrantFilter = QdrantFilterConverter.convertExpression(filter);
            PointsSelector pointsSelector =
                    PointsSelector.newBuilder().setFilter(qdrantFilter).build();

            client.deleteAsync(DeletePoints.newBuilder()
                            .setCollectionName(collectionName)
                            .setPoints(pointsSelector)
                            .build())
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(e);
        }
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

        List<ScoredPoint> results;
        try {
            results = client.searchAsync(searchBuilder.build()).get();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(e);
        }

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
        try {
            Filter emptyFilter = Filter.newBuilder().build();
            PointsSelector allPointsSelector =
                    PointsSelector.newBuilder().setFilter(emptyFilter).build();

            client.deleteAsync(DeletePoints.newBuilder()
                            .setCollectionName(collectionName)
                            .setPoints(allPointsSelector)
                            .build())
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(e);
        }
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
