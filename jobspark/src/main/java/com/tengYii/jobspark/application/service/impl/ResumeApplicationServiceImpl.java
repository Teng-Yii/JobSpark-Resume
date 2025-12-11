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
import com.tengYii.jobspark.model.dto.TaskStatusResponse;
import com.tengYii.jobspark.model.po.ResumeTaskPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
                processResumeAsync(taskId, request);
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
     * @param taskId  任务ID
     * @param request 原始请求
     */
    @Async
    private void processResumeAsync(String taskId, ResumeUploadRequest request) {

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

    /**
     * 获取任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态响应对象
     */
    @Override
    public TaskStatusResponse getTaskStatus(String taskId) {
        if (StringUtils.isEmpty(taskId)) {
            log.warn("获取任务状态失败，任务ID为空");
            return null;
        }

        try {
            ResumeTaskPO taskPO = resumeTaskService.getByTaskId(taskId);
            if (Objects.isNull(taskPO)) {
                log.warn("获取任务状态失败，任务不存在，taskId: {}", taskId);
                return null;
            }

            return convertToTaskStatusResponse(taskPO);
        } catch (Exception e) {
            log.error("获取任务状态异常，taskId: {}", taskId, e);
            return null;
        }
    }

    /**
     * 获取用户任务列表
     *
     * @param userId 用户ID（可选）
     * @param status 任务状态（可选）
     * @return 任务列表
     */
    @Override
    public List<TaskStatusResponse> getUserTasks(Long userId, String status) {
        try {
            List<ResumeTaskPO> taskPOList = resumeTaskService.getUserTasks(userId, status);

            if (CollectionUtils.isEmpty(taskPOList)) {
                return new ArrayList<>();
            }

            List<TaskStatusResponse> responseList = new ArrayList<>();
            for (ResumeTaskPO taskPO : taskPOList) {
                TaskStatusResponse response = convertToTaskStatusResponse(taskPO);
                if (Objects.nonNull(response)) {
                    responseList.add(response);
                }
            }

            return responseList;
        } catch (Exception e) {
            log.error("获取用户任务列表异常，userId: {}, status: {}", userId, status, e);
            return new ArrayList<>();
        }
    }

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     * @return 取消是否成功
     */
    @Override
    public Boolean cancelTask(String taskId) {
        if (StringUtils.isEmpty(taskId)) {
            log.warn("取消任务失败，任务ID为空");
            return Boolean.FALSE;
        }

        try {
            ResumeTaskPO taskPO = resumeTaskService.getByTaskId(taskId);
            if (Objects.isNull(taskPO)) {
                log.warn("取消任务失败，任务不存在，taskId: {}", taskId);
                return Boolean.FALSE;
            }

            // 只有处理中、解析中、存储中的任务才能取消
            String currentStatus = taskPO.getStatus();
            if (StringUtils.equals(currentStatus, TaskStatusEnum.COMPLETED.getCode()) ||
                    StringUtils.equals(currentStatus, TaskStatusEnum.FAILED.getCode())) {
                log.warn("取消任务失败，任务已完成或失败，taskId: {}, status: {}", taskId, currentStatus);
                return Boolean.FALSE;
            }

            // 更新任务状态为失败，并设置错误信息
            LocalDateTime nowTime = LocalDateTime.now();
            resumeTaskService.failTask(taskId, taskPO.getResumeId(), nowTime, "任务已被用户取消");

            log.info("任务取消成功，taskId: {}", taskId);
            return Boolean.TRUE;
        } catch (Exception e) {
            log.error("取消任务异常，taskId: {}", taskId, e);
            return Boolean.FALSE;
        }
    }

    /**
     * 将ResumeTaskPO转换为TaskStatusResponse
     *
     * @param taskPO 任务持久化对象
     * @return 任务状态响应对象
     */
    private TaskStatusResponse convertToTaskStatusResponse(ResumeTaskPO taskPO) {
        if (Objects.isNull(taskPO)) {
            return null;
        }

        TaskStatusResponse.TaskStatusResponseBuilder builder = TaskStatusResponse.builder()
                .taskId(taskPO.getTaskId())
                .status(taskPO.getStatus())
                .statusMessage(getStatusMessage(taskPO.getStatus()))
                .startTime(taskPO.getStartTime())
                .completeTime(taskPO.getCompleteTime())
                .resumeId(taskPO.getResumeId())
                .errorMessage(taskPO.getErrorMessage())
                .fileName(taskPO.getFileName())
                .originalFileName(taskPO.getOriginalFileName());

        // 计算进度百分比
        Integer progress = calculateProgress(taskPO.getStatus());
        builder.progress(progress);

        // 计算预估剩余时间
        Long estimatedRemainingSeconds = calculateEstimatedRemainingTime(taskPO);
        builder.estimatedRemainingSeconds(estimatedRemainingSeconds);

        return builder.build();
    }

    /**
     * 根据任务状态获取状态描述
     *
     * @param status 任务状态码
     * @return 状态描述
     */
    private String getStatusMessage(String status) {
        if (StringUtils.isEmpty(status)) {
            return "未知状态";
        }

        for (TaskStatusEnum statusEnum : TaskStatusEnum.values()) {
            if (StringUtils.equals(status, statusEnum.getCode())) {
                return statusEnum.getDesc();
            }
        }
        return "未知状态";
    }

    /**
     * 根据任务状态计算进度百分比
     *
     * @param status 任务状态码
     * @return 进度百分比（0-100）
     */
    private Integer calculateProgress(String status) {
        if (StringUtils.isEmpty(status)) {
            return 0;
        }

        if (StringUtils.equals(status, TaskStatusEnum.PROCESSING.getCode())) {
            return 10;
        } else if (StringUtils.equals(status, TaskStatusEnum.ANALYZING.getCode())) {
            return 50;
        } else if (StringUtils.equals(status, TaskStatusEnum.SAVING.getCode())) {
            return 80;
        } else if (StringUtils.equals(status, TaskStatusEnum.COMPLETED.getCode())) {
            return 100;
        } else if (StringUtils.equals(status, TaskStatusEnum.FAILED.getCode())) {
            return 0;
        }
        return 0;
    }

    /**
     * 计算预估剩余时间
     *
     * @param taskPO 任务持久化对象
     * @return 预估剩余时间（秒），如果任务已完成或失败则返回null
     */
    private Long calculateEstimatedRemainingTime(ResumeTaskPO taskPO) {
        if (Objects.isNull(taskPO) || Objects.isNull(taskPO.getStartTime())) {
            return null;
        }

        String status = taskPO.getStatus();

        // 已完成或失败的任务不需要预估时间
        if (StringUtils.equals(status, TaskStatusEnum.COMPLETED.getCode()) ||
                StringUtils.equals(status, TaskStatusEnum.FAILED.getCode())) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        long elapsedSeconds = ChronoUnit.SECONDS.between(taskPO.getStartTime(), now);

        // 根据当前状态和已用时间预估剩余时间
        if (StringUtils.equals(status, TaskStatusEnum.PROCESSING.getCode())) {
            // 处理中阶段预估还需要90秒
            return Math.max(90L, 120L - elapsedSeconds);
        } else if (StringUtils.equals(status, TaskStatusEnum.ANALYZING.getCode())) {
            // 解析中阶段预估还需要60秒
            return Math.max(60L, 90L - elapsedSeconds);
        } else if (StringUtils.equals(status, TaskStatusEnum.SAVING.getCode())) {
            // 存储中阶段预估还需要30秒
            return Math.max(30L, 45L - elapsedSeconds);
        }

        return 60L;
    }
}