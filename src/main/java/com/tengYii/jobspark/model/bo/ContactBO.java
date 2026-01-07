package com.tengYii.jobspark.model.bo;

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
public class ContactBO {

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */// 手机
    private String email;

    /**
     * 微信号（可选）
     */
    private String wechat;

    /**
     * 所在地（可选）
     */
    private String location;
}