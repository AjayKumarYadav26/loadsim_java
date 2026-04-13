package com.ktlo.simulator.service.web;

import com.ktlo.simulator.model.web.ProcessingJob;
import com.ktlo.simulator.model.web.SimulationConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.ktlo.simulator.model.web.ProcessingJob.OperationType;
import static com.ktlo.simulator.model.web.ProcessingJob.QualityLevel;
import static com.ktlo.simulator.model.web.SimulationConfig.SimulationType;

/**
 * CRITICAL SERVICE: Maps user operations to simulation endpoints.
 * This is the "brain" that connects the document processing disguise to actual simulation APIs.
 *
 * Mapping Strategy:
 * - ANALYZE operations → CPU-intensive simulations (text analysis is CPU-heavy)
 * - CONVERT operations → Database simulations (data format conversion uses DB)
 * - OPTIMIZE operations → CPU simulations (compression/optimization is CPU-heavy)
 * - REPORT operations → CPU simulations (report generation is computationally intensive)
 *
 * Quality levels determine simulation intensity:
 * - HIGH = Most intensive simulation (longest duration, highest load)
 * - MEDIUM = Moderate simulation
 * - LOW = Light simulation (shorter duration, lower load)
 */
@Slf4j
@Service
public class SimulationMappingService {

    /**
     * Map a processing job to its corresponding simulation configuration.
     * This method determines which simulation API to call based on:
     * 1. Operation type (ANALYZE, CONVERT, OPTIMIZE, REPORT)
     * 2. File type (PDF, CSV, XLSX, TXT)
     * 3. Quality level (HIGH, MEDIUM, LOW)
     *
     * @param job the processing job
     * @return simulation configuration with endpoint and parameters
     */
    public SimulationConfig mapToSimulation(ProcessingJob job) {
        OperationType operation = job.getOperationType();
        QualityLevel quality = job.getQuality();
        String fileType = job.getFileType().toUpperCase();

        log.info("Mapping job {} to simulation: operation={}, quality={}, fileType={}",
                job.getJobId(), operation, quality, fileType);

        SimulationConfig config;
        switch (operation) {
            case ANALYZE:
                config = mapAnalyzeOperation(fileType, quality);
                break;
            case CONVERT:
                config = mapConvertOperation(fileType, quality);
                break;
            case OPTIMIZE:
                config = mapOptimizeOperation(fileType, quality);
                break;
            case REPORT:
                config = mapReportOperation(fileType, quality);
                break;
            default:
                throw new IllegalArgumentException("Unknown operation type: " + operation);
        }

        log.info("Job {} mapped to simulation: type={}, endpoint={}",
                job.getJobId(), config.getType(), config.getEndpoint());

        return config;
    }

    /**
     * Map ANALYZE operation to CPU-intensive simulations.
     * Document analysis (text extraction, sentiment analysis) is CPU-heavy.
     *
     * Mapping:
     * - PDF + HIGH → CPU Exhaust (taskCount=100, duration=120)
     * - PDF + MEDIUM → CPU Intensive (duration=60)
     * - PDF/Other + LOW → Fibonacci calculation (n=40)
     */
    private SimulationConfig mapAnalyzeOperation(String fileType, QualityLevel quality) {
        switch (quality) {
            case HIGH: {
                SimulationConfig config = SimulationConfig.builder()
                        .type(SimulationType.CPU_LOAD)
                        .endpoint("/api/cpu/exhaust")
                        .method("POST")
                        .expectedDurationSeconds(120)
                        .description("CPU Exhaustion - Analyzing document with deep learning models")
                        .build();
                config.addParam("taskCount", 100);
                config.addParam("duration", 120);
                return config;
            }

            case MEDIUM: {
                SimulationConfig config = SimulationConfig.builder()
                        .type(SimulationType.CPU_LOAD)
                        .endpoint("/api/cpu/intensive")
                        .method("POST")
                        .expectedDurationSeconds(60)
                        .description("CPU Intensive Task - Medium quality text analysis")
                        .build();
                config.addParam("duration", 60);
                return config;
            }

            case LOW:
                return SimulationConfig.builder()
                        .type(SimulationType.CPU_LOAD)
                        .endpoint("/api/cpu/fibonacci/40")
                        .method("POST")
                        .expectedDurationSeconds(30)
                        .description("Fibonacci Calculation - Basic text extraction")
                        .build();

            default:
                throw new IllegalArgumentException("Unknown quality level: " + quality);
        }
    }

    /**
     * Map CONVERT operation to database simulations.
     * Format conversion requires database queries and data transformation.
     *
     * Mapping:
     * - CSV/XLSX + HIGH → Database Timeout
     * - CSV/XLSX + MEDIUM → Slow Query (delay=10s)
     * - CSV/XLSX + LOW → Slow Query (delay=5s)
     * - PDF + ANY → Database operations with varying delays
     */
    private SimulationConfig mapConvertOperation(String fileType, QualityLevel quality) {
        boolean isDataFile = fileType.equals("CSV") || fileType.equals("XLSX") || fileType.equals("XLS");

        if (isDataFile && quality == QualityLevel.HIGH) {
            return SimulationConfig.builder()
                    .type(SimulationType.DATABASE_FAILURE)
                    .endpoint("/api/db/timeout")
                    .method("POST")
                    .expectedDurationSeconds(90)
                    .description("Database Timeout - Complex format conversion with large dataset")
                    .build();
        }

        if (isDataFile && quality == QualityLevel.MEDIUM) {
            SimulationConfig config = SimulationConfig.builder()
                    .type(SimulationType.DATABASE_FAILURE)
                    .endpoint("/api/db/slow-query")
                    .method("POST")
                    .expectedDurationSeconds(50)
                    .description("Slow Database Query - Medium complexity conversion")
                    .build();
            config.addParam("delay", 10);
            return config;
        }

        // LOW quality or non-data files
        SimulationConfig config = SimulationConfig.builder()
                .type(SimulationType.DATABASE_FAILURE)
                .endpoint("/api/db/slow-query")
                .method("POST")
                .expectedDurationSeconds(25)
                .description("Database Query - Basic format conversion")
                .build();
        config.addParam("delay", 5);
        return config;
    }

