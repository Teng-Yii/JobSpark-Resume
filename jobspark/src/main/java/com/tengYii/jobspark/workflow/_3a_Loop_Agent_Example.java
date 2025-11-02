package com.tengYii.jobspark.workflow;

import com.tengYii.jobspark.model.CvReview;
import com.tengYii.jobspark.utils.StringLoader;
import com.tengYii.jobspark.utils.log.CustomLogging;
import com.tengYii.jobspark.utils.log.LogLevels;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;


import java.io.IOException;
import java.util.Map;

import static com.tengYii.jobspark.utils.ChatModelProvider.createChatModel;

public class _3a_Loop_Agent_Example {

    static {
        CustomLogging.setLevel(LogLevels.PRETTY, 300);  // 控制模型调用中显示的内容量
    }

    /**
     * 此示例演示如何实现一个CvReviewer代理，该代理可添加至循环中，与我们的CvTailor代理协同工作。我们将实现两个代理：
     * - ScoredCvTailor（接收简历并将其定制为CvReview格式（反馈/指导+评分））
     * - CvReviewer（接收定制后的简历和职位描述，返回CvReview对象（反馈+评分））
     * 此外，当评分超过特定阈值（例如0.7）时循环终止（退出条件）
     */

    // 1. 定义将驱动智能体的模型
    private static final ChatModel CHAT_MODEL = createChatModel();

    public static void main(String[] args) throws IOException {

        // 2. 在此包中定义两个子代理：
        //      - CvReviewer.java
        //      - CvTailor.java

        // 3. 使用代理服务创建所有代理
        CvReviewer cvReviewer = AgenticServices.agentBuilder(CvReviewer.class)
                .chatModel(CHAT_MODEL)
                .outputName("cvReview") // 每次迭代都会根据新反馈进行更新，为后续调整提供依据。
                .build();
        ScoredCvTailor scoredCvTailor = AgenticServices.agentBuilder(ScoredCvTailor.class)
                .chatModel(CHAT_MODEL)
                .outputName("cv") // 这将在每次迭代中更新，持续改进简历。
                .build();

        // 4. 构建序列
        UntypedAgent reviewedCvGenerator = AgenticServices // 除非你定义最终组合的代理，否则请使用无类型代理，详见_2_顺序代理示例
                .loopBuilder().subAgents(cvReviewer, scoredCvTailor) // 数量不限，顺序重要
                .outputName("cv") // 这是我们想要观察的最终输出（改进后的交叉验证结果）
                .exitCondition(agenticScope -> {
                    CvReview review = (CvReview) agenticScope.readState("cvReview");
                    System.out.println("检查退出条件与得分=" + review.score); // 我们记录中间分数
                    return review.score > 0.8;
                }) // 根据CvReviewer代理给出的评分确定退出条件，当评分>0.8时视为满意
                // 请注意，退出条件是在每次代理调用后进行检查，而不仅仅是在整个循环结束后检查。
                .maxIterations(3) // 为避免退出条件永远无法满足导致的无限循环，确保安全
                .build();

        // 5. 从 resources/documents/ 文本文件中加载原始参数
        // - master_cv.txt
        // - job_description_backend.txt
        String masterCv = StringLoader.loadFromResource("/documents/master_cv.txt");
        String jobDescription = StringLoader.loadFromResource("/documents/job_description_backend.txt");

        // 5. 由于我们使用的是无类型代理，因此需要传递一个参数映射表。
        Map<String, Object> arguments = Map.of(
                "cv", masterCv, // 从主简历开始，它将持续改进
                "jobDescription", jobDescription
        );

        // 5. 调用组合式代理生成定制简历
        String tailoredCv = (String) reviewedCvGenerator.invoke(arguments);

        // 6. 并打印生成的简历
        System.out.println("=== 已审核简历（UNTYPED） ===");
        System.out.println((String) tailoredCv);

        // 这份简历经过初步定制和审核后应该能通过。
        // 若想目睹其失败，不妨试试用/documents/job_description_fullstack.txt
        // 如例3b所示，其中我们还检查了CV的中间状态。
        // 并检索最终评审结果及评分。

    }
}
