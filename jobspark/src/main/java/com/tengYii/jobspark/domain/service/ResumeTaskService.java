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


    @Transactional(rollbackFor = Exception.class)
    public Boolean saveTask(ResumeTaskPO taskPO) {
        if (Objects.isNull(taskPO) || StringUtils.isEmpty(taskPO.getTaskId())) {
            log.warn("保存任务失败，任务对象或任务ID为空");
            return Boolean.FALSE;
        }

        try {
            // 设置过期时间（默认30分钟后过期）
            if (Objects.isNull(taskPO.getExpireTime())) {
                taskPO.setExpireTime(LocalDateTime.now().plusMinutes(30));
            }

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
            return resumeTaskRepository.selectByTaskId(taskId);
        } catch (Exception e) {
            log.error("查询任务失败，taskId: {}", taskId, e);
            return null;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean updateTaskStatus(String taskId, TaskStatusEnum status) {
        if (StringUtils.isEmpty(taskId) || Objects.isNull(status)) {
            log.warn("更新任务状态失败，参数为空");
            return Boolean.FALSE;
        }

        try {
            ResumeTaskPO updatePO = ResumeTaskPO.builder()
                    .taskId(taskId)
                    .status(status.getCode())
                    .updateTime(LocalDateTime.now())
                    .build();

            Integer result = resumeTaskRepository.updateByTaskId(updatePO);
            return result > 0;
        } catch (Exception e) {
            log.error("更新任务状态失败，taskId: {}, status: {}", taskId, status, e);
            return Boolean.FALSE;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean setTaskStartTime(String taskId) {
        if (StringUtils.isEmpty(taskId)) {
            log.warn("设置任务开始时间失败，任务ID为空");
            return Boolean.FALSE;
        }

        try {
            ResumeTaskPO updatePO = ResumeTaskPO.builder()
                    .taskId(taskId)
                    .startTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            Integer result = resumeTaskRepository.updateByTaskId(updatePO);
            return result > 0;
        } catch (Exception e) {
            log.error("设置任务开始时间失败，taskId: {}", taskId, e);
            return Boolean.FALSE;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean completeTask(String taskId, Long resumeId, CvBO cvBO) {
        if (StringUtils.isEmpty(taskId) || Objects.isNull(resumeId)) {
            log.warn("完成任务失败，参数为空");
            return Boolean.FALSE;
        }

        try {
            ResumeTaskPO updatePO = ResumeTaskPO.builder()
                    .taskId(taskId)
                    .status(TaskStatusEnum.COMPLETED.getCode())
                    .resumeId(resumeId)
                    .completeTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            Integer result = resumeTaskRepository.updateByTaskId(updatePO);

            if (result > 0) {
                log.info("任务完成，taskId: {}, resumeId: {}", taskId, resumeId);
            }

            return result > 0;
        } catch (Exception e) {
            log.error("完成任务失败，taskId: {}, resumeId: {}", taskId, resumeId, e);
            return Boolean.FALSE;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean failTask(String taskId, String errorMessage) {
        if (StringUtils.isEmpty(taskId)) {
            log.warn("任务失败处理失败，任务ID为空");
            return Boolean.FALSE;
        }

        try {
            ResumeTaskPO updatePO = ResumeTaskPO.builder()
                    .taskId(taskId)
                    .status(TaskStatusEnum.FAILED.getCode())
                    .errorMessage(errorMessage)
                    .completeTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            Integer result = resumeTaskRepository.updateByTaskId(updatePO);

            if (result > 0) {
                log.warn("任务失败，taskId: {}, errorMessage: {}", taskId, errorMessage);
            }

            return result > 0;
        } catch (Exception e) {
            log.error("任务失败处理异常，taskId: {}", taskId, e);
            return Boolean.FALSE;
        }
    }

    @Transactional(rollbackFor = Exception.class)
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
