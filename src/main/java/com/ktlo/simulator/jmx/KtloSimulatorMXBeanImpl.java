package com.ktlo.simulator.jmx;

import com.ktlo.simulator.model.ThreadPoolStatus;
import com.ktlo.simulator.service.ThreadPoolMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JMX MBean implementation for overall application metrics.
 */
@Slf4j
@Component
@ManagedResource(
        objectName = "com.ktlo.simulator:type=Application,name=KtloSimulator",
        description = "KTLO Simulator Application Metrics"
)
public class KtloSimulatorMXBeanImpl implements KtloSimulatorMXBean {

    @Autowired
    private ThreadPoolMonitorService threadPoolMonitorService;

    @Autowired
    private DataSource primaryDataSource;

    private final AtomicLong totalRequestCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private long startTimeMillis;

    @PostConstruct
    public void init() {
        startTimeMillis = System.currentTimeMillis();
        log.info("KtloSimulatorMXBean initialized and registered");
    }

    @Override
    @ManagedAttribute(description = "Active thread count")
    public int getActiveThreadCount() {
        try {
            ThreadPoolStatus status = threadPoolMonitorService.getThreadPoolStatus();
            return status.getActiveThreads();
        } catch (Exception e) {
            log.error("Failed to get active thread count", e);
            return -1;
        }
    }

    @Override
    @ManagedAttribute(description = "Queued task count")
    public int getQueuedTaskCount() {
        try {
            ThreadPoolStatus status = threadPoolMonitorService.getThreadPoolStatus();
            return status.getQueuedTasks();
        } catch (Exception e) {
            log.error("Failed to get queued task count", e);
            return -1;
        }
    }

    @Override
    @ManagedAttribute(description = "Total HTTP request count")
    public long getTotalRequestCount() {
        return totalRequestCount.get();
    }

    @Override
    @ManagedAttribute(description = "Total error count")
    public long getErrorCount() {
        return errorCount.get();
    }

    @Override
    @ManagedAttribute(description = "Application status")
    public String getApplicationStatus() {
        try {
            // Check threadpool
            ThreadPoolStatus threadPoolStatus = threadPoolMonitorService.getThreadPoolStatus();
            boolean threadPoolHealthy = !threadPoolStatus.isExhausted();

            // Check database
            boolean databaseHealthy = isDatabaseHealthy();

            if (threadPoolHealthy && databaseHealthy) {
                return "HEALTHY";
            } else if (threadPoolHealthy || databaseHealthy) {
                return "DEGRADED";
            } else {
                return "DOWN";
            }
        } catch (Exception e) {
            log.error("Failed to get application status", e);
            return "ERROR";
        }
    }

    @Override
    @ManagedAttribute(description = "Application uptime in milliseconds")
    public long getUptimeMillis() {
        return System.currentTimeMillis() - startTimeMillis;
    }

    @Override
    @ManagedAttribute(description = "Application uptime formatted")
    public String getUptimeFormatted() {
        long uptimeMs = getUptimeMillis();
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours % 24, minutes % 60, seconds % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    @Override
    @ManagedOperation(description = "Reset all metrics to zero")
    public void resetMetrics() {
        totalRequestCount.set(0);
        errorCount.set(0);
        log.info("All metrics reset to zero");
    }

    /**
     * Increment request counter (called by filter or interceptor).
     */
    public void incrementRequestCount() {
        totalRequestCount.incrementAndGet();
    }

    /**
     * Increment error counter (called by exception handler).
     */
    public void incrementErrorCount() {
        errorCount.incrementAndGet();
    }

    /**
     * Check database health.
     */
    private boolean isDatabaseHealthy() {
        try (Connection conn = primaryDataSource.getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}
