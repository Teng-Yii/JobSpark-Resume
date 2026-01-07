package com.tengYii.jobspark.infrastructure.repo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tengYii.jobspark.common.enums.TaskStatusEnum;
import com.tengYii.jobspark.model.po.ResumeTaskPO;
import com.tengYii.jobspark.infrastructure.mapper.ResumeTaskMapper;
import com.tengYii.jobspark.infrastructure.repo.ResumeTaskRepository;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 简历处理任务表 服务实现类
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-12-09
 */
@Service
public class ResumeTaskRepositoryImpl extends ServiceImpl<ResumeTaskMapper, ResumeTaskPO> implements ResumeTaskRepository {

    /**
     * 根据任务ID获取对应的任务信息
     *
     * @param taskId 任务ID
     * @return 对应的任务信息对象
     */
    @Override
    public ResumeTaskPO getByTaskId(String taskId) {
        if (StringUtils.isEmpty(taskId)) {
            return null;
        }

        LambdaQueryWrapper<ResumeTaskPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ResumeTaskPO::getTaskId, taskId);

        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 更新任务状态
     *
     * @param taskId         任务ID
     * @param taskStatusEnum 新的任务状态
     * @param updateTime     当前时间
     */
    @Override
    public void updateTaskStatus(String taskId, TaskStatusEnum taskStatusEnum, LocalDateTime updateTime) {
        LambdaUpdateWrapper<ResumeTaskPO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ResumeTaskPO::getTaskId, taskId)
                .set(ResumeTaskPO::getStatus, taskStatusEnum.getCode())
                .set(ResumeTaskPO::getUpdateTime, updateTime);

        baseMapper.update(null, updateWrapper);
    }

    /**
     * 获取用户任务列表
     *
     * @param userId 用户ID（可选）
     * @param status 任务状态（可选）
     * @return 任务列表
     */
    @Override
    public List<ResumeTaskPO> getUserTasks(Long userId, String status) {
        LambdaQueryWrapper<ResumeTaskPO> queryWrapper = new LambdaQueryWrapper<>();

        // 如果提供了用户ID，则按用户ID筛选
        if (Objects.nonNull(userId)) {
            queryWrapper.eq(ResumeTaskPO::getUserId, userId);
        }

        // 如果提供了状态，则按状态筛选
        if (StringUtils.isNotEmpty(status)) {
            queryWrapper.eq(ResumeTaskPO::getStatus, status);
        }

        // 按创建时间倒序排列，最新的任务在前面
        queryWrapper.orderByDesc(ResumeTaskPO::getCreateTime);

        return baseMapper.selectList(queryWrapper);
    }
}
