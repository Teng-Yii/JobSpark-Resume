package com.tengYii.jobspark.common.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 文件下载类型枚举
 * 定义支持的简历下载文件格式，包括PDF、HTML、DOCX三种类型
 *
 * @author TengYii
 * @since 1.0.0
 */
@Getter
public enum DownloadFileTypeEnum {

    /**
     * HTML格式
     * 超文本标记语言格式，适用于网页展示和在线预览
     */
    HTML("html", "text/html", "HTML格式"),

    /**
     * PDF格式
     * 便携式文档格式，适用于正式文档分发和打印
     */
    PDF("pdf", "application/pdf", "PDF格式"),

    /**
     * DOCX格式
     * Microsoft Word文档格式，适用于编辑和修改
     */
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "DOCX格式");

    /**
     * 文件格式
     */
    private final String fileFormat;

    /**
     * MIME类型/Content-Type
     */
    private final String contentType;

    /**
     * 格式描述
     */
    private final String description;

    /**
     * 构造函数
     *
     * @param fileFormat  文件格式
     * @param contentType MIME类型
     * @param description 格式描述
     */
    DownloadFileTypeEnum(String fileFormat, String contentType, String description) {
        this.fileFormat = fileFormat;
        this.contentType = contentType;
        this.description = description;
    }

    /**
     * 根据文件格式查找对应的枚举值
     *
     * @param fileFormat 文件格式
     * @return 对应的枚举值，如果未找到则默认返回PDF
     */
    public static DownloadFileTypeEnum getByFormat(String fileFormat) {
        // 使用StringUtils进行字符串判空
        if (StringUtils.isEmpty(fileFormat)) {
            return null;
        }

        // 遍历所有枚举值进行匹配
        for (DownloadFileTypeEnum type : values()) {
            // 使用StringUtils进行字符串相等性判断，忽略大小写
            if (StringUtils.equalsIgnoreCase(type.fileFormat, fileFormat)) {
                return type;
            }
        }
        return PDF;
    }
}