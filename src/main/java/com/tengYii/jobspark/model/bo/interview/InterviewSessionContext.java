package com.tengYii.jobspark.model.bo.interview;

import com.tengYii.jobspark.common.enums.InterviewDecisionEnum;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 面试会话上下文管理器
 * 用于在服务端维护会话状态，支持断点续传
 *
 * 设计原则：
 * 1. 会话上下文存储在服务端Map或Redis中
 * 2. 每次用户请求只需传递sessionId，无需传递完整上下文
 * 3. 根据会话状态动态生成对应的响应BO
 */
@Data
public class InterviewSessionContext {

    /**
     * 会话状态Map，用于存储所有活跃会话的上下文
     * key: sessionId
     */
    private static final ConcurrentHashMap<Long, InterviewSessionContext> SESSIONS = new ConcurrentHashMap<>();

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 简历ID
     */
    private Long resumeId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 职位描述
     */
    private String jobDescription;

    /**
     * 会话创建时间
     */
    private long createdTime;

    /**
     * JD对齐结果
     */
    private JDAlignmentResultBO jdAlignmentResult;

    /**
     * 面试计划
     */
    private InterviewPlanBO interviewPlan;

    /**
     * 当前阶段索引
     */
    private int currentStageIndex;

    /**
     * 当前问题索引
     */
    private int currentQuestionIndex;

    /**
     * 当前问题内容
     */
    private String currentQuestion;

    /**
     * 上一轮用户回答
     */
    private String lastUserAnswer;

    /**
     * 问答历史记录
     */
    private List<QARecord> qaHistory = new ArrayList<>();

    /**
     * 当前决策状态
     */
    private InterviewDecisionEnum currentDecision;

    /**
     * 已完成阶段索引集合
     */
    private List<Integer> finishedStages = new ArrayList<>();

    /**
     * 当前追问计数
     */
    private int probeCount = 0;

    /**
     * 最大追问次数
     */
    private static final int MAX_PROBE_COUNT = 3;

    /**
     * 面试是否已结束
     */
    private boolean finished = false;

    /**
     * 面试开始时间（毫秒）
     */
    private long startTime;

    /**
     * 获取会话上下文，如果不存在则创建
     */
    public static InterviewSessionContext getOrCreate(Long sessionId, Long userId, Long resumeId, String jobDescription) {
        return SESSIONS.computeIfAbsent(sessionId, k -> {
            InterviewSessionContext context = new InterviewSessionContext();
            context.sessionId = sessionId;
            context.userId = userId;
            context.resumeId = resumeId;
            context.jobDescription = jobDescription;
            context.startTime = System.currentTimeMillis();
            context.createdTime = System.currentTimeMillis();
            return context;
        });
    }

    /**
     * 根据sessionId获取会话上下文
     */
    public static InterviewSessionContext get(Long sessionId) {
        return SESSIONS.get(sessionId);
    }

    /**
     * 移除会话上下文
     */
    public static void remove(Long sessionId) {
        SESSIONS.remove(sessionId);
    }

    /**
     * 检查会话是否存在
     */
    public static boolean exists(Long sessionId) {
        return SESSIONS.containsKey(sessionId);
    }

    /**
     * 获取所有活跃会话数
     */
    public static int getActiveSessionCount() {
        return SESSIONS.size();
    }

    /**
     * 清理超时会话（超过30分钟）
     */
    public static void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        long timeout = 30 * 60 * 1000;
        SESSIONS.entrySet().removeIf(entry ->
                now - entry.getValue().getCreatedTime() > timeout);
    }

    /**
     * 判断是否可以继续追问
     */
    public boolean canProbe() {
        return probeCount < MAX_PROBE_COUNT && !finished;
    }

    /**
     * 增加追问计数
     */
    public void incrementProbeCount() {
        this.probeCount++;
    }

    /**
     * 重置追问计数
     */
    public void resetProbeCount() {
        this.probeCount = 0;
    }

    /**
     * 推进到下一个问题
     */
    public void moveToNextQuestion() {
        this.currentQuestionIndex++;
        this.probeCount = 0;
    }

    /**
     * 推进到下一阶段
     */
    public void moveToNextStage() {
        this.finishedStages.add(this.currentStageIndex);
        this.currentStageIndex++;
        this.currentQuestionIndex = 0;
        this.probeCount = 0;
    }

    /**
     * 结束面试
     */
    public void finish() {
        this.finished = true;
    }

    /**
     * 获取面试耗时（分钟）
     */
    public int getDurationMinutes() {
        return (int) ((System.currentTimeMillis() - startTime) / 60000);
    }

    /**
     * 问答记录内部类
     */
    @Data
    public static class QARecord {
        private int stageIndex;
        private String stageName;
        private String question;
        private String answer;
        private int score;
        private InterviewDecisionEnum decision;
        private String feedback;
        private boolean isProbe;
    }
}