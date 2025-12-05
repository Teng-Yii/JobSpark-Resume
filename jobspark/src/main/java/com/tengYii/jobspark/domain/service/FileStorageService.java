package com.tengYii.jobspark.domain.service;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.OSSObject;
import com.aliyuncs.exceptions.ClientException;
import com.tengYii.jobspark.common.constants.FileStoreConstants;
import com.tengYii.jobspark.model.dto.FileStorageResultDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * 文件存储服务类
 * 提供基于阿里云OSS的文件存储功能
 */
@Slf4j
@Service
public class FileStorageService {

    /**
     * OSS客户端实例（懒汉式单例）
     */
    private volatile OSS ossClient;

    /**
     * 构建OSS基本信息并创建客户端
     *
     * @return OSS客户端实例
     * @throws ClientException 客户端异常
     */
    public OSS getOssClient() throws ClientException {
        if (Objects.isNull(this.ossClient)) {
            synchronized (this) {
                if (Objects.isNull(this.ossClient)) {
                    try {
                        // 从环境变量中获取访问凭证
                        EnvironmentVariableCredentialsProvider credentialsProvider =
                                CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();

                        // 创建客户端配置
                        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
                        // 显式声明使用 V4 签名算法
                        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

                        // 创建OSS客户端实例
                        this.ossClient = OSSClientBuilder.create()
                                .endpoint(FileStoreConstants.OSS_ENDPOINT)
                                .credentialsProvider(credentialsProvider)
                                .region(FileStoreConstants.OSS_REGION)
                                .clientConfiguration(clientBuilderConfiguration)
                                .build();

                        log.info("OSS客户端懒加载创建成功");
                    } catch (Exception e) {
                        log.error("创建OSS客户端失败: {}", e.getMessage(), e);
                        throw new ClientException("创建OSS客户端失败: " + e.getMessage());
                    }
                }
            }
        }
        return this.ossClient;
    }

