package com.tengYii.jobspark.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务状态响应类
 *
 * @author tengYii
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusResponse {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务状态
     */
    private String status;

    /**
     * 状态描述
     */
    private String statusMessage;

    /**
     * 进度百分比（0-100）
     */
    private Integer progress;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;

    /**
     * 简历ID（完成后才有）
     */
    private Long resumeId;

    /**
     * 错误信息（失败时才有）
     */
    private String errorMessage;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 预估剩余时间（秒）
     */
    private Long estimatedRemainingSeconds;

    /**
     * 创建成功的任务状态响应
     *
     * @param taskId           任务ID
     * @param status           任务状态
     * @param statusMessage    状态描述
     * @param progress         进度百分比
     * @param startTime        开始时间
     * @param fileName         文件名
     * @param originalFileName 原始文件名
     * @return 任务状态响应对象
     */
    public static TaskStatusResponse success(String taskId, String status, String statusMessage,
                                             Integer progress, LocalDateTime startTime,
                                             String fileName, String originalFileName) {
        return TaskStatusResponse.builder()
                .taskId(taskId)
                .status(status)
                .statusMessage(statusMessage)
                .progress(progress)
                .startTime(startTime)
                .fileName(fileName)
                .originalFileName(originalFileName)
                .build();
    }

    /**
     * 创建完成状态的任务响应
     *
     * @param taskId           任务ID
     * @param status           任务状态
     * @param statusMessage    状态描述
     * @param startTime        开始时间
     * @param completeTime     完成时间
     * @param resumeId         简历ID
     * @param fileName         文件名
     * @param originalFileName 原始文件名
     * @return 任务状态响应对象
     */
    public static TaskStatusResponse completed(String taskId, String status, String statusMessage,
                                               LocalDateTime startTime, LocalDateTime completeTime,
                                               Long resumeId, String fileName, String originalFileName) {
        return TaskStatusResponse.builder()
                .taskId(taskId)
                .status(status)
                .statusMessage(statusMessage)
                .progress(100)
                .startTime(startTime)
                .completeTime(completeTime)
                .resumeId(resumeId)
                .fileName(fileName)
                .originalFileName(originalFileName)
                .build();
    }

    /**
     * 创建失败状态的任务响应
     *
     * @param taskId           任务ID
     * @param status           任务状态
     * @param statusMessage    状态描述
     * @param startTime        开始时间
     * @param completeTime     完成时间
     * @param errorMessage     错误信息
     * @param fileName         文件名
     * @param originalFileName 原始文件名
     * @return 任务状态响应对象
     */
    public static TaskStatusResponse failed(String taskId, String status, String statusMessage,
                                            LocalDateTime startTime, LocalDateTime completeTime,
                                            String errorMessage, String fileName, String originalFileName) {
        return TaskStatusResponse.builder()
                .taskId(taskId)
                .status(status)
                .statusMessage(statusMessage)
                .progress(0)
                .startTime(startTime)
                .completeTime(completeTime)
                .errorMessage(errorMessage)
                .fileName(fileName)
                .originalFileName(originalFileName)
                .build();
    }
}