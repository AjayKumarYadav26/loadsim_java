package com.ktlo.simulator.service.web;

import com.ktlo.simulator.model.web.ProcessingJob;
import com.ktlo.simulator.model.web.ProcessingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for tracking progress of active processing jobs in memory.
 * Provides real-time progress updates for the UI without constant database queries.
 */
@Slf4j
@Service
public class ProgressTrackingService {

    /**
     * In-memory cache of active job progress.
     * Key: jobId, Value: JobProgress
     */
    private final Map<String, JobProgress> activeJobs = new ConcurrentHashMap<>();

    /**
     * Start tracking a new job.
     *
     * @param job the job to track
     */
    public void startTracking(ProcessingJob job) {
        JobProgress progress = JobProgress.builder()
                .jobId(job.getJobId())
                .status(job.getStatus())
                .progress(0)
                .message("Initializing...")
                .startTime(LocalDateTime.now())
                .expectedDurationSeconds(0)
                .build();

        activeJobs.put(job.getJobId(), progress);
        log.debug("Started tracking job: {}", job.getJobId());
    }

    /**
     * Update progress for a job.
     *
     * @param jobId job identifier
     * @param progress progress percentage (0-100)
     * @param message status message to display
     */
    public void updateProgress(String jobId, int progress, String message) {
        JobProgress jobProgress = activeJobs.get(jobId);
        if (jobProgress != null) {
            jobProgress.setProgress(Math.min(100, Math.max(0, progress)));
            jobProgress.setMessage(message);
            jobProgress.setLastUpdate(LocalDateTime.now());
            log.debug("Updated progress for job {}: {}% - {}", jobId, progress, message);
        } else {
            log.warn("Attempted to update progress for unknown job: {}", jobId);
        }
    }

    /**
     * Update job status.
     *
     * @param jobId job identifier
     * @param status new status
     */
    public void updateStatus(String jobId, ProcessingStatus status) {
        JobProgress jobProgress = activeJobs.get(jobId);
        if (jobProgress != null) {
            jobProgress.setStatus(status);
            jobProgress.setLastUpdate(LocalDateTime.now());
            log.debug("Updated status for job {}: {}", jobId, status);

            // Remove from tracking if terminal status
            if (status.isTerminal()) {
                jobProgress.setCompletionTime(LocalDateTime.now());
                // Keep in memory for a short time to allow final status retrieval
                // Will be cleaned up by periodic cleanup
            }
        }
    }

    /**
     * Set expected duration for a job.
     *
     * @param jobId job identifier
     * @param expectedDurationSeconds expected duration in seconds
     */
    public void setExpectedDuration(String jobId, int expectedDurationSeconds) {
        JobProgress jobProgress = activeJobs.get(jobId);
        if (jobProgress != null) {
            jobProgress.setExpectedDurationSeconds(expectedDurationSeconds);
        }
    }

    /**
     * Get progress information for a job.
     *
     * @param jobId job identifier
     * @return job progress, or null if not found
     */
    public JobProgress getProgress(String jobId) {
        return activeJobs.get(jobId);
    }

    /**
     * Stop tracking a job (remove from memory).
     *
     * @param jobId job identifier
     */
    public void stopTracking(String jobId) {
        JobProgress removed = activeJobs.remove(jobId);
        if (removed != null) {
            log.debug("Stopped tracking job: {}", jobId);
        }
    }

    /**
     * Get all active jobs being tracked.
     *
     * @return list of active job progress objects
     */
    public List<JobProgress> getActiveJobs() {
        return activeJobs.values().stream()
                .filter(jp -> jp.getStatus() != null && jp.getStatus().isActive())
                .collect(Collectors.toList());
    }

    /**
     * Get count of active jobs.
     *
     * @return number of jobs currently being tracked
     */
    public int getActiveJobCount() {
        return (int) activeJobs.values().stream()
                .filter(jp -> jp.getStatus() != null && jp.getStatus().isActive())
                .count();
    }

    /**
     * Clean up old completed jobs from memory.
     * Should be called periodically (e.g., every 5 minutes).
     *
     * @param maxAgeMinutes maximum age in minutes for completed jobs
     * @return number of jobs cleaned up
     */
    public int cleanupOldJobs(int maxAgeMinutes) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(maxAgeMinutes);
        int cleanedUp = 0;

