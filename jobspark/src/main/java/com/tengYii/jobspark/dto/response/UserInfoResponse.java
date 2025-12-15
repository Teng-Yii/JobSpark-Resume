package com.tengYii.jobspark.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户信息响应DTO
 *
 * @author tengYii
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号（可选，唯一）
     */
    private String phone;

    /**
     * 用户邮箱（可选）
     */
    private String email;
}