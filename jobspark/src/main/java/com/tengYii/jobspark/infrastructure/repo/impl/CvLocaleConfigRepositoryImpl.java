package com.tengYii.jobspark.infrastructure.repo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tengYii.jobspark.model.po.CvLocaleConfigPO;
import com.tengYii.jobspark.infrastructure.mapper.CvLocaleConfigMapper;
import com.tengYii.jobspark.infrastructure.repo.CvLocaleConfigRepository;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 简历国际化配置表 服务实现类
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-11-16
 */
@Service
public class CvLocaleConfigRepositoryImpl extends ServiceImpl<CvLocaleConfigMapper, CvLocaleConfigPO> implements CvLocaleConfigRepository {

    @Override
    public CvLocaleConfigPO getByFormatMetaId(Long formatMetaId) {
        if(Objects.isNull(formatMetaId)){
            return null;
        }
        LambdaQueryWrapper<CvLocaleConfigPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CvLocaleConfigPO::getFormatMetaId, formatMetaId);

        return baseMapper.selectOne(queryWrapper);
    }
}
