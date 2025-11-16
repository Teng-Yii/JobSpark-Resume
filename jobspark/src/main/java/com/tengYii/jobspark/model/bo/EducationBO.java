package com.tengYii.jobspark.model.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 教育经历：校名、专业、起止日期与描述
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EducationBO {

    /**
     * 学校名称
     */
    private String school;

    /**
     * 专业名称
     */
    private String major;

    /**
     * 学历（如：本科/硕士/博士）
     */
    private String degree;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期（可为空表示在读）
     */
    private LocalDate endDate;

    /**
     * 描述（如：GPA/荣誉等，Markdown格式）
     */
    private String description;
}