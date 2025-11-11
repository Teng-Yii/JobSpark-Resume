package com.tengYii.jobspark.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ResumeUploadResponse {
    /**
     * 上传简历的唯一标识
     */
    private String resumeId;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 上传状态
     */
    private String status;

    /**
     * 上传消息
     */
    private String message;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

}