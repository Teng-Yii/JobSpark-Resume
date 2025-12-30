package com.tengYii.jobspark.model.llm;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 向量检索重排序，带分数的候选结果
 */
@Data
@AllArgsConstructor
public class ScoredCandidate {

    /**
     * 简历文本
     */
    private String content;

    /**
     * 匹配度得分
     */
    private int score;
}
