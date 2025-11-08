package com.tengYii.jobspark.domain.model.constants;

import java.util.Set;

public class ContentTypeConstants {
    /**
     * PDF文件
     */
    public static final String PDF = "application/pdf";

    /**
     * Word文档旧格式 .doc文件
     */
    public static final String DOC = "application/msword";

    /**
     * Word文档新格式 .docx文件
     */
    public static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    /**
     * 纯文本文件 .txt文件
     */
    public static final String TEXT = "text/plain";

    /**
     * 包含所有支持的文档类型的集合
     */
    public static final Set<String> SUPPORTED_TYPES = Set.of(PDF, DOC, DOCX, TEXT);
}
