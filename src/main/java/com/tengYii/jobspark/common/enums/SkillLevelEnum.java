package com.tengYii.jobspark.common.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 技能熟练度等级枚举
 *
 * @author TengYii
 * @since 2025-12-09
 */
@Getter
public enum SkillLevelEnum {

    /**
     * 了解 - 基础了解，能在指导下完成简单任务
     */
    BASIC("BASIC", "了解", 1),

    /**
     * 良好 - 有一定经验，能独立完成常规任务
     */
    GOOD("GOOD", "良好", 2),

    /**
     * 熟练 - 经验丰富，能高效解决复杂问题
     */
    PROFICIENT("PROFICIENT", "熟练", 3),

    /**
     * 精通 - 专家级别，能指导他人并优化架构
     */
    EXPERT("EXPERT", "精通", 4);

    /**
     * 枚举代码
     */
    private final String code;

    /**
     * 枚举描述
     */
    private final String description;

    /**
     * 等级权重（用于排序和比较）
     */
    private final Integer weight;

    /**
     * 构造方法
     *
     * @param code        枚举代码
     * @param description 枚举描述
     * @param weight      等级权重
     */
    SkillLevelEnum(String code, String description, Integer weight) {
        this.code = code;
        this.description = description;
        this.weight = weight;
    }

    /**
     * 根据代码获取枚举
     *
     * @param code 枚举代码
     * @return 对应的枚举，如果未找到则返回null
     */
    public static SkillLevelEnum getByCode(String code) {
        if (StringUtils.isEmpty(code)) {
            return null;
        }

        for (SkillLevelEnum level : values()) {
            if (Objects.equals(level.getCode(), code)) {
                return level;
            }
        }
        return null;
    }

    /**
     * 根据描述获取枚举
     *
     * @param description 枚举描述
     * @return 对应的枚举，如果未找到则返回null
     */
    public static SkillLevelEnum getByDescription(String description) {
        if (StringUtils.isEmpty(description)) {
            return null;
        }

        for (SkillLevelEnum level : values()) {
            if (Objects.equals(level.getDescription(), description)) {
                return level;
            }
        }
        return null;
    }

    /**
     * 判断代码是否有效
     *
     * @param code 枚举代码
     * @return 是否有效
     */
    public static boolean isValidCode(String code) {
        return Objects.nonNull(getByCode(code));
    }

    /**
     * 判断当前等级是否高于指定等级
     *
     * @param other 比较的等级
     * @return 是否高于指定等级
     */
    public boolean isHigherThan(SkillLevelEnum other) {
        if (Objects.isNull(other)) {
            return true;
        }
        return this.weight > other.weight;
    }

    /**
     * 判断当前等级是否低于指定等级
     *
     * @param other 比较的等级
     * @return 是否低于指定等级
     */
    public boolean isLowerThan(SkillLevelEnum other) {
        if (Objects.isNull(other)) {
            return false;
        }
        return this.weight < other.weight;
    }
}