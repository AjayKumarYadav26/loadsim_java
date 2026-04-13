package com.ktlo.simulator.controller.api;

import com.ktlo.simulator.model.web.ProcessingJob;
import com.ktlo.simulator.service.web.DocumentProcessingService;
import com.ktlo.simulator.service.web.ProgressTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API controller for processing operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/portal/processing")
@RequiredArgsConstructor
public class ProcessingApiController {

    private final DocumentProcessingService processingService;
    private final ProgressTrackingService progressService;

    /**
     * Start processing a job.
     *
     * @param jobId the job ID to start processing
     * @return response with status
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startProcessing(@RequestParam("jobId") String jobId) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("=== API ENDPOINT CALLED: Starting processing for job: {}", jobId);

            ProcessingJob job = processingService.getJobStatus(jobId);
            if (job == null) {
                log.error("Job not found: {}", jobId);
                response.put("success", false);
                response.put("error", "Job not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            log.info("Job found - Operation: {}, Quality: {}, Status: {}", 
                job.getOperationType(), job.getQuality(), job.getStatus());

            // Start async processing
            log.info("Calling processingService.startProcessing() for job: {}", jobId);
            processingService.startProcessing(jobId);
            log.info("processingService.startProcessing() call returned for job: {}", jobId);

            response.put("success", true);
            response.put("message", "Processing started");
            response.put("jobId", jobId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error starting processing for job {}: {}", jobId, e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get job status (polled by frontend).
     *
     * @param jobId the job ID
     * @return job status with progress
     */
    @GetMapping("/{jobId}/status")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable String jobId) {
        Map<String, Object> response = new HashMap<>();

        try {
            ProcessingJob job = processingService.getJobStatus(jobId);
            if (job == null) {
                response.put("success", false);
                response.put("error", "Job not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            response.put("success", true);
            response.put("jobId", job.getJobId());
            response.put("fileName", job.getFileName());
            response.put("status", job.getStatus().name());
            response.put("progress", job.getProgress());
            response.put("operationType", job.getOperationType().name());
            response.put("quality", job.getQuality().name());

            // Add processing details if available
            if (job.getSimulationType() != null) {
                response.put("simulationType", job.getSimulationType());
            }
            if (job.getResult() != null) {
                response.put("result", job.getResult());
            }
            if (job.getErrorMessage() != null) {
                response.put("error", job.getErrorMessage());
            }

            // Calculate elapsed time
            long elapsedSeconds = 0;
            long remainingSeconds = 0;
            
            if (job.getStartedAt() != null) {
                // Calculate elapsed time differently for in-progress vs completed jobs
                if (job.getCompletedAt() != null) {
                    elapsedSeconds = job.getDurationSeconds();
                } else {
                    // For in-progress jobs, calculate from startedAt to now
                    elapsedSeconds = java.time.Duration.between(
                        job.getStartedAt(), 
                        java.time.LocalDateTime.now()
                    ).getSeconds();
                }
                response.put("elapsedSeconds", elapsedSeconds);
            }

            // Estimate remaining time from progress tracking
            ProgressTrackingService.JobProgress progress = progressService.getProgress(jobId);
            if (progress != null && !job.getStatus().isTerminal()) {
                remainingSeconds = progress.getExpectedDurationSeconds() - progress.getElapsedSeconds();
                response.put("remainingSeconds", Math.max(0, remainingSeconds));
                
                // Add progress tracking message
                if (progress.getMessage() != null) {
                    response.put("progressMessage", progress.getMessage());
                }
            } else if (job.getStatus().isTerminal()) {
                response.put("remainingSeconds", 0);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting status for job {}: {}", jobId, e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Cancel a processing job.
     *
     * @param jobId the job ID to cancel
     * @return response with status
     */
    @PutMapping("/{jobId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelJob(@PathVariable String jobId) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Cancelling job: {}", jobId);

            boolean cancelled = processingService.cancelJob(jobId);
            if (cancelled) {
                response.put("success", true);
                response.put("message", "Job cancelled successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "Failed to cancel job");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Error cancelling job {}: {}", jobId, e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
