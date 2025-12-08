package com.tengYii.jobspark.infrastructure.repo;

import com.tengYii.jobspark.model.po.CvLocaleConfigPO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 简历国际化配置表 服务类
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-11-16
 */
public interface CvLocaleConfigRepository extends IService<CvLocaleConfigPO> {

    /**
     * 根据格式元数据ID查询对应的本地化配置对象。
     *
     * @param formatMetaId 格式元数据ID
     * @return 对应的本地化配置对象
     */
    CvLocaleConfigPO getByFormatMetaId(Long formatMetaId);
}
