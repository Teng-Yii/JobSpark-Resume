package com.tengYii.jobspark.common.exception;

import com.tengYii.jobspark.common.enums.ResultCodeEnum;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 渲染异常：覆盖模板渲染、Markdown解析、HTML转换、PDF/Docx 生成、IO错误等阶段。
 * 在各 Service 与门面中统一抛出，外层 Demo 主类统一捕获并打印详细上下文。
 * 继承自BusinessException，提供统一的错误码管理。
 *
 */
@Getter
public class RenderException extends BusinessException {

    /**
     * 出错阶段标识，例如 TEMPLATE, MARKDOWN, HTML, PDF, DOCX, IO
     */
    private final String stage;

    /**
     * 构造方法 - 使用阶段标识和错误码枚举
     *
     * @param stage      出错阶段标识
     * @param resultCode 错误码枚举
     */
    public RenderException(String stage, ResultCodeEnum resultCode) {
        super(resultCode);
        this.stage = stage;
    }

    /**
     * 构造方法 - 使用阶段标识、错误码枚举和自定义错误信息
     *
     * @param stage      出错阶段标识
     * @param resultCode 错误码枚举
     * @param message    自定义错误信息
     */
    public RenderException(String stage, ResultCodeEnum resultCode, String message) {
        super(resultCode, message);
        this.stage = stage;
    }

    /**
     * 构造方法 - 使用阶段标识、错误码枚举和异常原因
     *
     * @param stage      出错阶段标识
     * @param resultCode 错误码枚举
     * @param cause      异常原因
     */
    public RenderException(String stage, ResultCodeEnum resultCode, Throwable cause) {
        super(resultCode, cause);
        this.stage = stage;
    }

    /**
     * 构造方法 - 使用阶段标识、错误码枚举、自定义错误信息和异常原因
     *
     * @param stage      出错阶段标识
     * @param resultCode 错误码枚举
     * @param message    自定义错误信息
     * @param cause      异常原因
     */
    public RenderException(String stage, ResultCodeEnum resultCode, String message, Throwable cause) {
        super(resultCode, message, cause);
        this.stage = stage;
    }

    /**
     * 构造方法 - 兼容性构造方法，使用阶段标识和错误信息
     *
     * @param stage   出错阶段标识
     * @param message 错误信息
     */
    public RenderException(String stage, String message) {
        super(getResultCodeByStage(stage), message);
        this.stage = stage;
    }

    /**
     * 构造方法 - 兼容性构造方法，使用阶段标识、错误信息和异常原因
     *
     * @param stage   出错阶段标识
     * @param message 错误信息
     * @param cause   异常原因
     */
    public RenderException(String stage, String message, Throwable cause) {
        super(getResultCodeByStage(stage), message, cause);
        this.stage = stage;
    }

    /**
     * 根据阶段标识获取对应的错误码枚举
     *
     * @param stage 阶段标识
     * @return 对应的错误码枚举
     */
    private static ResultCodeEnum getResultCodeByStage(String stage) {
        if (StringUtils.isEmpty(stage)) {
            return ResultCodeEnum.SYSTEM_ERROR;
        }

        switch (stage.toUpperCase()) {
            case "TEMPLATE":
                return ResultCodeEnum.TEMPLATE_ERROR;
            case "MARKDOWN":
                return ResultCodeEnum.MARKDOWN_ERROR;
            case "HTML":
                return ResultCodeEnum.HTML_ERROR;
            case "PDF":
                return ResultCodeEnum.PDF_ERROR;
            case "DOCX":
                return ResultCodeEnum.DOCX_ERROR;
            case "IO":
                return ResultCodeEnum.IO_ERROR;
            default:
                return ResultCodeEnum.SYSTEM_ERROR;
        }
    }

    /**
     * 工厂方法：各阶段快捷构造
     */

    /**
     * 创建模板渲染异常
     *
     * @param message 错误信息
     * @param cause   异常原因
     * @return RenderException实例
     */
    public static RenderException template(String message, Throwable cause) {
        return new RenderException("TEMPLATE", ResultCodeEnum.TEMPLATE_ERROR, message, cause);
    }

    /**
     * 创建模板渲染异常
     *
     * @param message 错误信息
     * @return RenderException实例
     */
    public static RenderException template(String message) {
        return new RenderException("TEMPLATE", ResultCodeEnum.TEMPLATE_ERROR, message);
    }

    /**
     * 创建Markdown解析异常
     *
     * @param message 错误信息
     * @param cause   异常原因
     * @return RenderException实例
     */
    public static RenderException markdown(String message, Throwable cause) {
        return new RenderException("MARKDOWN", ResultCodeEnum.MARKDOWN_ERROR, message, cause);
    }

    /**
     * 创建Markdown解析异常
     *
     * @param message 错误信息
     * @return RenderException实例
     */
    public static RenderException markdown(String message) {
        return new RenderException("MARKDOWN", ResultCodeEnum.MARKDOWN_ERROR, message);
    }

    /**
     * 创建HTML转换异常
     *
     * @param message 错误信息
     * @param cause   异常原因
     * @return RenderException实例
     */
    public static RenderException html(String message, Throwable cause) {
        return new RenderException("HTML", ResultCodeEnum.HTML_ERROR, message, cause);
    }

    /**
     * 创建HTML转换异常
     *
     * @param message 错误信息
     * @return RenderException实例
     */
    public static RenderException html(String message) {
        return new RenderException("HTML", ResultCodeEnum.HTML_ERROR, message);
    }

    /**
     * 创建PDF生成异常
     *
     * @param message 错误信息
     * @param cause   异常原因
     * @return RenderException实例
     */
    public static RenderException pdf(String message, Throwable cause) {
        return new RenderException("PDF", ResultCodeEnum.PDF_ERROR, message, cause);
    }

    /**
     * 创建PDF生成异常
     *
     * @param message 错误信息
     * @return RenderException实例
     */
    public static RenderException pdf(String message) {
        return new RenderException("PDF", ResultCodeEnum.PDF_ERROR, message);
    }

    /**
     * 创建DOCX生成异常
     *
     * @param message 错误信息
     * @param cause   异常原因
     * @return RenderException实例
     */
    public static RenderException docx(String message, Throwable cause) {
        return new RenderException("DOCX", ResultCodeEnum.DOCX_ERROR, message, cause);
    }

    /**
     * 创建DOCX生成异常
     *
     * @param message 错误信息
     * @return RenderException实例
     */
    public static RenderException docx(String message) {
        return new RenderException("DOCX", ResultCodeEnum.DOCX_ERROR, message);
    }

    /**
     * 创建IO操作异常
     *
     * @param message 错误信息
     * @param cause   异常原因
     * @return RenderException实例
     */
    public static RenderException io(String message, Throwable cause) {
        return new RenderException("IO", ResultCodeEnum.IO_ERROR, message, cause);
    }

    /**
     * 创建IO操作异常
     *
     * @param message 错误信息
     * @return RenderException实例
     */
    public static RenderException io(String message) {
        return new RenderException("IO", ResultCodeEnum.IO_ERROR, message);
    }

    /**
     * 获取详细的异常信息，包含阶段信息
     *
     * @return 格式化的异常信息字符串
     */
    @Override
    public String getDetailMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("RenderException: ");
        sb.append("stage=").append(stage);
        sb.append(", code=").append(getCode());
        sb.append(", message=").append(getMessage());
        if (Objects.nonNull(getCause())) {
            sb.append(", cause=").append(getCause().getClass().getSimpleName());
            sb.append(":").append(getCause().getMessage());
        }
        return sb.toString();
    }
}