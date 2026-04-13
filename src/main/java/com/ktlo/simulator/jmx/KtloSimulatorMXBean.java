package com.ktlo.simulator.jmx;

/**
 * JMX MBean interface for overall application metrics.
 * Accessible via JConsole at: com.ktlo.simulator:type=Application,name=KtloSimulator
 */
public interface KtloSimulatorMXBean {

    /**
     * Get current number of active threads in the async executor.
     *
     * @return Active thread count
     */
    int getActiveThreadCount();

    /**
     * Get current number of queued tasks waiting for execution.
     *
     * @return Queued task count
     */
    int getQueuedTaskCount();

    /**
     * Get total number of HTTP requests processed.
     *
     * @return Total request count
     */
    long getTotalRequestCount();

    /**
     * Get total number of errors logged.
     *
     * @return Error count
     */
    long getErrorCount();

    /**
     * Get current application status.
     *
     * @return Application status (HEALTHY, DEGRADED, DOWN)
     */
    String getApplicationStatus();

    /**
     * Get uptime in milliseconds.
     *
     * @return Uptime in ms
     */
    long getUptimeMillis();

    /**
     * Get uptime as formatted string (e.g., "2h 15m 30s").
     *
     * @return Formatted uptime
     */
    String getUptimeFormatted();

    /**
     * Reset all metrics to zero.
     */
    void resetMetrics();
}
