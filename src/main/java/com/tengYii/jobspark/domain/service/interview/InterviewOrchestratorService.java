package com.tengYii.jobspark.domain.service.interview;

import com.tengYii.jobspark.domain.agent.interview.*;
import com.tengYii.jobspark.common.enums.InterviewDecisionEnum;
import com.tengYii.jobspark.dto.request.InterviewSimulationRequest;
import com.tengYii.jobspark.model.bo.interview.JavaInterviewResultBO;
import com.tengYii.jobspark.model.bo.interview.ReflectionResultBO;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.Skills;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * 面试编排服务类，负责协调多代理系统完成完整的面试流程。
 * <p>
 * 面试流程采用三层架构设计：
 * <ul>
 *   <li><b>顺序流</b>：JD对齐 → 计划制定 → 问题执行 → 结果反思 → 条件路由</li>
 *   <li><b>条件流</b>：根据反思结果选择后续动作：
 *     <ul>
 *       <li>PROBE - 追问（进入循环流）</li>
 *       <li>NEXT - 下一题</li>
 *       <li>STAGE_FINISH - 下一阶段</li>
 *       <li>FINISH - 结束面试</li>
 *     </ul>
 *   </li>
 *   <li><b>循环流</b>：追问、多轮问答、多阶段面试的循环处理</li>
 * </ul>
 * <p>
 * 核心组件：
 * <ul>
 *   <li>{@link JDAlignmentAgent} - JD与简历技能对齐分析</li>
 *   <li>{@link InterviewCoordinatorAgent} - 面试计划制定</li>
 *   <li>{@link JavaTechInterviewerAgent} - 技术问题执行</li>
 *   <li>{@link InterviewReflectorAgent} - 面试结果反思评估</li>
 * </ul>
 *
 * @see JavaInterviewPlanAndExecuteWorkflow
 * @see InterviewCoordinatorAgent
 * @see JavaTechInterviewerAgent
 * @see InterviewReflectorAgent
 */
@Service
public class InterviewOrchestratorService {

    /**
     * LangChain4j聊天模型，用于AI代理的语言处理
     */
    @Resource(name = "chatModel")
    private ChatModel chatModel;

    /**
     * 简历分析Skill，加载文件系统中的简历分析技能
     */
    Skills resumeAnalysisSkill = Skills.from(FileSystemSkillLoader.loadSkill(Path.of("skills/resume-analysis")));

    /**
     * 问题探查Skill，加载文件系统中的问题探查技能，用于追问场景
     */
    Skills questionProbingSkill = Skills.from(FileSystemSkillLoader.loadSkill(Path.of("skills/question-probing")));

    /**
     * JD对齐Skill，加载文件系统中的JD对齐技能，用于职位匹配分析
     */
    Skills jdAlignmentSkill = Skills.from(FileSystemSkillLoader.loadSkill(Path.of("skills/jd-alignment")));

    /**
     * JD对齐Agent，负责分析职位描述与候选人简历的技能匹配度
     */
    private JDAlignmentAgent jdAlignAgent;

    /**
     * 计划制定Agent，负责根据JD和简历制定面试计划
     */
    private InterviewCoordinatorAgent planner;

    /**
     * 问题执行Agent，负责根据计划执行技术面试提问
     */
    private JavaTechInterviewerAgent executor;

    /**
     * 反思评估Agent，负责评估候选人的回答并决定后续流程
     */
    private InterviewReflectorAgent reflector;

    /**
     * 追问循环流Agent，执行追问、多轮问答的循环处理
     * <p>
     * 最大迭代次数为3次，当反思结果不是PROBE时退出循环
     */
    private UntypedAgent probeLoop;

    /**
     * 条件路由Agent，根据反思结果决定后续动作
     * <ul>
     *   <li>PROBE - 执行追问循环</li>
     *   <li>NEXT - 执行下一题</li>
     *   <li>STAGE_FINISH - 进入下一阶段</li>
     *   <li>FINISH - 结束面试</li>
     * </ul>
     */
    private UntypedAgent decisionRouter;

