package com.tengYii.jobspark.dto.request;

import lombok.Data;

/**
 * 面试继续请求对象
 * 用户回答问题后继续面试时使用
 */
@Data
public class InterviewContinueRequest {

    /**
     * 用户回答内容
     */
    private String userAnswer;
}