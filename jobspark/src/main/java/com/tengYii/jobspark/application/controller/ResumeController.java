package com.tengYii.jobspark.application.controller;

import com.tengYii.jobspark.common.utils.login.UserContext;
import com.tengYii.jobspark.dto.request.ResumeOptimizedDownloadRequest;
import com.tengYii.jobspark.dto.request.ResumeOptimizeRequest;
import com.tengYii.jobspark.dto.response.ResumeOptimizedResponse;
import com.tengYii.jobspark.dto.response.ResumeUploadAsyncResponse;
import com.tengYii.jobspark.dto.request.ResumeUploadRequest;
import com.tengYii.jobspark.dto.response.TaskStatusResponse;
import com.tengYii.jobspark.application.service.ResumeApplicationService;
import com.tengYii.jobspark.application.validate.ResumeValidator;
import com.tengYii.jobspark.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


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
public class ResumeController {

    @Autowired
    private ResumeApplicationService resumeApplicationService;

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
     * 获取当前登录用户的ID。
     *
     * @return 当前登录用户的ID。
     */
    private Long getLoginUserId() {
        // 直接从ThreadLocal获取当前用户ID，无需手动传递
        return UserContext.getCurrentUserId();
    }
}