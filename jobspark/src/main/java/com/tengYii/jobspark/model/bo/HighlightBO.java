package com.tengYii.jobspark.model.bo;

import lombok.Builder;
import lombok.Data;

/**
 * 工作经历要点
 */
@Builder
@Data
public class HighlightBO {
    /**
     * 职责/业绩要点（Markdown格式）
     */
    private String highlightMarkdown;

    /**
     * 排序顺序（升序）
     */
    private Integer sortOrder;
}
