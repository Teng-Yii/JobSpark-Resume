package com.tengYii.jobspark.infrastructure.repo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tengYii.jobspark.infrastructure.mapper.AgentExecutionTraceMapper;
import com.tengYii.jobspark.infrastructure.repo.AgentExecutionTraceRepository;
import com.tengYii.jobspark.model.po.AgentExecutionTracePO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent执行轨迹表 Repository实现类
 *
 * @author Teng-Yii
 * @since 2026-04-14
 */
@Service
public class AgentExecutionTraceRepositoryImpl extends ServiceImpl<AgentExecutionTraceMapper, AgentExecutionTracePO> 
        implements AgentExecutionTraceRepository {

    @Override
    public AgentExecutionTracePO getByTraceId(String traceId) {
        if (traceId == null || traceId.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<AgentExecutionTracePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AgentExecutionTracePO::getTraceId, traceId);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public List<AgentExecutionTracePO> getBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<AgentExecutionTracePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AgentExecutionTracePO::getSessionId, sessionId)
                .orderByAsc(AgentExecutionTracePO::getStartTime);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<AgentExecutionTracePO> getByMemoryId(String memoryId) {
        if (memoryId == null || memoryId.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<AgentExecutionTracePO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AgentExecutionTracePO::getMemoryId, memoryId)
                .orderByAsc(AgentExecutionTracePO::getStartTime);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public void updateStatus(String traceId, String status, LocalDateTime endTime, Long durationMs) {
        LambdaUpdateWrapper<AgentExecutionTracePO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AgentExecutionTracePO::getTraceId, traceId)
                .set(AgentExecutionTracePO::getStatus, status)
                .set(AgentExecutionTracePO::getEndTime, endTime)
                .set(AgentExecutionTracePO::getDurationMs, durationMs)
                .set(AgentExecutionTracePO::getUpdatedTime, LocalDateTime.now());
        baseMapper.update(null, updateWrapper);
    }

    @Override
    public void updateError(String traceId, String errorMessage, String errorStackTrace) {
        LambdaUpdateWrapper<AgentExecutionTracePO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AgentExecutionTracePO::getTraceId, traceId)
                .set(AgentExecutionTracePO::getStatus, "FAILED")
                .set(AgentExecutionTracePO::getErrorMessage, errorMessage)
                .set(AgentExecutionTracePO::getErrorStackTrace, errorStackTrace)
                .set(AgentExecutionTracePO::getUpdatedTime, LocalDateTime.now());
        baseMapper.update(null, updateWrapper);
    }
}