package com.tengYii.jobspark.application.service;

import com.tengYii.jobspark.model.InterviewEvaluation;
import com.tengYii.jobspark.model.InterviewQuestion;
import com.tengYii.jobspark.model.InterviewSession;

import java.util.List;

public interface InterviewApplicationService {

    /**
     * 创建面试会话
     *
     * @param resumeId      简历ID
     * @param interviewType 面试类型
     * @param questionCount 面试问题数量
     * @return 新创建的面试会话对象
     */
    InterviewSession createInterviewSession(Long resumeId, String interviewType, int questionCount);

    /**
     * 评估面试答案
     *
     * @param sessionId 面试会话ID
     * @param answer    面试者的答案
     * @return 评估结果
     */
    InterviewEvaluation evaluateAnswer(String sessionId, String answer);

    /**
     * 完成面试评估并保存所有答案。
     *
     * @param sessionId  面试会话ID，用于标识该次面试。
     * @param allAnswers 所有问题的答案列表。
     */
    InterviewEvaluation completeInterview(String sessionId, List<String> allAnswers);

    /**
     * 根据简历ID和目标职位生成面试建议。
     *
     * @param resumeId       简历ID，用于查找对应的简历信息。
     * @param targetPosition 目标职位，用于匹配面试建议。
     * @return 生成的面试建议，可能包含多条建议。
     */
    String generateInterviewSuggestions(Long resumeId, String targetPosition);

    InterviewQuestion getCurrentQuestion(String sessionId);

    /**
     * 判断面试是否已经完成。
     *
     * @param sessionId 面试会话ID。
     * @return 如果面试已经完成则返回true，否则返回false。
     */
    boolean isInterviewCompleted(String sessionId);

    /**
     * 终止指定会话 ID 的面试
     *
     * @param sessionId 会话 ID
     */
    void terminateInterview(String sessionId);
}
