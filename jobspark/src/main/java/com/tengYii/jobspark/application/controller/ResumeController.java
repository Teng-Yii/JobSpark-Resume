package com.tengYii.jobspark.application.controller;

import com.tengYii.jobspark.application.dto.ResumeUploadRequest;
import com.tengYii.jobspark.application.dto.ResumeUploadResponse;
import com.tengYii.jobspark.application.service.ResumeApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeApplicationService resumeApplicationService;

    /**
     * 上传简历，并进行简历优化
     * @param request 简历上传请求对象
     * @return
     */
    @PostMapping("/upload")
    public ResponseEntity<ResumeUploadResponse> uploadResume(@ModelAttribute ResumeUploadRequest request) {

        ResumeUploadResponse response = resumeApplicationService.uploadResume(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{resumeId}/analysis")
    public ResponseEntity<?> getResumeAnalysis(@PathVariable String resumeId) {
        // 获取简历解析结果
        return ResponseEntity.ok(resumeApplicationService.getResumeAnalysis(resumeId));
    }

    @GetMapping("/{resumeId}/suggestions")
    public ResponseEntity<?> getOptimizationSuggestions(@PathVariable String resumeId) {
        // 获取优化建议
        return ResponseEntity.ok(resumeApplicationService.getOptimizationSuggestions(resumeId));
    }
}