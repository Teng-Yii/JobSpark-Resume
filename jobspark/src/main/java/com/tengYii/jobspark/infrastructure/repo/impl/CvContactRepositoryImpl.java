package com.tengYii.jobspark.infrastructure.repo.impl;

import com.tengYii.jobspark.model.po.CvContactPO;
import com.tengYii.jobspark.infrastructure.mapper.CvContactMapper;
import com.tengYii.jobspark.infrastructure.repo.CvContactRepository;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 简历联系方式表 服务实现类
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-11-16
 */
@Service
public class CvContactRepositoryImpl extends ServiceImpl<CvContactMapper, CvContactPO> implements CvContactRepository {

}
