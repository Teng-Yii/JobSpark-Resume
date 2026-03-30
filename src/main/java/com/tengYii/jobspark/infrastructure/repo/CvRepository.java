package com.tengYii.jobspark.infrastructure.repo;

import com.tengYii.jobspark.model.po.CvPO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 简历基本信息表 服务类
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-11-16
 */
public interface CvRepository extends IService<CvPO> {

    /**
     * 根据用户ID和简历类型获取简历对象列表
     *
     * @param userId   用户ID
     * @param resumeId 简历ID
     * @param cvType   简历类型
     * @return 匹配条件的简历对象列表
     */
    List<CvPO> getCvByCondition(Long userId, Long resumeId, String cvType);
}
