package com.ktlo.simulator.model.web;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity representing a document processing job.
 * Stores information about user uploads and their processing status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "processing_jobs", indexes = {
    @Index(name = "idx_job_status", columnList = "status"),
    @Index(name = "idx_job_created", columnList = "created_at")
})
public class ProcessingJob {

    /**
     * Unique identifier for the job (UUID format).
     */
    @Id
    @Column(name = "job_id", length = 36, nullable = false)
    private String jobId;

    /**
     * Original filename uploaded by the user.
     */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /**
     * File type/extension (PDF, CSV, XLSX, TXT).
     */
    @Column(name = "file_type", length = 10, nullable = false)
    private String fileType;

    /**
     * File size in bytes.
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * Stored file path on the server.
     */
    @Column(name = "file_path")
    private String filePath;

    /**
     * Type of operation selected by the user.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", length = 20, nullable = false)
    private OperationType operationType;

    /**
     * Quality level selected by the user.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "quality", length = 10, nullable = false)
    private QualityLevel quality;

    /**
     * Current status of the job.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private ProcessingStatus status = ProcessingStatus.QUEUED;

    /**
     * Current progress percentage (0-100).
     */
    @Column(name = "progress")
    @Builder.Default
    private Integer progress = 0;

    /**
     * Type of simulation triggered for this job (for admin transparency).
     */
    @Column(name = "simulation_type", length = 50)
    private String simulationType;

    /**
     * Simulation endpoint called (for admin transparency).
     */
    @Column(name = "simulation_endpoint")
    private String simulationEndpoint;

    /**
     * Result message or output from processing.
     */
    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    /**
     * Error message if job failed.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Timestamp when the job was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the job was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Timestamp when processing started.
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * Timestamp when processing completed (success or failure).
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Types of document processing operations available to users.
     */
    public enum OperationType {
        /**
         * Analyze document - extract text, sentiment analysis, keywords.
         */
        ANALYZE,

        /**
         * Convert document format - PDF to CSV, XLSX to PDF, etc.
         */
        CONVERT,

        /**
         * Optimize document - compress, reduce size, optimize images.
         */
        OPTIMIZE,

        /**
         * Generate report - create summary reports from data files.
         */
        REPORT
    }

    /**
     * Quality levels for processing operations.
     * Higher quality = longer processing time = more intensive simulation.
     */
    public enum QualityLevel {
        /**
         * Low quality - fast processing, basic results.
         */
        LOW,

        /**
         * Medium quality - balanced processing time and results.
         */
        MEDIUM,

        /**
         * High quality - best results, longest processing time.
         */
        HIGH
    }

    /**
     * Calculate the duration of the job in seconds.
     *
     * @return duration in seconds, or 0 if not completed
     */
    public long getDurationSeconds() {
        if (startedAt == null || completedAt == null) {
            return 0;
        }
        return java.time.Duration.between(startedAt, completedAt).getSeconds();
    }

    /**
     * Check if the job is in a terminal state.
     *
     * @return true if the job is completed, failed, or cancelled
     */
    public boolean isTerminal() {
        return status != null && status.isTerminal();
    }

    /**
     * Check if the job is currently active.
     *
     * @return true if the job is uploading or processing
     */
    public boolean isActive() {
        return status != null && status.isActive();
    }
}
