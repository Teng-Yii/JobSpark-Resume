package com.tengYii.jobspark.dto.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 面试模拟请求request
 *
 * @author tengYii
 * @since 1.0.0
 */
@Data
public class InterviewSimulationRequest implements Serializable {

    /**
     * 用户ID,唯一标识
     */
    private Long userId;

    /**
     * 简历ID
     */
    private String resumeId;

    /**
     * 用于根据目标职位描述信息针对想面试
     */
    private String jobDescription;

    /**
     * 用户对话内容
     */
    private String userMessage;
}
