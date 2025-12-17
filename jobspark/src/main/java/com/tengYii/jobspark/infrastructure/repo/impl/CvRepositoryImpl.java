package com.tengYii.jobspark.infrastructure.repo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tengYii.jobspark.model.po.CvPO;
import com.tengYii.jobspark.infrastructure.mapper.CvMapper;
import com.tengYii.jobspark.infrastructure.repo.CvRepository;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.Objects;

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

    /**
     * 根据简历ID和用户ID获取简历对象
     *
     * @param resumeId 简历ID
     * @param userId   用户ID
     * @return 匹配条件的简历对象
     */
    @Override
    public CvPO getCvByCondition(Long resumeId, Long userId) {

        if(Objects.isNull(resumeId) || Objects.isNull(userId)) {
            return null;
        }

        LambdaQueryWrapper<CvPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CvPO::getId, resumeId);
        queryWrapper.eq(CvPO::getUserId, userId);
        return this.getOne(queryWrapper);
    }
}
