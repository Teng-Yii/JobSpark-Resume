package com.tengYii.jobspark.domain.agent;

import com.tengYii.jobspark.model.bo.CvBO;
import com.tengYii.jobspark.model.llm.CvReview;
import dev.langchain4j.agentic.declarative.ExitCondition;
import dev.langchain4j.agentic.declarative.LoopAgent;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SubAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.V;

import java.util.Objects;

/**
 * 简历优化代理 - 智能循环优化系统
 * <p>
 * 这是一个高级的循环优化代理，通过协调CvReviewer和ScoredCvTailor两个子代理，
 * 实现简历的迭代优化过程。系统会持续审核、定制、再审核，直到简历达到理想的匹配度。
 * </p>
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li><strong>初始审核</strong>: CvReviewer对原始简历进行评分和反馈</li>
 *   <li><strong>智能定制</strong>: ScoredCvTailor基于反馈优化简历</li>
 *   <li><strong>循环验证</strong>: 重复审核-定制过程直到达到退出条件</li>
 *   <li><strong>结果输出</strong>: 返回最终优化的简历</li>
 * </ol>
 *
 * <h3>优化策略</h3>
 * <ul>
 *   <li><strong>渐进式改进</strong>: 每次迭代都基于前一次的反馈进行改进</li>
 *   <li><strong>质量控制</strong>: 通过评分阈值控制优化质量</li>
 *   <li><strong>防止过度优化</strong>: 限制最大迭代次数避免无限循环</li>
 *   <li><strong>智能退出</strong>: 当评分达到0.8以上时自动停止优化</li>
 * </ul>
 *
 * @author tengYii
 * @version 1.0
 * @since 2024-12-17
 */
public interface CvOptimizationAgent {

    /**
     * 智能简历优化主方法，它会协调多个子代理来完成简历的迭代优化过程。
     * 整个优化流程是自动化的，系统会持续改进简历直到达到预设的质量标准。
     *
     * @param cv             待优化的原始简历对象，包含完整的个人信息、工作经历、项目经验等
     * @param jobDescription 目标职位描述，用于指导简历优化方向和匹配度评估
     * @return 经过迭代优化后的简历对象，具有更高的职位匹配度和竞争力
     */
    @LoopAgent(
            outputName = "cv",
            maxIterations = 3,
            subAgents = {
                    @SubAgent(type = CvReviewer.class, outputName = "cvReview"),
                    @SubAgent(type = ScoredCvTailor.class, outputName = "cv")
            }
    )
    Result<CvBO> optimizeCv(CvBO cv, String jobDescription);

    /**
     * 判断简历优化是否达到退出条件
     *
     * @param agenticScope 代理作用域，用于读取简历审核结果
     * @return 是否达到退出条件
     */
    @ExitCondition(testExitAtLoopEnd = true)
    default boolean exitCondition(AgenticScope agenticScope) {
        try {
            // 从代理作用域中获取最新的审核结果
            CvReview review = (CvReview) agenticScope.readState("cvReview");

            // 防御性检查，确保审核结果不为空
            if (Objects.isNull(review)) {
                System.err.println("警告: 无法获取简历审核结果，继续优化...");
                return false;
            }

            // 输出当前评分，便于监控优化进度
            System.out.println("=== 简历优化进度检查 ===");
            System.out.println("当前评分: " + review.score);
            System.out.println("目标评分: 0.8 (推荐面试级别)");

            // 判断是否达到退出条件
            boolean shouldExit = review.score > 0.8;

            if (shouldExit) {
                System.out.println("✅ 简历质量达标，优化完成！");
                System.out.println("最终评分: " + review.score);
            } else {
                System.out.println("🔄 继续优化，目标评分: 0.8+");
                System.out.println("当前差距: " + String.format("%.2f", 0.8 - review.score));
            }

            return shouldExit;

        } catch (Exception e) {
            // 异常处理：记录错误但不中断优化流程
            System.err.println("退出条件检查异常: " + e.getMessage());
            System.err.println("默认继续优化流程...");
            return false;
        }
    }

    /**
     * 优化简历信息并输出简历概览
     *
     * @param cvBO 需要优化的简历对象
     * @return 优化后的简历对象，包含经过验证和评估的候选人信息
     */
    @Output
    default CvBO outputOptimizedCv(@V("cv") CvBO cvBO) {
        try {
            // 输出优化完成的提示信息
            System.out.println("=== 简历优化流程完成 ===");

            // 基础验证：检查简历对象是否为空
            if (cvBO == null) {
                throw new IllegalArgumentException("优化后的简历对象不能为空");
            }

            return cvBO;

        } catch (Exception e) {
            throw new IllegalStateException("简历优化失败", e);
        }
    }
}
