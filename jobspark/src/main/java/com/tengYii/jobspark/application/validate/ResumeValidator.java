package com.tengYii.jobspark.application.validate;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.google.common.base.Joiner;
import com.tengYii.jobspark.common.enums.DownloadFileTypeEnum;
import com.tengYii.jobspark.dto.request.LoginRequest;
import com.tengYii.jobspark.dto.request.ResumeOptimizeRequest;
import com.tengYii.jobspark.dto.request.ResumeOptimizedDownloadRequest;
import com.tengYii.jobspark.dto.request.ResumeUploadRequest;
import com.tengYii.jobspark.common.constants.ContentTypeConstants;
import com.tengYii.jobspark.common.constants.ParseConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 简历文件校验器
 */
public class ResumeValidator {

    /**
     * 支持上传文件的最大大小：5MB
     */
    public static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public static String validateLogin(LoginRequest loginRequest) {
        List<String> errorMessages = new ArrayList<>();

        // 参数验证
        if (Objects.isNull(loginRequest)) {
            errorMessages.add("登录请求不能为空");
        }

        if (StringUtils.isEmpty(loginRequest.getUsername())) {
            errorMessages.add("用户名不能为空");
        }

        if (StringUtils.isEmpty(loginRequest.getPassword())) {
            errorMessages.add("密码不能为空");
        }

        // 返回所有错误信息（逗号分隔）
        return Joiner.on(ParseConstant.COMMA).join(errorMessages);
    }

    /**
     * 统一校验入口（文件类型 + 文件大小 + userId）
     *
     * @param request 简历上传请求
     * @return 错误信息，无错误则返回空字符串
     */
    public static String validateUploadRequest(ResumeUploadRequest request) {
        List<String> errorMessages = new ArrayList<>();
        MultipartFile file = request.getFile();

        // 1. 校验文件是否为空
        if (Objects.isNull(file) || file.isEmpty()) {
            errorMessages.add("上传文件不能为空");
            return Joiner.on(ParseConstant.COMMA).join(errorMessages);
        }

        // 2. 校验文件类型（双重校验：后缀 + Content-Type）
        validateFileType(file, errorMessages);

        // 3. 校验文件大小
        validateFileSize(file, errorMessages);

        // 返回所有错误信息（逗号分隔）
        return Joiner.on(ParseConstant.COMMA).join(errorMessages);
    }

    /**
     * 双重校验：文件后缀 + Content-Type（避免单一校验被绕过）
     *
     * @param file          上传文件
     * @param errorMessages 错误信息列表
     */
    private static void validateFileType(MultipartFile file, List<String> errorMessages) {
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        // 2.1 校验文件名和后缀
        if (StringUtils.isEmpty(originalFilename)) {
            errorMessages.add("文件名不能为空或空白");
            return;
        }

        // 提取文件后缀（处理无后缀场景）
        String fileExtension = getFileExtension(originalFilename);
        if (!ContentTypeConstants.SUPPORTED_EXTENSIONS.contains(fileExtension)) {
            String supportedExts = String.join(", ", ContentTypeConstants.SUPPORTED_EXTENSIONS);
            errorMessages.add(String.format("不支持的文件后缀，仅支持：%s（当前后缀：%s）", supportedExts, fileExtension));
            return;
        }

        // 2.2 校验 Content-Type（允许 Content-Type 为 null，仅做辅助校验）
        if (StringUtils.isNotEmpty(contentType) &&
                !ContentTypeConstants.SUPPORTED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            // 检查是否为兼容的Content-Type（如md文件可能被识别为text/plain）
            String expectedContentType = ContentTypeConstants.EXTENSION_TO_CONTENT_TYPE.get(fileExtension);
            if (!StringUtils.equals(contentType.toLowerCase(), expectedContentType)) {
                String supportedContentTypes = String.join(", ", ContentTypeConstants.SUPPORTED_CONTENT_TYPES);
                errorMessages.add(String.format("文件类型与内容不匹配，当前 Content-Type：%s，支持的类型：%s",
                        contentType, supportedContentTypes));
            }
        }
    }

    /**
     * 提取文件后缀（健壮处理无后缀、多后缀场景）
     *
     * @param originalFilename 原始文件名
     * @return 文件后缀（小写），无后缀返回空字符串
     */
    private static String getFileExtension(String originalFilename) {
        int lastDotIndex = originalFilename.lastIndexOf(".");
        return lastDotIndex == -1 ? "" : originalFilename.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 校验文件大小（5MB 限制）
     *
     * @param file          上传文件
     * @param errorMessages 错误信息列表
     */
    private static void validateFileSize(MultipartFile file, List<String> errorMessages) {
        long fileSize = file.getSize();
        if (fileSize > MAX_FILE_SIZE) {
            double actualSize = fileSize / 1024.0 / 1024.0;
            errorMessages.add(String.format("文件大小超过限制，当前：%.1fMB，最大允许：5.0MB", actualSize));
        }
    }

    /**
     * 校验简历优化请求参数
     *
     * @param request 简历优化请求对象
     * @return 错误信息，无错误则返回空字符串
     */
    public static String validateOptimizeRequest(ResumeOptimizeRequest request) {
        List<String> errorMessages = new ArrayList<>();

        // 校验请求对象是否为空
        if (Objects.isNull(request)) {
            errorMessages.add("请求参数不能为空");
            return Joiner.on(ParseConstant.COMMA).join(errorMessages);
        }

        // 校验简历ID
        if (Objects.isNull(request.getResumeId())) {
            errorMessages.add("简历ID不能为空");
        }

        // 校验职位描述
        String jobDescription = request.getJobDescription();
        if (StringUtils.isEmpty(jobDescription)) {
            errorMessages.add("职位描述不能为空");
        } else {
            // 校验职位描述长度
            if (jobDescription.length() > 5000) {
                errorMessages.add("职位描述长度不能超过5000个字符");
            }

            // 校验职位描述内容不能只包含空白字符
            if (StringUtils.isBlank(jobDescription)) {
                errorMessages.add("职位描述不能只包含空白字符");
            }
        }

        // 返回所有错误信息（逗号分隔）
        return Joiner.on(ParseConstant.COMMA).join(errorMessages);
    }

    /**
     * 校验优化后简历下载请求参数
     *
     * @param request 优化后简历下载请求对象
     * @return 错误信息，无错误则返回空字符串
     */
    public static String validateOptimizedDownloadRequest(ResumeOptimizedDownloadRequest request) {
        // 校验请求对象是否为空（直接返回，避免创建不必要的List）
        if (Objects.isNull(request)) {
            return "请求参数不能为空";
        }

        List<String> errorMessages = new ArrayList<>();

        // 校验简历ID
        if (Objects.isNull(request.getOptimizedResumeId())) {
            errorMessages.add("简历ID不能为空");
        }

        // 校验文件下载类型
        String downloadFileType = request.getDownloadFileType();
        if (StringUtils.isEmpty(downloadFileType)) {
            errorMessages.add("文件下载类型不能为空");
        } else {
            // 仅在类型不为空时校验是否支持
            DownloadFileTypeEnum fileTypeEnum = DownloadFileTypeEnum.getByFormat(downloadFileType);
            if (Objects.isNull(fileTypeEnum)) {
                errorMessages.add("文件下载类型不支持");
            }
        }

        // 若无错误信息，直接返回空字符串，避免Joiner开销
        if (CollectionUtils.isEmpty(errorMessages)) {
            return StringUtils.EMPTY;
        }

        // 返回所有错误信息（逗号分隔）
        return Joiner.on(ParseConstant.COMMA).join(errorMessages);
    }
}