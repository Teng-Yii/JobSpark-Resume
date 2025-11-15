package com.tengYii.jobspark.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 面试相关DTO对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {
    private String answer;
}