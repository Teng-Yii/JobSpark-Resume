package com.tengYii.jobspark.application.service;

import com.tengYii.jobspark.application.dto.ResumeUploadRequest;
import com.tengYii.jobspark.application.dto.ResumeUploadResponse;
import com.tengYii.jobspark.domain.model.Resume;
import com.tengYii.jobspark.domain.service.ResumeAnalysisService;
import com.tengYii.jobspark.domain.service.ResumeOptimizationService;
import com.tengYii.jobspark.infrastructure.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeApplicationService {

    private final FileStorageService fileStorageService;
    private final ResumeAnalysisService resumeAnalysisService;
    private final ResumeOptimizationService resumeOptimizationService;

    public ResumeUploadResponse uploadResume(ResumeUploadRequest request) {
        try {
            // 1、保存文件
            String resumeId = fileStorageService.storeResumeFile(request.getFile());

            // 解析简历内容
            Resume resume = resumeAnalysisService.analyzeResume(request.getFile(), request.getUserId(), request.getUserMessage());

            return new ResumeUploadResponse(
                    resumeId,
                    request.getFile().getOriginalFilename(),
                    "SUCCESS",
                    "简历上传成功，已开始解析",
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            log.error("简历上传失败", e);
            return new ResumeUploadResponse(
                    null,
                    request.getFile().getOriginalFilename(),
                    "FAILED",
                    "简历上传失败: " + e.getMessage(),
                    LocalDateTime.now()
            );
        }
    }

    public Object getResumeAnalysis(String resumeId) {
        // 获取简历解析结果
        return resumeAnalysisService.getResumeAnalysis(resumeId);
    }

    public Object getOptimizationSuggestions(String resumeId) {
        // 获取优化建议
        return resumeOptimizationService.getOptimizationSuggestions(resumeId);
    }
}