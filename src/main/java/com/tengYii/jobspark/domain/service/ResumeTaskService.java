package com.tengYii.jobspark.domain.service;

import com.tengYii.jobspark.common.enums.TaskStatusEnum;
import com.tengYii.jobspark.infrastructure.repo.ResumeTaskRepository;
import com.tengYii.jobspark.model.po.ResumeTaskPO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    /**
     * 根据任务ID获取任务信息
     *
     * @param taskId 任务ID
     * @return 任务信息对象，若任务ID为空或查询失败则返回null
     */
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
     * @return 更新是否成功
     */
    public Boolean completeTask(String taskId, Long resumeId) {
        if (StringUtils.isEmpty(taskId) || Objects.isNull(resumeId)) {
            log.warn("完成任务失败，参数为空");
            return Boolean.FALSE;
        }

        try {
            LocalDateTime nowTime = LocalDateTime.now();
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

    /**
     * 处理任务失败并更新任务状态。
     *
     * @param taskId       任务ID。
     * @param resumeId     简历ID。
     * @param errorMessage 失败错误信息。
     */
    public void failTask(String taskId, Long resumeId, String errorMessage) {
        if (StringUtils.isEmpty(taskId)) {
            log.error("任务失败处理失败，任务ID为空");
        }

        try {
            LocalDateTime nowTime = LocalDateTime.now();
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

    /**
     * 获取用户任务列表
     *
     * @param userId 用户ID（可选）
     * @param status 任务状态（可选）
     * @return 任务列表
     */
    public List<ResumeTaskPO> getUserTasks(Long userId, String status) {
        try {
            return resumeTaskRepository.getUserTasks(userId, status);
        } catch (Exception e) {
            log.error("获取用户任务列表失败，userId: {}, status: {}", userId, status, e);
            return null;
        }
    }
}
