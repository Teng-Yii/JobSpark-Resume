package com.tengYii.jobspark.domain.service.cv;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 简历语义分块对象
 * <p>
 * 将简历按结构化边界拆分为多个chunk，每个chunk携带丰富的元数据用于检索增强。
 * 支持的分块类型包括：
 * <ul>
 *   <li><b>overview</b>：全简历摘要（粗粒度索引，用于层次化检索第一层）</li>
 *   <li><b>summary</b>：个人摘要</li>
 *   <li><b>skills</b>：专业技能聚合</li>
 *   <li><b>experience</b>：单条工作经历</li>
 *   <li><b>project</b>：单个项目经验</li>
 *   <li><b>education</b>：单条教育经历</li>
 * </ul>
 * </p>
 */
@Data
@Builder
public class ResumeChunk {

    /**
     * 分块类型
     */
    private String chunkType;

    /**
     * 分块文本内容
     */
    private String content;

    /**
     * 元数据，包含 resume_id、cv_type、chunk_index、industries、companies、skill_names、roles 等
     */
    private Map<String, String> metadata;
}
