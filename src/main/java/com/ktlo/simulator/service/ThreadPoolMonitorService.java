package com.ktlo.simulator.service;

import com.ktlo.simulator.model.ThreadPoolStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Service for monitoring threadpool status and metrics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThreadPoolMonitorService {

    private final ThreadPoolTaskExecutor taskExecutor;

    /**
     * Get current threadpool status with all metrics.
     *
     * @return ThreadPoolStatus with current metrics
     */
    public ThreadPoolStatus getThreadPoolStatus() {
        ThreadPoolExecutor executor = taskExecutor.getThreadPoolExecutor();

        int activeCount = executor.getActiveCount();
        int poolSize = executor.getPoolSize();
        int maxPoolSize = executor.getMaximumPoolSize();
        int corePoolSize = executor.getCorePoolSize();
        long completedTaskCount = executor.getCompletedTaskCount();
        long totalTaskCount = executor.getTaskCount();
        int queueSize = executor.getQueue().size();
        int queueCapacity = executor.getQueue().size() + executor.getQueue().remainingCapacity();
        int queueRemainingCapacity = executor.getQueue().remainingCapacity();

        // Calculate utilization percentage
        double threadUtilization = maxPoolSize > 0 ? (activeCount * 100.0 / maxPoolSize) : 0;

        // Determine if pool is exhausted (all threads active and queue full)
        boolean isExhausted = (activeCount >= maxPoolSize && queueRemainingCapacity == 0);

        return ThreadPoolStatus.builder()
                .activeThreads(activeCount)
                .poolSize(poolSize)
                .maxPoolSize(maxPoolSize)
                .corePoolSize(corePoolSize)
                .completedTasks(completedTaskCount)
                .totalTasks(totalTaskCount)
                .queuedTasks(queueSize)
                .queueCapacity(queueCapacity)
                .queueRemainingCapacity(queueRemainingCapacity)
                .isExhausted(isExhausted)
                .threadUtilization(threadUtilization)
                .build();
    }

    /**
     * Periodic monitoring task that logs threadpool status.
     * Runs every 30 seconds.
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void monitorThreadPool() {
        ThreadPoolStatus status = getThreadPoolStatus();

        if (status.getActiveThreads() > 0 || status.getQueuedTasks() > 0) {
            log.info("ThreadPool Status - Active: {}/{}, Queue: {}/{}, Completed: {}, Utilization: {:.1f}%",
                    status.getActiveThreads(),
                    status.getMaxPoolSize(),
                    status.getQueuedTasks(),
                    status.getQueueCapacity(),
                    status.getCompletedTasks(),
                    status.getThreadUtilization());

            if (status.isExhausted()) {
                log.warn("ThreadPool is EXHAUSTED! All threads busy and queue is full!");
            }
        }
    }
}
