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
}
