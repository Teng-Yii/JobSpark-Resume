package com.tengYii.jobspark.infrastructure.repo.impl;

import com.tengYii.jobspark.model.po.CvProjectPO;
import com.tengYii.jobspark.infrastructure.mapper.CvProjectMapper;
import com.tengYii.jobspark.infrastructure.repo.CvProjectRepository;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 简历项目经验表 服务实现类
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-11-16
 */
@Service
public class CvProjectRepositoryImpl extends ServiceImpl<CvProjectMapper, CvProjectPO> implements CvProjectRepository {

}
