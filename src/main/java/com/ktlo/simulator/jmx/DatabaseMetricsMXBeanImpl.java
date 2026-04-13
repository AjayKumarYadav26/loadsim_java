package com.ktlo.simulator.jmx;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JMX MBean implementation for database connection pool metrics.
 */
@Slf4j
@Component
@ManagedResource(
        objectName = "com.ktlo.simulator:type=Database,name=HikariCP",
        description = "HikariCP Database Connection Pool Metrics"
)
public class DatabaseMetricsMXBeanImpl implements DatabaseMetricsMXBean {

    @Autowired
    @Qualifier("primaryDataSource")
    private DataSource primaryDataSource;

    private final AtomicLong totalQueryCount = new AtomicLong(0);
    private final AtomicLong failedQueryCount = new AtomicLong(0);
    private volatile String lastError = "None";

    @PostConstruct
    public void init() {
        log.info("DatabaseMetricsMXBean initialized and registered");
    }

    @Override
    @ManagedAttribute(description = "Active connection count")
    public int getActiveConnections() {
        HikariPoolMXBean poolMXBean = getPoolMXBean();
        return poolMXBean != null ? poolMXBean.getActiveConnections() : -1;
    }

    @Override
    @ManagedAttribute(description = "Idle connection count")
    public int getIdleConnections() {
        HikariPoolMXBean poolMXBean = getPoolMXBean();
        return poolMXBean != null ? poolMXBean.getIdleConnections() : -1;
    }

    @Override
    @ManagedAttribute(description = "Total connection count")
    public int getTotalConnections() {
        HikariPoolMXBean poolMXBean = getPoolMXBean();
        return poolMXBean != null ? poolMXBean.getTotalConnections() : -1;
    }

    @Override
    @ManagedAttribute(description = "Maximum pool size")
    public int getMaxPoolSize() {
        if (primaryDataSource instanceof HikariDataSource) {
            return ((HikariDataSource) primaryDataSource).getMaximumPoolSize();
        }
        return -1;
    }

    @Override
    @ManagedAttribute(description = "Connection timeout in milliseconds")
    public long getConnectionTimeout() {
        if (primaryDataSource instanceof HikariDataSource) {
            return ((HikariDataSource) primaryDataSource).getConnectionTimeout();
        }
        return -1;
    }

    @Override
    @ManagedAttribute(description = "Total query count")
    public long getTotalQueryCount() {
        return totalQueryCount.get();
    }

    @Override
    @ManagedAttribute(description = "Failed query count")
    public long getFailedQueryCount() {
        return failedQueryCount.get();
    }

    @Override
    @ManagedAttribute(description = "Query failure rate percentage")
    public double getQueryFailureRate() {
        long total = totalQueryCount.get();
        if (total == 0) {
            return 0.0;
        }
        return (failedQueryCount.get() * 100.0) / total;
    }

    @Override
    @ManagedAttribute(description = "Database health status")
    public boolean isDatabaseHealthy() {
        try (Connection conn = primaryDataSource.getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            lastError = e.getMessage();
            return false;
        }
    }

    @Override
    @ManagedAttribute(description = "Last error message")
    public String getLastError() {
        return lastError;
    }

    @Override
    @ManagedAttribute(description = "Threads awaiting connection")
    public int getThreadsAwaitingConnection() {
        HikariPoolMXBean poolMXBean = getPoolMXBean();
        return poolMXBean != null ? poolMXBean.getThreadsAwaitingConnection() : -1;
    }

    @Override
    @ManagedOperation(description = "Reset query counters")
    public void resetCounters() {
        totalQueryCount.set(0);
        failedQueryCount.set(0);
        lastError = "None";
        log.info("Database metrics counters reset");
    }

    /**
     * Increment query counter (should be called by repository/service).
     */
    public void incrementQueryCount() {
        totalQueryCount.incrementAndGet();
    }

    /**
     * Increment failed query counter.
     */
    public void incrementFailedQueryCount(String error) {
        failedQueryCount.incrementAndGet();
        lastError = error;
    }

    /**
     * Get HikariCP pool MXBean.
     */
    private HikariPoolMXBean getPoolMXBean() {
        try {
            if (primaryDataSource instanceof HikariDataSource) {
                return ((HikariDataSource) primaryDataSource).getHikariPoolMXBean();
            }
        } catch (Exception e) {
            log.error("Failed to get HikariPoolMXBean", e);
        }
        return null;
    }
}
