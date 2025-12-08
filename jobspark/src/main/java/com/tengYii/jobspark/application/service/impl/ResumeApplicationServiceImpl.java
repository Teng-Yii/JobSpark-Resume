package com.tengYii.jobspark.application.service.impl;

import com.tengYii.jobspark.application.service.ResumeApplicationService;
import com.tengYii.jobspark.domain.service.ResumePersistenceService;
import com.tengYii.jobspark.model.bo.CvBO;
import com.tengYii.jobspark.model.dto.FileStorageResultDTO;
import com.tengYii.jobspark.model.dto.ResumeUploadRequest;
import com.tengYii.jobspark.model.dto.ResumeUploadResponse;
import com.tengYii.jobspark.domain.service.ResumeAnalysisService;
import com.tengYii.jobspark.domain.service.ResumeOptimizationService;
import com.tengYii.jobspark.domain.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeApplicationServiceImpl implements ResumeApplicationService {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ResumeAnalysisService resumeAnalysisService;

    @Autowired
    private ResumeOptimizationService resumeOptimizationService;

    @Autowired
    private ResumePersistenceService resumePersistenceService;

    public ResumeUploadResponse uploadResume(ResumeUploadRequest request) {
        try {
            // 1、保存文件
            FileStorageResultDTO storageResultDTO = fileStorageService.saveUploadedFile(request.getFile(), null);

            // 2. 解析简历内容
            CvBO cvBO = resumeAnalysisService.analyzeResume(request);

            // 3. 将结构化简历对象落库
            Long resumeId = resumePersistenceService.convertAndSaveCv(cvBO);

            // 4. 返回结果
            return ResumeUploadResponse.success(
                    resumeId,
                    storageResultDTO.getUniqueFileName(),
                    request.getFile().getSize(),
                    request.getFile().getContentType()
            );
        } catch (Exception e) {
            log.error("简历上传失败", e);
            return ResumeUploadResponse.failure(
                    request.getFile().getOriginalFilename(),
                    e.getMessage()
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