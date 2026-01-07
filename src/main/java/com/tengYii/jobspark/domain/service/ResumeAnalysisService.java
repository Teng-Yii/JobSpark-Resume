package com.tengYii.jobspark.domain.service;

import com.tengYii.jobspark.common.constants.FileStoreConstants;
import com.tengYii.jobspark.common.enums.ResultCodeEnum;
import com.tengYii.jobspark.common.exception.BusinessException;
import com.tengYii.jobspark.domain.agent.CvAnalysisAgent;
import com.tengYii.jobspark.model.bo.CvBO;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAnalysisService {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ChatModel chatModel;


    /**
     * 分析上传的简历并将其转换为结构化对象。
     *
     * @param fileName 简历文件名称，用于从oss中获取简历
     * @return 解析后的简历信息对象。
     */
    public CvBO analyzeResumeFile(String fileName) {

        // 读取文件内容
        try (InputStream inputStream = fileStorageService.downloadFileByBucketAndName(FileStoreConstants.BUCKET_NAME, fileName);
             PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(inputStream))) {

            // 创建PDFTextStripper实例
            PDFTextStripper pdfStripper = new PDFTextStripper();

            // 设置按页面位置排序（使文本按阅读顺序排列，不是按PDF内部顺序）
            pdfStripper.setSortByPosition(true);

            // 读取整个文档的文本内容
            String resumeText = pdfStripper.getText(document);
            log.info("[简历文本内容转换结构化对象开始] 简历文本内容: {}", resumeText);

            // 调用简历解析agent将简历转换为结构化对象（耗时操作）
            long currentTimeMillis = System.currentTimeMillis();
            CvAnalysisAgent cvAnalysisAgent = AgenticServices.createAgenticSystem(CvAnalysisAgent.class, chatModel);
            Result<CvBO> cvBOResult = cvAnalysisAgent.reviewCv(resumeText);
            log.info("解析简历内容耗时:{} ms", System.currentTimeMillis() - currentTimeMillis);

            // 获取调用的元数据信息
            TokenUsage tokenUsage = cvBOResult.tokenUsage();

            log.info("简历解析完成: inputTokenCount:{}, outputTokenCount:{}, totalTokenCount:{}",
                    tokenUsage.inputTokenCount(), tokenUsage.outputTokenCount(), tokenUsage.totalTokenCount());
            return cvBOResult.content();
        } catch (UndeclaredThrowableException e) {
            Throwable undeclaredThrowable = e.getUndeclaredThrowable();
            log.error("简历解析失败 (UndeclaredThrowableException): 真实异常类型: {}, 异常信息: {}",
                    undeclaredThrowable.getClass().getName(), undeclaredThrowable.getMessage(), undeclaredThrowable);
            throw new BusinessException(ResultCodeEnum.FILE_PARSE_ERROR, "简历解析失败(底层异常): " + undeclaredThrowable.getMessage(), e);
        } catch (Exception e) {
            log.error("简历解析失败", e);
            throw new BusinessException(ResultCodeEnum.FILE_PARSE_ERROR, "简历解析失败: " + e.getMessage(), e);
        }
    }
}