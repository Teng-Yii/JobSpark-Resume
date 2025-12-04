package com.tengYii.jobspark.common.constants;

import java.util.Map;
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
     * HTML文件
     */
    public static final String HTML = "text/html";

    /**
     * Markdown文件
     */
    public static final String MD = "text/markdown";


    /**
     * 支持的文件扩展名集合
     */
    public static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "txt", "md"
    );

    /**
     * 支持的Content-Type集合
     */
    public static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "text/markdown"
    );

    /**
     * 文件扩展名与Content-Type的映射关系
     */
    public static final Map<String, String> EXTENSION_TO_CONTENT_TYPE = Map.of(
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "txt", "text/plain",
            "md", "text/plain"
    );
}
