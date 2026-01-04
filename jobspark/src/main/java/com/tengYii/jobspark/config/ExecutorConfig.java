package com.tengYii.jobspark.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 *
 * @author tengYii
 */
@Slf4j
@Configuration
@EnableAsync
public class ExecutorConfig {

    /**
     * 简历处理任务执行器
     * <p>
     * 针对 IO 密集型任务优化（简历解析、OCR、数据库操作均涉及大量 IO 等待）
     *
     * @return 任务执行器
     */
    @Bean("resumeTaskExecutor")
    public Executor resumeTaskExecutor() {
        // 获取 JVM 可用的逻辑核数
        int processors = Runtime.getRuntime().availableProcessors();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心线程数：设置 2 * CPU核数
        // 理由：IO 密集型任务线程常处于等待状态，双倍核数可充分利用 CPU 时间片
        executor.setCorePoolSize(processors * 2);

        // 最大线程数：设置 4 * CPU核数
        // 理由：应对突发上传流量，允许在高峰期创建更多线程来处理积压任务
        executor.setMaxPoolSize(processors * 4);

        // 队列容量：100
        // 理由：作为缓冲区，不宜过大防止内存溢出，也不宜过小导致频繁创建非核心线程
        executor.setQueueCapacity(100);

        // 线程名前缀：方便日志排查
        executor.setThreadNamePrefix("resume-task-");

        // 拒绝策略：由调用线程处理 (CallerRunsPolicy)
        // 理由：当线程池过载时，由提交任务的 Web 线程直接执行，起到一种自然的"背压"（Backpressure）机制，
        // 既减缓了请求提交速度，又保证了核心业务数据（简历）绝不丢失。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 优雅停机：等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 等待时间：150秒
        // 理由：简历解析耗时较长，给足时间让已提交的任务执行完毕
        executor.setAwaitTerminationSeconds(150);

        executor.initialize();

        log.info("简历任务执行器初始化完成，Core: {}, Max: {}", processors * 2, processors * 4);
        return executor;
    }
}
