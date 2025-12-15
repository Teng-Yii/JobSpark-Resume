package com.tengYii.jobspark.dto.request;

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
    private Long resumeId;
    private String interviewType;
    private int questionCount;
}