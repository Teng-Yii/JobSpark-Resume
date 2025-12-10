package com.tengYii.jobspark.model.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 文件存储结果类
 */
@Getter
@Builder
public class FileStorageResultDTO {

    /**
     * 存储文件的桶名
     */
    private String bucketName;

    /**
     * 存储文件的唯一名称
     */
    private String uniqueFileName;

    /**
     * 存储文件的原始文件名
     */
    private String originalFileName;

    /**
     * 存储文件的大小，单位为字节。
     */
    private Long fileSize;

    /**
     * 存储文件的内容类型。
     */
    private String contentType;

    /**
     * 文件存储路径
     */
    private String filePath;
}

