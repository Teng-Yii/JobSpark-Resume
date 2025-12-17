package com.tengYii.jobspark.dto.request;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Objects;

/**
 * 简历优化请求DTO
 *
 * @author tengYii
 * @since 1.0.0
 */
@Data
public class ResumeOptimizeRequest implements Serializable {

    /**
     * 待优化的简历ID
     */
    private String resumeId;

    /**
     * 用于简历优化的目标职位描述信息
     */
    private String jobDescription;
}