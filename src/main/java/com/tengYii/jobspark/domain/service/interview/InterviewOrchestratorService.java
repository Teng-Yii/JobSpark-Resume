package com.tengYii.jobspark.domain.service.interview;

import com.tengYii.jobspark.domain.agent.interview.*;
import com.tengYii.jobspark.common.enums.InterviewDecisionEnum;
import com.tengYii.jobspark.dto.request.InterviewSimulationRequest;
import com.tengYii.jobspark.dto.response.ResumeDetailResponse;
import com.tengYii.jobspark.infrastructure.store.RedisChatMemoryStore;
import com.tengYii.jobspark.model.bo.interview.*;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.Skill;
import dev.langchain4j.skills.Skills;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.UUID;

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
     * Redis会话记忆持久化存储
     */
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

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
     * 追问循环Agent
     * <p>
     * 最大迭代次数为3次，当反思结果不是PROBE时退出循环
     * 注意：这是内部循环，由条件路由触发，不是主循环
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
     * 初始化所有Agent组件
     * 在Spring容器完成依赖注入后调用，确保ChatModel已正确注入
     */
    @PostConstruct
    public void initAgents() {

        ChatMemoryProvider redisChatMemoryProvider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(50)
                .chatMemoryStore(redisChatMemoryStore)
                .build();

        Skills combinedSkills = Skills.from(
                FileSystemSkillLoader.loadSkill(Path.of("skills/resume-analysis")),
                FileSystemSkillLoader.loadSkill(Path.of("skills/question-probing"))
        );

        jdAlignAgent = AgenticServices
                .agentBuilder(JDAlignmentAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(redisChatMemoryProvider)
                .toolProvider(jdAlignmentSkill.toolProvider())
                // 系统消息：告知LLM可用技能，要求先激活技能再执行
                .systemMessage("""
                        你拥有以下skills权限：
                        %s
                        当用户请求涉及上述skills时，必须先通过activate_skill工具激活skill，再执行操作。
                        """.formatted(jdAlignmentSkill.formatAvailableSkills()))

//                .outputKey("jdAlignResult")
                .build();

        planner = AgenticServices
                .agentBuilder(InterviewCoordinatorAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(redisChatMemoryProvider)
                .toolProvider(resumeAnalysisSkill.toolProvider())
                // 系统消息：告知LLM可用技能，要求先激活技能再执行
                .systemMessage("""
                        你拥有以下skills权限：
                        %s
                        当用户请求涉及上述skills时，必须先通过activate_skill工具激活skill，再执行操作。
                        """.formatted(resumeAnalysisSkill.formatAvailableSkills()))
                .outputKey("interviewPlan")
                .build();

        executor = AgenticServices
                .agentBuilder(JavaTechInterviewerAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(redisChatMemoryProvider)
                .toolProvider(combinedSkills.toolProvider())
                // 系统消息：告知LLM可用技能，要求先激活技能再执行
                .systemMessage("""
                        你拥有以下skills权限：
                        %s
                        当用户请求涉及上述skills时，必须先通过activate_skill工具激活skill，再执行操作。
                        """.formatted(resumeAnalysisSkill.formatAvailableSkills() + "\n" + questionProbingSkill.formatAvailableSkills()))
                .outputKey("currentQuestion")
                .build();

        reflector = AgenticServices
                .agentBuilder(InterviewReflectorAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(redisChatMemoryProvider)
                .toolProvider(resumeAnalysisSkill.toolProvider())
                // 系统消息：告知LLM可用技能，要求先激活技能再执行
                .systemMessage("""
                        你拥有以下skills权限：
                        %s
                        当用户请求涉及上述skills时，必须先通过activate_skill工具激活skill，再执行操作。
                        """.formatted(resumeAnalysisSkill.formatAvailableSkills()))
                .outputKey("reflection")
                .build();

        probeLoop = AgenticServices
                .loopBuilder()
                .subAgents(executor, reflector)
                .maxIterations(3)
                .exitCondition((scope, count) -> {
                    ReflectionResultBO res = scope.readState("reflection", null);
                    return res == null || !InterviewDecisionEnum.PROBE.equals(res.getDecision());
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
    }

    /**
     * 启动面试流程（首次交互）
     * 执行JD对齐和计划制定，返回首次问题和会话信息
     *
     * @param request 模拟面试请求对象
     * @param resumeDetail 简历详情
     * @return 首次会话响应
     */
    public InterviewResponseBO startInterview(InterviewSimulationRequest request, ResumeDetailResponse resumeDetail) {
        Long sessionId = generateSessionId();
        Long userId = request.getUserId();
        Long resumeId = Long.valueOf(request.getResumeId());

        InterviewSessionContext context = InterviewSessionContext.getOrCreate(
                sessionId, userId, resumeId, request.getJobDescription());

        String memoryId = buildMemoryId(userId, resumeId);

        // 调用JD对齐Agent
        JDAlignmentResultBO jdResult = jdAlignAgent.align(memoryId, request.getJobDescription(), resumeDetail);
        context.setJdAlignmentResult(jdResult);

        // 调用计划制定Agent
        InterviewPlanBO plan = planner.generateInterviewPlan(memoryId, resumeDetail, jdResult);
        context.setInterviewPlan(plan);

        // 调用问题生成Agent
        String firstQuestion = executor.generateQuestion(
                memoryId,
                plan,
                context.getCurrentStageIndex(),
                context.getCurrentQuestionIndex(),
                false
        );
        context.setCurrentQuestion(firstQuestion);

        return buildSessionResponse(sessionId, context, firstQuestion);
    }

    /**
     * 继续面试流程（后续交互）
     * 每轮用户回答后执行：反思 → 决策 → 生成下一问题
     *
     * @param sessionId 会话ID
     * @param userAnswer 用户回答
     * @return 进行中或结束响应
     */
    public InterviewResponseBO continueInterview(Long sessionId, String userAnswer) {
        InterviewSessionContext context = InterviewSessionContext.get(sessionId);
        if (context == null) {
            return InterviewResponseBO.error(sessionId, "会话不存在或已过期");
        }

        if (context.isFinished()) {
            return InterviewResponseBO.error(sessionId, "面试已结束");
        }

        String memoryId = buildMemoryId(context.getUserId(), context.getResumeId());
        context.setLastUserAnswer(userAnswer);

        // 调用反思评估Agent
        ReflectionResultBO reflection = reflector.reflect(
                memoryId,
                context.getCurrentQuestion(),
                userAnswer,
                ""
        );
        context.setCurrentDecision(reflection.getDecision());

        addQARecord(context, reflection);

        InterviewDecisionEnum decision = reflection.getDecision();

        if (InterviewDecisionEnum.FINISH.equals(decision)) {
            context.finish();
            return buildCompleteResponse(sessionId, context);
        }

        // 判断是否为追问模式
        boolean isProbe = InterviewDecisionEnum.PROBE.equals(decision) && context.canProbe();
        if (isProbe) {
            context.incrementProbeCount();
        } else if (InterviewDecisionEnum.NEXT.equals(decision)) {
            context.moveToNextQuestion();
        } else if (InterviewDecisionEnum.STAGE_FINISH.equals(decision)) {
            context.moveToNextStage();
        } else {
            context.moveToNextQuestion();
        }

        // 调用问题生成Agent
        String nextQuestion = executor.generateQuestion(
                memoryId,
                context.getInterviewPlan(),
                context.getCurrentStageIndex(),
                context.getCurrentQuestionIndex(),
                isProbe
        );
        context.setCurrentQuestion(nextQuestion);

        return buildProgressResponse(sessionId, context, reflection);
    }

    /**
     * 获取会话状态
     */
    public InterviewResponseBO getSessionStatus(Long sessionId) {
        InterviewSessionContext context = InterviewSessionContext.get(sessionId);
        if (context == null) {
            return InterviewResponseBO.error(sessionId, "会话不存在或已过期");
        }

        if (context.isFinished()) {
            return buildCompleteResponse(sessionId, context);
        }

        return buildProgressResponse(sessionId, context, null);
    }

    /**
     * 结束面试会话
     */
    public InterviewResponseBO finishInterview(Long sessionId) {
        InterviewSessionContext context = InterviewSessionContext.get(sessionId);
        if (context == null) {
            return InterviewResponseBO.error(sessionId, "会话不存在或已过期");
        }

        context.finish();
        return buildCompleteResponse(sessionId, context);
    }

    private Long generateSessionId() {
        return Math.abs(UUID.randomUUID().getMostSignificantBits());
    }

    private String buildMemoryId(Long userId, Long resumeId) {
        return userId + "_" + resumeId;
    }

    private InterviewResponseBO buildSessionResponse(Long sessionId, InterviewSessionContext context, String firstQuestion) {
        InterviewSessionBO sessionBO = new InterviewSessionBO();
        sessionBO.setSessionId(sessionId);
        sessionBO.setResumeId(context.getResumeId());
        sessionBO.setUserId(context.getUserId());
        sessionBO.setJobDescription(context.getJobDescription());
        sessionBO.setJdAlignmentResult(context.getJdAlignmentResult());
        sessionBO.setInterviewPlan(context.getInterviewPlan());
        sessionBO.setCurrentStageIndex(context.getCurrentStageIndex());
        sessionBO.setCurrentStageName(getStageName(context));
        sessionBO.setCurrentQuestionIndex(context.getCurrentQuestionIndex());
        sessionBO.setCurrentQuestion(firstQuestion);
        sessionBO.setTotalPlannedQuestions(calculateTotalQuestions(context.getInterviewPlan()));
        sessionBO.setStageInfos(buildStageInfos(context));

        return InterviewResponseBO.session(sessionId, sessionBO);
    }

    private InterviewResponseBO buildProgressResponse(Long sessionId, InterviewSessionContext context, ReflectionResultBO lastReflection) {
        InterviewProgressBO progressBO = new InterviewProgressBO();
        progressBO.setSessionId(sessionId);
        progressBO.setCurrentQuestionIndex(context.getCurrentQuestionIndex());
        progressBO.setCurrentStageIndex(context.getCurrentStageIndex());
        progressBO.setCurrentStageName(getStageName(context));
        progressBO.setCurrentQuestion(context.getCurrentQuestion());
        progressBO.setLastUserAnswer(context.getLastUserAnswer());
        progressBO.setLastReflection(lastReflection);
        progressBO.setCurrentDecision(context.getCurrentDecision());
        progressBO.setProgress(buildProgressInfo(context));

        return InterviewResponseBO.progress(sessionId, progressBO);
    }

    private InterviewResponseBO buildCompleteResponse(Long sessionId, InterviewSessionContext context) {
        InterviewCompleteBO completeBO = new InterviewCompleteBO();
        completeBO.setSessionId(sessionId);
        completeBO.setFinished(true);
        completeBO.setQaHistory(convertQARecords(context.getQaHistory()));
        completeBO.setStageResults(buildStageResults(context));
        completeBO.setStatistics(buildStatistics(context));
        completeBO.setTotalScore(calculateTotalScore(context.getQaHistory()));
        completeBO.setFinalFeedback(generateFinalFeedback(context));

        return InterviewResponseBO.complete(sessionId, completeBO);
    }

    private String getStageName(InterviewSessionContext context) {
        if (context.getInterviewPlan() == null) {
            return "未知阶段";
        }
        var stages = context.getInterviewPlan().getStages();
        if (stages == null || stages.isEmpty()) {
            return "未知阶段";
        }
        int index = Math.min(context.getCurrentStageIndex(), stages.size() - 1);
        return stages.get(index).getName();
    }

    private int calculateTotalQuestions(InterviewPlanBO plan) {
        if (plan == null || plan.getStages() == null) {
            return 0;
        }
        return plan.getStages().stream()
                .mapToInt(s -> s.getKeyTopics() != null ? s.getKeyTopics().size() : 0)
                .sum();
    }

    private java.util.List<InterviewSessionBO.StageInfo> buildStageInfos(InterviewSessionContext context) {
        if (context.getInterviewPlan() == null || context.getInterviewPlan().getStages() == null) {
            return java.util.Collections.emptyList();
        }

        return context.getInterviewPlan().getStages().stream()
                .map(stage -> {
                    InterviewSessionBO.StageInfo info = new InterviewSessionBO.StageInfo();
                    info.setStageIndex(stage.getOrder() - 1);
                    info.setStageName(stage.getName());
                    info.setPlannedQuestionCount(stage.getKeyTopics() != null ? stage.getKeyTopics().size() : 0);
                    info.setFinished(context.getFinishedStages().contains(stage.getOrder() - 1));
                    return info;
                })
                .toList();
    }

    private InterviewProgressBO.ProgressInfo buildProgressInfo(InterviewSessionContext context) {
        InterviewProgressBO.ProgressInfo info = new InterviewProgressBO.ProgressInfo();
        info.setCompletedQuestions(context.getQaHistory().size());
        info.setTotalQuestions(calculateTotalQuestions(context.getInterviewPlan()));
        info.setCurrentStageCompleted(context.getCurrentQuestionIndex());

        if (context.getInterviewPlan() != null && context.getInterviewPlan().getStages() != null) {
            int stageIdx = Math.min(context.getCurrentStageIndex(),
                    context.getInterviewPlan().getStages().size() - 1);
            if (stageIdx >= 0) {
                var stage = context.getInterviewPlan().getStages().get(stageIdx);
                info.setCurrentStageTotal(stage.getKeyTopics() != null ? stage.getKeyTopics().size() : 0);
            }
        }

        info.setCompletedStages(context.getFinishedStages().size());
        info.setTotalStages(context.getInterviewPlan() != null ?
                context.getInterviewPlan().getStages().size() : 0);
        return info;
    }

    private void addQARecord(InterviewSessionContext context, ReflectionResultBO reflection) {
        InterviewSessionContext.QARecord record = new InterviewSessionContext.QARecord();
        record.setStageIndex(context.getCurrentStageIndex());
        record.setStageName(getStageName(context));
        record.setQuestion(context.getCurrentQuestion());
        record.setAnswer(context.getLastUserAnswer());
        record.setScore(reflection.getScore());
        record.setDecision(reflection.getDecision());
        record.setFeedback(reflection.getFeedback());
        record.setProbe(context.getProbeCount() > 0);
        context.getQaHistory().add(record);
    }

    private java.util.List<InterviewCompleteBO.QARecord> convertQARecords(
            java.util.List<InterviewSessionContext.QARecord> records) {
        return records.stream()
                .map(r -> {
                    InterviewCompleteBO.QARecord record = new InterviewCompleteBO.QARecord();
                    record.setStageIndex(r.getStageIndex());
                    record.setStageName(r.getStageName());
                    record.setQuestion(r.getQuestion());
                    record.setAnswer(r.getAnswer());
                    record.setScore(r.getScore());
                    record.setDecision(r.getDecision());
                    record.setFeedback(r.getFeedback());
                    record.setProbe(r.isProbe());
                    return record;
                })
                .toList();
    }

    private java.util.List<InterviewCompleteBO.StageResult> buildStageResults(InterviewSessionContext context) {
        if (context.getInterviewPlan() == null || context.getInterviewPlan().getStages() == null) {
            return java.util.Collections.emptyList();
        }

        return context.getInterviewPlan().getStages().stream()
                .map(stage -> {
                    InterviewCompleteBO.StageResult result = new InterviewCompleteBO.StageResult();
                    result.setStageIndex(stage.getOrder() - 1);
                    result.setStageName(stage.getName());
                    result.setQuestionCount(stage.getKeyTopics() != null ? stage.getKeyTopics().size() : 0);
                    return result;
                })
                .toList();
    }

    private InterviewCompleteBO.Statistics buildStatistics(InterviewSessionContext context) {
        InterviewCompleteBO.Statistics stats = new InterviewCompleteBO.Statistics();
        stats.setTotalQuestions(context.getQaHistory().size());
        stats.setTotalProbes((int) context.getQaHistory().stream()
                .filter(InterviewSessionContext.QARecord::isProbe).count());
        stats.setDurationMinutes(context.getDurationMinutes());
        return stats;
    }

    private Integer calculateTotalScore(java.util.List<InterviewSessionContext.QARecord> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }
        return (int) records.stream().mapToInt(InterviewSessionContext.QARecord::getScore).average().orElse(0);
    }

    private String generateFinalFeedback(InterviewSessionContext context) {
        return "面试已完成，请查看详细结果。";
    }
}
