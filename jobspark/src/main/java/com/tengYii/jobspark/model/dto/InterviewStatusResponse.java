package com.tengYii.jobspark.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewStatusResponse {
    private String sessionId;
    private String status;
    private String currentQuestionId;
    private String currentQuestionContent;
}