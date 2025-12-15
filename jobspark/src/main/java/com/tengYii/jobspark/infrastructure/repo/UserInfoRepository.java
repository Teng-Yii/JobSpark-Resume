package com.tengYii.jobspark.infrastructure.repo;

import com.tengYii.jobspark.model.po.UserInfoPO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 用户基础信息表 服务类
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-12-15
 */
public interface UserInfoRepository extends IService<UserInfoPO> {


    /**
     * 根据用户名和密码获取用户信息。
     *
     * @param username 用户名
     * @param password 密码
     * @return 用户信息PO对象
     */
    UserInfoPO getUserInfoByCredentials(String username, String password);

    /**
     * 更新用户信息
     *
     * @param userInfoPO 用户信息实体
     */
    void updateUserInfo(UserInfoPO userInfoPO);
}
