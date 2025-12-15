package com.tengYii.jobspark.infrastructure.repo.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tengYii.jobspark.model.po.UserInfoPO;
import com.tengYii.jobspark.infrastructure.mapper.UserInfoMapper;
import com.tengYii.jobspark.infrastructure.repo.UserInfoRepository;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * <p>
 * 用户基础信息表 服务实现类
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-12-15
 */
@Service
public class UserInfoRepositoryImpl extends ServiceImpl<UserInfoMapper, UserInfoPO> implements UserInfoRepository {

    /**
     * 根据用户名和密码获取用户信息。
     *
     * @param username 用户名
     * @param password 密码
     * @return 用户信息PO对象
     */
    @Override
    public UserInfoPO getUserInfoByCredentials(String username, String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return null;
        }

        LambdaQueryWrapper<UserInfoPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfoPO::getUsername, username);
        queryWrapper.eq(UserInfoPO::getPassword, password);

        return this.getOne(queryWrapper);
    }
    /**
     * 更新用户信息
     *
     * @param userInfoPO 用户信息实体
     */
    @Override
    public void updateUserInfo(UserInfoPO userInfoPO) {
        if (Objects.isNull(userInfoPO) || Objects.isNull(userInfoPO.getId())) {
            return;
        }
        // 使用MyBatis-Plus的updateById方法进行更新
        this.updateById(userInfoPO);
    }
}
