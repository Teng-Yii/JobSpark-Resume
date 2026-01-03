package com.tengYii.jobspark.application.service;

import com.tengYii.jobspark.dto.request.ForgetPasswordRequest;
import com.tengYii.jobspark.dto.request.RegisterRequest;
import com.tengYii.jobspark.dto.response.LoginResponse;
import com.tengYii.jobspark.model.po.UserInfoPO;

/**
 * 认证应用服务接口
 *
 * @author tengYii
 * @since 1.0.0
 */
public interface AuthApplicationService {

    /**
     * 验证用户身份，返回用户ID
     *
     * @param username 用户名
     * @param password 密码
     * @return 用户ID，如果验证失败则返回null
     */
    Long authenticateUser(String username, String password);

    /**
     * 用户登出
     * 将token加入黑名单，使其失效
     *
     * @param token JWT token
     */
    void logout(String token);

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息PO对象
     */
    UserInfoPO getUserInfo(Long userId);

    /**
     * 刷新token
     *
     * @param token 当前的JWT token
     * @return 新的token信息
     */
    LoginResponse refreshToken(String token);

    /**
     * 用户注册
     *
     * @param registerRequest 注册请求参数
     */
    void register(RegisterRequest registerRequest);

    /**
     * 忘记密码
     *
     * @param forgetPasswordRequest 忘记密码请求参数
     */
    void forgetPassword(ForgetPasswordRequest forgetPasswordRequest);
}
