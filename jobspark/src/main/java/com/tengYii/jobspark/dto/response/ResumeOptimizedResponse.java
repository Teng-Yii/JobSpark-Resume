package com.tengYii.jobspark.dto.response;

import com.tengYii.jobspark.model.bo.CvBO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 简历优化响应DTO
 *
 * @author tengYii
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeOptimizedResponse {

    /**
     * 优化建议文本，用于指导用户修改简历。
     */
    private String suggestionText;

    /**
     * 优化后的简历ID，用于生成优化后的简历文件
     */
    private Long optimizedResumeId;

    /**
     * 优化历史记录
     */
    private List<CvBO.OptimizationRecord> optimizationHistory;
}
