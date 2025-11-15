package com.tengYii.jobspark.model.cv;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Markdown 富文本封装。
 * 仅存储 Markdown 字符串，渲染阶段由 CommonMark 解析。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RichText {
    private String markdown;
}