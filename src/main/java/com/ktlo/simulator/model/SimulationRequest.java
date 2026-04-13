package com.ktlo.simulator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Standard request model for simulation endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationRequest {

    public enum Intensity {
        LOW,
        MEDIUM,
        HIGH
    }

    @Builder.Default
    private int duration = 10; // seconds

    @Builder.Default
    private Intensity intensity = Intensity.MEDIUM;

    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();
}
