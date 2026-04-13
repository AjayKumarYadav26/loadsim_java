package com.ktlo.simulator.controller.api;

import com.ktlo.simulator.model.web.ProcessingJob;
import com.ktlo.simulator.service.web.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for analytics and statistics.
 */
@Slf4j
@RestController
@RequestMapping("/api/portal/analytics")
@RequiredArgsConstructor
public class AnalyticsApiController {

    private final AnalyticsService analyticsService;

    /**
     * Get summary statistics.
     *
     * @return summary stats (total jobs, avg time, success rate, etc.)
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummaryStats() {
        Map<String, Object> response = new HashMap<>();

        try {
            AnalyticsService.AnalyticsSummary summary = analyticsService.getSummaryStats();

            response.put("success", true);
            response.put("totalJobs", summary.getTotalJobs());
            response.put("completedJobs", summary.getCompletedJobs());
            response.put("failedJobs", summary.getFailedJobs());
            response.put("activeJobs", summary.getActiveJobs());
            response.put("successRate", Math.round(summary.getSuccessRate() * 10) / 10.0);
            response.put("avgProcessingTimeSeconds", summary.getAvgProcessingTimeSeconds());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting summary stats: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get job trends for the last N days.
     *
     * @param days number of days (default 7)
     * @return daily job counts
     */
    @GetMapping("/trends")
    public ResponseEntity<Map<String, Object>> getJobTrends(
            @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<AnalyticsService.DailyJobCount> trends = analyticsService.getJobTrends(days);

            response.put("success", true);
            response.put("trends", trends);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting job trends: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get breakdown of jobs by operation type.
     *
     * @return map of operation type to count
     */
    @GetMapping("/operations")
    public ResponseEntity<Map<String, Object>> getOperationBreakdown() {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Long> breakdown = analyticsService.getOperationBreakdown();

            response.put("success", true);
            response.put("operations", breakdown);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting operation breakdown: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get breakdown of jobs by status.
     *
     * @return map of status to count
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatusBreakdown() {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Long> breakdown = analyticsService.getStatusBreakdown();

            response.put("success", true);
            response.put("status", breakdown);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting status breakdown: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get breakdown of jobs by quality level.
     *
     * @return map of quality level to count
     */
    @GetMapping("/quality")
    public ResponseEntity<Map<String, Object>> getQualityBreakdown() {
        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Long> breakdown = analyticsService.getQualityBreakdown();

            response.put("success", true);
            response.put("quality", breakdown);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting quality breakdown: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get recent jobs (last N).
     *
     * @param limit number of jobs to return (default 10)
     * @return list of recent jobs
     */
    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentJobs(
            @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> response = new HashMap<>();

        try {
            List<ProcessingJob> recentJobs = analyticsService.getRecentJobs(limit);

            response.put("success", true);
            response.put("jobs", recentJobs);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting recent jobs: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
