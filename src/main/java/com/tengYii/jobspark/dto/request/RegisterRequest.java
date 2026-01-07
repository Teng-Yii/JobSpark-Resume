package com.tengYii.jobspark.dto.request;

import lombok.Data;

/**
 * 用户注册请求request
 *
 * @author tengYii
 * @since 1.0.0
 */
@Data
public class RegisterRequest {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 确认密码
     */
    private String confirmPassword;

    /**
     * 邮箱
     */
    private String email;
}