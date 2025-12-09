package com.tengYii.jobspark.application.service.impl;

import com.tengYii.jobspark.application.service.ResumeApplicationService;
import com.tengYii.jobspark.common.enums.TaskStatusEnum;
import com.tengYii.jobspark.domain.service.*;
import com.tengYii.jobspark.model.bo.CvBO;
import com.tengYii.jobspark.model.dto.FileStorageResultDTO;
import com.tengYii.jobspark.model.dto.ResumeUploadAsyncResponse;
import com.tengYii.jobspark.model.dto.ResumeUploadRequest;
import com.tengYii.jobspark.model.dto.ResumeUploadResponse;
import com.tengYii.jobspark.model.po.ResumeTaskPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


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

    @Autowired
    private ResumeTaskService resumeTaskService;

    @Autowired
    @Qualifier("resumeTaskExecutor")
    private Executor resumeTaskExecutor;

    /**
     * 上传简历
     * 将简历文件上传到OSS，并解析简历文件，保存结构化数据
     *
     * @param request 简历上传请求对象，包含要上传的简历信息
     * @return 简历上传响应对象
     */
    public ResumeUploadAsyncResponse uploadAndParseResumeAsync(ResumeUploadRequest request) {
        try {
            // 1. 立即保存文件（快速操作）
            FileStorageResultDTO storageResultDTO = fileStorageService.saveUploadedFile(request.getFile(), null);

            // 2. 生成任务ID
            String taskId = UUID.randomUUID().toString();

            // 3. 创建任务记录（状态：处理中）
            ResumeTaskPO taskPO = ResumeTaskPO.builder()
                    .taskId(taskId)
                    .fileName(storageResultDTO.getUniqueFileName())
                    .originalFileName(request.getFile().getOriginalFilename())
                    .fileSize(request.getFile().getSize())
                    .contentType(request.getFile().getContentType())
                    .status(TaskStatusEnum.PROCESSING.getCode())
                    .createTime(LocalDateTime.now())
                    .build();

            resumeTaskService.saveTask(taskPO);

            // 4. 异步执行耗时操作
            CompletableFuture.runAsync(() -> {
                processResumeAsync(taskId, request, storageResultDTO);
            }, resumeTaskExecutor);

            // 5. 立即返回任务ID
            return ResumeUploadAsyncResponse.success(taskId, storageResultDTO.getUniqueFileName());

        } catch (Exception e) {
            log.error("简历上传失败", e);
            return ResumeUploadAsyncResponse.failure(request.getFile().getOriginalFilename(), e.getMessage());
        }
    }

    /**
     * 异步处理简历解析
     *
     * @param taskId           任务ID
     * @param request          原始请求
     * @param storageResultDTO 文件存储结果
     */
    @Async
    private void processResumeAsync(String taskId, ResumeUploadRequest request, FileStorageResultDTO storageResultDTO) {
        try {
            // 更新任务状态为解析中
            resumeTaskService.updateTaskStatus(taskId, TaskStatusEnum.ANALYZING);

            // 解析简历内容（耗时操作）
            CvBO cvBO = resumeAnalysisService.analyzeResume(request);

            // 更新任务状态为存储中
            resumeTaskService.updateTaskStatus(taskId, TaskStatusEnum.SAVING);

            // 将结构化简历对象落库（耗时操作）
            Long resumeId = resumePersistenceService.convertAndSaveCv(cvBO);

            // 更新任务状态为完成
            resumeTaskService.completeTask(taskId, resumeId, cvBO);

        } catch (Exception e) {
            log.error("异步处理简历失败，taskId: {}", taskId, e);
            // 更新任务状态为失败
            resumeTaskService.failTask(taskId, e.getMessage());
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