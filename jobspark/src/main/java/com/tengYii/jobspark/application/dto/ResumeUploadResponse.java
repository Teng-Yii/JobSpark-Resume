package com.tengYii.jobspark.application.dto;

import lombok.Data;

@Data
public class ResumeUploadResponse {
    private String resumeId;
    private String fileName;
    private String status;
    private String message;
    private String uploadTime;

    public ResumeUploadResponse(String resumeId, String fileName, String status, String message, String uploadTime) {
        this.resumeId = resumeId;
        this.fileName = fileName;
        this.status = status;
        this.message = message;
        this.uploadTime = uploadTime;
    }
}