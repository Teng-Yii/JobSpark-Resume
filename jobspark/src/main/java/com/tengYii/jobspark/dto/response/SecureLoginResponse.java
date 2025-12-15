package com.tengYii.jobspark.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 安全的用户登录响应DTO
 * <p>
 * 基于安全最佳实践设计，仅返回必要的认证信息：
 * <ul>
 *   <li>访问令牌 - 用于后续API调用认证</li>
 *   <li>令牌类型 - 标准Bearer类型</li>
 *   <li>过期时间 - 便于客户端管理token生命周期</li>
 * </ul>
 * <p>
 * 安全考虑：
 * <ul>
 *   <li>不返回用户ID和用户名等敏感信息，避免信息泄露风险</li>
 *   <li>用户信息已包含在JWT token中，客户端可通过解析token获取</li>
 *   <li>如需获取用户详细信息，应通过专门的用户信息接口</li>
 * </ul>
 *
 * @author tengYii
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecureLoginResponse {

    /**
     * 访问令牌
     * <p>
     * JWT格式的访问令牌，包含用户身份信息和权限声明
     */
    private String accessToken;

    /**
     * 令牌类型（固定为Bearer）
     * <p>
     * 遵循OAuth 2.0标准，使用Bearer令牌类型
     */
    private String tokenType = "Bearer";

    /**
     * 令牌过期时间（秒）
     * <p>
     * 从当前时间开始计算的令牌有效期，用于客户端管理token生命周期
     */
    private Long expiresIn;

    /**
     * 构造函数，用于快速创建安全的登录响应对象
     *
     * @param accessToken 访问令牌
     * @param expiresIn   过期时间（秒）
     */
    public SecureLoginResponse(String accessToken, Long expiresIn) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
    }
}