package com.tengYii.jobspark.common.enums;

import lombok.Getter;

/**
 * 简历上传状态枚举
 */
@Getter
public enum ResumeUploadStatusEnum {
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    PROCESSING("PROCESSING", "处理中");

    private final String code;
    private final String description;

    ResumeUploadStatusEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
}