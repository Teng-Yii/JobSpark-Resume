package com.tengYii.jobspark.model.bo.interview;

import com.tengYii.jobspark.common.enums.InterviewDecisionEnum;
import lombok.Data;

/**
 * 统一的面试响应包装类
 * 根据当前面试阶段动态返回不同类型的响应
 */
@Data
public class InterviewResponseBO {

    /**
     * 响应类型
     */
    private ResponseType responseType;

    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 当前面试状态
     */
    private InterviewState interviewState;

    /**
     * 首次会话响应数据
     */
    private InterviewSessionBO session;

    /**
     * 进行中响应数据
     */
    private InterviewProgressBO progress;

    /**
     * 结束响应数据
     */
    private InterviewCompleteBO complete;

    /**
     * 响应类型枚举
     */
    public enum ResponseType {
        /**
         * 首次会话
         */
        SESSION,
        /**
         * 进行中
         */
        PROGRESS,
        /**
         * 面试结束
         */
        COMPLETE,
        /**
         * 错误
         */
        ERROR
    }

    /**
     * 面试状态枚举
     */
    public enum InterviewState {
        /**
         * 初始化中（JD对齐、计划制定）
         */
        INITIALIZING,
        /**
         * 进行中
         */
        IN_PROGRESS,
        /**
         * 已完成
         */
        COMPLETED,
        /**
         * 已中断
         */
        PAUSED,
        /**
         * 出错
         */
        ERROR
    }

    /**
     * 创建首次会话响应
     */
    public static InterviewResponseBO session(Long sessionId, InterviewSessionBO session) {
        InterviewResponseBO response = new InterviewResponseBO();
        response.setResponseType(ResponseType.SESSION);
        response.setSessionId(sessionId);
        response.setInterviewState(InterviewState.IN_PROGRESS);
        response.setSession(session);
        return response;
    }

    /**
     * 创建进行中响应
     */
    public static InterviewResponseBO progress(Long sessionId, InterviewProgressBO progress) {
        InterviewResponseBO response = new InterviewResponseBO();
        response.setResponseType(ResponseType.PROGRESS);
        response.setSessionId(sessionId);
        response.setInterviewState(InterviewState.IN_PROGRESS);
        response.setProgress(progress);
        return response;
    }

    /**
     * 创建结束响应
     */
    public static InterviewResponseBO complete(Long sessionId, InterviewCompleteBO complete) {
        InterviewResponseBO response = new InterviewResponseBO();
        response.setResponseType(ResponseType.COMPLETE);
        response.setSessionId(sessionId);
        response.setInterviewState(InterviewState.COMPLETED);
        response.setComplete(complete);
        return response;
    }

    /**
     * 创建错误响应
     */
    public static InterviewResponseBO error(Long sessionId, String message) {
        InterviewResponseBO response = new InterviewResponseBO();
        response.setResponseType(ResponseType.ERROR);
        response.setSessionId(sessionId);
        response.setInterviewState(InterviewState.ERROR);
        return response;
    }

    /**
     * 判断当前决策是否需要结束面试
     */
    public boolean isFinished() {
        if (progress != null && progress.getCurrentDecision() != null) {
            return InterviewDecisionEnum.FINISH.equals(progress.getCurrentDecision());
        }
        if (complete != null) {
            return complete.isFinished();
        }
        return false;
    }
}