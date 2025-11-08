package com.tengYii.jobspark.domain.cv.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 项目经验
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    private String name;              // 项目名称
    private String role;              // 角色/职责
    private RichText description;     // 项目描述（Markdown）
    private List<RichText> highlights;// 亮点/贡献（Markdown 列表）
}