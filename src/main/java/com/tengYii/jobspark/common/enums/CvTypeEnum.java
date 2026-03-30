package com.tengYii.jobspark.common.enums;

import lombok.Getter;

/**
 * 简历类型枚举
 * upload-用户上传简历（用于查询/解析/优化），excellent-优秀参考简历（用于简历优化参考）
 */
@Getter
public enum CvTypeEnum {

    UPLOAD("upload", "用户上传简历"),

    EXCELLENT("excellent", "优秀参考简历"),

    ;

    /**
     * 类型
     */
    private final String type;

    /**
     * 说明
     */
    private final String desc;

    CvTypeEnum(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    /**
     * 根据给定的type获取对应的高亮类型枚举值。
     *
     * @param type 用于匹配枚举值。
     * @return 对应的亮点类型枚举值，若未找到则返回 null。
     */
    public static CvTypeEnum getByType(String type) {
        for (CvTypeEnum cvTypeEnum : CvTypeEnum.values()) {
            if (cvTypeEnum.type.equals(type)) {
                return cvTypeEnum;
            }
        }
        return null;
    }
}
