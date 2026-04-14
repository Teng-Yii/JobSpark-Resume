package com.tengYii.jobspark.infrastructure.repo;

import com.tengYii.jobspark.model.po.AgentSessionStatsPO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * Agent会话聚合统计表 Repository接口
 *
 * @author Teng-Yii
 * @since 2026-04-14
 */
public interface AgentSessionStatsRepository extends IService<AgentSessionStatsPO> {

    /**
     * 根据sessionId获取统计信息
     *
     * @param sessionId 会话ID
     * @return 统计信息
     */
    AgentSessionStatsPO getBySessionId(String sessionId);

    /**
     * 增加Agent调用计数
     *
     * @param sessionId  会话ID
     * @param memoryId   Memory ID
     * @param success    是否成功
     * @param durationMs 耗时
     */
    void incrementAgentCall(String sessionId, String memoryId, boolean success, Long durationMs);

    /**
     * 增加工具调用计数
     *
     * @param sessionId 会话ID
     */
    void incrementToolCall(String sessionId);
}
