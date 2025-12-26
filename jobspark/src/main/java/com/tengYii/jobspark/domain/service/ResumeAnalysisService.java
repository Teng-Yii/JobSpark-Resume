package com.tengYii.jobspark.domain.service;

import com.tengYii.jobspark.domain.agent.CvAnalysisAgent;
import com.tengYii.jobspark.dto.response.ResumeOptimizedResponse;
import com.tengYii.jobspark.model.bo.CvBO;
import com.tengYii.jobspark.dto.request.ResumeUploadRequest;
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
     * @param request 包含简历文件的请求对象。
     * @return 解析后的简历信息对象。
     */
    public CvBO analyzeResume(ResumeUploadRequest request) {
        try {

            // 读取文件内容
            try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(request.getFile().getBytes()))) {

                // 创建PDFTextStripper实例
                PDFTextStripper pdfStripper = new PDFTextStripper();

                // 设置按页面位置排序（使文本按阅读顺序排列，不是按PDF内部顺序）
                pdfStripper.setSortByPosition(true);

                // 读取整个文档的文本内容
                String resumeText = pdfStripper.getText(document);

                // 调用简历解析agent将简历转换为结构化对象
                CvAnalysisAgent cvAnalysisAgent = AgenticServices.createAgenticSystem(CvAnalysisAgent.class, chatModel);
                Result<CvBO> cvBOResult = cvAnalysisAgent.reviewCv(resumeText);

                // 获取调用的元数据信息
                TokenUsage tokenUsage = cvBOResult.tokenUsage();

                log.info("简历解析完成: inputTokenCount:{}, outputTokenCount:{}, totalTokenCount:{}",
                        tokenUsage.inputTokenCount(), tokenUsage.outputTokenCount(), tokenUsage.totalTokenCount());
                return cvBOResult.content();

            }
        } catch (Exception e) {
            log.error("简历解析失败", e);
            throw new RuntimeException("简历解析失败: " + e.getMessage(), e);
        }
    }

    public ResumeOptimizedResponse getResumeAnalysis(CvBO cvBO) {

        ResumeOptimizedResponse optimizedResponse = new ResumeOptimizedResponse();
        // 获取简历分析结果
        // 这里可以返回解析后的结构化数据


        return optimizedResponse;
    }
}