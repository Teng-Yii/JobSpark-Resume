package com.tengYii.jobspark.application.controller;

import com.tengYii.jobspark.application.service.ResumeApplicationService;
import com.tengYii.jobspark.application.validate.ResumeValidator;
import com.tengYii.jobspark.common.exception.ValidationException;
import com.tengYii.jobspark.common.utils.login.UserContext;
import com.tengYii.jobspark.domain.service.ResumeRagService;
import com.tengYii.jobspark.dto.request.ResumeOptimizeRequest;
import com.tengYii.jobspark.dto.request.ResumeOptimizedDownloadRequest;
import com.tengYii.jobspark.dto.request.ResumeUploadRequest;
import com.tengYii.jobspark.dto.response.ResumeDetailResponse;
import com.tengYii.jobspark.dto.response.ResumeOptimizedResponse;
import com.tengYii.jobspark.dto.response.ResumeUploadAsyncResponse;
import com.tengYii.jobspark.dto.response.TaskStatusResponse;
import com.tengYii.jobspark.infrastructure.context.OptimizationProgressContext;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


/**
 * 简历controller
 * <p>
 * 专门处理简历相关的业务逻辑，包括简历上传、解析、优化等功能
 *
 * @author tengYii
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
@Slf4j
public class ResumeController {

    @Autowired
    private ResumeApplicationService resumeApplicationService;

    @Autowired
    private ResumeRagService resumeRagService;

    @Resource(name = "resumeTaskExecutor")
    private Executor resumeTaskExecutor;

    /**
     * 获取当前用户的简历列表
     *
     * @return 简历列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<ResumeDetailResponse>> getResumeList() {
        Long userId = getLoginUserId();
        List<ResumeDetailResponse> resumeList = resumeApplicationService.getResumeList(userId);
        return ResponseEntity.ok(resumeList);
    }

    /**
     * 上传简历，并进行简历解析
     *
     * @param request 简历上传请求对象
     * @return
     */
    @PostMapping("/upload")
    public ResponseEntity<ResumeUploadAsyncResponse> uploadResume(@ModelAttribute ResumeUploadRequest request) {
        Long userId = getLoginUserId();
        request.setUserId(userId);

        // 校验请求参数合法性
        String validationResult = ResumeValidator.validateUploadRequest(request);
        if (StringUtils.isNotBlank(validationResult)) {
            throw new ValidationException(String.valueOf(HttpStatus.BAD_REQUEST.value()), validationResult);
        }

        ResumeUploadAsyncResponse response = resumeApplicationService.uploadAndParseResumeAsync(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 获取优化建议及优化后的简历对象
     *
     * @param request 简历优化请求对象，包含resumeId和jobDescription
     * @return 简历优化返回结果
     */
    @PostMapping("/optimize")
    public ResponseEntity<ResumeOptimizedResponse> optimizeResume(@RequestBody ResumeOptimizeRequest request) {

        Long userId = getLoginUserId();
        request.setUserId(userId);

        // 校验请求参数合法性
        String validationResult = ResumeValidator.validateOptimizeRequest(request);
        if (StringUtils.isNotEmpty(validationResult)) {
            throw new ValidationException(String.valueOf(HttpStatus.BAD_REQUEST.value()), validationResult);
        }

        ResumeOptimizedResponse response = resumeApplicationService.optimizeResume(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 流式获取优化进度及最终结果
     *
     * @param request 简历优化请求对象
     * @return SseEmitter 对象，用于推送进度
     */
    @PostMapping("/optimize/stream")
    public SseEmitter streamOptimizeResume(@RequestBody ResumeOptimizeRequest request) {
        Long userId = getLoginUserId();
        request.setUserId(userId);

        // 校验请求参数合法性
        String validationResult = ResumeValidator.validateOptimizeRequest(request);
        if (StringUtils.isNotEmpty(validationResult)) {
            throw new ValidationException(String.valueOf(HttpStatus.BAD_REQUEST.value()), validationResult);
        }

        // 设置超时时间为 10 分钟
        SseEmitter emitter = new SseEmitter(600000L);

        emitter.onCompletion(() -> log.info("SSE连接已完成：userId={}", userId));

        emitter.onTimeout(() -> {
            log.error("SSE连接超时：userId={}", userId);
            emitter.complete();
        });

        emitter.onError((e) -> {
            log.error("SSE连接错误：userId={}", userId, e);
            emitter.complete();
        });

        CompletableFuture.runAsync(() -> {
            try {
                // 设置进度回调
                OptimizationProgressContext.setEmitter(message -> {
                    try {
                        emitter.send(SseEmitter.event().name("progress").data(message));
                    } catch (IOException e) {
                        log.error("发送进度消息失败", e);
                    }
                });

                // 执行优化逻辑
                ResumeOptimizedResponse response = resumeApplicationService.optimizeResume(request);
                // 发送最终结果
                emitter.send(SseEmitter.event().name("result").data(response));
                // 完成
                emitter.complete();

            } catch (Exception e) {
                log.error("简历优化流式处理异常", e);
                emitter.completeWithError(e);
            } finally {
                // 清理上下文
                OptimizationProgressContext.clear();
            }
        }, resumeTaskExecutor);

        return emitter;
    }

    /**
     * 生成已优化的简历文件
     *
     * @param request 优化请求DTO，包含resumeId和优化后的CvBO
     * @return 响应实体，包含生成的优化后PDF文件字节数组
     */
    @PostMapping("/generateOptimizedFile")
    public ResponseEntity<byte[]> generateOptimizedFile(@RequestBody ResumeOptimizedDownloadRequest request) {

        Long userId = getLoginUserId();
        request.setUserId(userId);

        // 校验请求参数合法性
        String validationResult = ResumeValidator.validateOptimizedDownloadRequest(request);
        if (StringUtils.isNotEmpty(validationResult)) {
            throw new ValidationException(String.valueOf(HttpStatus.BAD_REQUEST.value()), validationResult);
        }

        byte[] pdfBytes = resumeApplicationService.generateOptimizedFile(request);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=optimized_resume.pdf")
                .body(pdfBytes);
    }

    /**
     * 查询任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态响应
     */
    @GetMapping("/task/{taskId}/status")
    public ResponseEntity<TaskStatusResponse> getTaskStatus(@PathVariable String taskId) {
        if (StringUtils.isEmpty(taskId)) {
            throw new ValidationException(String.valueOf(HttpStatus.BAD_REQUEST.value()), "任务ID不能为空");
        }

        TaskStatusResponse response = resumeApplicationService.getTaskStatus(taskId);
        return ResponseEntity.ok(response);
    }

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     * @return 取消结果
     */
    @PostMapping("/task/{taskId}/cancel")
    public ResponseEntity<Void> cancelTask(@PathVariable String taskId) {
        if (StringUtils.isEmpty(taskId)) {
            throw new ValidationException(String.valueOf(HttpStatus.BAD_REQUEST.value()), "任务ID不能为空");
        }

        Boolean success = resumeApplicationService.cancelTask(taskId);
        if (Boolean.TRUE.equals(success)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 保存简历至向量数据库中
     *
     * @param resumeId 简历ID
     * @return 状态结果
     */
    @PostMapping("/{resumeId}/embedding")
    public ResponseEntity<Boolean> storeEmbedding(@PathVariable Long resumeId) {
        Long userId = getLoginUserId();

        // 校验resumeId是否为空
        if (Objects.isNull(resumeId)) {
            throw new ValidationException(String.valueOf(HttpStatus.BAD_REQUEST.value()), "简历ID不能为空");
        }

        // 调用service层方法
        Boolean result = resumeApplicationService.storeResumeEmbedding(resumeId, userId);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取当前登录用户的ID。
     *
     * @return 当前登录用户的ID。
     */
    private Long getLoginUserId() {
        // 直接从ThreadLocal获取当前用户ID，无需手动传递
        return UserContext.getCurrentUserId();
    }
}