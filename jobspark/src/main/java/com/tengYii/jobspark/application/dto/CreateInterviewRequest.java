package com.tengYii.jobspark.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建面试请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInterviewRequest {
    private String resumeId;
    private String interviewType;
    private int questionCount;
}