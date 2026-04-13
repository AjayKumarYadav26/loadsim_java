package com.ktlo.simulator.controller.api;

import com.ktlo.simulator.model.web.ProcessingJob;
import com.ktlo.simulator.repository.ProcessingJobRepository;
import com.ktlo.simulator.service.CpuLoadService;
import com.ktlo.simulator.service.DatabaseFailureService;
import com.ktlo.simulator.service.web.ProgressTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * REST API controller for admin operations.
 * Provides monitoring and control endpoints for simulations.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final ProcessingJobRepository jobRepository;
    private final ProgressTrackingService progressService;
    private final CpuLoadService cpuLoadService;
    private final DatabaseFailureService databaseFailureService;
    @Autowired(required = false)
    private MetricsEndpoint metricsEndpoint;

    @Autowired(required = false)
    private DataSource dataSource;

    /**
     * Get all active simulations (processing jobs).
     *
     * @return list of active jobs with simulation details
     */
    @GetMapping("/simulations/active")
    public ResponseEntity<Map<String, Object>> getActiveSimulations() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<ProcessingJob> activeJobs = jobRepository.findActiveJobs();

            response.put("success", true);
            response.put("count", activeJobs.size());
            response.put("simulations", activeJobs);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting active simulations: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get all jobs with revealed simulation types.
     *
     * @return list of all jobs
     */
    @GetMapping("/jobs/all")
    public ResponseEntity<Map<String, Object>> getAllJobs() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<ProcessingJob> allJobs = jobRepository.findAll();

            response.put("success", true);
            response.put("count", allJobs.size());
            response.put("jobs", allJobs);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting all jobs: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get system metrics (JMX, threadpool, database).
     *
     * @return system metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        Map<String, Object> response = new HashMap<>();

        try {
            // Thread metrics
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            Map<String, Object> threadMetrics = new HashMap<>();
            threadMetrics.put("threadCount", threadBean.getThreadCount());
            threadMetrics.put("peakThreadCount", threadBean.getPeakThreadCount());
            threadMetrics.put("daemonThreadCount", threadBean.getDaemonThreadCount());

            // Memory metrics
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> memoryMetrics = new HashMap<>();
            memoryMetrics.put("totalMemory", runtime.totalMemory());
            memoryMetrics.put("freeMemory", runtime.freeMemory());
            memoryMetrics.put("maxMemory", runtime.maxMemory());
            memoryMetrics.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());

            // Active jobs tracking
            Map<String, Object> jobMetrics = new HashMap<>();
            jobMetrics.put("activeJobs", progressService.getActiveJobCount());
            jobMetrics.put("trackedJobs", progressService.getActiveJobs().size());

            // CPU metrics
            Map<String, Object> cpuMetrics = new HashMap<>();
            cpuMetrics.put("availableProcessors", runtime.availableProcessors());

            response.put("success", true);
            response.put("threads", threadMetrics);
            response.put("memory", memoryMetrics);
            response.put("jobs", jobMetrics);
            response.put("cpu", cpuMetrics);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting system metrics: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Manually trigger a simulation.
     *
     * @param type simulation type (cpu, database)
     * @param params optional parameters
     * @return response with status
     */
    @PostMapping("/simulations/trigger")
    public ResponseEntity<Map<String, Object>> triggerSimulation(
            @RequestParam("type") String type,
            @RequestParam(required = false) Map<String, String> params) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Manual simulation trigger: type={}, params={}", type, params);

            switch (type.toLowerCase()) {
                case "cpu-exhaust":
                    int taskCount = params != null && params.containsKey("taskCount")
                        ? Integer.parseInt(params.get("taskCount")) : 100;
                    int duration = params != null && params.containsKey("duration")
                        ? Integer.parseInt(params.get("duration")) : 120;
                    cpuLoadService.exhaustThreadPool(taskCount, duration);
                    response.put("message", "CPU exhaustion started");
                    break;

                case "cpu-intensive":
                    int cpuDuration = params != null && params.containsKey("duration")
                        ? Integer.parseInt(params.get("duration")) : 60;
                    cpuLoadService.executeIntensiveTask(cpuDuration);
                    response.put("message", "CPU intensive task started");
                    break;

                case "cpu-fibonacci":
                    int n = params != null && params.containsKey("n")
                        ? Integer.parseInt(params.get("n")) : 40;
                    cpuLoadService.calculateFibonacci(n);
                    response.put("message", "Fibonacci calculation started");
                    break;

                case "cpu-primes":
                    int limit = params != null && params.containsKey("limit")
                        ? Integer.parseInt(params.get("limit")) : 1000000;
                    cpuLoadService.calculatePrimes(limit);
                    response.put("message", "Prime calculation started");
                    break;

                case "db-timeout":
                    databaseFailureService.triggerTimeout();
                    response.put("message", "Database timeout triggered");
                    break;

                case "db-slow-query":
                    int delay = params != null && params.containsKey("delay")
                        ? Integer.parseInt(params.get("delay")) : 5;
                    databaseFailureService.executeSlowQuery(delay);
                    response.put("message", "Slow query started");
                    break;

                case "db-connection-failure":
                    databaseFailureService.triggerConnectionFailure();
                    response.put("message", "Connection failure triggered");
                    break;

                default:
                    response.put("success", false);
                    response.put("error", "Unknown simulation type: " + type);
                    return ResponseEntity.badRequest().body(response);
            }

            response.put("success", true);
            response.put("type", type);
            response.put("params", params);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error triggering simulation: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
