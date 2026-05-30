package com.tengYii.jobspark.domain.service.interview;

import com.tengYii.jobspark.common.utils.SnowflakeUtil;
import com.tengYii.jobspark.config.listener.event.AgentInvocationEvent;
import com.tengYii.jobspark.domain.agent.interview.*;
import com.tengYii.jobspark.common.enums.InterviewDecisionEnum;
import com.tengYii.jobspark.dto.request.InterviewSimulationRequest;
import com.tengYii.jobspark.infrastructure.store.RedisChatMemoryStore;
import com.tengYii.jobspark.infrastructure.store.memory.DecisionIndex;
import com.tengYii.jobspark.infrastructure.store.memory.HybridCompactingChatMemory;
import com.tengYii.jobspark.infrastructure.store.memory.InterviewRuleBasedScorer;
import com.tengYii.jobspark.model.bo.interview.*;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.skills.FileSystemSkillLoader;
import dev.langchain4j.skills.Skills;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 面试编排服务类，负责协调多代理系统完成完整的面试流程。
 * <p>
 * 面试流程采用交互式架构设计：
 * <ul>
 *   <li><b>启动流程</b>：JD对齐 → 计划制定 → 生成首个问题</li>
 *   <li><b>交互循环</b>：用户回答 → 反思评估 → 决策 → 生成下一问题</li>
 *   <li><b>决策分支</b>：根据反思结果选择后续动作：
 *     <ul>
 *       <li>PROBE - 追问（增加追问计数，不推进问题索引）</li>
 *       <li>NEXT - 推进到下一问题</li>
 *       <li>STAGE_FINISH - 推进到下一阶段</li>
 *       <li>FINISH - 结束面试</li>
 *     </ul>
 *   </li>
 * </ul>
 * <p>
 * 核心组件：
 * <ul>
 *   <li>{@link JDAlignmentAgent} - JD与简历技能对齐分析</li>
 *   <li>{@link InterviewCoordinatorAgent} - 面试计划制定</li>
 *   <li>{@link JavaTechInterviewerAgent} - 技术问题执行</li>
 *   <li>{@link InterviewReflectorAgent} - 面试结果反思评估</li>
 * </ul>
 * <p>
 *
 * @see InterviewCoordinatorAgent
 * @see JavaTechInterviewerAgent
 * @see InterviewReflectorAgent
 */
