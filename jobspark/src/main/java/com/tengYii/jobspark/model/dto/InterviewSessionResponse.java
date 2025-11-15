package com.tengYii.jobspark.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSessionResponse {
    private String sessionId;
    private String resumeId;
    private String interviewType;
    private int questionCount;
    private String status;
}
