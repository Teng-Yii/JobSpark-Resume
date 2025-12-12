package com.tengYii.jobspark.application.controller;

import com.tengYii.jobspark.common.utils.login.UserContext;
import com.tengYii.jobspark.model.dto.ResumeUploadAsyncResponse;
import com.tengYii.jobspark.model.dto.ResumeUploadRequest;
import com.tengYii.jobspark.model.dto.ResumeUploadResponse;
import com.tengYii.jobspark.model.dto.TaskStatusResponse;
import com.tengYii.jobspark.application.service.ResumeApplicationService;
import com.tengYii.jobspark.application.validate.ResumeValidator;
import com.tengYii.jobspark.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeApplicationService resumeApplicationService;

    /**
     * 上传简历，并进行简历优化
     *
     * @param request 简历上传请求对象
     * @return
     */
    @PostMapping("/upload")
    public ResponseEntity<ResumeUploadAsyncResponse> uploadResume(@ModelAttribute ResumeUploadRequest request) {

        // 校验请求参数合法性
        String validationResult = ResumeValidator.validate(request);
        if (StringUtils.isNotBlank(validationResult)) {
            throw new ValidationException(String.valueOf(HttpStatus.BAD_REQUEST.value()), validationResult);
        }

        ResumeUploadAsyncResponse response = resumeApplicationService.uploadAndParseResumeAsync(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{resumeId}/analysis")
    public ResponseEntity<?> getResumeAnalysis(@PathVariable String resumeId) {
        // 获取简历解析结果
        return ResponseEntity.ok(resumeApplicationService.getResumeAnalysis(resumeId));
    }

    @PostMapping("/{resumeId}/suggestions")
    public ResponseEntity<?> getOptimizationSuggestions(@PathVariable String resumeId) {
        // 获取优化建议
        return ResponseEntity.ok(resumeApplicationService.getOptimizationSuggestions(resumeId));
    }

    @PostMapping("/test")
    public ResponseEntity<Void> getOptimizationSuggestions(@ModelAttribute ResumeUploadRequest request) throws IOException {

//        TextDocumentParser textDocumentParser = new TextDocumentParser();
//        Document parse = textDocumentParser.parse(request.getFile().getInputStream());
        // 获取优化建议
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(request.getFile().getBytes()))) {

            // 创建PDFTextStripper实例
            PDFTextStripper pdfStripper = new PDFTextStripper();

            // 设置按页面位置排序（使文本按阅读顺序排列，不是按PDF内部顺序）
            pdfStripper.setSortByPosition(true);

            // 读取整个文档的文本内容
            String resumeText = pdfStripper.getText(document);
        }
        return ResponseEntity.ok(null);
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
     * 查询用户任务列表
     *
     * @param userId 用户ID（可选）
     * @param status 任务状态（可选）
     * @return 任务列表
     */
    @GetMapping("/tasks")
    public ResponseEntity<List<TaskStatusResponse>> getUserTasks(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status) {

        List<TaskStatusResponse> tasks = resumeApplicationService.getUserTasks(userId, status);
        return ResponseEntity.ok(tasks);
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