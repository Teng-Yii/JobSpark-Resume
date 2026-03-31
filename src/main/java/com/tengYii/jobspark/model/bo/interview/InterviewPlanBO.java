package com.tengYii.jobspark.model.bo.interview;

import lombok.Data;

import java.util.List;

/**
 * 结构化的面试计划业务对象
 * 用于 Planner 的结构化输出，包含多个面试阶段及每个阶段的关键主题和策略
 */
@Data
public class InterviewPlanBO {

    /**
     * 面试阶段列表
     * 按顺序包含所有面试阶段，每个阶段包含主题和考察策略
     */
    private List<Stage> stages;

    /**
     * 面试阶段内部类
     * 定义单个面试阶段的结构，包括顺序、名称、关键主题和考察策略
     */
    @Data
    public static class Stage {

        /**
         * 阶段顺序编号
         * 从1开始递增，用于确定面试阶段的执行顺序
         */
        private int order;

        /**
         * 阶段名称
         * 例如："Java 并发编程"、"系统设计"、"项目经验"等
         */
        private String name;

        /**
         * 关键主题列表
         * 该阶段需要考察的核心知识点，例如：["ThreadLocal", "AQS", "CAS"]
         */
        private List<String> keyTopics;

        /**
         * 考察策略
         * 描述该阶段的提问策略，例如："由浅入深，结合项目"、"场景驱动"等
         */
        private String strategy;
    }
}
