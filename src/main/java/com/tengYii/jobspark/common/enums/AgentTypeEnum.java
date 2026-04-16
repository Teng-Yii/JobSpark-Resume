package com.tengYii.jobspark.common.enums;

import lombok.Getter;

/**
 * Agent类型枚举
 * 定义系统中所有的Agent类型，用于区分不同Agent的监听策略
 *
 * @author Teng-Yii
 * @since 2026-04-15
 */
@Getter
public enum AgentTypeEnum {

    /**
     * JD对齐Agent - 负责分析职位描述与候选人简历的技能匹配度
     */
    JD_ALIGNMENT("JDAlignmentAgent", "JD对齐Agent", true, true),

    /**
     * 面试协调Agent - 负责制定面试计划
     */
    INTERVIEW_COORDINATOR("InterviewCoordinatorAgent", "面试计划制定Agent", true, true),

    /**
     * Java技术面试官Agent - 负责执行技术面试提问
     */
    JAVA_TECH_INTERVIEWER("JavaTechInterviewerAgent", "Java技术面试官Agent", true, true),

    /**
     * 面试反思Agent - 负责评估候选人回答并决策
     */
    INTERVIEW_REFLECTOR("InterviewReflectorAgent", "面试反思评估Agent", true, true),

    /**
     * 通用Agent - 默认类型，用于未明确分类的Agent
     */
    GENERIC("GenericAgent", "通用Agent", true, false);

    /**
     * Agent类名标识
     */
    private final String agentClassName;

    /**
     * Agent中文名称
     */
    private final String displayName;

    /**
     * 是否启用持久化
     */
    private final boolean persistenceEnabled;

    /**
     * 是否启用详细日志
     */
    private final boolean detailedLogging;

    AgentTypeEnum(String agentClassName, String displayName, 
                  boolean persistenceEnabled, boolean detailedLogging) {
        this.agentClassName = agentClassName;
        this.displayName = displayName;
        this.persistenceEnabled = persistenceEnabled;
        this.detailedLogging = detailedLogging;
    }

    /**
     * 根据Agent类名获取对应的Agent类型
     *
     * @param agentClassName Agent类名
     * @return Agent类型枚举，未找到返回GENERIC
     */
    public static AgentTypeEnum fromAgentClassName(String agentClassName) {
        if (agentClassName == null || agentClassName.isEmpty()) {
            return GENERIC;
        }
        // 提取简单类名（去掉包路径）
        String simpleName = agentClassName;
        int lastDot = agentClassName.lastIndexOf('.');
        if (lastDot > 0) {
            simpleName = agentClassName.substring(lastDot + 1);
        }
        
        for (AgentTypeEnum type : values()) {
            if (type.agentClassName.equalsIgnoreCase(simpleName)) {
                return type;
            }
        }
        return GENERIC;
    }

    /**
     * 根据Agent接口类获取对应的Agent类型
     *
     * @param agentClass Agent接口类
     * @return Agent类型枚举
     */
    public static AgentTypeEnum fromAgentClass(Class<?> agentClass) {
        if (agentClass == null) {
            return GENERIC;
        }
        return fromAgentClassName(agentClass.getSimpleName());
    }
}
