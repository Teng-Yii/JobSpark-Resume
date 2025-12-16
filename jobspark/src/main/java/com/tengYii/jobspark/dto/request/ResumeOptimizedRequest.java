package com.tengYii.jobspark.dto.request;

import java.io.Serializable;
import java.util.Objects;

import com.tengYii.jobspark.common.enums.DownloadFileTypeEnum;
import com.tengYii.jobspark.model.bo.CvBO;
import lombok.Getter;

/**
 * 简历优化请求DTO
 * 用于接收前端传递的优化后的简历内容及对应简历ID
 */
@Getter
public class ResumeOptimizedRequest implements Serializable {

    /**
     * 简历ID，唯一标识简历
     */
    private Long resumeId;

    /**
     * 下载文件类型
     * 指定简历导出的文件格式，支持PDF、HTML、DOCX三种格式
     *
     * @see com.tengYii.jobspark.common.enums.DownloadFileTypeEnum
     */
    private DownloadFileTypeEnum downloadFileType;

    /**
     * 优化后的简历业务对象
     */
    private CvBO cvBO;


    /**
     * 判断请求参数是否有效
     * 必须包含resumeId、cvBO非空，允许downloadFileType不为空
     *
     * @return 是否有效
     */
    public boolean isValid() {
        return Objects.nonNull(resumeId)
                && Objects.nonNull(cvBO);
    }
}