package com.tengYii.jobspark.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简历异步上传响应类
 *
 * @author tengYii
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeUploadAsyncResponse {

    /**
     * 响应是否成功
     */
    private Boolean success;

    /**
     * 任务ID（成功时返回）
     */
    private String taskId;

    /**
     * 文件名（成功时返回）
     */
    private String fileName;

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 错误消息（失败时返回）
     */
    private String errorMessage;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 创建成功响应
     *
     * @param taskId   任务ID
     * @param fileName 文件名
     * @return 成功响应
     */
    public static ResumeUploadAsyncResponse success(String taskId, String fileName) {
        return ResumeUploadAsyncResponse.builder()
                .success(Boolean.TRUE)
                .taskId(taskId)
                .fileName(fileName)
                .message("文件上传成功，正在后台处理")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建成功响应（带原始文件名）
     *
     * @param taskId           任务ID
     * @param fileName         存储文件名
     * @param originalFileName 原始文件名
     * @return 成功响应
     */
    public static ResumeUploadAsyncResponse success(String taskId, String fileName, String originalFileName) {
        return ResumeUploadAsyncResponse.builder()
                .success(Boolean.TRUE)
                .taskId(taskId)
                .fileName(fileName)
                .originalFileName(originalFileName)
                .message("文件上传成功，正在后台处理")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建失败响应
     *
     * @param originalFileName 原始文件名
     * @param errorMessage     错误消息
     * @return 失败响应
     */
    public static ResumeUploadAsyncResponse failure(String originalFileName, String errorMessage) {
        return ResumeUploadAsyncResponse.builder()
                .success(Boolean.FALSE)
                .originalFileName(originalFileName)
                .errorMessage(errorMessage)
                .message("文件上传失败")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建失败响应（仅错误消息）
     *
     * @param errorMessage 错误消息
     * @return 失败响应
     */
    public static ResumeUploadAsyncResponse failure(String errorMessage) {
        return ResumeUploadAsyncResponse.builder()
                .success(Boolean.FALSE)
                .errorMessage(errorMessage)
                .message("文件上传失败")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
