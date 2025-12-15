package com.tengYii.jobspark.dto.response;

import com.tengYii.jobspark.common.enums.ResumeUploadStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 简历上传响应DTO
 *
 * @author tengYii
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeUploadResponse {

    /**
     * 上传简历的唯一标识
     */
    private Long resumeId;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 上传状态
     */
    private ResumeUploadStatusEnum status;

    /**
     * 上传消息
     */
    private String message;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 创建成功响应
     *
     * @param resumeId 简历ID
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param fileType 文件类型
     * @return 成功响应对象
     */
    public static ResumeUploadResponse success(Long resumeId, String fileName, Long fileSize, String fileType) {
        return new ResumeUploadResponse(
                resumeId,
                fileName,
                ResumeUploadStatusEnum.SUCCESS,
                "简历上传成功，已开始解析",
                LocalDateTime.now(),
                fileSize,
                fileType
        );
    }

    /**
     * 创建失败响应
     *
     * @param fileName 文件名
     * @param errorMessage 错误消息
     * @return 失败响应对象
     */
    public static ResumeUploadResponse failure(String fileName, String errorMessage) {
        return new ResumeUploadResponse(
                null,
                fileName,
                ResumeUploadStatusEnum.FAILED,
                "简历上传失败: " + errorMessage,
                LocalDateTime.now(),
                null,
                null
        );
    }
}

