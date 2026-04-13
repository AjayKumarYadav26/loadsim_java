package com.ktlo.simulator.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Global exception handler for centralized error handling.
 * Catches exceptions from all controllers and provides standardized error responses.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle SQL exceptions (database errors).
     */
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<Map<String, Object>> handleSQLException(SQLException ex, WebRequest request) {
        log.error("SQL Exception occurred: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(
                        "DATABASE_ERROR",
                        "Database operation failed: " + ex.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        request.getDescription(false)
                ));
    }

    /**
     * Handle SQL timeout exceptions.
     */
    @ExceptionHandler(SQLTimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleSQLTimeoutException(SQLTimeoutException ex, WebRequest request) {
        log.error("SQL Timeout Exception: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .body(createErrorResponse(
                        "DATABASE_TIMEOUT",
                        "Database query timed out: " + ex.getMessage(),
                        HttpStatus.GATEWAY_TIMEOUT.value(),
                        request.getDescription(false)
                ));
    }

    /**
     * Handle Spring Data Access exceptions.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(DataAccessException ex, WebRequest request) {
        log.error("Data Access Exception: {}", ex.getMessage(), ex);

        String errorMessage = "Database access error";
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        // Check for specific causes
        Throwable cause = ex.getRootCause();
        if (cause instanceof SQLTimeoutException) {
            errorMessage = "Database query timed out";
            status = HttpStatus.GATEWAY_TIMEOUT;
        } else if (cause instanceof SQLException) {
            errorMessage = "Database connection or query failed";
        }

        return ResponseEntity
                .status(status)
                .body(createErrorResponse(
                        "DATA_ACCESS_ERROR",
                        errorMessage + ": " + ex.getMessage(),
                        status.value(),
                        request.getDescription(false)
                ));
    }

    /**
     * Handle timeout exceptions.
     */
    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleTimeoutException(TimeoutException ex, WebRequest request) {
        log.error("Timeout Exception: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .body(createErrorResponse(
                        "TIMEOUT_ERROR",
                        "Operation timed out: " + ex.getMessage(),
                        HttpStatus.GATEWAY_TIMEOUT.value(),
                        request.getDescription(false)
                ));
    }

    /**
     * Handle rejected execution exceptions (threadpool exhaustion).
     */
    @ExceptionHandler(RejectedExecutionException.class)
    public ResponseEntity<Map<String, Object>> handleRejectedExecutionException(RejectedExecutionException ex, WebRequest request) {
        log.error("Rejected Execution Exception - Threadpool exhausted: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(createErrorResponse(
                        "THREADPOOL_EXHAUSTED",
                        "Server threadpool is exhausted, cannot accept more tasks: " + ex.getMessage(),
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        request.getDescription(false)
                ));
    }

    /**
     * Handle illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal Argument Exception: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(createErrorResponse(
                        "INVALID_ARGUMENT",
                        ex.getMessage(),
                        HttpStatus.BAD_REQUEST.value(),
                        request.getDescription(false)
                ));
    }

    /**
     * Handle illegal state exceptions.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        log.error("Illegal State Exception: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(createErrorResponse(
                        "INVALID_STATE",
                        ex.getMessage(),
                        HttpStatus.CONFLICT.value(),
                        request.getDescription(false)
                ));
    }

    /**
     * Handle all other uncaught exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(
                        "INTERNAL_ERROR",
                        "An unexpected error occurred: " + ex.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        request.getDescription(false)
                ));
    }

    /**
     * Helper method to create standardized error response.
     */
    private Map<String, Object> createErrorResponse(String errorCode, String message, int statusCode, String path) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", System.currentTimeMillis());
        error.put("status", statusCode);
        error.put("error", errorCode);
        error.put("message", message);
        error.put("path", path);

        return error;
    }
}
