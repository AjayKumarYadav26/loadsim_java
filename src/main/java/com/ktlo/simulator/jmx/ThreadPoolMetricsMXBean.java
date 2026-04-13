package com.ktlo.simulator.jmx;

/**
 * JMX MBean interface for threadpool metrics.
 * Accessible via JConsole at: com.ktlo.simulator:type=ThreadPool,name=AsyncExecutor
 */
public interface ThreadPoolMetricsMXBean {

    /**
     * Get core pool size.
     *
     * @return Core pool size
     */
    int getCorePoolSize();

    /**
     * Get maximum pool size.
     *
     * @return Maximum pool size
     */
    int getMaxPoolSize();

    /**
     * Get current active thread count.
     *
     * @return Active thread count
     */
    int getActiveCount();

    /**
     * Get current pool size (total threads created).
     *
     * @return Pool size
     */
    int getPoolSize();

    /**
     * Get total task count (submitted + queued + completed).
     *
     * @return Total task count
     */
    long getTaskCount();

    /**
     * Get completed task count.
     *
     * @return Completed task count
     */
    long getCompletedTaskCount();

    /**
     * Get current queue size.
     *
     * @return Queue size
     */
    int getQueueSize();

    /**
     * Get remaining queue capacity.
     *
     * @return Remaining capacity
     */
    int getQueueRemainingCapacity();

    /**
     * Check if threadpool is exhausted.
     *
     * @return True if exhausted
     */
    boolean isPoolExhausted();

    /**
     * Get thread utilization percentage.
     *
     * @return Utilization percentage (0-100)
     */
    double getThreadUtilization();

    /**
     * Get queue utilization percentage.
     *
     * @return Queue utilization percentage (0-100)
     */
    double getQueueUtilization();
}
