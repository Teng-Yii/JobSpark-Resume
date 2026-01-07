package com.tengYii.jobspark.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ResumeUploadRequest {
    /**
     * 简历文件
     */
    private MultipartFile file;

    /**
     * 用户ID,唯一标识
     */
    private Long userId;

    /**
     * 用户信息
     */
    private String userMessage;
}