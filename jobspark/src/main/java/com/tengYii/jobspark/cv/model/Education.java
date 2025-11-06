package com.tengYii.jobspark.cv.model;

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
public class Education {
    private String school;
    private String major;
    private LocalDate startDate;
    private LocalDate endDate; // 可为空（在读）
    private RichText description; // Markdown 描述
}