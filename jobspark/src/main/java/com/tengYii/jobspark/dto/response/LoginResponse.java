package com.tengYii.jobspark.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 令牌类型（通常为Bearer）
     */
    private String tokenType = "Bearer";

    /**
     * 令牌过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 构造函数，用于快速创建响应对象
     *
     * @param accessToken 访问令牌
     * @param expiresIn   过期时间
     * @param userId      用户ID
     * @param username    用户名
     */
    public LoginResponse(String accessToken, Long expiresIn, Long userId, String username) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.userId = userId;
        this.username = username;
    }
}