package com.tengYii.jobspark.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 工作/实习经历
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceBO {

    /**
     * 经历类型（全职/实习/兼职/ freelance）
     */
    private String type;

    /**
     * 公司名称
     */
    private String company;

    /**
     * 行业（如：互联网/金融）
     */
    private String industry;

    /**
     * 职位名称
     */
    private String role;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期（可为空表示在职）
     */
    private LocalDate endDate;

    /**
     * 工作概述（Markdown格式）
     */
    private String descriptionMarkdown;

    /**
     * 排序顺序（升序）
     */
    private Integer sortOrder;

    /**
     * 工作经历亮点（Markdown格式）
     */
    private List<HighlightBO> highlights;
}