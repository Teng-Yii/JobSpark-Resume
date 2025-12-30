package com.tengYii.jobspark.application.service.impl;

import com.tengYii.jobspark.application.service.ResumeApplicationService;
import com.tengYii.jobspark.common.enums.DeleteFlagEnum;
import com.tengYii.jobspark.common.enums.DownloadFileTypeEnum;
import com.tengYii.jobspark.common.enums.ResultCodeEnum;
import com.tengYii.jobspark.common.enums.TaskStatusEnum;
import com.tengYii.jobspark.common.exception.BusinessException;
import com.tengYii.jobspark.common.utils.SnowflakeUtil;
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
import com.tengYii.jobspark.dto.request.ResumeOptimizedDownloadRequest;
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
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Resource(name = "resumeTaskExecutor")
    private Executor resumeTaskExecutor;

    @Autowired
    private ResumeRagService resumeRagService;

    @Resource(name = "chatModel")
    private ChatModel chatModel;

    /** 任务处理阶段预估总耗时（秒） */
    private static final long PROCESSING_ESTIMATED_TOTAL_SECONDS = 70L;
    /** 任务处理阶段最小剩余时间（秒） */
    private static final long PROCESSING_MIN_REMAINING_SECONDS = 65L;

    /** 任务解析阶段预估总耗时（秒） */
    private static final long ANALYZING_ESTIMATED_TOTAL_SECONDS = 70L;
    /** 任务解析阶段最小剩余时间（秒） */
    private static final long ANALYZING_MIN_REMAINING_SECONDS = 10L;

    /** 任务保存阶段预估总耗时（秒） */
    private static final long SAVING_ESTIMATED_TOTAL_SECONDS = 75L;
    /** 任务保存阶段最小剩余时间（秒） */
    private static final long SAVING_MIN_REMAINING_SECONDS = 3L;

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
                processResumeAsync(userId, taskId, storageResultDTO.getUniqueFileName());
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
     * @param userId   用户ID
     * @param taskId   任务ID
     * @param fileName 存储文件名称
     */
    @Async
    private void processResumeAsync(Long userId, String taskId, String fileName) {

        Long resumeId = -1L;
        LocalDateTime nowTime = LocalDateTime.now();
        try {
            // 更新任务状态为解析中
            Boolean updateTaskStatus = resumeTaskService.updateTaskStatus(taskId, TaskStatusEnum.ANALYZING);
            if (Boolean.FALSE.equals(updateTaskStatus)) {
                log.error("异步处理简历解析，更新异步任务状态失败，taskId: {}", taskId);
            }

            // 解析简历内容（耗时操作）
            long currentTimeMillis = System.currentTimeMillis();
            CvBO cvBO = resumeAnalysisService.analyzeResumeFile(fileName);
            cvBO.setUserId(userId);
            log.info("解析简历内容耗时:{} ms", System.currentTimeMillis() - currentTimeMillis);

            // 更新任务状态为存储中
            updateTaskStatus = resumeTaskService.updateTaskStatus(taskId, TaskStatusEnum.SAVING);
            if (Boolean.FALSE.equals(updateTaskStatus)) {
                log.error("异步处理简历解析，更新异步任务状态失败，taskId: {}", taskId);
            }

            // 将结构化简历对象落库（耗时操作）
            resumeId = resumePersistenceService.convertAndSaveCv(cvBO, nowTime);

            // 更新任务状态为完成
            Boolean completed = resumeTaskService.completeTask(taskId, resumeId);
            if (completed) {
                log.info("任务完成，taskId: {}, resumeId: {}", taskId, resumeId);
            }
        } catch (Exception e) {
            log.error("异步处理简历失败，taskId: {}", taskId, e);
            // 更新任务状态为失败
            resumeTaskService.failTask(taskId, resumeId, e.getMessage());
        }
    }

    /**
     * 获取优化后的简历信息
     *
     * @param request 简历优化请求对象，包含resumeId和jobDescription
     * @return 优化后的简历响应对象
     */
    public ResumeOptimizedResponse optimizeResume(ResumeOptimizeRequest request) {

        // 从request对象中获取参数
        Long userId = request.getUserId();
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

        // 检索参考模板 (RAG)
        stopWatch.start("检索优秀简历模板");
        List<String> referenceTemplates = resumeRagService.retrieveTemplates(jobDescription, 3);
        stopWatch.stop();

        // 创建简历优化Agent，开始优化简历
        stopWatch.start("开始优化简历");
        CvOptimizationAgent cvOptimizationAgent = AgenticServices.createAgenticSystem(CvOptimizationAgent.class, chatModel);
        CvBO optimizeCv = cvOptimizationAgent.optimizeCv(cvBO, jobDescription, referenceTemplates);
        stopWatch.stop();

        // 优化后的简历落库
        stopWatch.start("优化后简历保存");
        // 设置当前时间用于保存
        LocalDateTime nowTime = LocalDateTime.now();
        // 将优化后的简历保存到数据库，并获取新的简历ID
        Long newResumeId = resumePersistenceService.convertAndSaveCv(optimizeCv, nowTime);
        log.info("优化后简历已保存，newResumeId: {}", newResumeId);
        stopWatch.stop();

        // 构建返回结果
        ResumeOptimizedResponse response = new ResumeOptimizedResponse();
        // 填充优化建议
        response.setSuggestionText(optimizeCv.getAdvice());
        response.setOptimizationHistory(optimizeCv.getOptimizationHistory());
        // 填充优化后的简历对象
        response.setOptimizedResumeId(newResumeId);

        log.info("简历优化完成，耗时信息：{}", stopWatch.prettyPrint());
        return response;
    }


    /**
     * 生成优化后的简历文件
     * 根据请求中的文件类型（HTML、PDF、DOCX）生成对应格式的简历文件
     * 转换流程：CvBO -> Markdown -> HTML -> PDF/DOCX
     *
     * @param request 包含简历优化请求信息的对象
     * @return 优化后的简历文件的字节数组
     */
    @Override
    public byte[] generateOptimizedFile(ResumeOptimizedDownloadRequest request) {
        Long resumeId = request.getOptimizedResumeId();
        String fileType = request.getDownloadFileType();
        log.info("开始生成优化简历文件，resumeId: {}, fileType: {}", resumeId, fileType);

        try {
            StopWatch stopWatch = new StopWatch();

            stopWatch.start("获取简历bo对象");
            // 使用用户Id进行校验
            CvPO cvPO = cvRepository.getCvByCondition(resumeId, request.getUserId());
            if (Objects.isNull(cvPO)) {
                throw new BusinessException(ResultCodeEnum.RESUME_NOT_FOUND, "简历不存在，请重新上传简历");
            }

            // 获取简历bo对象
            CvBO cvBO = resumePersistenceService.convertToCvBO(cvPO);
            stopWatch.stop();

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
            DownloadFileTypeEnum downloadFileType = DownloadFileTypeEnum.getByFormat(fileType);
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

            log.info("简历文件生成完成，resumeId: {}, fileType: {}, 总耗时: {} ms", resumeId, downloadFileType, stopWatch.getTotalTimeMillis());
            return result;
        } catch (Exception e) {
            log.error("生成简历文件失败，resumeId: {}, fileType: {}", resumeId, fileType, e);
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
            resumeTaskService.failTask(taskId, taskPO.getResumeId(), "任务已被用户取消");

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
        Integer progress = calculateProgress(taskPO);
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
     * 根据任务状态和耗时计算进度百分比
     *
     * @param taskPO 任务对象
     * @return 进度百分比（0-100）
     */
    private Integer calculateProgress(ResumeTaskPO taskPO) {
        if (Objects.isNull(taskPO) || StringUtils.isEmpty(taskPO.getStatus())) {
            return 0;
        }

        String status = taskPO.getStatus();
        // 1. 已完成或失败状态直接返回
        if (StringUtils.equals(status, TaskStatusEnum.COMPLETED.getCode())) {
            return 100;
        } else if (StringUtils.equals(status, TaskStatusEnum.FAILED.getCode())) {
            return 0;
        }

        // 2. 如果开始时间为空，返回初始进度
        if (Objects.isNull(taskPO.getStartTime())) {
            return 5;
        }

        // 3. 计算已耗时（秒）
        long elapsedSeconds = ChronoUnit.SECONDS.between(taskPO.getStartTime(), LocalDateTime.now());
        // 避免负数（系统时间回调等极端情况）
        elapsedSeconds = Math.max(0, elapsedSeconds);

        if (StringUtils.equals(status, TaskStatusEnum.PROCESSING.getCode())) {
            // 初始处理阶段，固定返回5%
            return 5;
        } else if (StringUtils.equals(status, TaskStatusEnum.ANALYZING.getCode())) {
            // 解析阶段（核心耗时阶段，假设约50-60秒）
            // 逻辑：基础值 10 + (已耗时 / 55.0 * 70)
            // 解释：从10%开始，随时间增长，最大增长到85%
            double progress = 10.0 + (elapsedSeconds / 55.0 * 70.0);
            return (int) Math.min(85, Math.round(progress));
        } else if (StringUtils.equals(status, TaskStatusEnum.SAVING.getCode())) {
            // 保存阶段（收尾阶段）
            // 逻辑：基础值 85 + ((已耗时 - 55) / 15.0 * 14)
            // 解释：假设进入此阶段已经过去约55秒，后续增长到99%
            // 使用 Math.max(elapsedSeconds, 55) 确保进度不回退，始终 >= 85
            double progress = 85.0 + ((Math.max(elapsedSeconds, 55) - 55.0) / 15.0 * 14.0);
            return (int) Math.min(99, Math.round(progress));
        }

        return 0;
    }

    /**
     * 计算预估剩余时间
     * <p>
     * 根据当前任务状态和已耗时时长，计算任务预计还需要多少秒完成。
     * 不同阶段有不同的预估基准时间和最小剩余时间保底。
     * </p>
     *
     * @param taskPO 任务持久化对象
     * @return 预估剩余时间（秒），如果任务已完成或失败则返回null
     */
    private Long calculateEstimatedRemainingTime(ResumeTaskPO taskPO) {
        // 1. 基础校验：对象为空或开始时间为空，无法计算
        if (Objects.isNull(taskPO) || Objects.isNull(taskPO.getStartTime())) {
            return null;
        }

        String status = taskPO.getStatus();
        // 校验状态字符串非空
        if (StringUtils.isEmpty(status)) {
            return null;
        }

        // 2. 终态检查：已完成或失败的任务不需要预估时间
        if (StringUtils.equals(status, TaskStatusEnum.COMPLETED.getCode()) ||
                StringUtils.equals(status, TaskStatusEnum.FAILED.getCode())) {
            return null;
        }

        // 3. 计算已耗时（秒）
        LocalDateTime now = LocalDateTime.now();
        long elapsedSeconds = ChronoUnit.SECONDS.between(taskPO.getStartTime(), now);
        // 避免时间回拨导致负数
        elapsedSeconds = Math.max(0L, elapsedSeconds);

        // 4. 根据状态计算剩余时间
        // 使用策略模式思想，匹配对应状态的配置参数
        if (StringUtils.equals(status, TaskStatusEnum.PROCESSING.getCode())) {
            // PROCESSING 阶段
            return calculateRemainingSeconds(elapsedSeconds, PROCESSING_ESTIMATED_TOTAL_SECONDS, PROCESSING_MIN_REMAINING_SECONDS);
        } else if (StringUtils.equals(status, TaskStatusEnum.ANALYZING.getCode())) {
            // ANALYZING 阶段
            return calculateRemainingSeconds(elapsedSeconds, ANALYZING_ESTIMATED_TOTAL_SECONDS, ANALYZING_MIN_REMAINING_SECONDS);
        } else if (StringUtils.equals(status, TaskStatusEnum.SAVING.getCode())) {
            // SAVING 阶段
            return calculateRemainingSeconds(elapsedSeconds, SAVING_ESTIMATED_TOTAL_SECONDS, SAVING_MIN_REMAINING_SECONDS);
        }

        // 其他未定义状态
        return null;
    }

    /**
     * 计算剩余时间的通用逻辑
     *
     * @param elapsedSeconds 已耗时（秒）
     * @param totalSeconds   预估总耗时（秒）
     * @param minSeconds     最小剩余时间保底（秒）
     * @return 剩余时间（秒）
     */
    private Long calculateRemainingSeconds(long elapsedSeconds, long totalSeconds, long minSeconds) {
        // 剩余时间 = 总预估时间 - 已耗时
        long remaining = totalSeconds - elapsedSeconds;
        // 必须保证至少返回 minSeconds，避免出现负数或 0（给用户即时的心理缓冲）
        return Math.max(minSeconds, remaining);
    }

    /**
     * 将简历保存到向量数据库
     *
     * @param resumeId 简历ID
     * @param userId   用户ID
     * @return 保存是否成功
     */
    @Override
    public Boolean storeResumeEmbedding(Long resumeId, Long userId) {
        log.info("开始将简历保存到向量数据库，resumeId: {}, userId: {}", resumeId, userId);

        // 使用cvRepository.getCvByCondition查询CvPO
        CvPO cvPO = cvRepository.getCvByCondition(resumeId, userId);

        // 校验CvPO是否存在
        if (Objects.isNull(cvPO)) {
            log.error("简历不存在，resumeId: {}, userId: {}", resumeId, userId);
            throw new BusinessException(ResultCodeEnum.RESUME_NOT_FOUND, "简历不存在");
        }

        try {
            // 将CvPO转换为CvBO
            CvBO cvBO = resumePersistenceService.convertToCvBO(cvPO);
            // 调用resumeRagService.storeCvBO保存到向量数据库
            resumeRagService.storeCvBO(cvBO);
            log.info("简历成功保存到向量数据库，resumeId: {}", resumeId);
            return Boolean.TRUE;
        } catch (Exception e) {
            log.error("保存简历到向量数据库失败，resumeId: {}", resumeId, e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "保存简历到向量数据库失败");
        }
    }
}