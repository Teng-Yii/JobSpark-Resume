package com.tengYii.jobspark.infrastructure.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 向量嵌入服务 - 生成文本的向量表示
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {
    
    /**
     * 为文本生成向量嵌入
     */
    public float[] generateEmbedding(String text) {
        try {
            // 这里应该调用实际的嵌入模型（如OpenAI的text-embedding-ada-002）
            // 暂时返回模拟向量
            return generateMockEmbedding(text);
            
        } catch (Exception e) {
            log.error("生成向量嵌入失败", e);
            return null;
        }
    }
    
    /**
     * 批量生成向量嵌入
     */
    public float[][] generateEmbeddings(List<String> texts) {
        return texts.stream()
            .map(this::generateEmbedding)
            .toArray(float[][]::new);
    }
    
    /**
     * 计算两个向量的余弦相似度
     */
    public double calculateCosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1 == null || vector2 == null || vector1.length != vector2.length) {
            return 0.0;
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += Math.pow(vector1[i], 2);
            norm2 += Math.pow(vector2[i], 2);
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * 生成模拟向量嵌入（用于测试）
     */
    private float[] generateMockEmbedding(String text) {
        // 生成384维的模拟向量（类似于常见的嵌入模型维度）
        float[] embedding = new float[384];
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] = (float) (Math.random() * 2 - 1); // 生成-1到1之间的随机数
        }
        
        // 归一化
        double norm = 0.0;
        for (float value : embedding) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        
        for (int i = 0; i < embedding.length; i++) {
            embedding[i] /= norm;
        }
        
        return embedding;
    }
}