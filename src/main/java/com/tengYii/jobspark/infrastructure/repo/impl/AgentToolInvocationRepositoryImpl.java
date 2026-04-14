package com.tengYii.jobspark.infrastructure.repo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tengYii.jobspark.infrastructure.mapper.AgentToolInvocationMapper;
import com.tengYii.jobspark.infrastructure.repo.AgentToolInvocationRepository;
import com.tengYii.jobspark.model.po.AgentToolInvocationPO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent工具调用记录表 Repository实现类
 *
 * @author Teng-Yii
 * @since 2026-04-14
 */
@Service
public class AgentToolInvocationRepositoryImpl extends ServiceImpl<AgentToolInvocationMapper, AgentToolInvocationPO> 
        implements AgentToolInvocationRepository {

    @Override
    public List<AgentToolInvocationPO> getByTraceId(String traceId) {
        if (traceId == null || traceId.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<AgentToolInvocationPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AgentToolInvocationPO::getTraceId, traceId)
                .orderByAsc(AgentToolInvocationPO::getInvocationOrder);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public void saveBatch(List<AgentToolInvocationPO> invocations) {
        if (invocations == null || invocations.isEmpty()) {
            return;
        }
        saveBatch(invocations, invocations.size());
    }
}
