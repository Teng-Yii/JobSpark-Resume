package com.tengYii.jobspark.infrastructure.repo.impl;

import com.tengYii.jobspark.model.po.CvPO;
import com.tengYii.jobspark.infrastructure.mapper.CvMapper;
import com.tengYii.jobspark.infrastructure.repo.CvRepository;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 简历基本信息表 服务实现类
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-11-16
 */
@Service
public class CvRepositoryImpl extends ServiceImpl<CvMapper, CvPO> implements CvRepository {

}
