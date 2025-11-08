package com.tengYii.jobspark.application.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ResumeUploadRequest {
    /**
     * 简历文件
     */
    private MultipartFile file;

    /**
     * 记忆id，用于隔离用户对话
     */
    private int memoryId;

    /**
     * 用户信息
     */
    private String userMessage;
}