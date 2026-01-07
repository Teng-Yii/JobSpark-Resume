package com.tengYii.jobspark.common.enums;

import lombok.Getter;

/**
 * 业务错误码枚举类
 * 用于定义系统中所有业务异常的错误码和错误信息
 */
@Getter
public enum ResultCodeEnum {

    /**
     * 通用业务错误码
     */
    SUCCESS("0000", "操作成功"),
    SYSTEM_ERROR("1000", "系统内部错误"),
    PARAM_ERROR("1001", "参数错误"),
    DATA_NOT_FOUND("1002", "数据不存在"),

    /**
     * 用户相关错误码
     */
    USER_NOT_EXIST("2001", "用户不存在"),
    USER_PERMISSION_DENIED("2002", "用户权限不足"),
    USER_STATUS_INVALID("2003", "用户状态异常"),

    /**
     * 渲染相关错误码
     */
    TEMPLATE_ERROR("3001", "模板渲染失败"),
    MARKDOWN_ERROR("3002", "Markdown解析失败"),
    HTML_ERROR("3003", "HTML转换失败"),
    PDF_ERROR("3004", "PDF生成失败"),
    DOCX_ERROR("3005", "DOCX生成失败"),
    IO_ERROR("3006", "文件IO操作失败"),

    /**
     * 校验相关错误码
     */
    VALIDATION_MISSING_FIELD("4001", "缺少必填字段"),
    VALIDATION_FIELD_MAPPING("4002", "模板字段映射不一致"),
    VALIDATION_ILLEGAL_DATA("4003", "非法数据或格式"),
    VALIDATION_CONSTRAINT_VIOLATION("4004", "数据约束违反"),

    /**
     * 文件处理相关错误码
     */
    FILE_UPLOAD_ERROR("5001", "文件上传失败"),
    FILE_SIZE_EXCEEDED("5002", "文件大小超出限制"),
    FILE_TYPE_NOT_SUPPORTED("5003", "不支持的文件类型"),
    FILE_PARSE_ERROR("5004", "文件解析失败"),
    FILE_STORAGE_ERROR("5005", "文件存储失败"),

    /**
     * 任务处理相关错误码
     */
    TASK_NOT_FOUND("6001", "任务不存在"),
    TASK_STATUS_INVALID("6002", "任务状态异常"),
    TASK_EXECUTION_ERROR("6003", "任务执行失败"),
    TASK_TIMEOUT("6004", "任务执行超时"),

    /**
     * 简历处理相关错误码
     */
    RESUME_NOT_FOUND("7001", "简历不存在"),
    RESUME_PARSE_ERROR("7002", "简历解析失败"),
    RESUME_OPTIMIZATION_ERROR("7003", "简历优化失败"),
    RESUME_GENERATION_ERROR("7004", "简历生成失败");

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误信息
     */
    private final String message;

    /**
     * 构造方法
     *
     * @param code    错误码
     * @param message 错误信息
     */
    ResultCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 根据错误码获取枚举实例
     *
     * @param code 错误码
     * @return 对应的枚举实例，如果未找到则返回null
     */
    public static ResultCodeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (ResultCodeEnum resultCode : values()) {
            if (code.equals(resultCode.getCode())) {
                return resultCode;
            }
        }
        return null;
    }

    /**
     * 判断是否为成功状态
     *
     * @return 如果是成功状态返回true，否则返回false
     */
    public boolean isSuccess() {
        return SUCCESS.equals(this);
    }
}