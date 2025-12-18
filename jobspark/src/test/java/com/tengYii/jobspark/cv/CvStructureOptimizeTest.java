package com.tengYii.jobspark.cv;

import com.tengYii.jobspark.common.utils.StringLoader;
import com.tengYii.jobspark.common.utils.llm.ChatModelProvider;
import com.tengYii.jobspark.domain.agent.CvOptimizationAgent;
import com.tengYii.jobspark.domain.agent.CvReviewer;
import com.tengYii.jobspark.domain.agent.ScoredCvTailor;
import com.tengYii.jobspark.model.bo.CvBO;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.Result;

import java.io.IOException;

public class CvStructureOptimizeTest {

    public static void main(String[] args)  throws IOException {
        // 显式加载ScoredCvTailor类，确保类加载器能够找到它
        try {
            Class.forName("com.tengYii.jobspark.domain.agent.ScoredCvTailor");
            Class.forName("com.tengYii.jobspark.domain.agent.CvReviewer");
            System.out.println("代理类加载成功");
        } catch (ClassNotFoundException e) {
            System.err.println("代理类加载失败: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        String masterCv = StringLoader.loadFromResource("/documents/master_cv.txt");
        CvBO mockCvBO = CvBOMock.createMockCvBO();
        String jobDescription = StringLoader.loadFromResource("/documents/job_description_backend.txt");
        ChatModel chatModel = ChatModelProvider.createChatModel();
        
        try {
            System.out.println("开始创建代理系统...");
            System.out.println("CvBO对象: " + mockCvBO.getName());
            System.out.println("职位描述长度: " + jobDescription.length());
            
            CvOptimizationAgent cvOptimizationAgent = AgenticServices.createAgenticSystem(CvOptimizationAgent.class, chatModel);
            System.out.println("代理系统创建成功，开始优化简历...");

//            Result<CvBO> cvBOResult = cvOptimizationAgent.optimizeCv(mockCvBO, jobDescription);
            CvBO optimizeCv = cvOptimizationAgent.optimizeCv(mockCvBO, jobDescription);;
    //        CvBO optimizeCv = cvOptimizationAgent.optimizeCv(masterCv, jobDescription);
            System.out.println("简历优化完成:");
            System.out.println(optimizeCv);
        } catch (Exception e) {
            System.err.println("创建代理系统失败: " + e.getMessage());
            e.printStackTrace();
            
            // 提供更详细的调试信息
            System.err.println("=== 调试信息 ===");
            System.err.println("CvBO类: " + mockCvBO.getClass().getName());
            System.err.println("职位描述: " + (jobDescription != null ? jobDescription.substring(0, Math.min(100, jobDescription.length())) : "null"));
            
            // 检查API密钥
            String apiKey = System.getenv("DASHSCOPE_API_KEY");
            System.err.println("API密钥是否设置: " + (apiKey != null && !apiKey.trim().isEmpty()));
        }
    }
}
