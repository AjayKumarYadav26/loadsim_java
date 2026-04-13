package com.ktlo.simulator.service;

import com.ktlo.simulator.repository.LoadSimulatorEntity;
import com.ktlo.simulator.repository.LoadSimulatorRepository;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

/**
 * Service for simulating various database failure scenarios.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseFailureService {

    private final LoadSimulatorRepository repository;

    @Qualifier("timeoutDataSource")
    private final DataSource timeoutDataSource;

    @Qualifier("invalidDataSource")
    private final DataSource invalidDataSource;

    @Qualifier("authFailureDataSource")
    private final DataSource authFailureDataSource;

    @Qualifier("primaryDataSource")
    private final DataSource primaryDataSource;

    /**
     * Trigger database timeout by using a very low connection timeout.
     */
    public String triggerTimeout() {
        log.warn("Triggering database timeout scenario");

        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(timeoutDataSource);

            // Execute a query that will likely timeout
            jdbcTemplate.query("SELECT * FROM loadsimulator LIMIT 10", rs -> {
                // Process results (if we get here)
            });

            return "Query executed successfully (no timeout occurred)";

        } catch (Exception e) {
            log.error("Database timeout occurred as expected: {}", e.getMessage());
            throw new RuntimeException("Database timeout: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a slow query using PostgreSQL pg_sleep().
     *
     * @param delaySeconds Number of seconds to delay
     */
    public String executeSlowQuery(int delaySeconds) {
        log.warn("Executing slow query with {}s delay", delaySeconds);

        try {
            List<LoadSimulatorEntity> results = repository.findAllWithDelay(delaySeconds);
            return String.format("Slow query completed after %ds. Found %d records",
                    delaySeconds, results.size());

        } catch (Exception e) {
            log.error("Slow query failed: {}", e.getMessage());
            throw new RuntimeException("Slow query failed: " + e.getMessage(), e);
        }
    }

    /**
     * Trigger schema mismatch by querying a non-existent table.
     */
    public String triggerSchemaMismatch() {
        log.warn("Triggering schema mismatch scenario");

        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(primaryDataSource);
            jdbcTemplate.queryForList("SELECT * FROM nonexistent_table");

            return "Query executed successfully (unexpected)";

        } catch (Exception e) {
            log.error("Schema mismatch occurred as expected: {}", e.getMessage());
            throw new RuntimeException("Schema mismatch: " + e.getMessage(), e);
        }
    }

    /**
     * Trigger connection failure by using invalid host.
     */
    public String triggerConnectionFailure() {
        log.warn("Triggering connection failure scenario");

        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(invalidDataSource);
            jdbcTemplate.queryForList("SELECT 1");

            return "Connection succeeded (unexpected)";

        } catch (Exception e) {
            log.error("Connection failure occurred as expected: {}", e.getMessage());
            throw new RuntimeException("Connection failure: " + e.getMessage(), e);
        }
    }

    /**
     * Trigger authentication failure by using wrong credentials.
     */
    public String triggerAuthFailure() {
        log.warn("Triggering authentication failure scenario");

        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(authFailureDataSource);
            jdbcTemplate.queryForList("SELECT 1");

            return "Authentication succeeded (unexpected)";

        } catch (Exception e) {
            log.error("Authentication failure occurred as expected: {}", e.getMessage());
            throw new RuntimeException("Authentication failure: " + e.getMessage(), e);
        }
    }

    /**
     * Simulate network partition by closing the connection pool.
     */
    public String simulateNetworkPartition() {
        log.warn("Simulating network partition by closing connection pool");

        try {
            if (primaryDataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) primaryDataSource;
                hikariDataSource.close();
                log.error("Connection pool closed - network partition simulated");
                return "Network partition simulated - connection pool closed";
            } else {
                return "Primary DataSource is not HikariDataSource, cannot simulate";
            }

        } catch (Exception e) {
            log.error("Failed to simulate network partition: {}", e.getMessage());
            throw new RuntimeException("Network partition simulation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test database connectivity.
     */
    public String testConnection() {
        log.info("Testing database connectivity");

        try {
            List<LoadSimulatorEntity> entities = repository.findAll();
            long count = repository.count();

            return String.format("Database connection OK. Found %d records", count);

        } catch (Exception e) {
            log.error("Database connection test failed: {}", e.getMessage());
            throw new RuntimeException("Database connection failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get connection pool statistics from HikariCP.
     */
    public String getConnectionPoolStats() {
        try {
            if (primaryDataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) primaryDataSource;

                int activeConnections = hikariDataSource.getHikariPoolMXBean().getActiveConnections();
                int idleConnections = hikariDataSource.getHikariPoolMXBean().getIdleConnections();
                int totalConnections = hikariDataSource.getHikariPoolMXBean().getTotalConnections();
                int threadsAwaitingConnection = hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection();

                return String.format("Connection Pool Stats - Active: %d, Idle: %d, Total: %d, Waiting: %d",
                        activeConnections, idleConnections, totalConnections, threadsAwaitingConnection);
            } else {
                return "Primary DataSource is not HikariDataSource";
            }

        } catch (Exception e) {
            log.error("Failed to get connection pool stats: {}", e.getMessage());
            return "Failed to get stats: " + e.getMessage();
        }
    }
}
