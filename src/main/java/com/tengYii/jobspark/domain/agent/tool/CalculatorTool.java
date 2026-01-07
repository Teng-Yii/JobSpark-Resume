package com.tengYii.jobspark.domain.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 示例工具类：计算器
 * <p>
 * 提供基本的数学计算功能，供Agent调用。
 * </p>
 */
@Slf4j
@Component
public class CalculatorTool {

    /**
     * 计算两个数字的和
     *
     * @param a 第一个数字
     * @param b 第二个数字
     * @return 两个数字的和
     */
    @Tool("计算两个数字的和")
    public double add(double a, double b) {
        log.info("CalculatorTool [add] method called with args: a={}, b={}", a, b);
        return a + b;
    }

    /**
     * 计算两个数字的积
     *
     * @param a 第一个数字
     * @param b 第二个数字
     * @return 两个数字的积
     */
    @Tool("计算两个数字的积")
    public double multiply(double a, double b) {
        log.info("CalculatorTool [multiply] method called with args: a={}, b={}", a, b);
        return a * b;
    }
}