package com.tengYii.jobspark.common.enums;

import lombok.Getter;

import java.util.Objects;

/**
 * 逻辑删除标志枚举
 */
@Getter
public enum DeleteFlagEnum {

    /**
     * 未删除
     */
    NOT_DELETED(0, "未删除"),

    /**
     * 已删除
     */
    DELETED(1, "已删除");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String description;

    /**
     * 构造函数
     *
     * @param code        状态码
     * @param description 状态描述
     */
    DeleteFlagEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }


    /**
     * 根据状态码获取枚举值
     *
     * @param code 状态码
     * @return 对应的枚举值，如果不存在则返回null
     */
    public static DeleteFlagEnum getByCode(Integer code) {
        if (Objects.isNull(code)) {
            return null;
        }

        for (DeleteFlagEnum deleteFlag : DeleteFlagEnum.values()) {
            if (Objects.equals(deleteFlag.getCode(), code)) {
                return deleteFlag;
            }
        }
        return null;
    }

    /**
     * 判断是否为已删除状态
     *
     * @param code 状态码
     * @return true-已删除，false-未删除
     */
    public static boolean isDeleted(Integer code) {
        return Objects.equals(DELETED.getCode(), code);
    }

    /**
     * 判断是否为未删除状态
     *
     * @param code 状态码
     * @return true-未删除，false-已删除
     */
    public static boolean isNotDeleted(Integer code) {
        return Objects.equals(NOT_DELETED.getCode(), code);
    }
}
