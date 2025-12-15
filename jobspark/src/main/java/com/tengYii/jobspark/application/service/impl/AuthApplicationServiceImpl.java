package com.tengYii.jobspark.application.service.impl;

import com.tengYii.jobspark.application.service.AuthApplicationService;
import com.tengYii.jobspark.common.utils.login.JwtTokenUtil;
import com.tengYii.jobspark.dto.response.LoginResponse;
import com.tengYii.jobspark.infrastructure.repo.UserInfoRepository;
import com.tengYii.jobspark.model.po.UserInfoPO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 认证应用服务实现类
 *
 * @author tengYii
 * @since 1.0.0
 */
@Slf4j
@Service
public class AuthApplicationServiceImpl implements AuthApplicationService {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserInfoRepository userInfoRepository;

    /**
     * JWT访问令牌过期时间（秒）
     */
    @Value("${jwt.access-token-expiration:7200}")
    private Long accessTokenExpiration;

    /**
     * 验证用户身份，返回用户ID
     *
     * @param username 用户名
     * @param password 密码
     * @return 用户ID，如果验证失败则返回null
     */
    @Override
    public Long authenticateUser(String username, String password) {

        // 参数校验
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return null;
        }

        // 查询用户数据
        UserInfoPO userInfoPO = userInfoRepository.getUserInfoByCredentials(username, password);

        if (Objects.nonNull(userInfoPO)) {
            // 登录成功，更新最近登录时间
            userInfoPO.setLastLoginTime(LocalDateTime.now());
            userInfoRepository.updateUserInfo(userInfoPO);
            return userInfoPO.getId();
        }
        return null;
    }

    /**
     * 用户登出
     * 将token加入黑名单，使其失效
     *
     * @param token JWT token
     */
    @Override
    public void logout(String token) {

        // 参数校验
        if (StringUtils.isEmpty(token)) {
            return;
        }

        try {
            // 将token加入黑名单
            // 设置黑名单保留时间为token的剩余有效期
            jwtTokenUtil.addTokenToBlacklist(token, accessTokenExpiration);

        } catch (Exception e) {
            // 如果token无效，忽略异常
            // 登出操作应该是幂等的
        }
    }

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息PO对象
     */
    @Override
    public UserInfoPO getUserInfo(Long userId) {
        // 参数校验
        if (Objects.isNull(userId)) {
            return null;
        }

        // 根据用户ID获取用户信息
        return userInfoRepository.getById(userId);
    }

    /**
     * 刷新token
     *
     * @param token 当前的JWT token
     * @return 新的token信息
     */
    @Override
    public LoginResponse refreshToken(String token) {
        // 参数校验
        if (StringUtils.isEmpty(token)) {
            return null;
        }

        Long userId = null;
        try {
            // 验证当前token是否有效
            Boolean isValid = jwtTokenUtil.validateToken(token);
            if (Objects.isNull(isValid) || !isValid) {
                return null;
            }

            // 从token中获取用户ID
            userId = jwtTokenUtil.getUserIdFromToken(token);
            if (Objects.isNull(userId)) {
                return null;
            }

            // 查询用户信息
            UserInfoPO userInfoPO = userInfoRepository.getById(userId);
            if (Objects.isNull(userInfoPO)) {
                return null;
            }

            // 将旧token加入黑名单
            jwtTokenUtil.addTokenToBlacklist(token, accessTokenExpiration);

            // 生成新token
            String username = userInfoPO.getUsername();
            String newToken = jwtTokenUtil.generateAccessToken(userId, username);

            // 构造并返回新的登录响应
            return new LoginResponse(newToken, accessTokenExpiration, userId, username);

        } catch (Exception e) {
            // 异常日志输出，包含userId和token
            log.error("刷新token失败，userId: {}, token: {}, 异常信息: {}", userId, token, e.getMessage());
            return null;
        }
    }
}