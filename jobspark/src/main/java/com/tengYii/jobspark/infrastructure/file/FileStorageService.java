package com.tengYii.jobspark.infrastructure.file;

import com.tengYii.jobspark.domain.model.constants.ContentTypeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService() {
        this.fileStorageLocation = Paths.get("uploads/resumes")
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("无法创建文件存储目录", ex);
        }
    }

    public String storeResumeFile(MultipartFile file) {
        try {
            // 验证文件类型
            String fileName = validateAndGenerateFileName(file);

            // 复制文件到目标位置
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("文件存储成功: {}", fileName);
            return fileName.replace(".", "_"); // 返回文件ID（去除扩展名）
        } catch (IOException ex) {
            throw new RuntimeException("文件存储失败", ex);
        }
    }

    private String validateAndGenerateFileName(MultipartFile file) {
        // 验证文件类型
        String contentType = file.getContentType();
        if (!isSupportedContentType(contentType)) {
            throw new RuntimeException("不支持的文件类型: " + contentType);
        }

        // 生成唯一文件名
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        return UUID.randomUUID().toString() + fileExtension;
    }

    private boolean isSupportedContentType(String contentType) {
        return contentType != null &&
                ContentTypeConstants.SUPPORTED_TYPES.contains(contentType);
    }

    public byte[] loadResumeFile(String fileId) {
        try {
            String fileName = fileId.replace("_", ".");
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            return Files.readAllBytes(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("文件读取失败: " + fileId, ex);
        }
    }

    public String getFileContent(String fileId) {
        try {
            byte[] fileBytes = loadResumeFile(fileId);
            return new String(fileBytes);
        } catch (Exception ex) {
            throw new RuntimeException("文件内容读取失败: " + fileId, ex);
        }
    }
}