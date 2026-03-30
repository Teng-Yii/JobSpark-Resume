package com.tengYii.jobspark.infrastructure.repo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tengYii.jobspark.common.enums.DeleteFlagEnum;
import com.tengYii.jobspark.model.po.CvPO;
import com.tengYii.jobspark.infrastructure.mapper.CvMapper;
import com.tengYii.jobspark.infrastructure.repo.CvRepository;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
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
     * 根据用户ID和简历类型获取简历对象列表
     *
     * @param userId   用户ID
     * @param resumeId 简历ID
     * @param cvType   简历类型
     * @return 匹配条件的简历对象列表
     */
    @Override
    public List<CvPO> getCvByCondition(Long userId, Long resumeId, String cvType) {
        // 查询用户未删除的简历列表，按cvType过滤
        LambdaQueryWrapper<CvPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CvPO::getUserId, userId)
                .eq(Objects.nonNull(resumeId), CvPO::getId, resumeId)
                .eq(StringUtils.isNotEmpty(cvType), CvPO::getCvType, cvType)
                .eq(CvPO::getDeleteFlag, DeleteFlagEnum.NOT_DELETED.getCode())
                .orderByDesc(CvPO::getUpdatedTime);

        return this.list(queryWrapper);
    }
}
