package com.tengYii.jobspark.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 证书/获奖
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateBO {

    /**
     * 证书/奖项名称
     */
    private String name;

    /**
     * 颁发机构
     */
    private String issuer;

    /**
     * 获得日期
     */
    private LocalDate date;

    /**
     * 证书描述（如：等级/分数，Markdown格式）
     */
    private String descriptionMarkdown;

    /**
     * 排序顺序（升序）
     */
    private Integer sortOrder;
}