package com.tengYii.jobspark.application.service.impl;

import com.tengYii.jobspark.application.service.ResumeApplicationService;
import com.tengYii.jobspark.common.enums.DeleteFlagEnum;
import com.tengYii.jobspark.common.enums.DownloadFileTypeEnum;
import com.tengYii.jobspark.common.enums.ResultCodeEnum;
import com.tengYii.jobspark.common.enums.TaskStatusEnum;
import com.tengYii.jobspark.common.exception.BusinessException;
import com.tengYii.jobspark.common.utils.SnowflakeUtil;
import com.tengYii.jobspark.common.utils.llm.ChatModelProvider;
import com.tengYii.jobspark.config.cv.DocxConfig;
import com.tengYii.jobspark.config.cv.HtmlConfig;
import com.tengYii.jobspark.config.cv.MarkdownConfig;
import com.tengYii.jobspark.config.cv.PdfConfig;
import com.tengYii.jobspark.domain.agent.CvOptimizationAgent;
import com.tengYii.jobspark.domain.render.doc.DocxService;
import com.tengYii.jobspark.domain.render.markdown.MarkdownService;
import com.tengYii.jobspark.domain.render.markdown.TemplateFieldMapper;
import com.tengYii.jobspark.domain.render.markdown.TemplateService;
import com.tengYii.jobspark.domain.render.pdf.PdfService;
import com.tengYii.jobspark.domain.service.*;
import com.tengYii.jobspark.dto.request.ResumeOptimizedRequest;
import com.tengYii.jobspark.dto.request.ResumeOptimizeRequest;
import com.tengYii.jobspark.dto.response.ResumeOptimizedResponse;
import com.tengYii.jobspark.infrastructure.repo.CvRepository;
import com.tengYii.jobspark.model.bo.CvBO;
import com.tengYii.jobspark.dto.response.FileStorageResultDTO;
import com.tengYii.jobspark.dto.response.ResumeUploadAsyncResponse;
import com.tengYii.jobspark.dto.request.ResumeUploadRequest;
import com.tengYii.jobspark.dto.response.TaskStatusResponse;
import com.tengYii.jobspark.model.po.CvPO;
import com.tengYii.jobspark.model.po.ResumeTaskPO;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;


