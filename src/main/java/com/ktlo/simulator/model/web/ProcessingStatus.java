package com.ktlo.simulator.model.web;

/**
 * Status values for document processing jobs.
 * Tracks the lifecycle of a processing request from upload to completion.
 */
public enum ProcessingStatus {
    /**
     * Job has been created and is waiting to be processed.
     */
    QUEUED,

    /**
     * File is currently being uploaded to the server.
     */
    UPLOADING,

    /**
     * Job is currently being processed (simulation is running).
     */
    PROCESSING,

    /**
     * Job completed successfully.
     */
    COMPLETED,

    /**
     * Job failed due to an error during processing.
     */
    FAILED,

    /**
     * Job was cancelled by the user or system.
     */
    CANCELLED;

    /**
     * Check if this status represents a terminal state (job is finished).
     *
     * @return true if the status is COMPLETED, FAILED, or CANCELLED
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    /**
     * Check if this status represents an active state (job is in progress).
     *
     * @return true if the status is UPLOADING or PROCESSING
     */
    public boolean isActive() {
        return this == UPLOADING || this == PROCESSING;
    }
}
