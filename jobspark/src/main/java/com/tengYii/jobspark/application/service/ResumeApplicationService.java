package com.tengYii.jobspark.application.service;

import com.tengYii.jobspark.model.dto.ResumeUploadAsyncResponse;
import com.tengYii.jobspark.model.dto.ResumeUploadRequest;
import com.tengYii.jobspark.model.dto.ResumeUploadResponse;

public interface ResumeApplicationService {

    /**
     * 上传简历（异步操作）
     * 将简历文件上传到OSS，并解析简历文件，保存结构化数据
     *
     * @param request 简历上传请求对象，包含要上传的简历信息
     * @return 简历上传响应对象
     */
    ResumeUploadAsyncResponse uploadAndParseResumeAsync(ResumeUploadRequest request);

    /**
     * 根据简历ID获取简历分析结果。
     *
     * @param resumeId 简历的唯一标识符。
     * @return 简历分析的结果对象。
     */
    Object getResumeAnalysis(String resumeId);

    /**
     * 根据提供的简历ID获取优化建议。
     *
     * @param resumeId 简历的唯一标识符。
     * @return 包含优化建议的对象。
     */
    Object getOptimizationSuggestions(String resumeId);
}
