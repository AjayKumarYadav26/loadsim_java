package com.ktlo.simulator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Standard response model for simulation endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResponse {

    public enum Status {
        SUCCESS,
        FAILURE,
        IN_PROGRESS,
        PARTIAL
    }

    private Status status;
    private String message;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Builder.Default
    private Map<String, Object> details = new HashMap<>();

    public static SimulationResponse success(String message) {
        return SimulationResponse.builder()
                .status(Status.SUCCESS)
                .message(message)
                .build();
    }

    public static SimulationResponse failure(String message) {
        return SimulationResponse.builder()
                .status(Status.FAILURE)
                .message(message)
                .build();
    }

    public static SimulationResponse inProgress(String message) {
        return SimulationResponse.builder()
                .status(Status.IN_PROGRESS)
                .message(message)
                .build();
    }

    public SimulationResponse addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }
}
