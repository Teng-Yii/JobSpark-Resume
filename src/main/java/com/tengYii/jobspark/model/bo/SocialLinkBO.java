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
public class SocialLinkBO {

    /**
     * 社交链接名称（如：GitHub/CSDN等）
     *
     * @see com.tengYii.jobspark.common.enums.SocialLinkTypeEnum
     */
    private String label;

    /**
     * 链接地址
     */
    private String url;

    /**
     * 排序顺序（升序）
     */
    private Integer sortOrder;
}