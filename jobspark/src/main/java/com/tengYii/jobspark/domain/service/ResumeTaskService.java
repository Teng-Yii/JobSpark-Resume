package com.tengYii.jobspark.domain.service;

//import com.tengYii.jobspark.infrastructure.mapper.ResumeTaskMapper;

import com.aliyuncs.utils.StringUtils;
import com.tengYii.jobspark.common.enums.TaskStatusEnum;
import com.tengYii.jobspark.infrastructure.repo.ResumeTaskRepository;
import com.tengYii.jobspark.model.bo.CvBO;
import com.tengYii.jobspark.model.po.ResumeTaskPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 简历任务服务实现类
 *
 * @author tengYii
 */
@Slf4j
@Service
public class ResumeTaskService {

    @Autowired
    private ResumeTaskRepository resumeTaskRepository;


    /**
     * 保存任务信息到数据库中
     *
     * @param taskPO 任务信息实体对象
     * @return 保存成功返回true，否则返回false
     */
    public Boolean saveTask(ResumeTaskPO taskPO) {
        if (Objects.isNull(taskPO) || StringUtils.isEmpty(taskPO.getTaskId())) {
            log.warn("保存任务失败，任务对象或任务ID为空");
            return Boolean.FALSE;
        }

        try {
            return resumeTaskRepository.save(taskPO);
        } catch (Exception e) {
            log.error("保存任务失败，taskId: {}", taskPO.getTaskId(), e);
            return Boolean.FALSE;
        }
    }

    public ResumeTaskPO getByTaskId(String taskId) {
        if (StringUtils.isEmpty(taskId)) {
            log.warn("查询任务失败，任务ID为空");
            return null;
        }

        try {
            return resumeTaskRepository.getByTaskId(taskId);
        } catch (Exception e) {
            log.error("查询任务失败，taskId: {}", taskId, e);
            return null;
        }
    }

    /**
     * 更新任务状态
     *
     * @param taskId 任务ID
     * @param status 新的任务状态
     * @return 更新成功返回true，否则返回false
     */
    public Boolean updateTaskStatus(String taskId, TaskStatusEnum status) {
        if (StringUtils.isEmpty(taskId) || Objects.isNull(status)) {
            log.warn("更新任务状态失败，参数为空");
            return Boolean.FALSE;
        }

        try {
            resumeTaskRepository.updateTaskStatus(taskId, status, LocalDateTime.now());
            return Boolean.TRUE;
        } catch (Exception e) {
            log.error("更新任务状态失败，taskId: {}, status: {}", taskId, status, e);
            return Boolean.FALSE;
        }
    }

    /**
     * 完成任务并更新任务状态
     *
     * @param taskId   任务ID
     * @param resumeId 简历ID
     * @param nowTime  完成任务的当前时间
     * @return 更新是否成功
     */
    public Boolean completeTask(String taskId, Long resumeId, LocalDateTime nowTime) {
        if (StringUtils.isEmpty(taskId) || Objects.isNull(resumeId)) {
            log.warn("完成任务失败，参数为空");
            return Boolean.FALSE;
        }

        try {
            ResumeTaskPO resumeTaskPO = resumeTaskRepository.getByTaskId(taskId);
            resumeTaskPO.setResumeId(resumeId);
            resumeTaskPO.setStatus(TaskStatusEnum.COMPLETED.getCode());
            resumeTaskPO.setCompleteTime(nowTime);
            resumeTaskPO.setUpdateTime(nowTime);

            return resumeTaskRepository.updateById(resumeTaskPO);
        } catch (Exception e) {
            log.error("完成任务失败，taskId: {}, resumeId: {}", taskId, resumeId, e);
            return Boolean.FALSE;
        }
    }

    public void failTask(String taskId, Long resumeId, LocalDateTime nowTime, String errorMessage) {
        if (StringUtils.isEmpty(taskId)) {
            log.error("任务失败处理失败，任务ID为空");
        }

        try {
            ResumeTaskPO resumeTaskPO = resumeTaskRepository.getByTaskId(taskId);
            resumeTaskPO.setResumeId(resumeId);
            resumeTaskPO.setStatus(TaskStatusEnum.FAILED.getCode());
            resumeTaskPO.setCompleteTime(nowTime);
            resumeTaskPO.setErrorMessage(errorMessage);
            resumeTaskPO.setUpdateTime(nowTime);

            resumeTaskRepository.updateById(resumeTaskPO);
        } catch (Exception e) {
            log.error("任务失败处理异常，taskId: {}", taskId, e);
        }
    }

    @Transactional(rollbackFor = Throwable.class)
    public Boolean incrementRetryCount(String taskId) {
        if (StringUtils.isEmpty(taskId)) {
            log.warn("增加重试次数失败，任务ID为空");
            return Boolean.FALSE;
        }

        try {
            Integer result = resumeTaskRepository.incrementRetryCount(taskId);
            return result > 0;
        } catch (Exception e) {
            log.error("增加重试次数失败，taskId: {}", taskId, e);
            return Boolean.FALSE;
        }
    }

    public List<ResumeTaskPO> findTimeoutTasks(Integer timeoutMinutes) {
        if (Objects.isNull(timeoutMinutes) || timeoutMinutes <= 0) {
            timeoutMinutes = 30;
        }

        try {
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
            return resumeTaskRepository.selectTimeoutTasks(timeoutThreshold);
        } catch (Exception e) {
            log.error("查询超时任务失败，timeoutMinutes: {}", timeoutMinutes, e);
            return List.of();
        }
    }

    public List<ResumeTaskPO> getTasksByUserId(Long userId) {
        if (Objects.isNull(userId)) {
            log.warn("根据用户ID查询任务失败，用户ID为空");
            return List.of();
        }

        try {
            return resumeTaskRepository.selectByUserId(userId);
        } catch (Exception e) {
            log.error("根据用户ID查询任务失败，userId: {}", userId, e);
            return List.of();
        }
    }

    public List<ResumeTaskPO> getTasksByStatus(TaskStatusEnum status) {
        if (Objects.isNull(status)) {
            log.warn("根据状态查询任务失败，状态为空");
            return List.of();
        }

        try {
            return resumeTaskRepository.selectByStatus(status);
        } catch (Exception e) {
            log.error("根据状态查询任务失败，status: {}", status, e);
            return List.of();
        }
    }
}
