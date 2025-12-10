package com.tengYii.jobspark.infrastructure.repo;

import com.tengYii.jobspark.common.enums.TaskStatusEnum;
import com.tengYii.jobspark.model.po.ResumeTaskPO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;

/**
 * <p>
 * 简历处理任务表 服务类
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-12-09
 */
public interface ResumeTaskRepository extends IService<ResumeTaskPO> {

    /**
     * 根据任务ID获取对应的任务信息
     *
     * @param taskId 任务ID
     * @return 对应的任务信息对象
     */
    ResumeTaskPO getByTaskId(String taskId);

    /**
     * 更新任务状态
     *
     * @param taskId         任务ID
     * @param taskStatusEnum 新的任务状态
     * @param updateTime     当前时间
     */
    void updateTaskStatus(String taskId, TaskStatusEnum taskStatusEnum, LocalDateTime updateTime);
}
