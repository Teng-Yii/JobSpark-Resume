package com.tengYii.jobspark.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ResumeUploadRequest {
    /**
     * 简历文件
     */
    private MultipartFile file;

    /**
     * 用户ID,唯一标识
     */
    private Long userId;

    /**
     * 用户信息
     */
    private String userMessage;

    /**
     * 简历类型，upload-用户上传简历（用于查询/解析/优化），excellent-优秀参考简历（用于简历优化参考）
     *
     * @see com.tengYii.jobspark.common.enums.CvTypeEnum
     */
    private String cvType;
}