package com.ktlo.simulator.jmx;

import com.ktlo.simulator.model.ThreadPoolStatus;
import com.ktlo.simulator.service.ThreadPoolMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * JMX MBean implementation for threadpool metrics.
 */
@Slf4j
@Component
@ManagedResource(
        objectName = "com.ktlo.simulator:type=ThreadPool,name=AsyncExecutor",
        description = "Async ThreadPool Executor Metrics"
)
public class ThreadPoolMetricsMXBeanImpl implements ThreadPoolMetricsMXBean {

    @Autowired
    private ThreadPoolMonitorService threadPoolMonitorService;

    @PostConstruct
    public void init() {
        log.info("ThreadPoolMetricsMXBean initialized and registered");
    }

    @Override
    @ManagedAttribute(description = "Core pool size")
    public int getCorePoolSize() {
        return getStatus().getCorePoolSize();
    }

    @Override
    @ManagedAttribute(description = "Maximum pool size")
    public int getMaxPoolSize() {
        return getStatus().getMaxPoolSize();
    }

    @Override
    @ManagedAttribute(description = "Active thread count")
    public int getActiveCount() {
        return getStatus().getActiveThreads();
    }

    @Override
    @ManagedAttribute(description = "Current pool size")
    public int getPoolSize() {
        return getStatus().getPoolSize();
    }

    @Override
    @ManagedAttribute(description = "Total task count")
    public long getTaskCount() {
        return getStatus().getTotalTasks();
    }

    @Override
    @ManagedAttribute(description = "Completed task count")
    public long getCompletedTaskCount() {
        return getStatus().getCompletedTasks();
    }

    @Override
    @ManagedAttribute(description = "Current queue size")
    public int getQueueSize() {
        return getStatus().getQueuedTasks();
    }

    @Override
    @ManagedAttribute(description = "Remaining queue capacity")
    public int getQueueRemainingCapacity() {
        ThreadPoolStatus status = getStatus();
        return status.getQueueCapacity() - status.getQueuedTasks();
    }

    @Override
    @ManagedAttribute(description = "Is threadpool exhausted")
    public boolean isPoolExhausted() {
        return getStatus().isExhausted();
    }

    @Override
    @ManagedAttribute(description = "Thread utilization percentage")
    public double getThreadUtilization() {
        ThreadPoolStatus status = getStatus();
        if (status.getMaxPoolSize() == 0) {
            return 0.0;
        }
        return (status.getActiveThreads() * 100.0) / status.getMaxPoolSize();
    }

    @Override
    @ManagedAttribute(description = "Queue utilization percentage")
    public double getQueueUtilization() {
        ThreadPoolStatus status = getStatus();
        if (status.getQueueCapacity() == 0) {
            return 0.0;
        }
        return (status.getQueuedTasks() * 100.0) / status.getQueueCapacity();
    }

    /**
     * Get current threadpool status.
     */
    private ThreadPoolStatus getStatus() {
        try {
            return threadPoolMonitorService.getThreadPoolStatus();
        } catch (Exception e) {
            log.error("Failed to get threadpool status", e);
            return ThreadPoolStatus.builder()
                    .corePoolSize(0)
                    .maxPoolSize(0)
                    .activeThreads(0)
                    .poolSize(0)
                    .queuedTasks(0)
                    .queueCapacity(0)
                    .completedTasks(0)
                    .totalTasks(0)
                    .isExhausted(false)
                    .build();
        }
    }
}
