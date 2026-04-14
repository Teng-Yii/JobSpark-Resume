package com.tengYii.jobspark.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Agent执行状态枚举
 *
 * @author Teng-Yii
 * @since 2026-04-14
 */
@Getter
@AllArgsConstructor
public enum AgentExecutionStatusEnum {

    RUNNING("RUNNING", "执行中"),
    SUCCESS("SUCCESS", "执行成功"),
    FAILED("FAILED", "执行失败");

    private final String code;
    private final String desc;

    public static AgentExecutionStatusEnum fromCode(String code) {
        for (AgentExecutionStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