    /**
     * Map OPTIMIZE operation to CPU simulations.
     * Document optimization (compression, image optimization) is CPU-heavy.
     *
     * Mapping:
     * - HIGH → CPU Exhaust (taskCount=50, duration=60)
     * - MEDIUM → CPU Intensive (duration=30)
     * - LOW → Fibonacci calculation (n=35)
     */
    private SimulationConfig mapOptimizeOperation(String fileType, QualityLevel quality) {
        SimulationConfig config;
        switch (quality) {
            case HIGH:
                config = SimulationConfig.builder()
                        .type(SimulationType.CPU_LOAD)
                        .endpoint("/api/cpu/exhaust")
                        .method("POST")
                        .expectedDurationSeconds(60)
                        .description("CPU Exhaustion - Deep optimization with multiple passes")
                        .build();
                config.addParam("taskCount", 50);
                config.addParam("duration", 60);
                return config;

            case MEDIUM:
                config = SimulationConfig.builder()
                        .type(SimulationType.CPU_LOAD)
                        .endpoint("/api/cpu/intensive")
                        .method("POST")
                        .expectedDurationSeconds(30)
                        .description("CPU Intensive - Standard optimization")
                        .build();
                config.addParam("duration", 30);
                return config;

            case LOW:
                return SimulationConfig.builder()
                        .type(SimulationType.CPU_LOAD)
                        .endpoint("/api/cpu/fibonacci/35")
                        .method("POST")
                        .expectedDurationSeconds(15)
                        .description("Fibonacci Calculation - Basic compression")
                        .build();

            default:
                throw new IllegalArgumentException("Unknown quality level: " + quality);
        }
    }

    /**
     * Map REPORT operation to CPU simulations.
     * Report generation involves heavy computation (data aggregation, charts, PDF rendering).
     *
     * Mapping:
     * - HIGH → Prime number calculation (limit=10,000,000)
     * - MEDIUM → Prime number calculation (limit=5,000,000)
     * - LOW → Prime number calculation (limit=1,000,000)
     */
    private SimulationConfig mapReportOperation(String fileType, QualityLevel quality) {
        SimulationConfig config;
        switch (quality) {
            case HIGH:
                config = SimulationConfig.builder()
                        .type(SimulationType.CPU_LOAD)
                        .endpoint("/api/cpu/primes")
                        .method("POST")
                        .expectedDurationSeconds(90)
                        .description("Prime Calculation - Comprehensive report with analytics")
                        .build();
                config.addParam("limit", 10000000);
                return config;

            case MEDIUM:
                config = SimulationConfig.builder()
                        .type(SimulationType.CPU_LOAD)
                        .endpoint("/api/cpu/primes")
                        .method("POST")
                        .expectedDurationSeconds(45)
                        .description("Prime Calculation - Standard report generation")
                        .build();
                config.addParam("limit", 5000000);
                return config;

            case LOW:
                config = SimulationConfig.builder()
                        .type(SimulationType.CPU_LOAD)
                        .endpoint("/api/cpu/primes")
                        .method("POST")
                        .expectedDurationSeconds(20)
                        .description("Prime Calculation - Basic summary report")
                        .build();
                config.addParam("limit", 1000000);
                return config;

            default:
                throw new IllegalArgumentException("Unknown quality level: " + quality);
        }
    }

    /**
     * Get a human-readable description of what processing will occur.
     * This is shown to users during processing (maintains the disguise).
     *
     * @param job the processing job
     * @return user-friendly description
     */
    public String getProcessingDescription(ProcessingJob job) {
        OperationType operation = job.getOperationType();
        QualityLevel quality = job.getQuality();

        switch (operation) {
            case ANALYZE:
                switch (quality) {
                    case HIGH:
                        return "Performing deep analysis with AI models...";
                    case MEDIUM:
                        return "Extracting text and analyzing content...";
                    case LOW:
                        return "Scanning document structure...";
                    default:
                        return "Analyzing document...";
                }
            case CONVERT:
                switch (quality) {
                    case HIGH:
                        return "Converting format with maximum quality preservation...";
                    case MEDIUM:
                        return "Converting document format...";
                    case LOW:
                        return "Performing quick format conversion...";
                    default:
                        return "Converting document...";
                }
            case OPTIMIZE:
                switch (quality) {
                    case HIGH:
                        return "Applying advanced optimization algorithms...";
                    case MEDIUM:
                        return "Optimizing document size and quality...";
                    case LOW:
                        return "Compressing document...";
                    default:
                        return "Optimizing document...";
                }
            case REPORT:
                switch (quality) {
                    case HIGH:
                        return "Generating comprehensive analytical report...";
                    case MEDIUM:
                        return "Creating standard report with charts...";
                    case LOW:
                        return "Building summary report...";
                    default:
                        return "Generating report...";
                }
            default:
                return "Processing document...";
        }
    }
}
