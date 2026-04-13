package com.ktlo.simulator.repository;

import com.ktlo.simulator.model.web.ProcessingJob;
import com.ktlo.simulator.model.web.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for managing ProcessingJob entities.
 * Provides database operations for document processing jobs.
 */
@Repository
public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, String> {

    /**
     * Find all jobs with a specific status.
     *
     * @param status the status to search for
     * @return list of jobs with the given status
     */
    List<ProcessingJob> findByStatus(ProcessingStatus status);

    /**
     * Find all jobs with a specific status, ordered by creation time (newest first).
     *
     * @param status the status to search for
     * @return list of jobs with the given status, ordered by created_at DESC
     */
    List<ProcessingJob> findByStatusOrderByCreatedAtDesc(ProcessingStatus status);

    /**
     * Find the most recent jobs, limited to a specific count.
     *
     * @param limit maximum number of jobs to return
     * @return list of recent jobs
     */
    @Query("SELECT j FROM ProcessingJob j ORDER BY j.createdAt DESC")
    List<ProcessingJob> findRecentJobs(@Param("limit") int limit);

    /**
     * Find jobs created after a specific timestamp.
     *
     * @param timestamp the cutoff timestamp
     * @return list of jobs created after the timestamp
     */
    List<ProcessingJob> findByCreatedAtAfter(LocalDateTime timestamp);

    /**
     * Find jobs created between two timestamps.
     *
     * @param start start timestamp (inclusive)
     * @param end end timestamp (inclusive)
     * @return list of jobs in the time range
     */
    List<ProcessingJob> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Count jobs by status.
     *
     * @param status the status to count
     * @return number of jobs with the given status
     */
    long countByStatus(ProcessingStatus status);

    /**
     * Find active jobs (UPLOADING or PROCESSING).
     *
     * @return list of active jobs
     */
    @Query("SELECT j FROM ProcessingJob j WHERE j.status IN ('UPLOADING', 'PROCESSING') ORDER BY j.createdAt DESC")
    List<ProcessingJob> findActiveJobs();

    /**
     * Find all jobs for a specific operation type.
     *
     * @param operationType the operation type
     * @return list of jobs with the given operation type
     */
    List<ProcessingJob> findByOperationType(ProcessingJob.OperationType operationType);

    /**
     * Count total jobs.
     *
     * @return total number of jobs
     */
    @Query("SELECT COUNT(j) FROM ProcessingJob j")
    long countTotalJobs();

    /**
     * Calculate average processing duration for completed jobs.
     *
     * @return average duration in seconds, or 0 if no completed jobs
     */
    @Query("SELECT AVG(EXTRACT(EPOCH FROM (j.completedAt - j.startedAt))) FROM ProcessingJob j WHERE j.status = 'COMPLETED' AND j.startedAt IS NOT NULL AND j.completedAt IS NOT NULL")
    Double getAverageProcessingDuration();

    /**
     * Find jobs that have been stuck in PROCESSING for longer than a specified duration.
     * Useful for detecting hung jobs.
     *
     * @param cutoffTime jobs started before this time are considered stuck
     * @return list of potentially stuck jobs
     */
    @Query("SELECT j FROM ProcessingJob j WHERE j.status = 'PROCESSING' AND j.startedAt < :cutoffTime")
    List<ProcessingJob> findStuckJobs(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Delete old completed jobs (cleanup operation).
     *
     * @param cutoffTime jobs completed before this time will be deleted
     * @return number of deleted jobs
     */
    @Query("DELETE FROM ProcessingJob j WHERE j.status IN ('COMPLETED', 'FAILED', 'CANCELLED') AND j.completedAt < :cutoffTime")
    int deleteOldCompletedJobs(@Param("cutoffTime") LocalDateTime cutoffTime);
}
