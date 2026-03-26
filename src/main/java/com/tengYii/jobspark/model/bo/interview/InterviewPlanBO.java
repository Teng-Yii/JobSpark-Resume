package com.tengYii.jobspark.model.bo.interview;

import lombok.Data;

import java.util.List;

/**
 * 结构化的面试计划 (用于 Planner 的 Structured Output)
 */
@Data
public class InterviewPlanBO {

    private List<Stage> stages;

    @Data
    public static class Stage {
        private int order;
        private String name; // e.g., "Java 并发编程"
        private List<String> keyTopics; // e.g., ["ThreadLocal", "AQS"]
        private String strategy; // e.g., "由浅入深，结合项目"
    }
}
