package com.tengYii.jobspark.domain.agent.cv;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tengYii.jobspark.infrastructure.context.OptimizationProgressContext;
import com.tengYii.jobspark.model.bo.cv.CvBO;
import com.tengYii.jobspark.model.llm.CvReview;
import dev.langchain4j.agentic.declarative.ChatMemoryProviderSupplier;
import dev.langchain4j.agentic.declarative.ExitCondition;
import dev.langchain4j.agentic.declarative.LoopAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.agentic.scope.AgenticScopeAccess;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.V;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历优化代理 - 智能循环优化系统
 * <p>
 * 这是一个高级的循环优化代理，通过协调CvReviewer和ScoredCvTailor两个子代理，
 * 实现简历的迭代优化过程。系统会持续审核、定制、再审核，直到简历达到理想的匹配度。
 *
 * @author tengYii
 * @version 1.0
 * @since 2025-12-17
 */
public interface CvOptimizationAgent extends AgenticScopeAccess {

    Logger log = LoggerFactory.getLogger(CvOptimizationAgent.class);

    /**
     * 智能简历优化主方法，它会协调多个子代理来完成简历的迭代优化过程。
     * 整个优化流程是自动化的，系统会持续改进简历直到达到预设的质量标准。
     *
     * @param cv                 待优化的原始简历对象，包含完整的个人信息、工作经历、项目经验等
     * @param jobDescription     目标职位描述，用于指导简历优化方向和匹配度评估
     * @param referenceTemplates 参考的优秀简历模板列表，用于指导简历优化的风格和结构
     * @return 经过迭代优化后的简历对象，具有更高的职位匹配度和竞争力
     */
    @LoopAgent(
            outputKey = "cv",
            maxIterations = 3,
            subAgents = {CvReviewer.class, ScoredCvTailor.class}
    )
    CvBO optimizeCv(@MemoryId String memoryId, @V("cv")CvBO cv,  @V("jobDescription")String jobDescription,  @V("referenceTemplates")List<String> referenceTemplates);

    /**
     * 判断简历优化是否达到退出条件
     *
     * @param agenticScope 代理作用域，用于读取简历审核结果
     * @return 是否达到退出条件
     */
    @ExitCondition(testExitAtLoopEnd = true)
    static boolean exitCondition(AgenticScope agenticScope) {
        try {
            // 从代理作用域中获取最新的审核结果
            CvReview review = (CvReview) agenticScope.readState("cvReview");

            // 尝试获取CvBO对象并设置建议
            Object cvObj = agenticScope.readState("cv");
            if (cvObj instanceof CvBO cv) {
                // 记录本次优化历史
                cv.addOptimizationRecord(review.getFeedback(), review.getScore());
                // 更新最新的建议
                cv.setAdvice(review.getFeedback());
            }

            // 输出当前评分，便于监控优化进度
            log.info("=== 简历优化进度检查 ===");
            log.info("当前评分: {}", review.getScore());
            log.info("目标评分: 0.8 (推荐面试级别)");

            // 判断是否达到退出条件
            boolean shouldExit = review.getScore() > 0.8;

            // 构建进度消息文本
            String progressMsg;
            String status;
            if (shouldExit) {
                progressMsg = String.format("✅ 简历质量达标，优化完成！最终评分: %.2f", review.getScore());
                status = "COMPLETED";
                log.info(progressMsg);
            } else {
                progressMsg = String.format("🔄 继续优化，当前评分: %.2f，目标评分: 0.8+，差距: %.2f",
                        review.getScore(), 0.8 - review.getScore());
                status = "PROCESSING";
                log.info(progressMsg);
            }

            // 构建 JSON 格式的进度消息
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("message", progressMsg);
            messageMap.put("score", review.getScore());
            messageMap.put("feedback", review.getFeedback());
            messageMap.put("status", status);

            // 序列化为 JSON 字符串并发送
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonMessage = objectMapper.writeValueAsString(messageMap);
            OptimizationProgressContext.emit(jsonMessage);

            return shouldExit;

        } catch (Exception e) {
            // 异常处理：记录错误但不中断优化流程
            log.error("退出条件检查异常: {}", e.getMessage());
            log.error("默认继续优化流程...");
            return false;
        }
    }

    @ChatMemoryProviderSupplier
    static ChatMemory chatMemory(Object memoryId) {
        return MessageWindowChatMemory.withMaxMessages(10);
    }

//    /**
//     * 优化简历信息并输出简历概览
//     *
//     * @param cvBO 需要优化的简历对象
//     * @return 优化后的简历对象，包含经过验证和评估的候选人信息
//     */
//    @Output
//    static Result<CvBO> outputOptimizedCv(@V("cv") CvBO cvBO) {
//        try {
//            // 输出优化完成的提示信息
//            System.out.println("=== 简历优化流程完成 ===");
//            return Result.<CvBO>builder()
//                    .content(cvBO)
//                    .build();
//        } catch (Exception e) {
//            throw new IllegalStateException("简历优化失败", e);
//        }
//    }
}
