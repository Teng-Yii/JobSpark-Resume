package com.tengYii.jobspark.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewEvaluationResponse {
    private double overallScore;
    private double technicalScore;
    private double communicationScore;
    private double problemSolvingScore;
    private double teamworkScore;
    private String strengths;
    private String improvementSuggestions;
}
