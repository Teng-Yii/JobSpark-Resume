package com.tengYii.jobspark.workflow;

import com.tengYii.jobspark.model.CvReview;
import com.tengYii.jobspark.utils.StringLoader;
import com.tengYii.jobspark.utils.log.CustomLogging;
import com.tengYii.jobspark.utils.log.LogLevels;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tengYii.jobspark.utils.ChatModelProvider.createChatModel;

public class _3b_Loop_Agent_Example_States_And_Fail {

    static {
        CustomLogging.setLevel(LogLevels.PRETTY, 300);  // 控制模型调用中显示的内容量
    }

    /**
     * 在此我们构建与3a相同的循环代理，但这次应会观察到其失败：
     * 因尝试将简历强行匹配至不符的职位描述。
     * 除最终简历外，我们还将返回最新评分与反馈，
     * 从而可验证是否获得良好评分，以及该简历是否值得提交。
     * 我们还展示了一个检查评审中间状态的技巧（这些状态会在每次循环中被覆盖）
     * 方法是在每次检查退出条件时（即每次调用代理后）将状态存储到列表中。
     */

    private static final ChatModel CHAT_MODEL = createChatModel();

    public static void main(String[] args) throws IOException {

        // 1. 创建所有子代理（与之前相同）
        CvReviewer cvReviewer = AgenticServices.agentBuilder(CvReviewer.class)
                .chatModel(CHAT_MODEL)
                .outputName("cvReview") // 每次迭代都会根据新反馈进行更新，为下一次调整提供依据。
                .build();
        ScoredCvTailor scoredCvTailor = AgenticServices.agentBuilder(ScoredCvTailor.class)
                .chatModel(CHAT_MODEL)
                .outputName("cv") // 这将在每次迭代中更新，持续改进简历。
                .build();

        // 2. 构建序列并在每次退出条件检查时保存评估结果
        // 了解退出条件是否满足，还是仅达到最大迭代次数，这一点可能很重要。
        // (例如，约翰可能根本不想费心申请这份工作。).
        // 你可以修改输出变量，使其同时包含最后的得分和反馈，并在循环结束后自行检查结果。
        // 您也可以将中间值存储在可变列表中以便后续检查。
        // 下面的代码同时实现了这两件事。
        List<CvReview> reviewHistory = new ArrayList<>();

        UntypedAgent reviewedCvGenerator = AgenticServices // 除非你定义生成的组合代理，否则请使用无类型代理，详见下文。
                .loopBuilder().subAgents(cvReviewer, scoredCvTailor) // 数量不限，顺序重要
                .outputName("cvAndReview") // 这是我们想要观察的最终输出结果
                .output(agenticScope -> {
                    Map<String, Object> cvAndReview = Map.of(
                            "cv", agenticScope.readState("cv"),
                            "finalReview", agenticScope.readState("cvReview")
                    );
                    return cvAndReview;
                })
                .exitCondition(scope -> {
                    CvReview review = (CvReview) scope.readState("cvReview");
                    reviewHistory.add(review); // 在每次代理调用时捕获 score+feedback
                    System.out.println("Exit check with score=" + review.score);    // 会输出6次，因为退出条件是在每次代理调用后进行检查，而不仅仅是在整个循环结束后检查。
                    return review.score >= 0.8;
                })
                .maxIterations(3) // 为避免退出条件永远无法满足导致的无限循环，确保安全
                .build();

        // 3.  从 resources/documents/ 文本文件中加载原始参数
        // - master_cv.txt
        // - job_description_backend.txt
        String masterCv = StringLoader.loadFromResource("/documents/master_cv.txt");
        String fluteJobDescription = "We are looking for a passionate flute teacher to join our music academy.";

        // 4. 由于我们使用的是无类型代理，因此需要传递一个参数映射表。
        Map<String, Object> arguments = Map.of(
                "cv", masterCv, // 从主简历开始，它将持续改进
                "jobDescription", fluteJobDescription
        );

        // 5. 调用组合式代理生成定制简历
        Map<String, Object> cvAndReview = (Map<String, Object>) reviewedCvGenerator.invoke(arguments);

        System.out.println("=== REVIEWED CV FOR FLUTE TEACHER ===");
        System.out.println(cvAndReview.get("cv")); // 循环后的最终简历

        // 现在你可以在输出映射中获取最终审核结果，以便进行检查。
        // 如果最终评分和反馈符合您的要求
        CvReview review = (CvReview) cvAndReview.get("finalReview");
        System.out.println("=== FLUTE TEACHER 的最终反馈 ===");
        System.out.println("CV" + (review.score >= 0.8 ? " 通过" : " 不通过") + " 得分=" + review.score);
        System.out.println("最终反馈: " + review.feedback);

        // 在评论历史记录中，您可查阅所有评论的完整历史记录。
        System.out.println("=== FLUTE TEACHER 完整的评估历史 ===");
        System.out.println(reviewHistory);

    }
}
