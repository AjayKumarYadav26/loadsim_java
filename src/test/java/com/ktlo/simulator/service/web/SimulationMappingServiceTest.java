package com.ktlo.simulator.service.web;

import com.ktlo.simulator.model.web.ProcessingJob;
import com.ktlo.simulator.model.web.SimulationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static com.ktlo.simulator.model.web.ProcessingJob.OperationType.*;
import static com.ktlo.simulator.model.web.ProcessingJob.QualityLevel.*;
import static com.ktlo.simulator.model.web.SimulationConfig.SimulationType.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SimulationMappingService.
 * Validates that all operation/quality combinations map to correct simulation endpoints.
 */
class SimulationMappingServiceTest {

    private SimulationMappingService mappingService;

    @BeforeEach
    void setUp() {
        mappingService = new SimulationMappingService();
    }

    // =========================================================================
    // ANALYZE Operation Tests
    // =========================================================================

    @Test
    @DisplayName("ANALYZE + PDF + HIGH -> CPU Exhaust")
    void testAnalyzePdfHigh() {
        ProcessingJob job = createJob("test.pdf", ANALYZE, HIGH);
        SimulationConfig config = mappingService.mapToSimulation(job);

        assertEquals(CPU_LOAD, config.getType());
        assertEquals("/api/cpu/exhaust", config.getEndpoint());
        assertEquals("POST", config.getMethod());
        assertEquals(120, config.getExpectedDurationSeconds());
        assertEquals(100, config.getParams().get("taskCount"));
        assertEquals(120, config.getParams().get("duration"));
    }

    @Test
    @DisplayName("ANALYZE + PDF + MEDIUM -> CPU Intensive")
    void testAnalyzePdfMedium() {
        ProcessingJob job = createJob("test.pdf", ANALYZE, MEDIUM);
        SimulationConfig config = mappingService.mapToSimulation(job);

        assertEquals(CPU_LOAD, config.getType());
        assertEquals("/api/cpu/intensive", config.getEndpoint());
        assertEquals("POST", config.getMethod());
        assertEquals(60, config.getExpectedDurationSeconds());
        assertEquals(60, config.getParams().get("duration"));
    }

    @Test
    @DisplayName("ANALYZE + PDF + LOW -> Fibonacci")
    void testAnalyzePdfLow() {
        ProcessingJob job = createJob("test.pdf", ANALYZE, LOW);
        SimulationConfig config = mappingService.mapToSimulation(job);

        assertEquals(CPU_LOAD, config.getType());
        assertEquals("/api/cpu/fibonacci/40", config.getEndpoint());
        assertEquals("POST", config.getMethod());
        assertEquals(30, config.getExpectedDurationSeconds());
    }

    @Test
    @DisplayName("ANALYZE works for all file types")
    void testAnalyzeMultipleFileTypes() {
        String[] fileTypes = {"test.pdf", "test.csv", "test.xlsx", "test.txt"};

        for (String filename : fileTypes) {
            ProcessingJob job = createJob(filename, ANALYZE, HIGH);
            SimulationConfig config = mappingService.mapToSimulation(job);

            assertEquals(CPU_LOAD, config.getType());
            assertEquals("/api/cpu/exhaust", config.getEndpoint());
        }
    }

    // =========================================================================
    // CONVERT Operation Tests
    // =========================================================================

    @Test
    @DisplayName("CONVERT + CSV + HIGH -> Database Timeout")
    void testConvertCsvHigh() {
        ProcessingJob job = createJob("test.csv", CONVERT, HIGH);
        SimulationConfig config = mappingService.mapToSimulation(job);

        assertEquals(DATABASE_FAILURE, config.getType());
        assertEquals("/api/db/timeout", config.getEndpoint());
        assertEquals("POST", config.getMethod());
        assertEquals(90, config.getExpectedDurationSeconds());
    }

    @Test
    @DisplayName("CONVERT + XLSX + MEDIUM -> Slow Query (10s)")
    void testConvertXlsxMedium() {
        ProcessingJob job = createJob("test.xlsx", CONVERT, MEDIUM);
        SimulationConfig config = mappingService.mapToSimulation(job);

        assertEquals(DATABASE_FAILURE, config.getType());
        assertEquals("/api/db/slow-query", config.getEndpoint());
        assertEquals(50, config.getExpectedDurationSeconds());
        assertEquals(10, config.getParams().get("delay"));
    }

    @Test
    @DisplayName("CONVERT + CSV + LOW -> Slow Query (5s)")
    void testConvertCsvLow() {
        ProcessingJob job = createJob("test.csv", CONVERT, LOW);
        SimulationConfig config = mappingService.mapToSimulation(job);

        assertEquals(DATABASE_FAILURE, config.getType());
        assertEquals("/api/db/slow-query", config.getEndpoint());
        assertEquals(25, config.getExpectedDurationSeconds());
        assertEquals(5, config.getParams().get("delay"));
    }