    /**
     * 多轮问答循环Agent，支持完整的面试流程
     * <p>
     * 最大迭代次数为30次，当反思结果为FINISH时退出循环
     */
    private UntypedAgent interviewLoop;

    /**
     * 主工作流，串联所有Agent完成完整的面试流程
     * <p>
     * 执行顺序：JD对齐 → 制定计划 → 多轮问答循环
     */
    private JavaInterviewPlanAndExecuteWorkflow workflow;

    /**
     * 初始化所有Agent组件
     * 在Spring容器完成依赖注入后调用，确保ChatModel已正确注入
     */
    @PostConstruct
    public void initAgents() {
        jdAlignAgent = AgenticServices
                .agentBuilder(JDAlignmentAgent.class)
                .chatModel(chatModel)
                .toolProvider(jdAlignmentSkill.toolProvider())
                .outputKey("jdAlignResult")
                .build();

        planner = AgenticServices
                .agentBuilder(InterviewCoordinatorAgent.class)
                .chatModel(chatModel)
                .toolProvider(resumeAnalysisSkill.toolProvider())
                .outputKey("interviewPlan")
                .build();

        executor = AgenticServices
                .agentBuilder(JavaTechInterviewerAgent.class)
                .chatModel(chatModel)
                .toolProvider(resumeAnalysisSkill.toolProvider())
                .toolProvider(questionProbingSkill.toolProvider())
                .outputKey("currentQuestion")
                .build();

        reflector = AgenticServices
                .agentBuilder(InterviewReflectorAgent.class)
                .chatModel(chatModel)
                .toolProvider(resumeAnalysisSkill.toolProvider())
                .outputKey("reflection")
                .build();

        probeLoop = AgenticServices
                .loopBuilder()
                .subAgents(executor, reflector)
                .maxIterations(3)
                .exitCondition((scope, count) -> {
                    ReflectionResultBO res = scope.readState("reflection", null);
                    return res.getDecision() != InterviewDecisionEnum.PROBE;
                })
                .build();

        decisionRouter = AgenticServices
                .conditionalBuilder()
                .subAgents(scope -> {
                    ReflectionResultBO r = scope.readState("reflection", null);
                    return r != null && InterviewDecisionEnum.PROBE.equals(r.getDecision());
                }, probeLoop)
                .subAgents(scope -> {
                    ReflectionResultBO r = scope.readState("reflection", null);
                    return r != null && InterviewDecisionEnum.NEXT.equals(r.getDecision());
                }, executor)
                .subAgents(scope -> {
                    ReflectionResultBO r = scope.readState("reflection", null);
                    return r != null && InterviewDecisionEnum.STAGE_FINISH.equals(r.getDecision());
                }, executor)
                .build();

        interviewLoop = AgenticServices
                .loopBuilder()
                .subAgents(executor, reflector, decisionRouter)
                .maxIterations(30)
                .exitCondition((scope, count) -> {
                    ReflectionResultBO r = scope.readState("reflection", null);
                    return r != null && InterviewDecisionEnum.FINISH.equals(r.getDecision());
                })
                .build();

        workflow = AgenticServices
                .sequenceBuilder(JavaInterviewPlanAndExecuteWorkflow.class)
                .subAgents(
                        jdAlignAgent,
                        planner,
                        interviewLoop
                )
                .outputKey("interviewResult")
                .build();
    }

    /**
     * 启动面试流程的对外接口方法
     *
     * @param request 模拟面试请求对象
     * @return 包含当前阶段信息和可恢复上下文的面试结果
     */
    public JavaInterviewResultBO startInterview(InterviewSimulationRequest request) {

        long userId = request.getUserId();
        Long resumeId = Long.valueOf(request.getResumeId());
        String jobDescription = request.getJobDescription();
        String userAnswer = request.getUserAnswer();
        return workflow.startInterview(userId, resumeId, jobDescription, userAnswer);
    }

}
