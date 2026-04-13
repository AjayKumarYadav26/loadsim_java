package com.ktlo.simulator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for async threadpool with limited capacity.
 * This allows us to simulate threadpool exhaustion scenarios.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${async.executor.core-pool-size:10}")
    private int corePoolSize;

    @Value("${async.executor.max-pool-size:20}")
    private int maxPoolSize;

    @Value("${async.executor.queue-capacity:100}")
    private int queueCapacity;

    @Value("${async.executor.thread-name-prefix:ktlo-async-}")
    private String threadNamePrefix;

    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);

        // CallerRunsPolicy: When threadpool is exhausted, the caller thread executes the task
        // This prevents RejectedExecutionException but can slow down the caller
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        return executor;
    }

    /**
     * Separate thread pool for CPU simulation tasks to prevent blocking document processing.
     */
    @Bean(name = "simulationExecutor")
    public ThreadPoolTaskExecutor simulationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);  // More threads for simulation
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("simulation-");

        // AbortPolicy: Reject new tasks when pool is exhausted (prevents blocking callers)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

        executor.setWaitForTasksToCompleteOnShutdown(false);  // Don't wait for simulations
        executor.setAwaitTerminationSeconds(0);
        executor.initialize();

        return executor;
    }
}