    @Test
    @DisplayName("CONVERT + PDF -> Slow Query (non-data file)")
    void testConvertPdf() {
        ProcessingJob job = createJob("test.pdf", CONVERT, HIGH);
        SimulationConfig config = mappingService.mapToSimulation(job);

        // PDF is not a data file, so it uses the LOW quality mapping
        assertEquals(DATABASE_FAILURE, config.getType());
        assertEquals("/api/db/slow-query", config.getEndpoint());
        assertEquals(5, config.getParams().get("delay"));
    }

    // =========================================================================
    // OPTIMIZE Operation Tests
    // =========================================================================

    @Test
    @DisplayName("OPTIMIZE + HIGH -> CPU Exhaust")
    void testOptimizeHigh() {
        ProcessingJob job = createJob("test.pdf", OPTIMIZE, HIGH);
        SimulationConfig config = mappingService.mapToSimulation(job);

        assertEquals(CPU_LOAD, config.getType());
        assertEquals("/api/cpu/exhaust", config.getEndpoint());
        assertEquals("POST", config.getMethod());
        assertEquals(60, config.getExpectedDurationSeconds());
        assertEquals(50, config.getParams().get("taskCount"));
        assertEquals(60, config.getParams().get("duration"));
    }

    @Test
    @DisplayName("OPTIMIZE + MEDIUM -> CPU Intensive")
    void testOptimizeMedium() {
        ProcessingJob job = createJob("test.csv", OPTIMIZE, MEDIUM);
        SimulationConfig config = mappingService.mapToSimulation(job);

        assertEquals(CPU_LOAD, config.getType());
        assertEquals("/api/cpu/intensive", config.getEndpoint());
        assertEquals(30, config.getExpectedDurationSeconds());
        assertEquals(30, config.getParams().get("duration"));
    }

    @Test
    @DisplayName("OPTIMIZE + LOW -> Fibonacci(35)")
    void testOptimizeLow() {
        ProcessingJob job = createJob("test.xlsx", OPTIMIZE, LOW);
        SimulationConfig config = mappingService.mapToSimulation(job);

        assertEquals(CPU_LOAD, config.getType());
        assertEquals("/api/cpu/fibonacci/35", config.getEndpoint());
        assertEquals(15, config.getExpectedDurationSeconds());
    }

    // =========================================================================
    // REPORT Operation Tests
    // =========================================================================

    @Test
    @DisplayName("REPORT + HIGH -> Prime Calculation (10M)")
    void testReportHigh() {
        ProcessingJob job = createJob("test.csv", REPORT, HIGH);
        SimulationConfig config = mappingService.mapToSimulation(job);

        assertEquals(CPU_LOAD, config.getType());
        assertEquals("/api/cpu/primes", config.getEndpoint());
        assertEquals("POST", config.getMethod());
        assertEquals(90, config.getExpectedDurationSeconds());
        assertEquals(10000000, config.getParams().get("limit"));
    }

    @Test
    @DisplayName("REPORT + MEDIUM -> Prime Calculation (5M)")
    void testReportMedium() {
        ProcessingJob job = createJob("test.txt", REPORT, MEDIUM);
        SimulationConfig config = mappingService.mapToSimulation(job);

        assertEquals(CPU_LOAD, config.getType());
        assertEquals("/api/cpu/primes", config.getEndpoint());
        assertEquals(45, config.getExpectedDurationSeconds());
        assertEquals(5000000, config.getParams().get("limit"));
    }

    @Test
    @DisplayName("REPORT + LOW -> Prime Calculation (1M)")
    void testReportLow() {
        ProcessingJob job = createJob("test.xlsx", REPORT, LOW);
        SimulationConfig config = mappingService.mapToSimulation(job);

        assertEquals(CPU_LOAD, config.getType());
        assertEquals("/api/cpu/primes", config.getEndpoint());
        assertEquals(20, config.getExpectedDurationSeconds());
        assertEquals(1000000, config.getParams().get("limit"));
    }

    // =========================================================================
    // Processing Description Tests
    // =========================================================================

    @Test
    @DisplayName("Get processing description for ANALYZE operations")
    void testAnalyzeDescriptions() {
        ProcessingJob highJob = createJob("test.pdf", ANALYZE, HIGH);
        ProcessingJob mediumJob = createJob("test.pdf", ANALYZE, MEDIUM);
        ProcessingJob lowJob = createJob("test.pdf", ANALYZE, LOW);

        String highDesc = mappingService.getProcessingDescription(highJob);
        String mediumDesc = mappingService.getProcessingDescription(mediumJob);
        String lowDesc = mappingService.getProcessingDescription(lowJob);

        assertTrue(highDesc.contains("deep analysis"));
        assertTrue(mediumDesc.contains("Extracting text"));
        assertTrue(lowDesc.contains("Scanning"));
    }

