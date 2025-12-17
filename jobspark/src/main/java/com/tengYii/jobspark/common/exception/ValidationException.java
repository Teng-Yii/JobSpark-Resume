package com.tengYii.jobspark.common.exception;

import com.tengYii.jobspark.common.enums.ResultCodeEnum;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 领域校验异常：用于必填字段缺失、模板字段映射不一致等场景。
 * 所有校验异常均抛出该类型，便于在演示主类统一捕获并打印具体上下文。
 * 继承自BusinessException，提供统一的错误码管理。
 */
@Getter
public class ValidationException extends BusinessException {

    /**
     * 构造方法 - 使用错误码枚举
     *
     * @param resultCode 错误码枚举
     */
    public ValidationException(ResultCodeEnum resultCode) {
        super(resultCode);
    }

    /**
     * 构造方法 - 使用错误码枚举和自定义错误信息
     *
     * @param resultCode 错误码枚举
     * @param message    自定义错误信息
     */
    public ValidationException(ResultCodeEnum resultCode, String message) {
        super(resultCode, message);
    }

    /**
     * 构造方法 - 使用错误码枚举和异常原因
     *
     * @param resultCode 错误码枚举
     * @param cause      异常原因
     */
    public ValidationException(ResultCodeEnum resultCode, Throwable cause) {
        super(resultCode, cause);
    }

    /**
     * 构造方法 - 使用错误码枚举、自定义错误信息和异常原因
     *
     * @param resultCode 错误码枚举
     * @param message    自定义错误信息
     * @param cause      异常原因
     */
    public ValidationException(ResultCodeEnum resultCode, String message, Throwable cause) {
        super(resultCode, message, cause);
    }

    /**
     * 构造方法 - 兼容性构造方法，使用错误码和错误信息
     *
     * @param code    错误码
     * @param message 错误信息
     */
    public ValidationException(String code, String message) {
        super(getResultCodeByCode(code), message);
    }

    /**
     * 构造方法 - 兼容性构造方法，使用错误码、错误信息和异常原因
     *
     * @param code    错误码
     * @param message 错误信息
     * @param cause   异常原因
     */
    public ValidationException(String code, String message, Throwable cause) {
        super(getResultCodeByCode(code), message, cause);
    }

    /**
     * 根据错误码字符串获取对应的错误码枚举
     *
     * @param code 错误码字符串
     * @return 对应的错误码枚举
     */
    private static ResultCodeEnum getResultCodeByCode(String code) {
        if (StringUtils.isEmpty(code)) {
            return ResultCodeEnum.PARAM_ERROR;
        }

        switch (code) {
            case "VALIDATION_MISSING_FIELD":
                return ResultCodeEnum.VALIDATION_MISSING_FIELD;
            case "VALIDATION_FIELD_MAPPING":
                return ResultCodeEnum.VALIDATION_FIELD_MAPPING;
            case "VALIDATION_ILLEGAL_DATA":
                return ResultCodeEnum.VALIDATION_ILLEGAL_DATA;
            default:
                return ResultCodeEnum.VALIDATION_CONSTRAINT_VIOLATION;
        }
    }

    /**
     * 快捷构造：缺少必填字段
     *
     * @param field 字段名称
     * @return ValidationException实例
     */
    public static ValidationException missing(String field) {
        String message = "缺少必填字段: " + field;
        return new ValidationException(ResultCodeEnum.VALIDATION_MISSING_FIELD, message);
    }

    /**
     * 快捷构造：模板字段映射不一致
     *
     * @param detail 详细信息
     * @return ValidationException实例
     */
    public static ValidationException mapping(String detail) {
        String message = "模板字段映射不一致: " + detail;
        return new ValidationException(ResultCodeEnum.VALIDATION_FIELD_MAPPING, message);
    }

    /**
     * 快捷构造：非法数据或格式
     *
     * @param detail 详细信息
     * @return ValidationException实例
     */
    public static ValidationException illegal(String detail) {
        String message = "非法数据: " + detail;
        return new ValidationException(ResultCodeEnum.VALIDATION_ILLEGAL_DATA, message);
    }

    /**
     * 快捷构造：数据约束违反
     *
     * @param detail 详细信息
     * @return ValidationException实例
     */
    public static ValidationException constraintViolation(String detail) {
        String message = "数据约束违反: " + detail;
        return new ValidationException(ResultCodeEnum.VALIDATION_CONSTRAINT_VIOLATION, message);
    }

    /**
     * 获取详细的异常信息
     *
     * @return 格式化的异常信息字符串
     */
    @Override
    public String getDetailMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("ValidationException: ");
        sb.append("code=").append(getCode());
        sb.append(", message=").append(getMessage());
        if (Objects.nonNull(getCause())) {
            sb.append(", cause=").append(getCause().getClass().getSimpleName());
            sb.append(":").append(getCause().getMessage());
        }
        return sb.toString();
    }
}