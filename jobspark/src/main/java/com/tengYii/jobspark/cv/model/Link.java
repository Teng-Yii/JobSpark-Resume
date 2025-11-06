package com.tengYii.jobspark.cv.model;

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
public class Link {
    private String label;    // 链接名称
    private String url;      // 链接地址
}