package com.ktlo.simulator.service.web;

import com.ktlo.simulator.model.web.ProcessingJob;
import com.ktlo.simulator.model.web.ProcessingStatus;
import com.ktlo.simulator.model.web.SimulationConfig;
import com.ktlo.simulator.repository.ProcessingJobRepository;
import com.ktlo.simulator.service.CpuLoadService;
import com.ktlo.simulator.service.DatabaseFailureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service for processing documents by triggering simulation APIs.
 * Orchestrates the workflow: job creation → simulation trigger → progress tracking → completion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private final ProcessingJobRepository jobRepository;
    private final SimulationMappingService mappingService;
    private final ProgressTrackingService progressService;
    private final CpuLoadService cpuLoadService;
    private final DatabaseFailureService databaseFailureService;
    private final FileStorageService fileStorageService;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Start processing a job by triggering the appropriate simulation.
     *
     * @param jobId the job ID to process
     * @return CompletableFuture for async processing
     */
    @Async
    public CompletableFuture<Void> startProcessing(String jobId) {
        log.info("=== ASYNC METHOD ENTERED: startProcessing for job: {} on thread: {}", 
            jobId, Thread.currentThread().getName());

        Optional<ProcessingJob> jobOpt = jobRepository.findById(jobId);
        if (!jobOpt.isPresent()) {
            log.error("Job not found in async method: {}", jobId);
            return CompletableFuture.completedFuture(null);
        }

        ProcessingJob job = jobOpt.get();
        log.info("Job found - Operation: {}, Quality: {}, Status: {}", 
            job.getOperationType(), job.getQuality(), job.getStatus());

        try {
            // Check if already processing
            if (job.getStatus() == ProcessingStatus.PROCESSING && job.getStartedAt() != null) {
                log.warn("Job {} is already being processed", jobId);
                return CompletableFuture.completedFuture(null);
            }

            log.info("=== UPDATING JOB STATUS TO PROCESSING for job: {}", jobId);
            
            // Update job status to PROCESSING (only set startedAt if not already set)
            job.setStatus(ProcessingStatus.PROCESSING);
            job.setProgress(0);
            if (job.getStartedAt() == null) {
                job.setStartedAt(LocalDateTime.now());
            }
            jobRepository.save(job);

            log.info("=== GETTING SIMULATION CONFIGURATION for job: {}", jobId);
            
            // Get simulation configuration
            SimulationConfig simulationConfig = mappingService.mapToSimulation(job);
            job.setSimulationType(simulationConfig.getType().name());
            job.setSimulationEndpoint(simulationConfig.getEndpoint());
            jobRepository.save(job);

            log.info("=== SIMULATION MAPPED: Job {} -> Type: {}, Endpoint: {}, Duration: {}s",
                jobId, simulationConfig.getType(), simulationConfig.getEndpoint(), 
                simulationConfig.getExpectedDurationSeconds());

            log.info("=== STARTING PROGRESS TRACKING for job: {}", jobId);
            
            // Start progress tracking
            progressService.startTracking(job);
            progressService.setExpectedDuration(jobId, simulationConfig.getExpectedDurationSeconds());
            progressService.updateProgress(jobId, 0, "Starting " + job.getOperationType().name().toLowerCase() + " operation...");

            log.info("=== TRIGGERING SIMULATION for job: {}", jobId);
            
            // Trigger the appropriate simulation (async)
            triggerSimulation(job, simulationConfig);

            log.info("=== SIMULATION TRIGGERED - Starting progress loop for job: {}", jobId);
            
            // Simulate progress updates while waiting for simulation to complete
            long startTime = System.currentTimeMillis();
            int expectedDuration = simulationConfig.getExpectedDurationSeconds();

            while (true) {
                long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;

                // Update simulated progress
                progressService.simulateProgress(jobId, elapsedSeconds);

                // Update job progress in database
                ProcessingJob currentJob = jobRepository.findById(jobId).orElse(null);
                if (currentJob != null) {
                    ProgressTrackingService.JobProgress progress = progressService.getProgress(jobId);
                    if (progress != null) {
                        currentJob.setProgress(progress.getProgress());
                        jobRepository.save(currentJob);
                    }
                }

                // Check if expected duration has elapsed
                if (elapsedSeconds >= expectedDuration) {
                    break;
                }

                // Sleep for 2 seconds before next update
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // Mark as completed
            job.setStatus(ProcessingStatus.COMPLETED);
            job.setProgress(100);
            job.setCompletedAt(LocalDateTime.now());
            job.setResult("Processing completed successfully");
            jobRepository.save(job);
            progressService.markCompleted(jobId, "Processing completed successfully");

            log.info("Job {} completed successfully", jobId);

        } catch (Exception e) {
            log.error("Error processing job {}: {}", jobId, e.getMessage(), e);

            // Mark as failed
            job.setStatus(ProcessingStatus.FAILED);
            job.setCompletedAt(LocalDateTime.now());
            job.setErrorMessage(e.getMessage());
            jobRepository.save(job);
            progressService.markFailed(jobId, e.getMessage());

            // Cleanup file
            fileStorageService.deleteFile(job.getFilePath());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Trigger the appropriate simulation based on configuration.
     *
     * @param job the processing job
     * @param config the simulation configuration
     */
    private void triggerSimulation(ProcessingJob job, SimulationConfig config) {
        log.info("Triggering simulation: {} for job {}", config.getType(), job.getJobId());

        try {
            switch (config.getType()) {
                case CPU_LOAD:
                    triggerCpuSimulation(config);
                    break;
                case DATABASE_FAILURE:
                    triggerDatabaseSimulation(config);
                    break;
                case NONE:
                    log.info("No simulation configured for job {}", job.getJobId());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown simulation type: " + config.getType());
            }
        } catch (Exception e) {
            log.error("Simulation trigger failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to trigger simulation: " + e.getMessage(), e);
        }
    }

    /**
     * Trigger CPU load simulation.
     */
    private void triggerCpuSimulation(SimulationConfig config) {
        log.info("=== ENTERING triggerCpuSimulation - Endpoint: {}, Params: {}", 
            config.getEndpoint(), config.getParams());
            
        String endpoint = config.getEndpoint();
        Map<String, Object> params = config.getParams();

        if (endpoint.contains("/exhaust")) {
            Integer taskCount = (Integer) params.get("taskCount");
            Integer duration = (Integer) params.get("duration");
            log.info("=== CALLING cpuLoadService.exhaustThreadPool(taskCount={}, duration={})", 
                taskCount, duration);
            cpuLoadService.exhaustThreadPool(taskCount != null ? taskCount : 100,
                                            duration != null ? duration : 120);
            log.info("=== cpuLoadService.exhaustThreadPool() call returned");

        } else if (endpoint.contains("/intensive")) {
            Integer duration = (Integer) params.get("duration");
            log.info("=== CALLING cpuLoadService.executeIntensiveTask(duration={})", duration);
            cpuLoadService.executeIntensiveTask(duration != null ? duration : 60);

        } else if (endpoint.contains("/fibonacci")) {
            Integer n = (Integer) params.get("n");
            log.info("=== CALLING cpuLoadService.calculateFibonacci(n={})", n);
            cpuLoadService.calculateFibonacci(n != null ? n : 40);

        } else if (endpoint.contains("/primes")) {
            // Handle both Integer and Long types
            Object limitObj = params.get("limit");
            int limit = 1000000; // default
            if (limitObj instanceof Integer) {
                limit = (Integer) limitObj;
            } else if (limitObj instanceof Long) {
                limit = ((Long) limitObj).intValue();
            }
            log.info("=== CALLING cpuLoadService.calculatePrimes(limit={})", limit);
            cpuLoadService.calculatePrimes(limit);
        }

        log.info("=== CPU simulation triggered successfully: {}", endpoint);
    }

    /**
     * Trigger database failure simulation.
     */
    private void triggerDatabaseSimulation(SimulationConfig config) {
        String endpoint = config.getEndpoint();
        Map<String, Object> params = config.getParams();

        if (endpoint.contains("/timeout")) {
            databaseFailureService.triggerTimeout();

        } else if (endpoint.contains("/slow-query")) {
            Integer delay = (Integer) params.get("delay");
            databaseFailureService.executeSlowQuery(delay != null ? delay : 5);

        } else if (endpoint.contains("/connection-failure")) {
            databaseFailureService.triggerConnectionFailure();
        }

        log.info("Database simulation triggered: {}", endpoint);
    }

    /**
     * Cancel a processing job.
     *
     * @param jobId the job ID to cancel
     * @return true if cancelled successfully
     */
    public boolean cancelJob(String jobId) {
        log.info("Cancelling job: {}", jobId);

        Optional<ProcessingJob> jobOpt = jobRepository.findById(jobId);
        if (!jobOpt.isPresent()) {
            log.error("Job not found: {}", jobId);
            return false;
        }

        ProcessingJob job = jobOpt.get();

        if (job.getStatus().isTerminal()) {
            log.warn("Cannot cancel job {} - already in terminal state: {}", jobId, job.getStatus());
            return false;
        }

        job.setStatus(ProcessingStatus.CANCELLED);
        job.setCompletedAt(LocalDateTime.now());
        jobRepository.save(job);
        progressService.stopTracking(jobId);

        // Cleanup file
        fileStorageService.deleteFile(job.getFilePath());

        log.info("Job {} cancelled successfully", jobId);
        return true;
    }

    /**
     * Get job status with current progress.
     *
     * @param jobId the job ID
     * @return processing job or null if not found
     */
    public ProcessingJob getJobStatus(String jobId) {
        return jobRepository.findById(jobId).orElse(null);
    }
}
