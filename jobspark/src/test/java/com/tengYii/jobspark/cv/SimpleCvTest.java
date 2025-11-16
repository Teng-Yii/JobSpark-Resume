package com.tengYii.jobspark.cv;

import com.tengYii.jobspark.common.utils.llm.ChatModelProvider;
import com.tengYii.jobspark.model.bo.CvBO;
import dev.langchain4j.model.chat.ChatModel;

public class SimpleCvTest {

    public static void main(String[] args) {
        // 测试基本功能是否正常
        try {
            System.out.println("开始测试基本功能...");
            
            // 1. 测试Mock数据创建
            CvBO mockCv = CvBOMock.createMockCvBO();
            System.out.println("Mock CvBO创建成功: " + mockCv.getName());
            
            // 2. 测试ChatModel创建
            ChatModel chatModel = ChatModelProvider.createChatModel();
            System.out.println("ChatModel创建成功");

            
            System.out.println("所有基本功能测试通过！");
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}