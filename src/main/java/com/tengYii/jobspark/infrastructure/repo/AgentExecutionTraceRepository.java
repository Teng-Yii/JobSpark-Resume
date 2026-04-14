package com.tengYii.jobspark.infrastructure.repo;

import com.tengYii.jobspark.model.po.AgentExecutionTracePO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * Agent执行轨迹表 Repository接口
 *
 * @author Teng-Yii
 * @since 2026-04-14
 */
public interface AgentExecutionTraceRepository extends IService<AgentExecutionTracePO> {

    /**
     * 根据traceId获取执行轨迹
     *
     * @param traceId 追踪ID
     * @return 执行轨迹
     */
    AgentExecutionTracePO getByTraceId(String traceId);

    /**
     * 根据sessionId获取所有执行轨迹
     *
     * @param sessionId 会话ID
     * @return 执行轨迹列表
     */
    List<AgentExecutionTracePO> getBySessionId(String sessionId);

    /**
     * 根据memoryId获取所有执行轨迹
     *
     * @param memoryId Memory ID
     * @return 执行轨迹列表
     */
    List<AgentExecutionTracePO> getByMemoryId(String memoryId);

    /**
     * 更新执行状态
     *
     * @param traceId   追踪ID
     * @param status    状态
     * @param endTime   结束时间
     * @param durationMs 耗时
     */
    void updateStatus(String traceId, String status, java.time.LocalDateTime endTime, Long durationMs);

    /**
     * 更新错误信息
     *
     * @param traceId       追踪ID
     * @param errorMessage  错误信息
     * @param errorStackTrace 错误堆栈
     */
    void updateError(String traceId, String errorMessage, String errorStackTrace);
}
