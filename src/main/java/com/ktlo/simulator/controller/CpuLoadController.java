package com.ktlo.simulator.controller;

import com.ktlo.simulator.model.SimulationResponse;
import com.ktlo.simulator.model.ThreadPoolStatus;
import com.ktlo.simulator.service.CpuLoadService;
import com.ktlo.simulator.service.ThreadPoolMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * REST controller for CPU load and threadpool exhaustion simulation.
 */
@Slf4j
@RestController
@RequestMapping("/api/cpu")
@RequiredArgsConstructor
@Tag(name = "CPU Load Simulation", description = "APIs for simulating high CPU load and threadpool exhaustion")
public class CpuLoadController {

    private final CpuLoadService cpuLoadService;
    private final ThreadPoolMonitorService threadPoolMonitorService;

    /**
     * Exhaust the threadpool by submitting many CPU-intensive tasks.
     * POST /api/cpu/exhaust?taskCount=25&duration=60
     */
    @Operation(summary = "Exhaust threadpool",
               description = "Submits multiple CPU-intensive tasks to exhaust the threadpool (max 20 threads). Use this to simulate high CPU load.")
    @PostMapping("/exhaust")
    public ResponseEntity<SimulationResponse> exhaustThreadPool(
            @Parameter(description = "Number of tasks to submit (default: 100)") @RequestParam(defaultValue = "100") int taskCount,
            @Parameter(description = "Duration in seconds for each task (default: 120)") @RequestParam(defaultValue = "120") int duration) {

        log.info("Exhausting threadpool: {} tasks, {} seconds each", taskCount, duration);

        try {
            cpuLoadService.exhaustThreadPool(taskCount, duration);

            ThreadPoolStatus status = threadPoolMonitorService.getThreadPoolStatus();

            SimulationResponse response = SimulationResponse.inProgress(
                    String.format("Submitted %d CPU-intensive tasks (%ds each) to exhaust threadpool", taskCount, duration))
                    .addDetail("tasksSubmitted", taskCount)
                    .addDetail("taskDuration", duration)
                    .addDetail("threadPoolStatus", status);

            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            log.error("Failed to exhaust threadpool", e);
            return ResponseEntity.internalServerError()
                    .body(SimulationResponse.failure("Failed to exhaust threadpool: " + e.getMessage()));
        }
    }

    /**
     * Execute a single CPU-intensive task.
     * POST /api/cpu/intensive?duration=10
     */
    @PostMapping("/intensive")
    public ResponseEntity<SimulationResponse> executeIntensiveTask(
            @RequestParam(defaultValue = "60") int duration) {

        log.info("Starting CPU-intensive task: {} seconds", duration);

        try {
            CompletableFuture<String> future = cpuLoadService.executeIntensiveTask(duration);

            SimulationResponse response = SimulationResponse.inProgress(
                    String.format("CPU-intensive task started (%d seconds)", duration))
                    .addDetail("duration", duration)
                    .addDetail("totalTasksExecuted", cpuLoadService.getTaskCount());

            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            log.error("Failed to execute intensive task", e);
            return ResponseEntity.internalServerError()
                    .body(SimulationResponse.failure("Failed to execute task: " + e.getMessage()));
        }
    }

    /**
     * Calculate Fibonacci number (CPU-intensive).
     * POST /api/cpu/fibonacci/35
     */
    @PostMapping("/fibonacci/{n}")
    public ResponseEntity<SimulationResponse> calculateFibonacci(@PathVariable int n) {

        if (n > 50) {
            return ResponseEntity.badRequest()
                    .body(SimulationResponse.failure("Fibonacci index too large (max 50)"));
        }

        log.info("Calculating Fibonacci number: {}", n);

        try {
            CompletableFuture<Long> future = cpuLoadService.calculateFibonacci(n);

            SimulationResponse response = SimulationResponse.inProgress(
                    String.format("Fibonacci calculation started (n=%d)", n))
                    .addDetail("n", n)
                    .addDetail("totalTasksExecuted", cpuLoadService.getTaskCount());

            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            log.error("Failed to calculate Fibonacci", e);
            return ResponseEntity.internalServerError()
                    .body(SimulationResponse.failure("Failed to calculate Fibonacci: " + e.getMessage()));
        }
    }

    /**
     * Calculate prime numbers up to limit (CPU-intensive).
     * POST /api/cpu/primes?limit=100000
     */
    @PostMapping("/primes")
    public ResponseEntity<SimulationResponse> calculatePrimes(
            @RequestParam(defaultValue = "10000000") int limit) {

        if (limit > 100000000) {
            return ResponseEntity.badRequest()
                    .body(SimulationResponse.failure("Limit too large (max 100,000,000)"));
        }

        log.info("Calculating primes up to: {}", limit);

        try {
            CompletableFuture<Integer> future = cpuLoadService.calculatePrimes(limit);

            SimulationResponse response = SimulationResponse.inProgress(
                    String.format("Prime calculation started (limit=%d)", limit))
                    .addDetail("limit", limit)
                    .addDetail("totalTasksExecuted", cpuLoadService.getTaskCount());

            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            log.error("Failed to calculate primes", e);
            return ResponseEntity.internalServerError()
                    .body(SimulationResponse.failure("Failed to calculate primes: " + e.getMessage()));
        }
    }

    /**
     * Get current threadpool status.
     * GET /api/cpu/status
     */
    @GetMapping("/status")
    public ResponseEntity<ThreadPoolStatus> getThreadPoolStatus() {
        ThreadPoolStatus status = threadPoolMonitorService.getThreadPoolStatus();
        return ResponseEntity.ok(status);
    }
}
