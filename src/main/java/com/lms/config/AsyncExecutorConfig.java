package com.lms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

/**
 * Configuration for async task execution and scheduling.
 * Prevents thread starvation and clock leap issues by providing proper thread pool sizing.
 */
@Configuration
@Slf4j
public class AsyncExecutorConfig {

    /**
     * Primary async executor for @Async annotated methods.
     * Uses core threads equal to available processors and max threads of 2x processors.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(2, Runtime.getRuntime().availableProcessors()));
        executor.setMaxPoolSize(Math.max(4, Runtime.getRuntime().availableProcessors() * 2));
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("lms-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("TaskExecutor initialized with corePoolSize={}, maxPoolSize={}",
            executor.getCorePoolSize(), executor.getMaxPoolSize());
        return executor;
    }

    /**
     * Task scheduler for scheduled tasks (@Scheduled annotated methods).
     * Uses a separate thread pool to avoid conflicts with async executor.
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(Math.max(2, Runtime.getRuntime().availableProcessors()));
        scheduler.setThreadNamePrefix("lms-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.initialize();
        log.info("TaskScheduler initialized with poolSize={}", scheduler.getPoolSize());
        return scheduler;
    }
}

