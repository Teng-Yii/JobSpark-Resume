package com.tengYii.jobspark.model;

import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.rerank.TextReRank;
import com.alibaba.dashscope.rerank.TextReRankParam;
import com.alibaba.dashscope.rerank.TextReRankResult;
import com.alibaba.dashscope.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

/**
 * 测试dashscope中重排序模型调用
 */
@SpringBootTest
public class rerankModelTest {

    /**
     * 调用rerank模型对文档进行重排序
     * <p>
     * rerank模型会根据查询与文档的相关性进行打分排序
     * </p>
     *
     * @param query     查询文本（通常是用户的问题或职位描述）
     * @param documents 待排序的文档列表
     * @param topN      返回前N个最相关的文档
     * @return RankingResult 排序结果，包含相关性分数
     * @throws ApiException           API异常
     * @throws NoApiKeyException      无API Key异常
     * @throws InputRequiredException 输入参数异常
     */
    public static TextReRankResult callWithRerank(String query, List<String> documents, Integer topN)
            throws ApiException, NoApiKeyException, InputRequiredException {
        TextReRank textReRank = new TextReRank();
        TextReRankParam param = TextReRankParam.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .model("qwen3-vl-rerank")
                .query(query)
                .documents(documents)
                .topN(topN)
                // 返回原始文档
                .returnDocuments(true)
                .build();
        return textReRank.call(param);
    }

    @Test
    public void testRerankModel() {
        try {
            // 测试rerank模型调用
            System.out.println("\n=== 测试rerank模型调用 ===");
            String query = "需要招聘一名Java高级开发工程师，熟悉Spring Boot、MySQL，有分布式系统经验优先";
            List<String> documents = Arrays.asList(
                    "张三，5年Java开发经验，熟悉Spring Boot、MySQL，曾参与电商系统开发",
                    "李四，前端开发工程师，熟悉Vue、React，有3年工作经验",
                    "王五，Java高级工程师，精通Spring Cloud、分布式系统、微服务架构",
                    "赵六，测试工程师，熟悉自动化测试，有Selenium经验",
                    "孙七，Java开发工程师，熟悉MySQL、Redis，有金融项目经验"
            );
            TextReRankResult rerankResult = callWithRerank(query, documents, 3);
            System.out.println(JsonUtils.toJson(rerankResult));

        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            // 使用日志框架记录异常信息
            System.err.println("调用模型服务时发生错误: " + e.getMessage());
        }
    }
}
