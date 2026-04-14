package com.tengYii.jobspark.infrastructure.repo;

import com.tengYii.jobspark.model.po.AgentToolInvocationPO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * Agent工具调用记录表 Repository接口
 *
 * @author Teng-Yii
 * @since 2026-04-14
 */
public interface AgentToolInvocationRepository extends IService<AgentToolInvocationPO> {

    /**
     * 根据traceId获取工具调用记录
     *
     * @param traceId 追踪ID
     * @return 工具调用记录列表
     */
    List<AgentToolInvocationPO> getByTraceId(String traceId);

    /**
     * 批量保存工具调用记录
     *
     * @param invocations 工具调用记录列表
     */
    void saveBatch(List<AgentToolInvocationPO> invocations);
}
