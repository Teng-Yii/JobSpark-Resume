package com.tengYii.jobspark.common.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 技能分类枚举
 *
 * @author TengYii
 * @since 2025-12-09
 */
@Getter
public enum SkillCategoryEnum {

    /**
     * 编程语言
     */
    PROGRAMMING_LANGUAGE("PROGRAMMING_LANGUAGE", "编程语言"),

    /**
     * 开发框架
     */
    FRAMEWORK("FRAMEWORK", "开发框架"),

    /**
     * 数据库技术
     */
    DATABASE("DATABASE", "数据库技术"),

    /**
     * 中间件技术
     */
    MIDDLEWARE("MIDDLEWARE", "中间件技术"),

    /**
     * 云计算平台
     */
    CLOUD_PLATFORM("CLOUD_PLATFORM", "云计算平台"),

    /**
     * 开发工具
     */
    DEVELOPMENT_TOOL("DEVELOPMENT_TOOL", "开发工具"),

    /**
     * 版本控制
     */
    VERSION_CONTROL("VERSION_CONTROL", "版本控制"),

    /**
     * 移动开发
     */
    MOBILE_DEVELOPMENT("MOBILE_DEVELOPMENT", "移动开发"),

    /**
     * DevOps运维
     */
    DEVOPS("DEVOPS", "DevOps运维"),

    /**
     * 测试技术
     */
    TESTING("TESTING", "测试技术"),

    /**
     * 系统架构
     */
    ARCHITECTURE("ARCHITECTURE", "系统架构"),

    /**
     * 大数据技术
     */
    BIG_DATA("BIG_DATA", "大数据技术"),

    /**
     * 人工智能
     */
    AI_ML("AI_ML", "人工智能"),

    /**
     * 网络安全
     */
    SECURITY("SECURITY", "网络安全"),

    /**
     * 操作系统
     */
    OPERATING_SYSTEM("OPERATING_SYSTEM", "操作系统"),

    /**
     * 容器技术
     */
    CONTAINER("CONTAINER", "容器技术"),

    /**
     * 消息队列
     */
    MESSAGE_QUEUE("MESSAGE_QUEUE", "消息队列"),

    /**
     * 搜索引擎
     */
    SEARCH_ENGINE("SEARCH_ENGINE", "搜索引擎"),

    /**
     * 监控运维
     */
    MONITORING("MONITORING", "监控运维"),

    /**
     * 项目管理
     */
    PROJECT_MANAGEMENT("PROJECT_MANAGEMENT", "项目管理"),

    /**
     * 软技能
     */
    SOFT_SKILL("SOFT_SKILL", "软技能"),

    /**
     * 其他技能
     */
    OTHER("OTHER", "其他技能");

    /**
     * 枚举代码
     */
    private final String code;

    /**
     * 枚举描述
     */
    private final String description;

    /**
     * 构造方法
     *
     * @param code        枚举代码
     * @param description 枚举描述
     */
    SkillCategoryEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据代码获取枚举
     *
     * @param code 枚举代码
     * @return 对应的枚举，如果未找到则返回null
     */
    public static SkillCategoryEnum getByCode(String code) {
        if (StringUtils.isEmpty(code)) {
            return null;
        }

        for (SkillCategoryEnum category : values()) {
            if (Objects.equals(category.getCode(), code)) {
                return category;
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
}
