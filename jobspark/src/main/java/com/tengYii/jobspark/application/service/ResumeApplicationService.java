package com.tengYii.jobspark.application.service;

import com.tengYii.jobspark.dto.response.ResumeUploadAsyncResponse;
import com.tengYii.jobspark.dto.request.ResumeUploadRequest;
import com.tengYii.jobspark.dto.response.TaskStatusResponse;
import com.tengYii.jobspark.model.bo.CvBO;

import java.util.List;

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
     * @param resumeId 简历的唯一标识符
     * @param userId   用户ID
     * @return 简历分析的结果对象
     */
    CvBO getResumeAnalysis(Long resumeId, Long userId);

    /**
     * 根据提供的简历ID获取优化建议
     *
     * @param resumeId 简历的唯一标识符
     * @return 包含优化建议的对象
     */
    Object getOptimizationSuggestions(String resumeId);

    /**
     * 获取任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态响应对象
     */
    TaskStatusResponse getTaskStatus(String taskId);

    /**
     * 获取用户任务列表
     *
     * @param userId 用户ID（可选）
     * @param status 任务状态（可选）
     * @return 任务列表
     */
    List<TaskStatusResponse> getUserTasks(Long userId, String status);

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     * @return 取消是否成功
     */
    Boolean cancelTask(String taskId);
}