        List<String> jobsToRemove = activeJobs.entrySet().stream()
                .filter(entry -> {
                    JobProgress jp = entry.getValue();
                    return jp.getCompletionTime() != null &&
                           jp.getCompletionTime().isBefore(cutoffTime);
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        for (String jobId : jobsToRemove) {
            activeJobs.remove(jobId);
            cleanedUp++;
        }

        if (cleanedUp > 0) {
            log.info("Cleaned up {} completed jobs from progress tracking", cleanedUp);
        }

        return cleanedUp;
    }

    /**
     * Calculate estimated time remaining for a job.
     *
     * @param jobId job identifier
     * @return estimated seconds remaining, or -1 if cannot be calculated
     */
    public long getEstimatedTimeRemaining(String jobId) {
        JobProgress jp = activeJobs.get(jobId);
        if (jp == null || jp.getProgress() == 0 || jp.getExpectedDurationSeconds() == 0) {
            return -1;
        }

        // Calculate based on expected duration and current progress
        double remainingPercentage = (100.0 - jp.getProgress()) / 100.0;
        return (long) (jp.getExpectedDurationSeconds() * remainingPercentage);
    }

    /**
     * Simulate progress for a job based on expected duration.
     * This creates a smooth progress bar for simulations that don't report actual progress.
     *
     * @param jobId job identifier
     * @param elapsedSeconds seconds elapsed since start
     */
    public void simulateProgress(String jobId, long elapsedSeconds) {
        JobProgress jp = activeJobs.get(jobId);
        if (jp == null || jp.getExpectedDurationSeconds() == 0) {
            return;
        }

        // Calculate progress based on elapsed time vs expected duration
        int calculatedProgress = (int) ((elapsedSeconds * 100.0) / jp.getExpectedDurationSeconds());
        calculatedProgress = Math.min(95, calculatedProgress); // Never go to 100% automatically

        if (calculatedProgress > jp.getProgress()) {
            // Update message based on progress
            String newMessage;
            if (calculatedProgress < 25) {
                newMessage = "Processing...";
            } else if (calculatedProgress < 50) {
                newMessage = "Continuing processing...";
            } else if (calculatedProgress < 75) {
                newMessage = "Almost there...";
            } else {
                newMessage = "Finalizing...";
            }
            updateProgress(jobId, calculatedProgress, newMessage);
        }
    }

    /**
     * Mark a job as completed successfully.
     *
     * @param jobId job identifier
     * @param resultMessage result message
     */
    public void markCompleted(String jobId, String resultMessage) {
        updateProgress(jobId, 100, resultMessage);
        updateStatus(jobId, ProcessingStatus.COMPLETED);
    }

    /**
     * Mark a job as failed.
     *
     * @param jobId job identifier
     * @param errorMessage error message
     */
    public void markFailed(String jobId, String errorMessage) {
        updateProgress(jobId, activeJobs.getOrDefault(jobId, new JobProgress()).getProgress(), errorMessage);
        updateStatus(jobId, ProcessingStatus.FAILED);
    }

    /**
     * In-memory representation of job progress.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobProgress {
        /**
         * Job identifier.
         */
        private String jobId;

        /**
         * Current status.
         */
        private ProcessingStatus status;

        /**
         * Progress percentage (0-100).
         */
        @Builder.Default
        private int progress = 0;

        /**
         * Current status message.
         */
        private String message;

        /**
         * Expected duration in seconds.
         */
        private int expectedDurationSeconds;

        /**
         * When tracking started.
         */
        private LocalDateTime startTime;

        /**
         * Last update timestamp.
         */
        private LocalDateTime lastUpdate;

        /**
         * When job completed (if terminal status).
         */
        private LocalDateTime completionTime;

        /**
         * Calculate elapsed time in seconds.
         *
         * @return elapsed seconds since start
         */
        public long getElapsedSeconds() {
            if (startTime == null) {
                return 0;
            }
            LocalDateTime endTime = completionTime != null ? completionTime : LocalDateTime.now();
            return java.time.Duration.between(startTime, endTime).getSeconds();
        }
    }
}
