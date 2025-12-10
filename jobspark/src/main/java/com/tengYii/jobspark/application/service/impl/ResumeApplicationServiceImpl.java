package com.tengYii.jobspark.application.service.impl;

import com.tengYii.jobspark.application.service.ResumeApplicationService;
import com.tengYii.jobspark.common.enums.DeleteFlagEnum;
import com.tengYii.jobspark.common.enums.TaskStatusEnum;
import com.tengYii.jobspark.common.utils.SnowflakeUtil;
import com.tengYii.jobspark.domain.service.*;
import com.tengYii.jobspark.model.bo.CvBO;
import com.tengYii.jobspark.model.dto.FileStorageResultDTO;
import com.tengYii.jobspark.model.dto.ResumeUploadAsyncResponse;
import com.tengYii.jobspark.model.dto.ResumeUploadRequest;
import com.tengYii.jobspark.model.po.ResumeTaskPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

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
            String taskId = String.valueOf(SnowflakeUtil.snowflakeId());

            // 3. 创建任务记录（状态：处理中）
            LocalDateTime nowTime = LocalDateTime.now();
            ResumeTaskPO taskPO = ResumeTaskPO.builder()
                    .taskId(taskId)
//                    .userId(null)
                    .fileName(storageResultDTO.getUniqueFileName())
                    .originalFileName(request.getFile().getOriginalFilename())
                    .fileSize(request.getFile().getSize())
                    .contentType(request.getFile().getContentType())
                    .filePath(storageResultDTO.getFilePath())
                    .status(TaskStatusEnum.PROCESSING.getCode())
                    .startTime(nowTime)
                    .createTime(nowTime)
                    .updateTime(nowTime)
                    .deleteFlag(DeleteFlagEnum.NOT_DELETED.getCode())
                    .build();

            Boolean saveTaskSuccessFlag = resumeTaskService.saveTask(taskPO);
            if (Boolean.FALSE.equals(saveTaskSuccessFlag)) {
                log.error("简历上传解析，创建异步任务记录失败，userId: {}", request.getUserId());
            }

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

        Long resumeId = -1L;
        LocalDateTime nowTime = LocalDateTime.now();
        try {
            StopWatch stopWatch = new StopWatch();

            stopWatch.start("更新异步任务状态为解析中");
            // 更新任务状态为解析中
            Boolean updateTaskStatus = resumeTaskService.updateTaskStatus(taskId, TaskStatusEnum.ANALYZING);
            if (Boolean.FALSE.equals(updateTaskStatus)) {
                log.error("异步处理简历解析，更新异步任务状态失败，taskId: {}", taskId);
            }
            stopWatch.stop();

            // 解析简历内容（耗时操作）
            stopWatch.start("agent解析简历内容");
            CvBO cvBO = resumeAnalysisService.analyzeResume(request);
            stopWatch.stop();

            // 更新任务状态为存储中
            stopWatch.start("更新异步任务状态为存储中");
            updateTaskStatus = resumeTaskService.updateTaskStatus(taskId, TaskStatusEnum.SAVING);
            if (Boolean.FALSE.equals(updateTaskStatus)) {
                log.error("异步处理简历解析，更新异步任务状态失败，taskId: {}", taskId);
            }
            stopWatch.stop();

            // 将结构化简历对象落库（耗时操作）
            stopWatch.start("将结构化简历对象落库");

            resumeId = resumePersistenceService.convertAndSaveCv(cvBO, nowTime);
            stopWatch.stop();

            // 更新任务状态为完成
            Boolean completed = resumeTaskService.completeTask(taskId, resumeId, nowTime);
            if (completed) {
                log.info("任务完成，taskId: {}, resumeId: {}", taskId, resumeId);
            }

        } catch (Exception e) {
            log.error("异步处理简历失败，taskId: {}", taskId, e);
            // 更新任务状态为失败
            resumeTaskService.failTask(taskId, resumeId, nowTime, e.getMessage());
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