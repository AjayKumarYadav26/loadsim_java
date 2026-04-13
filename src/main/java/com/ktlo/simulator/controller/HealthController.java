package com.ktlo.simulator.controller;

import com.ktlo.simulator.model.ThreadPoolStatus;
import com.ktlo.simulator.service.ThreadPoolMonitorService;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Health monitoring controller for application health checks.
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@Tag(name = "Health & Monitoring", description = "APIs for monitoring application health, threadpool metrics, and database status")
public class HealthController {

    @Autowired
    private ThreadPoolMonitorService threadPoolMonitorService;

    @Autowired
    @Qualifier("primaryDataSource")
    private DataSource primaryDataSource;

    /**
     * Overall application health status.
     *
     * @return Health status
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Check threadpool health
            ThreadPoolStatus threadPoolStatus = threadPoolMonitorService.getThreadPoolStatus();
            boolean threadPoolHealthy = !threadPoolStatus.isExhausted();

            // Check database health
            boolean databaseHealthy = isDatabaseHealthy();

            // Overall health
            boolean healthy = threadPoolHealthy && databaseHealthy;

            health.put("status", healthy ? "UP" : "DOWN");
            health.put("timestamp", System.currentTimeMillis());
            health.put("components", Map.of(
                    "threadpool", threadPoolHealthy ? "UP" : "DOWN",
                    "database", databaseHealthy ? "UP" : "DOWN"
            ));

            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }

    /**
     * Threadpool health and metrics.
     *
     * @return Threadpool status
     */
    @GetMapping("/threadpool")
    public ResponseEntity<Map<String, Object>> getThreadPoolHealth() {
        try {
            ThreadPoolStatus status = threadPoolMonitorService.getThreadPoolStatus();

            Map<String, Object> response = new HashMap<>();
            response.put("status", status.isExhausted() ? "EXHAUSTED" : "HEALTHY");
            response.put("timestamp", System.currentTimeMillis());
            response.put("metrics", Map.of(
                    "activeThreads", status.getActiveThreads(),
                    "corePoolSize", status.getCorePoolSize(),
                    "maxPoolSize", status.getMaxPoolSize(),
                    "poolSize", status.getPoolSize(),
                    "queuedTasks", status.getQueuedTasks(),
                    "queueCapacity", status.getQueueCapacity(),
                    "completedTasks", status.getCompletedTasks(),
                    "totalTasks", status.getTotalTasks(),
                    "isExhausted", status.isExhausted(),
                    "utilizationPercent", String.format("%.2f%%",
                            (status.getActiveThreads() * 100.0) / status.getMaxPoolSize())
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Threadpool health check failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("error", e.getMessage());
            return ResponseEntity.status(503).body(error);
        }
    }

    /**
     * Database health and connection pool metrics.
     *
     * @return Database health status
     */
    @GetMapping("/database")
    public ResponseEntity<Map<String, Object>> getDatabaseHealth() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Test database connection
            boolean connectionOk = testDatabaseConnection();

            // Get HikariCP pool metrics
            Map<String, Object> poolMetrics = getConnectionPoolMetrics();

            response.put("status", connectionOk ? "HEALTHY" : "UNHEALTHY");
            response.put("timestamp", System.currentTimeMillis());
            response.put("connectionTest", connectionOk ? "SUCCESS" : "FAILED");
            response.put("poolMetrics", poolMetrics);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Database health check failed", e);
            response.put("status", "ERROR");
            response.put("error", e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Test database connection.
     */
    private boolean testDatabaseConnection() {
        try (Connection conn = primaryDataSource.getConnection()) {
            return conn.isValid(5); // 5 second timeout
        } catch (Exception e) {
            log.error("Database connection test failed", e);
            return false;
        }
    }

    /**
     * Check if database is healthy.
     */
    private boolean isDatabaseHealthy() {
        try {
            return testDatabaseConnection();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get HikariCP connection pool metrics.
     */
    private Map<String, Object> getConnectionPoolMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            if (primaryDataSource instanceof HikariDataSource) {
                HikariDataSource hikariDS = (HikariDataSource) primaryDataSource;
                HikariPoolMXBean poolMXBean = hikariDS.getHikariPoolMXBean();

                if (poolMXBean != null) {
                    metrics.put("activeConnections", poolMXBean.getActiveConnections());
                    metrics.put("idleConnections", poolMXBean.getIdleConnections());
                    metrics.put("totalConnections", poolMXBean.getTotalConnections());
                    metrics.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());
                } else {
                    metrics.put("poolName", hikariDS.getPoolName());
                    metrics.put("maxPoolSize", hikariDS.getMaximumPoolSize());
                    metrics.put("note", "Pool MXBean not available");
                }
            } else {
                metrics.put("type", primaryDataSource.getClass().getSimpleName());
                metrics.put("note", "Not a HikariCP datasource");
            }
        } catch (Exception e) {
            log.error("Failed to get connection pool metrics", e);
            metrics.put("error", e.getMessage());
        }

        return metrics;
    }
}
