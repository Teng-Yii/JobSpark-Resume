package com.tengYii.jobspark.common.exception;

import com.tengYii.jobspark.common.enums.ResultCodeEnum;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 业务异常基类
 * 用于统一处理业务逻辑中的异常情况，提供标准化的错误码和错误信息
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码枚举
     */
    private final ResultCodeEnum resultCode;

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误信息
     */
    private final String message;

    /**
     * 构造方法 - 使用错误码枚举
     *
     * @param resultCode 错误码枚举
     */
    public BusinessException(ResultCodeEnum resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    /**
     * 构造方法 - 使用错误码枚举和自定义错误信息
     *
     * @param resultCode 错误码枚举
     * @param message    自定义错误信息
     */
    public BusinessException(ResultCodeEnum resultCode, String message) {
        super(StringUtils.isNotEmpty(message) ? message : resultCode.getMessage());
        this.resultCode = resultCode;
        this.code = resultCode.getCode();
        this.message = StringUtils.isNotEmpty(message) ? message : resultCode.getMessage();
    }

    /**
     * 构造方法 - 使用错误码枚举和异常原因
     *
     * @param resultCode 错误码枚举
     * @param cause      异常原因
     */
    public BusinessException(ResultCodeEnum resultCode, Throwable cause) {
        super(resultCode.getMessage(), cause);
        this.resultCode = resultCode;
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
    }

    /**
     * 构造方法 - 使用错误码枚举、自定义错误信息和异常原因
     *
     * @param resultCode 错误码枚举
     * @param message    自定义错误信息
     * @param cause      异常原因
     */
    public BusinessException(ResultCodeEnum resultCode, String message, Throwable cause) {
        super(StringUtils.isNotEmpty(message) ? message : resultCode.getMessage(), cause);
        this.resultCode = resultCode;
        this.code = resultCode.getCode();
        this.message = StringUtils.isNotEmpty(message) ? message : resultCode.getMessage();
    }

    /**
     * 构造方法 - 直接使用错误码和错误信息（兼容性方法）
     *
     * @param code    错误码
     * @param message 错误信息
     */
    public BusinessException(String code, String message) {
        super(message);
        this.resultCode = ResultCodeEnum.getByCode(code);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造方法 - 直接使用错误码、错误信息和异常原因（兼容性方法）
     *
     * @param code    错误码
     * @param message 错误信息
     * @param cause   异常原因
     */
    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = ResultCodeEnum.getByCode(code);
        this.code = code;
        this.message = message;
    }

    /**
     * 创建系统错误异常
     *
     * @return BusinessException实例
     */
    public static BusinessException systemError() {
        return new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
    }

    /**
     * 创建系统错误异常
     *
     * @param message 自定义错误信息
     * @return BusinessException实例
     */
    public static BusinessException systemError(String message) {
        return new BusinessException(ResultCodeEnum.SYSTEM_ERROR, message);
    }

    /**
     * 创建系统错误异常
     *
     * @param cause 异常原因
     * @return BusinessException实例
     */
    public static BusinessException systemError(Throwable cause) {
        return new BusinessException(ResultCodeEnum.SYSTEM_ERROR, cause);
    }

    /**
     * 创建参数错误异常
     *
     * @return BusinessException实例
     */
    public static BusinessException paramError() {
        return new BusinessException(ResultCodeEnum.PARAM_ERROR);
    }

    /**
     * 创建参数错误异常
     *
     * @param message 自定义错误信息
     * @return BusinessException实例
     */
    public static BusinessException paramError(String message) {
        return new BusinessException(ResultCodeEnum.PARAM_ERROR, message);
    }

    /**
     * 创建数据不存在异常
     *
     * @return BusinessException实例
     */
    public static BusinessException dataNotFound() {
        return new BusinessException(ResultCodeEnum.DATA_NOT_FOUND);
    }

    /**
     * 创建数据不存在异常
     *
     * @param message 自定义错误信息
     * @return BusinessException实例
     */
    public static BusinessException dataNotFound(String message) {
        return new BusinessException(ResultCodeEnum.DATA_NOT_FOUND, message);
    }

    /**
     * 判断是否为成功状态
     *
     * @return 如果是成功状态返回true，否则返回false
     */
    public boolean isSuccess() {
        return Objects.nonNull(resultCode) && resultCode.isSuccess();
    }

    /**
     * 获取详细的异常信息
     *
     * @return 格式化的异常信息字符串
     */
    public String getDetailMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("BusinessException: ");
        sb.append("code=").append(code);
        sb.append(", message=").append(message);
        if (Objects.nonNull(getCause())) {
            sb.append(", cause=").append(getCause().getClass().getSimpleName());
            sb.append(":").append(getCause().getMessage());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getDetailMessage();
    }
}