package com.tengYii.jobspark.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 项目经验
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectBO {

    /**
     * 项目名称
     */
    private String name;

    /**
     * 项目开始日期
     */
    private LocalDate startDate;

    /**
     * 项目结束日期
     */
    private LocalDate endDate;

    /**
     * 角色/职责
     */
    private String role;

    /**
     * 项目描述（Markdown格式）
     */
    private String descriptionMarkdown;

    /**
     * 排序顺序（升序）
     */
    private Integer sortOrder;

    /**
     * 项目经历亮点（Markdown格式）
     */
    private List<HighlightBO> highlights;
}