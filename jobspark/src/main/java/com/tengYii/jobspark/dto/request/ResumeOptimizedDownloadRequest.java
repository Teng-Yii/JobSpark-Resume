package com.tengYii.jobspark.dto.request;

import java.io.Serializable;

import lombok.Getter;

/**
 * 优化后简历下载请求request
 */
@Getter
public class ResumeOptimizedDownloadRequest implements Serializable {

    /**
     * 用户ID,唯一标识
     */
    private Long userId;

    /**
     * 简历ID，唯一标识简历
     */
    private Long optimizedResumeId;

    /**
     * 下载文件类型
     * 指定简历导出的文件格式，支持PDF、HTML、DOCX三种格式
     *
     * @see com.tengYii.jobspark.common.enums.DownloadFileTypeEnum
     */
    private String downloadFileType;
}