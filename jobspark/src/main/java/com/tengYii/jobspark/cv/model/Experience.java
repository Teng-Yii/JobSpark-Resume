package com.tengYii.jobspark.cv.model;

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
public class Experience {
    private String company;
    private String role;
    private LocalDate startDate;
    private LocalDate endDate;            // 可为空（在职）
    private List<RichText> highlights;    // 职责/业绩要点（Markdown 列表）
}