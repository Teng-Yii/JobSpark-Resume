package com.tengYii.jobspark.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 面试相关DTO对象
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
class CreateInterviewRequest {
    private String resumeId;
    private String interviewType;
    private int questionCount;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class SubmitAnswerRequest {
    private String answer;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class CompleteInterviewRequest {
    private List<String> allAnswers;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class InterviewSessionResponse {
    private String sessionId;
    private String resumeId;
    private String interviewType;
    private int questionCount;
    private String status;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class InterviewQuestionResponse {
    private String questionId;
    private String content;
    private String type;
    private String skillTag;
    private String difficulty;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class InterviewEvaluationResponse {
    private double overallScore;
    private double technicalScore;
    private double communicationScore;
    private double problemSolvingScore;
    private double teamworkScore;
    private String strengths;
    private String improvementSuggestions;
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class InterviewStatusResponse {
    private String sessionId;
    private String status;
    private String currentQuestionId;
    private String currentQuestionContent;
}