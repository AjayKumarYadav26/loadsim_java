package com.ktlo.simulator.controller.web;

import com.ktlo.simulator.model.web.ProcessingJob;
import com.ktlo.simulator.model.web.ProcessingStatus;
import com.ktlo.simulator.repository.ProcessingJobRepository;
import com.ktlo.simulator.service.web.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling file uploads for the Document Processing Portal.
 * Accepts multipart file uploads and creates processing jobs.
 */
@Slf4j
@RestController
@RequestMapping("/api/portal/upload")
@RequiredArgsConstructor
public class DocumentUploadController {

    private final FileStorageService fileStorageService;
    private final ProcessingJobRepository jobRepository;

    /**
     * Upload a document for processing.
     *
     * @param file the uploaded file
     * @param operationType the operation to perform (ANALYZE, CONVERT, OPTIMIZE, REPORT)
     * @param quality the quality level (HIGH, MEDIUM, LOW)
     * @return response with job ID and status
     */
    @PostMapping("/document")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("operationType") String operationType,
            @RequestParam("quality") String quality) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Received file upload: filename={}, size={}, operation={}, quality={}",
                    file.getOriginalFilename(), file.getSize(), operationType, quality);

            // Validate file
            fileStorageService.validateFile(file);

            // Store file and get job ID
            FileStorageService.FileStorageResult storageResult = fileStorageService.storeFile(file);

            // Create processing job entity
            ProcessingJob job = ProcessingJob.builder()
                    .jobId(storageResult.getJobId())
                    .fileName(storageResult.getFileName())
                    .fileType(fileStorageService.getFileExtension(storageResult.getFileName()))
                    .fileSize(storageResult.getFileSize())
                    .filePath(storageResult.getFilePath())
                    .operationType(ProcessingJob.OperationType.valueOf(operationType.toUpperCase()))
                    .quality(ProcessingJob.QualityLevel.valueOf(quality.toUpperCase()))
                    .status(ProcessingStatus.QUEUED)
                    .progress(0)
                    .createdAt(LocalDateTime.now())
                    .build();

            // Save to database
            jobRepository.save(job);

            log.info("Created processing job: jobId={}, fileName={}, operation={}, quality={}",
                    job.getJobId(), job.getFileName(), job.getOperationType(), job.getQuality());

            // Build success response
            response.put("success", true);
            response.put("jobId", job.getJobId());
            response.put("fileName", job.getFileName());
            response.put("fileSize", job.getFileSize());
            response.put("operationType", job.getOperationType());
            response.put("quality", job.getQuality());
            response.put("message", "File uploaded successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error during file upload: {}", e.getMessage());
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            log.error("Error during file upload", e);
            response.put("success", false);
            response.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get upload statistics.
     *
     * @return storage statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUploadStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("totalFiles", fileStorageService.getFileCount());
            stats.put("totalSize", fileStorageService.getTotalStorageSize());
            stats.put("totalJobs", jobRepository.count());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting upload stats", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(stats);
        }
    }
}
