package com.tengYii.jobspark.infrastructure.repo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tengYii.jobspark.infrastructure.mapper.AgentSessionStatsMapper;
import com.tengYii.jobspark.infrastructure.repo.AgentSessionStatsRepository;
import com.tengYii.jobspark.model.po.AgentSessionStatsPO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Agent会话聚合统计表 Repository实现类
 *
 * @author Teng-Yii
 * @since 2026-04-14
 */
@Service
public class AgentSessionStatsRepositoryImpl extends ServiceImpl<AgentSessionStatsMapper, AgentSessionStatsPO> 
        implements AgentSessionStatsRepository {

    @Override
    public AgentSessionStatsPO getBySessionId(String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<AgentSessionStatsPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AgentSessionStatsPO::getSessionId, sessionId);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public void incrementAgentCall(String sessionId, String memoryId, boolean success, Long durationMs) {
        AgentSessionStatsPO stats = getBySessionId(sessionId);
        
        if (stats == null) {
            // 首次创建
            stats = AgentSessionStatsPO.builder()
                    .sessionId(sessionId)
                    .memoryId(memoryId)
                    .totalAgentCalls(1)
                    .totalToolCalls(0)
                    .totalDurationMs(durationMs != null ? durationMs : 0L)
                    .successCount(success ? 1 : 0)
                    .failedCount(success ? 0 : 1)
                    .firstCallTime(LocalDateTime.now())
                    .lastCallTime(LocalDateTime.now())
                    .deleteFlag(0)
                    .createdTime(LocalDateTime.now())
                    .build();
            save(stats);
        } else {
            // 更新统计
            LambdaUpdateWrapper<AgentSessionStatsPO> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(AgentSessionStatsPO::getSessionId, sessionId)
                    .setSql("total_agent_calls = total_agent_calls + 1")
                    .setSql("total_duration_ms = total_duration_ms + " + (durationMs != null ? durationMs : 0))
                    .setSql(success ? "success_count = success_count + 1" : "failed_count = failed_count + 1")
                    .set(AgentSessionStatsPO::getLastCallTime, LocalDateTime.now())
                    .set(AgentSessionStatsPO::getUpdatedTime, LocalDateTime.now());
            baseMapper.update(null, updateWrapper);
        }
    }

    @Override
    public void incrementToolCall(String sessionId) {
        LambdaUpdateWrapper<AgentSessionStatsPO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AgentSessionStatsPO::getSessionId, sessionId)
                .setSql("total_tool_calls = total_tool_calls + 1")
                .set(AgentSessionStatsPO::getUpdatedTime, LocalDateTime.now());
        baseMapper.update(null, updateWrapper);
    }
}
