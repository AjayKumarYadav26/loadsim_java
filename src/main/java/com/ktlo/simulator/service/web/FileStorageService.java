package com.ktlo.simulator.service.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for managing file uploads and storage.
 * Handles file validation, storage, and cleanup operations.
 */
@Slf4j
@Service
public class FileStorageService {

    /**
     * Allowed file extensions for upload.
     */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "csv", "xlsx", "txt", "xls");

    /**
     * Maximum file size in bytes (50 MB).
     */
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    @Value("${app.upload.dir:${java.io.tmpdir}/ktlo-uploads}")
    private String uploadDir;

    @Value("${app.upload.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    @Value("${app.upload.cleanup.max-age-hours:24}")
    private int maxAgeHours;

    private Path uploadPath;

    /**
     * Initialize the upload directory on service startup.
     */
    @PostConstruct
    public void init() {
        try {
            uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
            } else {
                log.info("Upload directory exists: {}", uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", uploadDir, e);
            throw new RuntimeException("Could not initialize file storage", e);
        }
    }

    /**
     * Store an uploaded file and return the job ID and file path.
     *
     * @param file the uploaded multipart file
     * @return FileStorageResult containing job ID and file path
     * @throws IOException if file storage fails
     * @throws IllegalArgumentException if file validation fails
     */
    public FileStorageResult storeFile(MultipartFile file) throws IOException {
        // Validate file
        validateFile(file);

        // Generate unique job ID
        String jobId = UUID.randomUUID().toString();

        // Sanitize filename
        String originalFilename = file.getOriginalFilename();
        String sanitizedFilename = sanitizeFilename(originalFilename);

        // Create unique filename with job ID prefix
        String storedFilename = jobId + "_" + sanitizedFilename;

        // Store file
        Path targetPath = uploadPath.resolve(storedFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("Stored file: {} (jobId: {}, size: {} bytes)", sanitizedFilename, jobId, file.getSize());

        return FileStorageResult.builder()
                .jobId(jobId)
                .filePath(targetPath.toString())
                .fileName(sanitizedFilename)
                .fileSize(file.getSize())
                .build();
    }

    /**
     * Validate uploaded file (type, size, content).
     *
     * @param file the file to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateFile(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                String.format("File size exceeds maximum allowed size of %d MB", MAX_FILE_SIZE / (1024 * 1024))
            );
        }

        // Check file extension
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }

        String extension = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                String.format("File type '%s' is not allowed. Allowed types: %s",
                    extension, String.join(", ", ALLOWED_EXTENSIONS))
            );
        }

        // Verify file content type matches extension (basic check)
        String contentType = file.getContentType();
        if (contentType != null && !isContentTypeValid(extension, contentType)) {
            log.warn("Content type mismatch: extension={}, contentType={}", extension, contentType);
            // Don't throw exception, just log warning - browsers can send different content types
        }
    }

    /**
     * Get file extension from filename.
     *
     * @param filename the filename
     * @return file extension (without dot)
     */
    public String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * Sanitize filename to prevent directory traversal and special characters.
     *
     * @param filename the original filename
     * @return sanitized filename
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unnamed_file";
        }

        // Remove path separators and null bytes
        String sanitized = filename.replaceAll("[/\\\\\\x00]", "");

        // Remove or replace other potentially dangerous characters
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Limit filename length
        if (sanitized.length() > 200) {
            String extension = getFileExtension(sanitized);
            String nameWithoutExt = sanitized.substring(0, sanitized.length() - extension.length() - 1);
            sanitized = nameWithoutExt.substring(0, 195) + "." + extension;
        }

        return sanitized;
    }

    /**
     * Check if content type matches the file extension.
     *
     * @param extension file extension
     * @param contentType MIME content type
     * @return true if valid match
     */
    private boolean isContentTypeValid(String extension, String contentType) {
        switch (extension.toLowerCase()) {
            case "pdf":
                return contentType.contains("pdf");
            case "csv":
                return contentType.contains("csv") || contentType.contains("text") || contentType.contains("plain");
            case "xlsx":
            case "xls":
                return contentType.contains("spreadsheet") || contentType.contains("excel") ||
                       contentType.contains("officedocument");
            case "txt":
                return contentType.contains("text") || contentType.contains("plain");
            default:
                return true; // Allow by default
        }
    }

    /**
     * Delete a stored file.
     *
     * @param filePath path to the file to delete
     * @return true if deleted successfully
     */
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                log.info("Deleted file: {}", filePath);
            }
            return deleted;
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filePath, e);
            return false;
        }
    }

    /**
     * Cleanup old files that exceed the max age.
     * Called periodically or manually.
     *
     * @return number of files deleted
     */
    public int cleanupOldFiles() {
        if (!cleanupEnabled) {
            log.debug("File cleanup is disabled");
            return 0;
        }

        LocalDateTime cutoffTime = LocalDateTime.now().minus(maxAgeHours, ChronoUnit.HOURS);
        int deletedCount = 0;

        try (Stream<Path> paths = Files.walk(uploadPath)) {
            List<Path> oldFiles = paths
                .filter(Files::isRegularFile)
                .filter(path -> {
                    try {
                        LocalDateTime fileTime = LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(path).toInstant(),
                            java.time.ZoneId.systemDefault()
                        );
                        return fileTime.isBefore(cutoffTime);
                    } catch (IOException e) {
                        log.error("Error checking file time: {}", path, e);
                        return false;
                    }
                })
                .collect(Collectors.toList());

            for (Path file : oldFiles) {
                try {
                    Files.delete(file);
                    deletedCount++;
                    log.debug("Deleted old file: {}", file.getFileName());
                } catch (IOException e) {
                    log.error("Failed to delete old file: {}", file, e);
                }
            }

            if (deletedCount > 0) {
                log.info("Cleaned up {} old files (older than {} hours)", deletedCount, maxAgeHours);
            }

        } catch (IOException e) {
            log.error("Error during file cleanup", e);
        }

        return deletedCount;
    }

    /**
     * Get total size of all uploaded files.
     *
     * @return total size in bytes
     */
    public long getTotalStorageSize() {
        try (Stream<Path> paths = Files.walk(uploadPath)) {
            return paths
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0L;
                    }
                })
                .sum();
        } catch (IOException e) {
            log.error("Error calculating storage size", e);
            return 0L;
        }
    }

    /**
     * Get count of stored files.
     *
     * @return number of files in storage
     */
    public long getFileCount() {
        try (Stream<Path> paths = Files.walk(uploadPath)) {
            return paths.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            log.error("Error counting files", e);
            return 0L;
        }
    }

    /**
     * Result object for file storage operations.
     */
    @lombok.Data
    @lombok.Builder
    public static class FileStorageResult {
        private String jobId;
        private String filePath;
        private String fileName;
        private long fileSize;
    }
}
