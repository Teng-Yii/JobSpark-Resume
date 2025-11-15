package com.tengYii.jobspark.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestionResponse {
    private String questionId;
    private String content;
    private String type;
    private String skillTag;
    private String difficulty;
}