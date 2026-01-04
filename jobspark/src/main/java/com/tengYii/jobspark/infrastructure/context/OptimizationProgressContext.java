package com.tengYii.jobspark.infrastructure.context;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * 优化进度上下文，用于存储当前线程的进度发送器
 */
public class OptimizationProgressContext {

    /**
     * 使用ThreadLocal存储当前线程的消息消费者（如SseEmitter::send）
     */
    private static final ThreadLocal<Consumer<String>> PROGRESS_EMITTER = new ThreadLocal<>();

    /**
     * 设置当前线程的进度发送器
     *
     * @param emitter 消息消费者
     */
    public static void setEmitter(Consumer<String> emitter) {
        PROGRESS_EMITTER.set(emitter);
    }

    /**
     * 获取当前线程的进度发送器
     *
     * @return 消息消费者
     */
    public static Consumer<String> getEmitter() {
        return PROGRESS_EMITTER.get();
    }

    /**
     * 清除当前线程的进度发送器
     */
    public static void clear() {
        PROGRESS_EMITTER.remove();
    }

    /**
     * 发送进度消息
     *
     * @param message 消息内容
     */
    public static void emit(String message) {
        Consumer<String> emitter = getEmitter();
        if (Objects.nonNull(emitter) && StringUtils.isNotEmpty(message)) {
            emitter.accept(message);
        }
    }
}