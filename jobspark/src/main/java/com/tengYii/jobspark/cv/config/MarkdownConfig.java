package com.tengYii.jobspark.cv.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Markdown 渲染配置：控制模板到 Markdown 的生成细节。
 * 例如：是否更紧凑、标题级别偏移等。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkdownConfig {

    @Builder.Default
    private FormatConfig format = FormatConfig.defaults();

    /** 标题级别偏移：0 表示按模板原样，1 表示 h1->h2, h2->h3 ... */
    @Builder.Default
    private int headingOffset = 0;

    /** 列表是否紧凑（减少空行） */
    @Builder.Default
    private boolean compactList = true;

    /** 是否在文档顶端生成姓名/标题区块 */
    @Builder.Default
    private boolean includeHeaderBlock = true;

    public static MarkdownConfig defaults() {
        return MarkdownConfig.builder().build();
    }
}