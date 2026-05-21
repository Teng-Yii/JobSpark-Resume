package com.tengYii.jobspark.domain.service.cv;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 轻量级 BM25 打分器
 * <p>
 * 用于混合检索中对向量召回的候选结果进行关键词相关性重排序。
 * 采用经典的 BM25 公式：
 * <pre>
 *   score(D, Q) = sum(IDF(q_i) * (f(q_i, D) * (k1 + 1)) / (f(q_i, D) + k1 * (1 - b + b * |D| / avgDL)))
 * </pre>
 * 其中分词采用简单规则：保留中文、英文、数字字符，其余作为分隔符。
 * </p>
 */
public class Bm25Scorer {

    private static final double K1 = 1.5;
    private static final double B = 0.75;

    /**
     * 对候选文档列表计算与查询的 BM25 相关性分数
     *
     * @param documents 候选文档列表（已去重）
     * @param query     查询文本
     * @return 文档内容 -> BM25 分数 的映射
     */
    public static Map<String, Double> score(List<String> documents, String query) {
        if (CollectionUtils.isEmpty(documents) || StringUtils.isEmpty(query)) {
            return Collections.emptyMap();
        }

        List<List<String>> docTokens = documents.stream()
                .map(Bm25Scorer::tokenize)
                .collect(Collectors.toList());
        List<String> queryTokens = tokenize(query);

        if (queryTokens.isEmpty()) {
            return Collections.emptyMap();
        }

        double avgDocLen = docTokens.stream().mapToInt(List::size).average().orElse(0.0);
        int N = documents.size();

        // 计算每个查询词的 IDF
        Map<String, Double> idfMap = new HashMap<>();
        for (String term : queryTokens) {
            int n = 0;
            for (List<String> tokens : docTokens) {
                if (tokens.contains(term)) {
                    n++;
                }
            }
            double idf = Math.log((N - n + 0.5) / (n + 0.5) + 1.0);
            idfMap.put(term, idf);
        }

        // 计算每个文档的 BM25 分数
        Map<String, Double> scores = new LinkedHashMap<>();
        for (int i = 0; i < documents.size(); i++) {
            List<String> tokens = docTokens.get(i);
            int docLen = tokens.size();
            double score = 0.0;

            for (String term : queryTokens) {
                long tf = tokens.stream().filter(t -> t.equals(term)).count();
                double idf = idfMap.getOrDefault(term, 0.0);
                double denom = tf + K1 * (1 - B + B * docLen / Math.max(avgDocLen, 1.0));
                if (denom > 0) {
                    score += idf * (tf * (K1 + 1.0)) / denom;
                }
            }
            scores.put(documents.get(i), score);
        }
        return scores;
    }

    /**
     * 简单分词：保留中文、英文、数字字符，其余作为分隔符
     */
    private static List<String> tokenize(String text) {
        if (StringUtils.isEmpty(text)) {
            return Collections.emptyList();
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        for (char c : text.toLowerCase().toCharArray()) {
            if (Character.isLetterOrDigit(c) || isChinese(c)) {
                sb.append(c);
            } else {
                if (sb.length() > 0) {
                    tokens.add(sb.toString());
                    sb.setLength(0);
                }
            }
        }
        if (sb.length() > 0) {
            tokens.add(sb.toString());
        }
        return tokens;
    }

    private static boolean isChinese(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A;
    }
}
