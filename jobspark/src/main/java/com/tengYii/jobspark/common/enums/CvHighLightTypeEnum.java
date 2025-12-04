package com.tengYii.jobspark.common.enums;

import lombok.Getter;

/**
 * 亮点类型枚举（工作经历、项目经历、专业技能）
 */
@Getter
public enum CvHighLightTypeEnum {

    EXPERIENCE(2, "工作经历"),

    PROJECT(1, "项目经历"),

    SKILL(1, "专业技能"),

    ;

    /**
     * 类型
     */
    private final Integer type;

    /**
     * 说明
     */
    private final String desc;

    CvHighLightTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    /**
     * 根据给定的type获取对应的高亮类型枚举值。
     *
     * @param type 用于匹配枚举值。
     * @return 对应的亮点类型枚举值，若未找到则返回 null。
     */
    public static CvHighLightTypeEnum getByDim(Integer type) {
        for (CvHighLightTypeEnum highLightTypeEnum : CvHighLightTypeEnum.values()) {
            if (highLightTypeEnum.type.equals(type)) {
                return highLightTypeEnum;
            }
        }
        return null;
    }
}
