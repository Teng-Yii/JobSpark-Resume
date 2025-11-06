package com.tengYii.jobspark.cv.errors;

/**
 * 领域校验异常：用于必填字段缺失、模板字段映射不一致等场景。
 * 所有校验异常均抛出该类型，便于在演示主类统一捕获并打印具体上下文。
 *
 * 编码：UTF-8；Java 17。
 */
public class ValidationException extends RuntimeException {

  private final String code;

  public ValidationException(String code, String message) {
    super(message);
    this.code = code;
  }

  public ValidationException(String code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  /** 快捷构造：缺少必填字段 */
  public static ValidationException missing(String field) {
    return new ValidationException("VALIDATION_MISSING_FIELD", "缺少必填字段: " + field);
  }

  /** 快捷构造：模板字段映射不一致 */
  public static ValidationException mapping(String detail) {
    return new ValidationException("VALIDATION_FIELD_MAPPING", "模板字段映射不一致: " + detail);
  }

  /** 快捷构造：非法数据或格式 */
  public static ValidationException illegal(String detail) {
    return new ValidationException("VALIDATION_ILLEGAL_DATA", "非法数据: " + detail);
  }
}