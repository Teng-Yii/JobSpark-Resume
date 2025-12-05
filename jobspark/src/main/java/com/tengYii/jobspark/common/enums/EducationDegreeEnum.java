package com.tengYii.jobspark.common.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 学历枚举
 */
@Getter
public enum EducationDegreeEnum {

    /**
     * 博士
     */
    DOCTORAL("博士", "博士研究生", 1),

    /**
     * 硕士
     */
    MASTER("硕士", "硕士研究生", 2),

    /**
     * 本科
     */
    BACHELOR("本科", "本科学士", 3),

    /**
     * 专科
     */
    ASSOCIATE("专科", "专科学历", 4),

    /**
     * 高中
     */
    HIGH_SCHOOL("高中", "高中学历", 5),

    /**
     * 中专
     */
    TECHNICAL_SECONDARY("中专", "中等专业学校", 6),

    /**
     * 初中
     */
    JUNIOR_HIGH("初中", "初中学历", 7),

    /**
     * 小学
     */
    PRIMARY("小学", "小学学历", 8),

    /**
     * 其他
     */
    OTHER("其他", "其他学历", 99);

    /**
     * 学历名称
     */
    private final String degree;

    /**
     * 学历描述
     */
    private final String description;

    /**
     * 排序权重（数字越小学历越高）
     */
    private final Integer sortOrder;

    /**
     * 构造函数
     *
     * @param degree      学历名称
     * @param description 学历描述
     * @param sort        排序权重
     */
    EducationDegreeEnum(String degree, String description, Integer sort) {
        this.degree = degree;
        this.description = description;
        this.sortOrder = sort;
    }

    /**
     * 根据学历名称获取枚举值
     *
     * @param degree 学历名称
     * @return 对应的枚举值，如果不存在则返回null
     */
    public static EducationDegreeEnum getByDegree(String degree) {
        if (StringUtils.isEmpty(degree)) {
            return null;
        }

        for (EducationDegreeEnum educationDegree : EducationDegreeEnum.values()) {
            if (StringUtils.equals(educationDegree.getDegree(), degree)) {
                return educationDegree;
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
    public static EducationDegreeEnum getBySort(Integer sort) {
        if (Objects.isNull(sort)) {
            return null;
        }

        for (EducationDegreeEnum educationDegree : EducationDegreeEnum.values()) {
            if (Objects.equals(educationDegree.getSortOrder(), sort)) {
                return educationDegree;
            }
        }
        return null;
    }

    /**
     * 判断是否为高等教育学历（本科及以上）
     *
     * @param educationDegree 学历枚举
     * @return true-高等教育学历，false-非高等教育学历
     */
    public static boolean isHigherEducation(EducationDegreeEnum educationDegree) {
        if (Objects.isNull(educationDegree)) {
            return false;
        }
        return educationDegree == DOCTORAL || educationDegree == MASTER || educationDegree == BACHELOR;
    }

    /**
     * 判断是否为研究生学历（硕士及以上）
     *
     * @param educationDegree 学历枚举
     * @return true-研究生学历，false-非研究生学历
     */
    public static boolean isGraduateEducation(EducationDegreeEnum educationDegree) {
        if (Objects.isNull(educationDegree)) {
            return false;
        }
        return educationDegree == DOCTORAL || educationDegree == MASTER;
    }

    /**
     * 比较两个学历的高低
     *
     * @param degree1 学历1
     * @param degree2 学历2
     * @return 正数表示degree1学历更高，负数表示degree2学历更高，0表示相同
     */
    public static int compareDegree(EducationDegreeEnum degree1, EducationDegreeEnum degree2) {
        if (Objects.isNull(degree1) && Objects.isNull(degree2)) {
            return 0;
        }
        if (Objects.isNull(degree1)) {
            return 1;
        }
        if (Objects.isNull(degree2)) {
            return -1;
        }

        // 排序值越小学历越高，所以用degree2的排序值减去degree1的排序值
        return degree2.getSortOrder() - degree1.getSortOrder();
    }
}