@Slf4j
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
     * Spring事件发布器，用于手动发布Agent调用事件
     * （独立@Agent不走AgentListener生命周期，需手动发布）
     */
    @Resource
    private ApplicationEventPublisher eventPublisher;

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
     * 共享决策索引，跨Agent记录和查询关键决策
     */
    private final DecisionIndex sharedDecisionIndex = new DecisionIndex();

    /**
     * 初始化所有Agent组件
     * 在Spring容器完成依赖注入后调用，确保ChatModel已正确注入
     */
    @PostConstruct
    public void initAgents() {
        // 使用混合压缩记忆替代简单滑动窗口，实现重要性分级 + 摘要压缩
        ChatMemoryProvider hybridMemoryProvider = memoryId -> HybridCompactingChatMemory.builder()
                .id(memoryId)
                .chatModel(chatModel)
                .store(redisChatMemoryStore)
                .scorer(new InterviewRuleBasedScorer())
                .compactThreshold(30)       // 超过30条消息触发压缩
                .recentMessageCount(6)      // 始终保留最近6条消息
                .decisionIndex(sharedDecisionIndex)  // 跨Agent共享决策索引
                .build();

        // 注意：独立 @Agent 不走 AgentListener 生命周期回调，
        // Agent 调用事件改为在服务层手动发布（见 publishAgentStart/publishAgentEnd）
        jdAlignAgent = AgenticServices
                .agentBuilder(JDAlignmentAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(hybridMemoryProvider)
                .toolProvider(jdAlignmentSkill.toolProvider())
                .systemMessage("""
                        你拥有以下skills权限：
                        %s
                        当用户请求涉及上述skills时，必须先通过activate_skill工具激活skill，再执行操作。
                        """.formatted(jdAlignmentSkill.formatAvailableSkills()))
                .outputKey("jdAlignResult")
                .build();

        planner = AgenticServices
                .agentBuilder(InterviewCoordinatorAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(hybridMemoryProvider)
                .outputKey("interviewPlan")
                .build();

        executor = AgenticServices
                .agentBuilder(JavaTechInterviewerAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(hybridMemoryProvider)
                .toolProvider(questionProbingSkill.toolProvider())
                .systemMessage(buildInterviewerSystemMessage(questionProbingSkill.formatAvailableSkills()))
                .outputKey("questionBO")
                .build();

        reflector = AgenticServices
                .agentBuilder(InterviewReflectorAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(hybridMemoryProvider)
                .outputKey("reflection")
                .build();
    }

    /**
     * 获取共享决策索引
     * 供外部服务查询面试过程中的关键决策记录
     *
     * @return 共享的DecisionIndex实例
     */
    public DecisionIndex getSharedDecisionIndex() {
        return sharedDecisionIndex;
    }

    /**
     * 启动面试流程（首次交互）
     * 执行JD对齐和计划制定，返回首次问题和会话信息
     *
     * @param request         模拟面试请求对象
     * @param resumeContextBO 简历上下文对象
     * @return 首次会话响应
     */
    public InterviewResponseBO startInterview(InterviewSimulationRequest request, ResumeContextBO resumeContextBO) {
        String sessionId = generateSessionId();
        Long userId = request.getUserId();
        Long resumeId = Long.valueOf(request.getResumeId());
        log.info("创建面试会话: sessionId={}, userId={}, resumeId={}", sessionId, userId, resumeId);

        // 设置会话ID，用于Trace ID关联
        String memoryId = buildMemoryId(userId, resumeId);

        InterviewSessionContext context = InterviewSessionContext.getOrCreate(
                sessionId, userId, resumeId, request.getJobDescription());

        // 填充简历信息至会话上下文
        context.setResumeContextBO(resumeContextBO);

        // 调用JD对齐Agent
        JDAlignmentResultBO jdResult = invokeAndPublish(memoryId, sessionId, "JDAlignmentAgent", "jdAlign",
                Map.of("jobDescription", request.getJobDescription(), "resumeContext", resumeContextBO),
                () -> jdAlignAgent.align(memoryId, request.getJobDescription(), resumeContextBO));
        context.setJdAlignmentResult(jdResult);

        // 调用计划制定Agent
        InterviewPlanBO plan = invokeAndPublish(memoryId, sessionId, "InterviewCoordinatorAgent", "planner",
                Map.of("resumeContext", resumeContextBO, "jdResult", jdResult),
                () -> planner.generateInterviewPlan(memoryId, resumeContextBO, jdResult));
        context.setInterviewPlan(plan);

        // 调用问题生成Agent
        InterviewQuestionBO firstQuestionBO = invokeAndPublish(memoryId, sessionId, "JavaTechInterviewerAgent", "executor",
                Map.of("plan", plan, "stageIndex", context.getCurrentStageIndex(),
                        "questionIndex", context.getCurrentQuestionIndex(), "isProbe", false),
                () -> executor.generateQuestion(
                        memoryId,
                        plan,
                        context.getCurrentStageIndex(),
                        context.getCurrentQuestionIndex(),
                        false
                ));
        context.setCurrentQuestionBO(firstQuestionBO);
        context.setCurrentQuestion(firstQuestionBO.getQuestionContent());

        return buildSessionResponse(sessionId, context, firstQuestionBO.getQuestionContent());
    }

    /**
     * 继续面试流程（后续交互）
     * 每轮用户回答后执行：反思 → 决策 → 生成下一问题
     *
     * @param sessionId  会话ID
     * @param userAnswer 用户回答
     * @return 进行中或结束响应
     */
    public InterviewResponseBO continueInterview(String sessionId, String userAnswer) {
        InterviewSessionContext context = InterviewSessionContext.get(sessionId);
        if (Objects.isNull(context)) {
            return InterviewResponseBO.error(sessionId, "会话不存在或已过期");
        }

        if (context.isFinished()) {
            return InterviewResponseBO.error(sessionId, "面试已结束");
        }

        String memoryId = buildMemoryId(context.getUserId(), context.getResumeId());
        context.setLastUserAnswer(userAnswer);

        ResumeContextBO resumeContextBO = context.getResumeContextBO();

        // 调用反思评估Agent
        ReflectionResultBO reflection = invokeAndPublish(memoryId, sessionId, "InterviewReflectorAgent", "reflector",
                Map.of("question", context.getCurrentQuestion(), "answer", userAnswer,
                        "resumeContext", resumeContextBO),
                () -> reflector.reflect(memoryId, context.getCurrentQuestion(), userAnswer, resumeContextBO));
        context.setCurrentDecision(reflection.getDecision());

        addQARecord(context, reflection);

        InterviewDecisionEnum decision = reflection.getDecision();

        if (InterviewDecisionEnum.FINISH.equals(decision)) {
            context.finish();
            return buildCompleteResponse(sessionId, context);
        }

        // 根据决策结果更新会话状态
        // 追问模式：decision=PROBE 且追问次数未超限
        boolean isProbe = InterviewDecisionEnum.PROBE.equals(decision) && context.canProbe();
        if (isProbe) {
            // 追问模式：增加追问计数，不推进问题/阶段
            context.incrementProbeCount();
        } else {
            // 非追问模式：根据决策推进面试进度
            if (InterviewDecisionEnum.STAGE_FINISH.equals(decision)) {
                context.moveToNextStage();
            } else {
                // NEXT、FINISH（FINISH已在上面处理）、或其他情况：推进到下一问题
                context.moveToNextQuestion();
            }
        }

        // 调用问题生成Agent
        InterviewQuestionBO nextQuestionBO = invokeAndPublish(memoryId, sessionId, "JavaTechInterviewerAgent", "executor",
                Map.of("plan", context.getInterviewPlan(), "stageIndex", context.getCurrentStageIndex(),
                        "questionIndex", context.getCurrentQuestionIndex(), "isProbe", isProbe),
                () -> executor.generateQuestion(
                        memoryId,
                        context.getInterviewPlan(),
                        context.getCurrentStageIndex(),
                        context.getCurrentQuestionIndex(),
                        isProbe
                ));
        context.setCurrentQuestionBO(nextQuestionBO);
        context.setCurrentQuestion(nextQuestionBO.getQuestionContent());

        return buildProgressResponse(sessionId, context, reflection);
    }

    /**
     * 获取会话状态
     */
    public InterviewResponseBO getSessionStatus(String sessionId) {
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
    public InterviewResponseBO finishInterview(String sessionId) {
        InterviewSessionContext context = InterviewSessionContext.get(sessionId);
        if (context == null) {
            return InterviewResponseBO.error(sessionId, "会话不存在或已过期");
        }

        context.finish();
        return buildCompleteResponse(sessionId, context);
    }

    /**
     * 调用 Agent 并手动发布调用事件。
     * <p>
     * 独立 @Agent 不走 AgentListener 生命周期，需在服务层手动发布事件。
     * </p>
     *
     * @param memoryId  Memory ID
     * @param sessionId 会话 ID
     * @param agentName Agent 名称
     * @param agentId   Agent 标识
     * @param inputs    Agent 入参（用于 input_summary）
     * @param call      Agent 调用
     * @return Agent 返回结果
     */
    private <T> T invokeAndPublish(String memoryId, String sessionId, String agentName, String agentId,
                                    Map<String, Object> inputs, Supplier<T> call) {
        String traceId = agentId + "_" + memoryId + "_" + System.currentTimeMillis();
        LocalDateTime startTime = LocalDateTime.now();

        // 发布 START 事件
        try {
            eventPublisher.publishEvent(new AgentInvocationEvent(
                    this, traceId, sessionId, memoryId,
                    agentName, agentId, null, startTime, inputs
            ));
        } catch (Exception e) {
            log.warn("发布Agent START事件失败: {}", e.getMessage());
        }

        try {
            T result = call.get();
            LocalDateTime endTime = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

            // 发布 END 事件
            try {
                eventPublisher.publishEvent(new AgentInvocationEvent(
                        this, traceId, sessionId, memoryId,
                        agentName, agentId, null,
                        startTime, endTime, durationMs,
                        inputs, result
                ));
            } catch (Exception e) {
                log.warn("发布Agent END事件失败: {}", e.getMessage());
            }

            return result;
        } catch (Exception ex) {
            LocalDateTime endTime = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();

            // 发布 ERROR 事件
            try {
                eventPublisher.publishEvent(new AgentInvocationEvent(
                        this, traceId, sessionId, memoryId,
                        agentName, agentId, null,
                        startTime, endTime, durationMs,
                        inputs, ex.getMessage(), getStackTrace(ex)
                ));
            } catch (Exception e) {
                log.warn("发布Agent ERROR事件失败: {}", e.getMessage());
            }

            throw ex instanceof RuntimeException re ? re : new RuntimeException(ex);
        }
    }

    private String getStackTrace(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement e : t.getStackTrace()) {
            sb.append(e.toString()).append("\n");
            if (sb.length() > 2000) break;
        }
        return sb.toString();
    }

    private String generateSessionId() {
        return String.valueOf(SnowflakeUtil.snowflakeId());
    }

    private String buildMemoryId(Long userId, Long resumeId) {
        return userId + "_" + resumeId;
    }

    private InterviewResponseBO buildSessionResponse(String sessionId, InterviewSessionContext context, String firstQuestion) {
        InterviewSessionBO sessionBO = new InterviewSessionBO();
        sessionBO.setSessionId(sessionId);
        sessionBO.setCurrentStageIndex(context.getCurrentStageIndex());
        sessionBO.setCurrentStageName(getStageName(context));
        sessionBO.setCurrentQuestionIndex(context.getCurrentQuestionIndex());
        sessionBO.setCurrentQuestion(firstQuestion);
        sessionBO.setTotalPlannedQuestions(calculateTotalQuestions(context.getInterviewPlan()));
        sessionBO.setStageInfos(buildStageInfos(context));
        sessionBO.setWelcomeMessage("欢迎参加Java技术面试，我会根据您的简历和职位要求进行个性化提问。请放松心态，展现您的真实水平。");

        return InterviewResponseBO.session(sessionId, sessionBO);
    }

    private InterviewResponseBO buildProgressResponse(String sessionId, InterviewSessionContext context, ReflectionResultBO lastReflection) {
        InterviewProgressBO progressBO = new InterviewProgressBO();
        progressBO.setSessionId(sessionId);
        progressBO.setCurrentQuestionIndex(context.getCurrentQuestionIndex());
        progressBO.setCurrentStageIndex(context.getCurrentStageIndex());
        progressBO.setCurrentStageName(getStageName(context));
        progressBO.setCurrentQuestion(context.getCurrentQuestion());
        progressBO.setProgress(buildProgressInfo(context));
        progressBO.setFinished(context.isFinished());

        return InterviewResponseBO.progress(sessionId, progressBO);
    }

    private InterviewResponseBO buildCompleteResponse(String sessionId, InterviewSessionContext context) {
        InterviewCompleteBO completeBO = new InterviewCompleteBO();
        completeBO.setSessionId(sessionId);
        completeBO.setFinished(true);
        completeBO.setQaHistory(convertQARecordsForCandidate(context.getQaHistory()));
        completeBO.setStatistics(buildStatistics(context));
        completeBO.setTotalScore(calculateTotalScore(context.getQaHistory()));
        completeBO.setFinalFeedback(generateFinalFeedback(context));
        completeBO.setImprovementAreas(extractWeakAreas(context.getQaHistory()));
        completeBO.setStrongAreas(extractStrongAreas(context.getQaHistory()));

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

    /**
     * 转换问答记录（候选人视角）- 不包含评估信息
     */
    private java.util.List<InterviewCompleteBO.QARecord> convertQARecordsForCandidate(
            java.util.List<InterviewSessionContext.QARecord> records) {
        return records.stream()
                .map(r -> {
                    InterviewCompleteBO.QARecord record = new InterviewCompleteBO.QARecord();
                    record.setStageName(r.getStageName());
                    record.setQuestion(r.getQuestion());
                    record.setAnswer(r.getAnswer());
                    return record;
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

    /**
     * 从问答历史中提取需要加强的知识点
     * 基于评分较低的记录分析
     */
    private java.util.List<String> extractWeakAreas(java.util.List<InterviewSessionContext.QARecord> records) {
        if (records == null || records.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return records.stream()
                .filter(r -> r.getScore() < 60)
                .map(InterviewSessionContext.QARecord::getStageName)
                .distinct()
                .limit(3)
                .toList();
    }

    /**
     * 从问答历史中提取表现优秀的知识点
     * 基于评分较高的记录分析
     */
    private java.util.List<String> extractStrongAreas(java.util.List<InterviewSessionContext.QARecord> records) {
        if (records == null || records.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return records.stream()
                .filter(r -> r.getScore() >= 80)
                .map(InterviewSessionContext.QARecord::getStageName)
                .distinct()
                .limit(3)
                .toList();
    }

    private String generateFinalFeedback(InterviewSessionContext context) {
        int totalScore = calculateTotalScore(context.getQaHistory());

        if (totalScore >= 80) {
            return "恭喜！您在本次面试中表现优秀，展现了扎实的技术功底。建议继续保持学习深度，关注新技术趋势。";
        } else if (totalScore >= 60) {
            return "面试已完成，您的表现达到基本要求。建议针对薄弱环节加强学习，多做项目实践提升经验。";
        } else {
            return "面试已完成。建议您系统复习Java核心技术栈，加强项目经验积累，平时多练习编码和系统设计。";
        }
    }

    /**
     * 构建面试执行Agent的系统消息
     * 组合 skill 激活信息与面试官角色提示词
     *
     * @param availableSkills 可用的skills描述
     * @return 完整的系统消息
     */
    private String buildInterviewerSystemMessage(String availableSkills) {
        return """
                你是一位拥有10年以上开发经验的资深Java技术面试官。
                
                ## 核心原则
                1. **个性化提问**：必须结合候选人的简历项目经历来定制问题，不要问空泛的理论问题
                2. **深度追问**：根据候选人的回答质量动态调整，对深入的回答进行追问
                3. **难度适配**：候选人回答困难时适当降低难度，回答优秀时继续深挖
                4. **场景化问题**：技术问题要结合实际业务场景，特别是候选人简历中提到的技术栈
                
                ## 追问模式说明
                - 当 isProbe=true 时，表示需要对上一轮回答进行深入追问
                - 追问应基于候选人的回答细节进行针对性提问
                - 追问方向：原理深挖、场景应用、解决方案对比、最佳实践等
                
                ## 提问技巧
                - 如果候选人提到 Redis，必须问具体的缓存使用场景、缓存策略、雪崩处理等
                - 如果候选人提到微服务，要问服务治理、熔断限流、分布式事务等
                - 如果候选人提到并发，要问具体的并发场景、线程安全解决方案等
                - 项目问题要深挖：为什么用这个技术？遇到的最大挑战？如何解决的？
                
                ## 输出要求
                输出 JSON 格式的 InterviewQuestionBO 对象，包含：
                - stageName: 当前阶段名称
                - stageOrder: 当前阶段序号
                - topicName: 考察主题名称
                - questionContent: 面试官提问内容（问题文本）
                - intentAnalyses: 出题意图分析列表
                - followUpPlans: 追问预案列表
                - simplifiedQuestion: 备选简化问题
                
                ## Skills权限
                你拥有以下skills权限：
                %s
                当任务涉及上述skills时，必须先通过activate_skill工具激活skill，再执行操作。
                """.formatted(availableSkills);
    }
}
