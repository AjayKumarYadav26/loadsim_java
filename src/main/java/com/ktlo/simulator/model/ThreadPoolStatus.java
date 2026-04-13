package com.ktlo.simulator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing the current status of the threadpool.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreadPoolStatus {
    private int activeThreads;
    private int queuedTasks;
    private long completedTasks;
    private long totalTasks; // Total tasks ever scheduled
    private int poolSize;
    private int maxPoolSize;
    private int corePoolSize;
    private int queueCapacity;
    private int queueRemainingCapacity;
    private boolean isExhausted;
    private double threadUtilization; // Percentage (0-100)
}
