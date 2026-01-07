package com.tengYii.jobspark.infrastructure.repo.impl;

import com.tengYii.jobspark.model.po.CvHighlightPO;
import com.tengYii.jobspark.infrastructure.mapper.CvHighlightMapper;
import com.tengYii.jobspark.infrastructure.repo.CvHighlightRepository;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 统一亮点表（工作经历/项目经历的亮点） 服务实现类
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-11-16
 */
@Service
public class CvHighlightRepositoryImpl extends ServiceImpl<CvHighlightMapper, CvHighlightPO> implements CvHighlightRepository {

}
