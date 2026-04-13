package com.ktlo.simulator.model.web;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for triggering a specific simulation based on user operations.
 * This DTO maps document processing operations to their corresponding simulation endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationConfig {

    /**
     * Type of simulation to trigger (CPU, DATABASE, etc.).
     */
    private SimulationType type;

    /**
     * API endpoint to call for this simulation.
     * Example: "/api/cpu/exhaust", "/api/db/timeout"
     */
    private String endpoint;

    /**
     * HTTP method for the endpoint (GET, POST, etc.).
     * Defaults to POST for most simulations.
     */
    @Builder.Default
    private String method = "POST";

    /**
     * Parameters to pass to the simulation endpoint.
     * Key-value pairs for query parameters or request body.
     */
    @Builder.Default
    private Map<String, Object> params = new HashMap<>();

    /**
     * Expected duration of the simulation in seconds.
     * Used for progress tracking and UI estimates.
     */
    private int expectedDurationSeconds;

    /**
     * Description of what this simulation does.
     * Displayed in admin panel for transparency.
     */
    private String description;

    /**
     * Add a parameter to the configuration.
     *
     * @param key parameter name
     * @param value parameter value
     * @return this config for method chaining
     */
    public SimulationConfig addParam(String key, Object value) {
        this.params.put(key, value);
        return this;
    }

    /**
     * Types of simulations available in the system.
     */
    public enum SimulationType {
        /**
         * CPU exhaustion simulations - exhaust threadpool, intensive calculations.
         */
        CPU_LOAD,

        /**
         * Database failure simulations - timeouts, connection failures, slow queries.
         */
        DATABASE_FAILURE,

        /**
         * No simulation - actual file processing (if needed).
         */
        NONE
    }
}
