package com.tengYii.jobspark.infrastructure.repo.impl;

import com.tengYii.jobspark.model.po.ResumeTaskPO;
import com.tengYii.jobspark.infrastructure.mapper.ResumeTaskMapper;
import com.tengYii.jobspark.infrastructure.repo.ResumeTaskRepository;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

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

}