    @Test
    @DisplayName("Get processing description for CONVERT operations")
    void testConvertDescriptions() {
        ProcessingJob highJob = createJob("test.csv", CONVERT, HIGH);
        ProcessingJob mediumJob = createJob("test.csv", CONVERT, MEDIUM);
        ProcessingJob lowJob = createJob("test.csv", CONVERT, LOW);

        String highDesc = mappingService.getProcessingDescription(highJob);
        String mediumDesc = mappingService.getProcessingDescription(mediumJob);
        String lowDesc = mappingService.getProcessingDescription(lowJob);

        assertTrue(highDesc.contains("maximum quality"));
        assertTrue(mediumDesc.contains("Converting document"));
        assertTrue(lowDesc.contains("quick format"));
    }

    @Test
    @DisplayName("Get processing description for OPTIMIZE operations")
    void testOptimizeDescriptions() {
        ProcessingJob highJob = createJob("test.pdf", OPTIMIZE, HIGH);
        String desc = mappingService.getProcessingDescription(highJob);
        assertTrue(desc.contains("advanced optimization"));
    }

    @Test
    @DisplayName("Get processing description for REPORT operations")
    void testReportDescriptions() {
        ProcessingJob highJob = createJob("test.csv", REPORT, HIGH);
        String desc = mappingService.getProcessingDescription(highJob);
        assertTrue(desc.contains("comprehensive analytical"));
    }

    // =========================================================================
    // Edge Cases and Validation Tests
    // =========================================================================

    @Test
    @DisplayName("All configurations have valid endpoints")
    void testAllConfigurationsHaveEndpoints() {
        ProcessingJob.OperationType[] operations = {ANALYZE, CONVERT, OPTIMIZE, REPORT};
        ProcessingJob.QualityLevel[] qualities = {HIGH, MEDIUM, LOW};
        String[] files = {"test.pdf", "test.csv", "test.xlsx", "test.txt"};

        for (ProcessingJob.OperationType op : operations) {
            for (ProcessingJob.QualityLevel qual : qualities) {
                for (String file : files) {
                    ProcessingJob job = createJob(file, op, qual);
                    SimulationConfig config = mappingService.mapToSimulation(job);

                    assertNotNull(config, "Config should not be null for " + op + "/" + qual + "/" + file);
                    assertNotNull(config.getType(), "Type should not be null");
                    assertNotNull(config.getEndpoint(), "Endpoint should not be null");
                    assertNotNull(config.getMethod(), "Method should not be null");
                    assertTrue(config.getExpectedDurationSeconds() > 0, "Duration should be positive");
                    assertNotNull(config.getDescription(), "Description should not be null");
                }
            }
        }
    }

    @Test
    @DisplayName("All configurations have appropriate expected durations")
    void testExpectedDurations() {
        // HIGH quality should have longer durations than LOW
        ProcessingJob highJob = createJob("test.pdf", ANALYZE, HIGH);
        ProcessingJob lowJob = createJob("test.pdf", ANALYZE, LOW);

        SimulationConfig highConfig = mappingService.mapToSimulation(highJob);
        SimulationConfig lowConfig = mappingService.mapToSimulation(lowJob);

        assertTrue(highConfig.getExpectedDurationSeconds() > lowConfig.getExpectedDurationSeconds(),
                "HIGH quality should take longer than LOW quality");
    }

    @Test
    @DisplayName("Case insensitive file type handling")
    void testCaseInsensitiveFileTypes() {
        ProcessingJob upperJob = createJob("TEST.PDF", ANALYZE, HIGH);
        ProcessingJob lowerJob = createJob("test.pdf", ANALYZE, HIGH);
        ProcessingJob mixedJob = createJob("Test.Pdf", ANALYZE, HIGH);

        SimulationConfig upperConfig = mappingService.mapToSimulation(upperJob);
        SimulationConfig lowerConfig = mappingService.mapToSimulation(lowerJob);
        SimulationConfig mixedConfig = mappingService.mapToSimulation(mixedJob);

        assertEquals(upperConfig.getEndpoint(), lowerConfig.getEndpoint());
        assertEquals(lowerConfig.getEndpoint(), mixedConfig.getEndpoint());
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    /**
     * Create a test ProcessingJob with specified parameters.
     */
    private ProcessingJob createJob(String filename, ProcessingJob.OperationType operation,
                                   ProcessingJob.QualityLevel quality) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1);

        return ProcessingJob.builder()
                .jobId("test-job-" + System.nanoTime())
                .fileName(filename)
                .fileType(extension)
                .operationType(operation)
                .quality(quality)
                .build();
    }
}
