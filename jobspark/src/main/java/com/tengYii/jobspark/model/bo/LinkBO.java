package com.tengYii.jobspark.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 社交链接（如 GitHub、博客、个人网站等）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkBO {

    /**
     * 社交链接名称（如：GitHub/CSDN）
     */
    private String label;

    /**
     * 链接地址
     */
    private String url;
}