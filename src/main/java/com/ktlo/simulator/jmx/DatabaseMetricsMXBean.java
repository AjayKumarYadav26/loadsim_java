package com.ktlo.simulator.jmx;

/**
 * JMX MBean interface for database connection pool metrics.
 * Accessible via JConsole at: com.ktlo.simulator:type=Database,name=HikariCP
 */
public interface DatabaseMetricsMXBean {

    /**
     * Get active connection count.
     *
     * @return Active connections
     */
    int getActiveConnections();

    /**
     * Get idle connection count.
     *
     * @return Idle connections
     */
    int getIdleConnections();

    /**
     * Get total connection count.
     *
     * @return Total connections
     */
    int getTotalConnections();

    /**
     * Get maximum pool size.
     *
     * @return Max pool size
     */
    int getMaxPoolSize();

    /**
     * Get connection timeout in milliseconds.
     *
     * @return Connection timeout
     */
    long getConnectionTimeout();

    /**
     * Get total query count.
     *
     * @return Total queries executed
     */
    long getTotalQueryCount();

    /**
     * Get failed query count.
     *
     * @return Failed query count
     */
    long getFailedQueryCount();

    /**
     * Get query failure rate percentage.
     *
     * @return Failure rate (0-100)
     */
    double getQueryFailureRate();

    /**
     * Check if database is healthy.
     *
     * @return True if healthy
     */
    boolean isDatabaseHealthy();

    /**
     * Get last error message.
     *
     * @return Last error or "None"
     */
    String getLastError();

    /**
     * Get number of threads awaiting connection.
     *
     * @return Threads waiting
     */
    int getThreadsAwaitingConnection();

    /**
     * Reset query counters.
     */
    void resetCounters();
}
