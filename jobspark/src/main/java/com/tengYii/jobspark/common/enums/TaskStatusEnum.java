package com.tengYii.jobspark.common.enums;

import lombok.Getter;

/**
 * 任务状态枚举
 */
@Getter
public enum TaskStatusEnum {
    /**
     * 处理中（文件已上传，准备开始解析）
     */
    PROCESSING("PROCESSING", "处理中"),

    /**
     * 解析中（正在调用大模型解析）
     */
    ANALYZING("ANALYZING", "解析中"),

    /**
     * 存储中（正在保存到数据库）
     */
    SAVING("SAVING", "存储中"),

    /**
     * 完成
     */
    COMPLETED("COMPLETED", "完成"),

    /**
     * 失败
     */
    FAILED("FAILED", "失败");

    private final String code;
    private final String desc;

    TaskStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}