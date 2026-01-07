package com.tengYii.jobspark.dto.request;

import lombok.Data;

/**
 * 忘记密码请求request
 *
 * @author tengYii
 * @since 1.0.0
 */
@Data
public class ForgetPasswordRequest {

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 验证码
     */
    private String code;

    /**
     * 新密码
     */
    private String newPassword;
}