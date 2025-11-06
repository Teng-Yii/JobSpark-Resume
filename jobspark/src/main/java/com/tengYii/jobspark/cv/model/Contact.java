package com.tengYii.jobspark.cv.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 联系方式：至少需要一种可用联系方式。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contact {
    private String phone;        // 手机
    private String email;        // 邮箱
    private String wechat;       // 微信（可选）
    private String location;     // 所在地（可选）
}