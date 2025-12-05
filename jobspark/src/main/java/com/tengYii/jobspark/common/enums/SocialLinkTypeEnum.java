package com.tengYii.jobspark.common.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 社交链接类型枚举
 */
@Getter
public enum SocialLinkTypeEnum {

    /**
     * GitHub代码托管平台
     */
    GITHUB("GitHub", "代码托管平台", 1),

    /**
     * Gitee码云平台
     */
    GITEE("Gitee", "码云平台", 2),

    /**
     * CSDN技术博客
     */
    CSDN("CSDN", "技术博客平台", 3),

    /**
     * 博客园
     */
    CNBLOGS("博客园", "技术博客平台", 4),

    /**
     * 掘金技术社区
     */
    JUEJIN("掘金", "技术社区", 5),

    /**
     * 知乎
     */
    ZHIHU("知乎", "知识分享平台", 6),

    /**
     * Stack Overflow
     */
    STACKOVERFLOW("Stack Overflow", "技术问答平台", 7),

    /**
     * LeetCode刷题平台
     */
    LEETCODE("LeetCode", "算法练习平台", 8),

    /**
     * 牛客网
     */
    NOWCODER("牛客网", "求职练习平台", 9),

    /**
     * LinkedIn职业社交
     */
    LINKEDIN("LinkedIn", "职业社交平台", 10),

    /**
     * 个人博客网站
     */
    PERSONAL_BLOG("个人博客", "个人技术博客", 11),

    /**
     * 个人作品集网站
     */
    PORTFOLIO("作品集", "个人作品展示", 12),

    /**
     * 开源中国
     */
    OSCHINA("开源中国", "开源技术社区", 13),

    /**
     * 思否SegmentFault
     */
    SEGMENTFAULT("思否", "技术问答社区", 14),

    /**
     * 简书
     */
    JIANSHU("简书", "写作平台", 15),

    /**
     * 微信公众号
     */
    WECHAT_OFFICIAL("微信公众号", "技术分享平台", 16),

    /**
     * 其他自定义链接
     */
    OTHER("其他", "其他社交链接", 99);

    /**
     * 链接标签名称
     * -- GETTER --
     * 获取链接标签名称
     *
     * @return 链接标签名称
     */
    private final String label;

    /**
     * 链接描述
     * -- GETTER --
     * 获取链接描述
     *
     * @return 链接描述
     */
    private final String description;

    /**
     * 排序权重（数字越小越靠前）
     * -- GETTER --
     * 获取排序权重
     *
     * @return 排序权重
     */
    private final Integer sortOrder;

    /**
     * 构造函数
     *
     * @param label       链接标签名称
     * @param description 链接描述
     * @param sort        排序权重
     */
    SocialLinkTypeEnum(String label, String description, Integer sort) {
        this.label = label;
        this.description = description;
        this.sortOrder = sort;
    }

    /**
     * 根据标签名称获取枚举值
     *
     * @param label 标签名称
     * @return 对应的枚举值，如果不存在则返回null
     */
    public static SocialLinkTypeEnum getByLabel(String label) {
        if (StringUtils.isEmpty(label)) {
            return null;
        }

        for (SocialLinkTypeEnum linkType : SocialLinkTypeEnum.values()) {
            if (StringUtils.equals(linkType.getLabel(), label)) {
                return linkType;
            }
        }
        return null;
    }

    /**
     * 根据排序权重获取枚举值
     *
     * @param sort 排序权重
     * @return 对应的枚举值，如果不存在则返回null
     */
    public static SocialLinkTypeEnum getBySort(Integer sort) {
        if (Objects.isNull(sort)) {
            return null;
        }

        for (SocialLinkTypeEnum linkType : SocialLinkTypeEnum.values()) {
            if (Objects.equals(linkType.getSortOrder(), sort)) {
                return linkType;
            }
        }
        return null;
    }

    /**
     * 判断是否为代码托管平台
     *
     * @param linkType 社交链接类型
     * @return true-是代码托管平台，false-不是
     */
    public static boolean isCodeHosting(SocialLinkTypeEnum linkType) {
        if (Objects.isNull(linkType)) {
            return false;
        }
        return linkType == GITHUB || linkType == GITEE;
    }

    /**
     * 判断是否为技术博客平台
     *
     * @param linkType 社交链接类型
     * @return true-是技术博客平台，false-不是
     */
    public static boolean isTechBlog(SocialLinkTypeEnum linkType) {
        if (Objects.isNull(linkType)) {
            return false;
        }
        return linkType == CSDN || linkType == CNBLOGS || linkType == JUEJIN ||
                linkType == PERSONAL_BLOG || linkType == JIANSHU;
    }
}