@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeApplicationServiceImpl implements ResumeApplicationService {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ResumeAnalysisService resumeAnalysisService;

    @Autowired
    private ResumeOptimizationService resumeOptimizationService;

    @Autowired
    private ResumePersistenceService resumePersistenceService;

    @Autowired
    private ResumeTaskService resumeTaskService;

    @Autowired
    private CvRepository cvRepository;

    @Autowired
    @Qualifier("resumeTaskExecutor")
    private Executor resumeTaskExecutor;

    private final ChatModel chatModel = ChatModelProvider.createChatModel();

    /**
     * 上传简历
     * 将简历文件上传到OSS，并解析简历文件，保存结构化数据
     *
     * @param request 简历上传请求对象，包含要上传的简历信息
     * @return 简历上传响应对象
     */
    public ResumeUploadAsyncResponse uploadAndParseResumeAsync(ResumeUploadRequest request) {
        try {
            // 1. 立即保存文件（快速操作）
            FileStorageResultDTO storageResultDTO = fileStorageService.saveUploadedFile(request.getFile(), null);

            // 2. 生成任务ID
            String taskId = String.valueOf(SnowflakeUtil.snowflakeId());

            // 3. 创建任务记录（状态：处理中）
            Long userId = request.getUserId();
            LocalDateTime nowTime = LocalDateTime.now();
            ResumeTaskPO taskPO = ResumeTaskPO.builder()
                    .taskId(taskId)
                    .userId(userId)
                    .fileName(storageResultDTO.getUniqueFileName())
                    .originalFileName(request.getFile().getOriginalFilename())
                    .fileSize(request.getFile().getSize())
                    .contentType(request.getFile().getContentType())
                    .filePath(storageResultDTO.getFilePath())
                    .status(TaskStatusEnum.PROCESSING.getCode())
                    .startTime(nowTime)
                    .createTime(nowTime)
                    .updateTime(nowTime)
                    .deleteFlag(DeleteFlagEnum.NOT_DELETED.getCode())
                    .build();

            Boolean saveTaskSuccessFlag = resumeTaskService.saveTask(taskPO);
            if (Boolean.FALSE.equals(saveTaskSuccessFlag)) {
                log.error("简历上传解析，创建异步任务记录失败，userId: {}", userId);
            }

            // 4. 异步执行耗时操作
            CompletableFuture.runAsync(() -> {
                processResumeAsync(taskId, request);
            }, resumeTaskExecutor);

            // 5. 立即返回任务ID
            return ResumeUploadAsyncResponse.success(taskId, storageResultDTO.getUniqueFileName());

        } catch (Exception e) {
            log.error("简历上传失败", e);
            return ResumeUploadAsyncResponse.failure(request.getFile().getOriginalFilename(), e.getMessage());
        }
    }

    /**
     * 异步处理简历解析
     *
     * @param taskId  任务ID
     * @param request 原始请求
     */
    @Async
    private void processResumeAsync(String taskId, ResumeUploadRequest request) {

        Long resumeId = -1L;
        LocalDateTime nowTime = LocalDateTime.now();
        try {
            StopWatch stopWatch = new StopWatch();

            stopWatch.start("更新异步任务状态为解析中");
            // 更新任务状态为解析中
            Boolean updateTaskStatus = resumeTaskService.updateTaskStatus(taskId, TaskStatusEnum.ANALYZING);
            if (Boolean.FALSE.equals(updateTaskStatus)) {
                log.error("异步处理简历解析，更新异步任务状态失败，taskId: {}", taskId);
            }
            stopWatch.stop();

            // 解析简历内容（耗时操作）
            stopWatch.start("agent解析简历内容");
            CvBO cvBO = resumeAnalysisService.analyzeResume(request);
            cvBO.setUserId(request.getUserId());
            stopWatch.stop();

            // 更新任务状态为存储中
            stopWatch.start("更新异步任务状态为存储中");
            updateTaskStatus = resumeTaskService.updateTaskStatus(taskId, TaskStatusEnum.SAVING);
            if (Boolean.FALSE.equals(updateTaskStatus)) {
                log.error("异步处理简历解析，更新异步任务状态失败，taskId: {}", taskId);
            }
            stopWatch.stop();

            // 将结构化简历对象落库（耗时操作）
            stopWatch.start("将结构化简历对象落库");

            resumeId = resumePersistenceService.convertAndSaveCv(cvBO, nowTime);
            stopWatch.stop();

            // 更新任务状态为完成
            Boolean completed = resumeTaskService.completeTask(taskId, resumeId, nowTime);
            if (completed) {
                log.info("任务完成，taskId: {}, resumeId: {}", taskId, resumeId);
            }

        } catch (Exception e) {
            log.error("异步处理简历失败，taskId: {}", taskId, e);
            // 更新任务状态为失败
            resumeTaskService.failTask(taskId, resumeId, nowTime, e.getMessage());
        }
    }

    /**
     * 获取优化后的简历信息
     *
     * @param request 简历优化请求对象，包含resumeId和jobDescription
     * @param userId  用户ID
     * @return 优化后的简历响应对象
     */
    public ResumeOptimizedResponse optimizeResume(ResumeOptimizeRequest request, Long userId) {

        // 从request对象中获取参数
        Long resumeId = Long.parseLong(request.getResumeId());
        String jobDescription = request.getJobDescription();
        log.info("简历优化开始：userId:{}, resumeId: {}, jobDescription: {}", userId, resumeId, jobDescription);

        StopWatch stopWatch = new StopWatch("简历优化");
        stopWatch.start("根据条件查询简历对象");
        // 使用用户Id进行校验
        CvPO cvPO = cvRepository.getCvByCondition(resumeId, userId);
        if (Objects.isNull(cvPO)) {
            throw new BusinessException(ResultCodeEnum.RESUME_NOT_FOUND, "简历不存在，请重新上传简历");
        }
        stopWatch.stop();

        // 获取简历bo对象
        stopWatch.start("转换简历bo对象");
        CvBO cvBO = resumePersistenceService.convertToCvBO(cvPO);
        stopWatch.stop();

        // 创建简历优化Agent，开始优化简历
        stopWatch.start("开始优化简历");
        CvOptimizationAgent cvOptimizationAgent = AgenticServices.createAgenticSystem(CvOptimizationAgent.class, chatModel);
//        Result<CvBO> cvBOResult = cvOptimizationAgent.optimizeCv(cvBO, jobDescription);
        CvBO optimizeCv = cvOptimizationAgent.optimizeCv(cvBO, jobDescription);;
        stopWatch.stop();


        ResumeOptimizedResponse response = resumeAnalysisService.getResumeAnalysis(cvBO);
        return new ResumeOptimizedResponse();
    }


    /**
     * 生成优化后的简历文件
     * 根据请求中的文件类型（HTML、PDF、DOCX）生成对应格式的简历文件
     * 转换流程：CvBO -> Markdown -> HTML -> PDF/DOCX
     *
     * @param request 包含简历优化请求信息的对象，必须包含resumeId、downloadFileType和cvBO
     * @return 优化后的简历文件的字节数组
     * @throws IllegalArgumentException 当请求参数无效或文件类型不支持时抛出
     * @throws RuntimeException         当文件生成过程中发生异常时抛出
     */
    @Override
    public byte[] generateOptimizedFile(ResumeOptimizedRequest request) {
        log.info("开始生成优化简历文件，resumeId: {}, fileType: {}",
                request.getResumeId(), request.getDownloadFileType());

        // 参数校验
        if (!request.isValid()) {
            log.error("简历优化请求参数无效");
            throw new IllegalArgumentException("请求参数无效，必须包含resumeId和cvBO");
        }

        DownloadFileTypeEnum downloadFileType = request.getDownloadFileType();
        if (Objects.isNull(downloadFileType)) {
            log.warn("未指定下载文件类型，默认使用PDF格式");
            downloadFileType = DownloadFileTypeEnum.PDF;
        }

        CvBO cvBO = request.getCvBO();

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start("生成简历文件");

            // 第一步：CvBO -> Markdown
            stopWatch.start("CvBO转换为Markdown");
            String markdownContent = convertCvToMarkdown(cvBO);
            stopWatch.stop();
            log.debug("Markdown内容生成完成，长度: {}", markdownContent.length());

            // 第二步：Markdown -> HTML
            stopWatch.start("Markdown转换为HTML");
            String htmlContent = convertMarkdownToHtml(markdownContent);
            stopWatch.stop();
            log.debug("HTML内容生成完成，长度: {}", htmlContent.length());

            byte[] result;

            // 第三步：根据文件类型生成最终文件
            switch (downloadFileType) {
                case HTML:
                    stopWatch.start("生成HTML文件");
                    result = htmlContent.getBytes("UTF-8");
                    stopWatch.stop();
                    log.info("HTML文件生成完成，大小: {} bytes", result.length);
                    break;

                case PDF:
                    stopWatch.start("HTML转换为PDF");
                    result = convertHtmlToPdf(htmlContent);
                    stopWatch.stop();
                    log.info("PDF文件生成完成，大小: {} bytes", result.length);
                    break;

                case DOCX:
                    stopWatch.start("HTML转换为DOCX");
                    result = convertHtmlToDocx(htmlContent);
                    stopWatch.stop();
                    log.info("DOCX文件生成完成，大小: {} bytes", result.length);
                    break;

                default:
                    log.error("不支持的文件类型: {}", downloadFileType);
                    throw new IllegalArgumentException("不支持的文件类型: " + downloadFileType);
            }

            stopWatch.stop();
            log.info("简历文件生成完成，resumeId: {}, fileType: {}, 总耗时: {} ms",
                    request.getResumeId(), downloadFileType, stopWatch.getTotalTimeMillis());

            return result;

        } catch (Exception e) {
            log.error("生成简历文件失败，resumeId: {}, fileType: {}",
                    request.getResumeId(), downloadFileType, e);
            throw new RuntimeException("生成简历文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将CvBO对象转换为Markdown格式
     *
     * @param cvBO 简历业务对象
     * @return Markdown格式的简历内容
     * @throws RuntimeException 当转换过程中发生异常时抛出
     */
    private String convertCvToMarkdown(CvBO cvBO) {
        try {
            // 创建模板服务
            TemplateService templateService = new TemplateService();

            // 使用默认的Markdown配置
            MarkdownConfig markdownConfig = MarkdownConfig.defaults();

            // 创建字段映射器，使用空的别名映射
            TemplateFieldMapper fieldMapper = TemplateFieldMapper.builder()
                    .aliases(java.util.Map.of())
                    .build();

            // 渲染Markdown内容
            String markdownContent = templateService.renderMarkdown(cvBO, markdownConfig, fieldMapper);

            if (StringUtils.isEmpty(markdownContent)) {
                log.warn("生成的Markdown内容为空");
                throw new RuntimeException("生成的Markdown内容为空");
            }

            return markdownContent;

        } catch (Exception e) {
            log.error("CvBO转换为Markdown失败", e);
            throw new RuntimeException("CvBO转换为Markdown失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将Markdown内容转换为HTML格式
     *
     * @param markdownContent Markdown格式的内容
     * @return HTML格式的内容
     * @throws RuntimeException 当转换过程中发生异常时抛出
     */
    private String convertMarkdownToHtml(String markdownContent) {
        try {
            // 创建Markdown服务
            MarkdownService markdownService = new MarkdownService();

            // 使用默认的HTML配置
            HtmlConfig htmlConfig = HtmlConfig.defaults();

            // 转换为HTML
            String htmlContent = markdownService.toHtmlFromMarkdown(markdownContent, htmlConfig);

            if (StringUtils.isEmpty(htmlContent)) {
                log.warn("生成的HTML内容为空");
                throw new RuntimeException("生成的HTML内容为空");
            }

            return htmlContent;

        } catch (Exception e) {
            log.error("Markdown转换为HTML失败", e);
            throw new RuntimeException("Markdown转换为HTML失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将HTML内容转换为PDF文件的字节数组
     *
     * @param htmlContent HTML格式的内容
     * @return PDF文件的字节数组
     * @throws RuntimeException 当转换过程中发生异常时抛出
     */
    private byte[] convertHtmlToPdf(String htmlContent) {
        File tempFile = null;
        try {
            // 创建PDF服务
            PdfService pdfService = new PdfService();

            // 使用默认的PDF配置
            PdfConfig pdfConfig = PdfConfig.defaults();

            // 创建临时输出目录
            File tempDir = Files.createTempDirectory("resume-pdf").toFile();
            String fileName = "resume_" + System.currentTimeMillis();

            // 生成PDF文件
            tempFile = pdfService.toPdf(htmlContent, pdfConfig, tempDir, fileName);

            if (Objects.isNull(tempFile) || !tempFile.exists() || tempFile.length() == 0) {
                log.error("PDF文件生成失败或文件为空");
                throw new RuntimeException("PDF文件生成失败");
            }

            // 读取文件内容为字节数组
            byte[] pdfBytes = Files.readAllBytes(tempFile.toPath());
            log.debug("PDF文件读取完成，大小: {} bytes", pdfBytes.length);

            return pdfBytes;

        } catch (Exception e) {
            log.error("HTML转换为PDF失败", e);
            throw new RuntimeException("HTML转换为PDF失败: " + e.getMessage(), e);
        } finally {
            // 清理临时文件
            if (Objects.nonNull(tempFile) && tempFile.exists()) {
                try {
                    Files.deleteIfExists(tempFile.toPath());
                    // 尝试删除临时目录（如果为空）
                    File parentDir = tempFile.getParentFile();
                    if (Objects.nonNull(parentDir) && parentDir.exists()) {
                        parentDir.delete();
                    }
                } catch (IOException e) {
                    log.warn("清理临时PDF文件失败: {}", tempFile.getAbsolutePath(), e);
                }
            }
        }
    }

    /**
     * 将HTML内容转换为DOCX文件的字节数组
     *
     * @param htmlContent HTML格式的内容
     * @return DOCX文件的字节数组
     * @throws RuntimeException 当转换过程中发生异常时抛出
     */
    private byte[] convertHtmlToDocx(String htmlContent) {
        File tempFile = null;
        try {
            // 创建DOCX服务
            DocxService docxService = new DocxService();

            // 使用默认的DOCX配置
            DocxConfig docxConfig = DocxConfig.defaults();

            // 创建临时输出目录
            File tempDir = Files.createTempDirectory("resume-docx").toFile();
            String fileName = "resume_" + System.currentTimeMillis();

            // 生成DOCX文件
            tempFile = docxService.toDocx(htmlContent, docxConfig, tempDir, fileName);

            if (Objects.isNull(tempFile) || !tempFile.exists() || tempFile.length() == 0) {
                log.error("DOCX文件生成失败或文件为空");
                throw new RuntimeException("DOCX文件生成失败");
            }

            // 读取文件内容为字节数组
            byte[] docxBytes = Files.readAllBytes(tempFile.toPath());
            log.debug("DOCX文件读取完成，大小: {} bytes", docxBytes.length);

            return docxBytes;

        } catch (Exception e) {
            log.error("HTML转换为DOCX失败", e);
            throw new RuntimeException("HTML转换为DOCX失败: " + e.getMessage(), e);
        } finally {
            // 清理临时文件
            if (Objects.nonNull(tempFile) && tempFile.exists()) {
                try {
                    Files.deleteIfExists(tempFile.toPath());
                    // 尝试删除临时目录（如果为空）
                    File parentDir = tempFile.getParentFile();
                    if (Objects.nonNull(parentDir) && parentDir.exists()) {
                        parentDir.delete();
                    }
                } catch (IOException e) {
                    log.warn("清理临时DOCX文件失败: {}", tempFile.getAbsolutePath(), e);
                }
            }
        }
    }

    /**
     * 获取任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态响应对象
     */
    @Override
    public TaskStatusResponse getTaskStatus(String taskId) {
        if (StringUtils.isEmpty(taskId)) {
            log.warn("获取任务状态失败，任务ID为空");
            return null;
        }

        try {
            ResumeTaskPO taskPO = resumeTaskService.getByTaskId(taskId);
            if (Objects.isNull(taskPO)) {
                log.warn("获取任务状态失败，任务不存在，taskId: {}", taskId);
                return null;
            }

            return convertToTaskStatusResponse(taskPO);
        } catch (Exception e) {
            log.error("获取任务状态异常，taskId: {}", taskId, e);
            return null;
        }
    }

    /**
     * 获取用户任务列表
     *
     * @param userId 用户ID（可选）
     * @param status 任务状态（可选）
     * @return 任务列表
     */
    @Override
    public List<TaskStatusResponse> getUserTasks(Long userId, String status) {
        try {
            List<ResumeTaskPO> taskPOList = resumeTaskService.getUserTasks(userId, status);

            if (CollectionUtils.isEmpty(taskPOList)) {
                return new ArrayList<>();
            }

            List<TaskStatusResponse> responseList = new ArrayList<>();
            for (ResumeTaskPO taskPO : taskPOList) {
                TaskStatusResponse response = convertToTaskStatusResponse(taskPO);
                if (Objects.nonNull(response)) {
                    responseList.add(response);
                }
            }

            return responseList;
        } catch (Exception e) {
            log.error("获取用户任务列表异常，userId: {}, status: {}", userId, status, e);
            return new ArrayList<>();
        }
    }

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     * @return 取消是否成功
     */
    @Override
    public Boolean cancelTask(String taskId) {
        if (StringUtils.isEmpty(taskId)) {
            log.warn("取消任务失败，任务ID为空");
            return Boolean.FALSE;
        }

        try {
            ResumeTaskPO taskPO = resumeTaskService.getByTaskId(taskId);
            if (Objects.isNull(taskPO)) {
                log.warn("取消任务失败，任务不存在，taskId: {}", taskId);
                return Boolean.FALSE;
            }

            // 只有处理中、解析中、存储中的任务才能取消
            String currentStatus = taskPO.getStatus();
            if (StringUtils.equals(currentStatus, TaskStatusEnum.COMPLETED.getCode()) ||
                    StringUtils.equals(currentStatus, TaskStatusEnum.FAILED.getCode())) {
                log.warn("取消任务失败，任务已完成或失败，taskId: {}, status: {}", taskId, currentStatus);
                return Boolean.FALSE;
            }

            // 更新任务状态为失败，并设置错误信息
            LocalDateTime nowTime = LocalDateTime.now();
            resumeTaskService.failTask(taskId, taskPO.getResumeId(), nowTime, "任务已被用户取消");

            log.info("任务取消成功，taskId: {}", taskId);
            return Boolean.TRUE;
        } catch (Exception e) {
            log.error("取消任务异常，taskId: {}", taskId, e);
            return Boolean.FALSE;
        }
    }

    /**
     * 将ResumeTaskPO转换为TaskStatusResponse
     *
     * @param taskPO 任务持久化对象
     * @return 任务状态响应对象
     */
    private TaskStatusResponse convertToTaskStatusResponse(ResumeTaskPO taskPO) {
        if (Objects.isNull(taskPO)) {
            return null;
        }

        TaskStatusResponse.TaskStatusResponseBuilder builder = TaskStatusResponse.builder()
                .taskId(taskPO.getTaskId())
                .status(taskPO.getStatus())
                .statusMessage(getStatusMessage(taskPO.getStatus()))
                .startTime(taskPO.getStartTime())
                .completeTime(taskPO.getCompleteTime())
                .resumeId(taskPO.getResumeId())
                .errorMessage(taskPO.getErrorMessage())
                .fileName(taskPO.getFileName())
                .originalFileName(taskPO.getOriginalFileName());

        // 计算进度百分比
        Integer progress = calculateProgress(taskPO.getStatus());
        builder.progress(progress);

        // 计算预估剩余时间
        Long estimatedRemainingSeconds = calculateEstimatedRemainingTime(taskPO);
        builder.estimatedRemainingSeconds(estimatedRemainingSeconds);

        return builder.build();
    }

    /**
     * 根据任务状态获取状态描述
     *
     * @param status 任务状态码
     * @return 状态描述
     */
    private String getStatusMessage(String status) {
        if (StringUtils.isEmpty(status)) {
            return "未知状态";
        }

        for (TaskStatusEnum statusEnum : TaskStatusEnum.values()) {
            if (StringUtils.equals(status, statusEnum.getCode())) {
                return statusEnum.getDesc();
            }
        }
        return "未知状态";
    }

    /**
     * 根据任务状态计算进度百分比
     *
     * @param status 任务状态码
     * @return 进度百分比（0-100）
     */
    private Integer calculateProgress(String status) {
        if (StringUtils.isEmpty(status)) {
            return 0;
        }

        if (StringUtils.equals(status, TaskStatusEnum.PROCESSING.getCode())) {
            return 10;
        } else if (StringUtils.equals(status, TaskStatusEnum.ANALYZING.getCode())) {
            return 50;
        } else if (StringUtils.equals(status, TaskStatusEnum.SAVING.getCode())) {
            return 80;
        } else if (StringUtils.equals(status, TaskStatusEnum.COMPLETED.getCode())) {
            return 100;
        } else if (StringUtils.equals(status, TaskStatusEnum.FAILED.getCode())) {
            return 0;
        }
        return 0;
    }

    /**
     * 计算预估剩余时间
     *
     * @param taskPO 任务持久化对象
     * @return 预估剩余时间（秒），如果任务已完成或失败则返回null
     */
    private Long calculateEstimatedRemainingTime(ResumeTaskPO taskPO) {
        if (Objects.isNull(taskPO) || Objects.isNull(taskPO.getStartTime())) {
            return null;
        }

        String status = taskPO.getStatus();

        // 已完成或失败的任务不需要预估时间
        if (StringUtils.equals(status, TaskStatusEnum.COMPLETED.getCode()) ||
                StringUtils.equals(status, TaskStatusEnum.FAILED.getCode())) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        long elapsedSeconds = ChronoUnit.SECONDS.between(taskPO.getStartTime(), now);

        // 根据当前状态和已用时间预估剩余时间
        if (StringUtils.equals(status, TaskStatusEnum.PROCESSING.getCode())) {
            // 处理中阶段预估还需要90秒
            return Math.max(90L, 120L - elapsedSeconds);
        } else if (StringUtils.equals(status, TaskStatusEnum.ANALYZING.getCode())) {
            // 解析中阶段预估还需要60秒
            return Math.max(60L, 90L - elapsedSeconds);
        } else if (StringUtils.equals(status, TaskStatusEnum.SAVING.getCode())) {
            // 存储中阶段预估还需要30秒
            return Math.max(30L, 45L - elapsedSeconds);
        }

        return 60L;
    }
}