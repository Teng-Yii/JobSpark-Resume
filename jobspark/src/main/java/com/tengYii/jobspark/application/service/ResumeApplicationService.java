package com.tengYii.jobspark.application.service;

import com.tengYii.jobspark.dto.request.ResumeOptimizedDownloadRequest;
import com.tengYii.jobspark.dto.request.ResumeOptimizeRequest;
import com.tengYii.jobspark.dto.response.ResumeDetailResponse;
import com.tengYii.jobspark.dto.response.ResumeOptimizedResponse;
import com.tengYii.jobspark.dto.response.ResumeUploadAsyncResponse;
import com.tengYii.jobspark.dto.request.ResumeUploadRequest;
import com.tengYii.jobspark.dto.response.TaskStatusResponse;

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
     * 获取优化后的简历信息
     *
     * @param request 简历优化请求对象
     * @return 优化后的简历响应对象
     */
    ResumeOptimizedResponse optimizeResume(ResumeOptimizeRequest request);

    /**
     * 生成优化后的简历文件。
     *
     * @param request 包含简历优化请求信息的对象。
     * @return 优化后的简历文件的字节数组。
     */
    byte[] generateOptimizedFile(ResumeOptimizedDownloadRequest request);

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
     * 获取用户简历列表
     *
     * @param userId 用户ID
     * @return 简历列表
     */
    List<ResumeDetailResponse> getResumeList(Long userId);

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     * @return 取消是否成功
     */
    Boolean cancelTask(String taskId);

    /**
     * 将简历保存到向量数据库
     *
     * @param resumeId 简历ID
     * @param userId   用户ID
     * @return 保存是否成功
     */
    Boolean storeResumeEmbedding(Long resumeId, Long userId);
}
