package com.tengYii.jobspark.domain.cv.errors;

import lombok.Getter;

/**
 * 渲染异常：覆盖模板渲染、Markdown解析、HTML转换、PDF/Docx 生成、IO错误等阶段。
 * 在各 Service 与门面中统一抛出，外层 Demo 主类统一捕获并打印详细上下文。
 *
 * 编码：UTF-8；Java 17。
 */
@Getter
public class RenderException extends RuntimeException {

    private final String stage; // 出错阶段标识，例如 TEMPLATE, MARKDOWN, HTML, PDF, DOCX, IO

    public RenderException(String stage, String message) {
        super(message);
        this.stage = stage;
    }

    public RenderException(String stage, String message, Throwable cause) {
        super(message, cause);
        this.stage = stage;
    }

    // 工厂方法：各阶段快捷构造

    public static RenderException template(String message, Throwable cause) {
        return new RenderException("TEMPLATE", message, cause);
    }

    public static RenderException template(String message) {
        return new RenderException("TEMPLATE", message);
    }

    public static RenderException markdown(String message, Throwable cause) {
        return new RenderException("MARKDOWN", message, cause);
    }

    public static RenderException markdown(String message) {
        return new RenderException("MARKDOWN", message);
    }

    public static RenderException html(String message, Throwable cause) {
        return new RenderException("HTML", message, cause);
    }

    public static RenderException html(String message) {
        return new RenderException("HTML", message);
    }

    public static RenderException pdf(String message, Throwable cause) {
        return new RenderException("PDF", message, cause);
    }

    public static RenderException pdf(String message) {
        return new RenderException("PDF", message);
    }

    public static RenderException docx(String message, Throwable cause) {
        return new RenderException("DOCX", message, cause);
    }

    public static RenderException docx(String message) {
        return new RenderException("DOCX", message);
    }

    public static RenderException io(String message, Throwable cause) {
        return new RenderException("IO", message, cause);
    }

    public static RenderException io(String message) {
        return new RenderException("IO", message);
    }
}