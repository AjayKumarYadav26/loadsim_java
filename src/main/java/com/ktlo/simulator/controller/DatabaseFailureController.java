package com.ktlo.simulator.controller;

import com.ktlo.simulator.model.SimulationResponse;
import com.ktlo.simulator.service.DatabaseFailureService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for database failure simulation.
 */
@Slf4j
@RestController
@RequestMapping("/api/db")
@RequiredArgsConstructor
@Tag(name = "Database Failure Simulation", description = "APIs for simulating various database failure scenarios (timeouts, connection failures, schema errors)")
public class DatabaseFailureController {

    private final DatabaseFailureService databaseFailureService;

    /**
     * Trigger database timeout scenario.
     * POST /api/db/timeout
     */
    @PostMapping("/timeout")
    public ResponseEntity<SimulationResponse> triggerTimeout() {
        log.info("DB timeout scenario requested");

        try {
            String result = databaseFailureService.triggerTimeout();
            return ResponseEntity.ok(SimulationResponse.success(result));

        } catch (Exception e) {
            log.error("DB timeout scenario failed", e);
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(SimulationResponse.failure("Database timeout: " + e.getMessage()));
        }
    }

    /**
     * Execute slow query.
     * POST /api/db/slow-query?delay=5
     */
    @PostMapping("/slow-query")
    public ResponseEntity<SimulationResponse> executeSlowQuery(
            @RequestParam(defaultValue = "5") int delay) {

        if (delay < 1 || delay > 60) {
            return ResponseEntity.badRequest()
                    .body(SimulationResponse.failure("Delay must be between 1 and 60 seconds"));
        }

        log.info("Slow query scenario requested: {}s delay", delay);

        try {
            String result = databaseFailureService.executeSlowQuery(delay);
            return ResponseEntity.ok(SimulationResponse.success(result)
                    .addDetail("delay", delay));

        } catch (Exception e) {
            log.error("Slow query scenario failed", e);
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body(SimulationResponse.failure("Slow query failed: " + e.getMessage()));
        }
    }

    /**
     * Trigger schema mismatch error.
     * POST /api/db/schema-mismatch
     */
    @PostMapping("/schema-mismatch")
    public ResponseEntity<SimulationResponse> triggerSchemaMismatch() {
        log.info("Schema mismatch scenario requested");

        try {
            String result = databaseFailureService.triggerSchemaMismatch();
            return ResponseEntity.ok(SimulationResponse.success(result));

        } catch (Exception e) {
            log.error("Schema mismatch scenario succeeded", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SimulationResponse.failure("Schema mismatch: " + e.getMessage()));
        }
    }

    /**
     * Trigger connection failure.
     * POST /api/db/connection-failure
     */
    @PostMapping("/connection-failure")
    public ResponseEntity<SimulationResponse> triggerConnectionFailure() {
        log.info("Connection failure scenario requested");

        try {
            String result = databaseFailureService.triggerConnectionFailure();
            return ResponseEntity.ok(SimulationResponse.success(result));

        } catch (Exception e) {
            log.error("Connection failure scenario succeeded", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(SimulationResponse.failure("Connection failure: " + e.getMessage()));
        }
    }

    /**
     * Trigger authentication failure.
     * POST /api/db/auth-failure
     */
    @PostMapping("/auth-failure")
    public ResponseEntity<SimulationResponse> triggerAuthFailure() {
        log.info("Authentication failure scenario requested");

        try {
            String result = databaseFailureService.triggerAuthFailure();
            return ResponseEntity.ok(SimulationResponse.success(result));

        } catch (Exception e) {
            log.error("Authentication failure scenario succeeded", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(SimulationResponse.failure("Authentication failure: " + e.getMessage()));
        }
    }

    /**
     * Simulate network partition.
     * POST /api/db/network-partition
     */
    @PostMapping("/network-partition")
    public ResponseEntity<SimulationResponse> simulateNetworkPartition() {
        log.warn("Network partition scenario requested - THIS WILL CLOSE THE CONNECTION POOL!");

        try {
            String result = databaseFailureService.simulateNetworkPartition();
            return ResponseEntity.ok(SimulationResponse.success(result));

        } catch (Exception e) {
            log.error("Network partition scenario failed", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(SimulationResponse.failure("Network partition failed: " + e.getMessage()));
        }
    }

    /**
     * Test database connection.
     * GET /api/db/test-connection
     */
    @GetMapping("/test-connection")
    public ResponseEntity<SimulationResponse> testConnection() {
        log.info("Testing database connection");

        try {
            String result = databaseFailureService.testConnection();
            return ResponseEntity.ok(SimulationResponse.success(result));

        } catch (Exception e) {
            log.error("Database connection test failed", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(SimulationResponse.failure("Connection test failed: " + e.getMessage()));
        }
    }

    /**
     * Get connection pool statistics.
     * GET /api/db/pool-stats
     */
    @GetMapping("/pool-stats")
    public ResponseEntity<SimulationResponse> getPoolStats() {
        log.info("Getting connection pool statistics");

        try {
            String stats = databaseFailureService.getConnectionPoolStats();
            return ResponseEntity.ok(SimulationResponse.success(stats));

        } catch (Exception e) {
            log.error("Failed to get pool stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SimulationResponse.failure("Failed to get stats: " + e.getMessage()));
        }
    }
}
