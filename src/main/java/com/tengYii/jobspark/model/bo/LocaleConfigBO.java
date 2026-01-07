package com.tengYii.jobspark.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 国际化与日期格式配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocaleConfigBO {

    /**
     * 语言标识（如：zh-CN, en-US, ja-JP）
     */
    private String locale;

    /**
     * 本地化日期格式
     */
    private String datePattern;

    /**
     * 区块名称本地化（如：{"education":"教育经历","experience":"工作经历"}）
     */
    private String sectionLabels;
}