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
     * @deprecated 由于密码加密存储，请勿使用此方法验证密码。请使用 {@link com.baomidou.mybatisplus.extension.service.IService#getOne} 查询用户后手动校验密码。
     */
    @Deprecated
    UserInfoPO getUserInfoByCredentials(String username, String password);

    /**
     * 更新用户信息
     *
     * @param userInfoPO 用户信息实体
     */
    void updateUserInfo(UserInfoPO userInfoPO);
}
