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
     * 根据简历ID和用户ID获取简历对象
     *
     * @param resumeId 简历ID
     * @param userId   用户ID
     * @return 匹配条件的简历对象
     */
    CvPO getCvByCondition(Long resumeId, Long userId);

    /**
     * 根据用户ID获取简历对象列表
     *
     * @param userId 用户ID
     * @return 匹配条件的简历对象列表
     */
    List<CvPO> getCvByCondition(Long userId);
}
