package com.tengYii.jobspark.application.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ResumeUploadRequest {
    private MultipartFile file;
    private String jobTitle;
    private String industry;

    public ResumeUploadRequest(MultipartFile file, String jobTitle, String industry) {
        this.file = file;
        this.jobTitle = jobTitle;
        this.industry = industry;
    }
}