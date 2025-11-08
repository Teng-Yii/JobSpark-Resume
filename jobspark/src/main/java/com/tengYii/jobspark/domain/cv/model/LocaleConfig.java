package com.tengYii.jobspark.domain.cv.model;

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
public class LocaleConfig {
    private String locale;        // 如 zh-CN, en-US
    private String datePattern;   // 如 yyyy.MM
}