    /**
     * 保存上传文件到OSS
     *
     * @param file 上传的文件
     * @param bucketName 存储桶名称，如果为空则自动生成
     * @return 文件存储信息对象
     * @throws ClientException 客户端异常
     */
    public FileStorageResultDTO saveUploadedFile(MultipartFile file, String bucketName) throws ClientException {
        if (Objects.isNull(file) || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        OSS ossClient = null;
        try {
            // 构建OSS客户端
            ossClient = getOssClient();

            // 如果未指定bucket名称，则生成唯一的bucket名称
            if (StringUtils.isEmpty(bucketName)) {
                bucketName = generateUniqueBucketName(FileStoreConstants.BUCKET_PREFIX);
            }

            // 创建存储空间（如果不存在）
            createBucketIfNotExists(ossClient, bucketName);

            // 生成唯一的文件名
            String uniqueFileName = generateUniqueObjectName(file);

            // 上传文件到OSS
            try (InputStream inputStream = file.getInputStream()) {
                ossClient.putObject(bucketName, uniqueFileName, inputStream);
                log.info("文件上传成功 - Bucket: {}, Object: {}", bucketName, uniqueFileName);
            }

            // 返回文件存储结果
            return FileStorageResultDTO.builder()
                    .bucketName(bucketName)
                    .uniqueFileName(uniqueFileName)
                    .originalFileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .build();

        } catch (OSSException oe) {
            log.error("OSS服务异常 - ErrorCode: {}, ErrorMessage: {}", oe.getErrorCode(), oe.getErrorMessage());
            throw new ClientException("OSS服务异常: " + oe.getErrorMessage());
        } catch (IOException e) {
            log.error("文件读取异常: {}", e.getMessage(), e);
            throw new ClientException("文件读取异常: " + e.getMessage());
        } finally {
            // 确保关闭OSS客户端
            closeOssClient(ossClient);
        }
    }

    /**
     * 根据bucket和文件名下载文件
     *
     * @param bucketName 存储桶名称
     * @param objectName 文件对象名称
     * @return 文件输入流
     * @throws ClientException 客户端异常
     */
    public InputStream downloadFileByBucketAndName(String bucketName, String objectName) throws ClientException {
        if (StringUtils.isEmpty(bucketName)) {
            throw new IllegalArgumentException("存储桶名称不能为空");
        }
        if (StringUtils.isEmpty(objectName)) {
            throw new IllegalArgumentException("文件对象名称不能为空");
        }

        OSS ossClient = null;
        try {
            // 构建OSS客户端
            ossClient = getOssClient();

            // 检查文件是否存在
            if (!ossClient.doesObjectExist(bucketName, objectName)) {
                throw new ClientException("文件不存在: " + bucketName + "/" + objectName);
            }

            // 获取文件对象
            OSSObject ossObject = ossClient.getObject(bucketName, objectName);
            log.info("文件下载成功 - Bucket: {}, Object: {}", bucketName, objectName);

            return ossObject.getObjectContent();

        } catch (OSSException oe) {
            log.error("OSS服务异常 - ErrorCode: {}, ErrorMessage: {}", oe.getErrorCode(), oe.getErrorMessage());
            throw new ClientException("OSS服务异常: " + oe.getErrorMessage());
        } catch (Exception e) {
            log.error("文件下载异常: {}", e.getMessage(), e);
            throw new ClientException("文件下载异常: " + e.getMessage());
        }
        // 注意：这里不关闭ossClient，因为返回的InputStream还需要使用连接
        // 调用方需要负责关闭InputStream
    }

    /**
     * 辅助方法：生成唯一的Bucket名称
     *
     * @param prefix 前缀
     * @return 唯一的Bucket名称
     */
    public String generateUniqueBucketName(String prefix) {
        if (StringUtils.isEmpty(prefix)) {
            prefix = "default";
        }

        // 获取当前时间戳
        String timestamp = String.valueOf(System.currentTimeMillis());
        // 生成随机数
        Random random = new Random();
        int randomNum = random.nextInt(10000);

        // 组合生成唯一名称（转换为小写，符合OSS命名规范）
        return (prefix + "-" + timestamp + "-" + randomNum).toLowerCase();
    }

    /**
     * 辅助方法：生成唯一的文件对象名称
     *
     * @param file 上传的文件
     * @return 唯一的对象名称
     */
    private String generateUniqueObjectName(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";

        // 提取文件扩展名
        if (StringUtils.isNotEmpty(originalFileName) && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        // 生成带时间戳和UUID的唯一文件名
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().replace("-", "");

        return "resumes/" + timestamp + "/" + uuid + fileExtension;
    }

    /**
     * 辅助方法：创建存储桶（如果不存在）
     *
     * @param ossClient OSS客户端
     * @param bucketName 存储桶名称
     */
    private void createBucketIfNotExists(OSS ossClient, String bucketName) {
        try {
            if (!ossClient.doesBucketExist(bucketName)) {
                ossClient.createBucket(bucketName);
                log.info("存储桶创建成功: {}", bucketName);
            } else {
                log.debug("存储桶已存在: {}", bucketName);
            }
        } catch (OSSException e) {
            log.error("创建存储桶失败 - Bucket: {}, Error: {}", bucketName, e.getErrorMessage());
            throw e;
        }
    }

    /**
     * 辅助方法：安全关闭OSS客户端
     *
     * @param ossClient OSS客户端实例
     */
    private void closeOssClient(OSS ossClient) {
        if (Objects.nonNull(ossClient)) {
            try {
                ossClient.shutdown();
                log.debug("OSS客户端已关闭");
            } catch (Exception e) {
                log.warn("关闭OSS客户端时发生异常: {}", e.getMessage());
            }
        }
    }

    /**
     * 辅助方法：删除OSS文件
     *
     * @param bucketName 存储桶名称
     * @param objectName 文件对象名称
     * @return 删除是否成功
     */
    public boolean deleteFile(String bucketName, String objectName) {
        if (StringUtils.isEmpty(bucketName) || StringUtils.isEmpty(objectName)) {
            log.warn("删除文件参数不能为空 - Bucket: {}, Object: {}", bucketName, objectName);
            return false;
        }

        OSS ossClient = null;
        try {
            ossClient = getOssClient();

            if (ossClient.doesObjectExist(bucketName, objectName)) {
                ossClient.deleteObject(bucketName, objectName);
                log.info("文件删除成功 - Bucket: {}, Object: {}", bucketName, objectName);
                return true;
            } else {
                log.warn("要删除的文件不存在 - Bucket: {}, Object: {}", bucketName, objectName);
                return false;
            }

        } catch (Exception e) {
            log.error("删除文件失败 - Bucket: {}, Object: {}, Error: {}", bucketName, objectName, e.getMessage());
            return false;
        } finally {
            closeOssClient(ossClient);
        }
    }
}