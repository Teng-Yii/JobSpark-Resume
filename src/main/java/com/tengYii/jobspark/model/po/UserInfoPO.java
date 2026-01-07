package com.tengYii.jobspark.model.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 用户基础信息表
 * </p>
 *
 * @author Teng-Yii
 * @since 2025-12-15
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("user_info")
public class UserInfoPO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名（登录账号，必填）
     */
    private String username;

    /**
     * 密码（加密存储，必填）
     */
    private String password;

    /**
     * 手机号（可选，唯一）
     */
    private String phone;

    /**
     * 邮箱（可选，唯一）
     */
    private String email;

    /**
     * 账号状态：1-正常 0-禁用
     */
    private Boolean status;

    /**
     * 逻辑删除：0-未删除 1-已删除
     */
    private Boolean deleteFlag;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
}
