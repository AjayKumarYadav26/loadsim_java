package com.ktlo.simulator.service.web;

import com.ktlo.simulator.model.web.ProcessingJob;
import com.ktlo.simulator.model.web.ProcessingStatus;
import com.ktlo.simulator.repository.ProcessingJobRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for generating analytics and statistics about processing jobs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ProcessingJobRepository jobRepository;

    /**
     * Get summary statistics for all jobs.
     *
     * @return summary statistics
     */
    public AnalyticsSummary getSummaryStats() {
        List<ProcessingJob> allJobs = jobRepository.findAll();

        long totalJobs = allJobs.size();
        long completedJobs = allJobs.stream()
                .filter(j -> j.getStatus() == ProcessingStatus.COMPLETED)
                .count();
        long failedJobs = allJobs.stream()
                .filter(j -> j.getStatus() == ProcessingStatus.FAILED)
                .count();
        long activeJobs = allJobs.stream()
                .filter(j -> j.getStatus().isActive())
                .count();

        double successRate = totalJobs > 0 ? (completedJobs * 100.0 / totalJobs) : 0;

        // Calculate average processing time (only for completed jobs)
        double avgProcessingTime = allJobs.stream()
                .filter(j -> j.getStatus() == ProcessingStatus.COMPLETED && j.getDurationSeconds() > 0)
                .mapToLong(ProcessingJob::getDurationSeconds)
                .average()
                .orElse(0);

        return AnalyticsSummary.builder()
                .totalJobs(totalJobs)
                .completedJobs(completedJobs)
                .failedJobs(failedJobs)
                .activeJobs(activeJobs)
                .successRate(successRate)
                .avgProcessingTimeSeconds((long) avgProcessingTime)
                .build();
    }

    /**
     * Get job trends for the last N days.
     *
     * @param days number of days to look back
     * @return list of daily job counts
     */
    public List<DailyJobCount> getJobTrends(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        List<ProcessingJob> recentJobs = jobRepository.findByCreatedAtAfter(cutoffDate);

        // Group jobs by date
        Map<String, Long> jobsByDate = recentJobs.stream()
                .collect(Collectors.groupingBy(
                        j -> j.getCreatedAt().toLocalDate().toString(),
                        Collectors.counting()
                ));

        // Convert to list of DailyJobCount
        return jobsByDate.entrySet().stream()
                .map(entry -> DailyJobCount.builder()
                        .date(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());
    }

    /**
     * Get breakdown of jobs by operation type.
     *
     * @return map of operation type to count
     */
    public Map<String, Long> getOperationBreakdown() {
        List<ProcessingJob> allJobs = jobRepository.findAll();

        return allJobs.stream()
                .collect(Collectors.groupingBy(
                        j -> j.getOperationType().name(),
                        Collectors.counting()
                ));
    }

    /**
     * Get breakdown of jobs by status.
     *
     * @return map of status to count
     */
    public Map<String, Long> getStatusBreakdown() {
        List<ProcessingJob> allJobs = jobRepository.findAll();

        return allJobs.stream()
                .collect(Collectors.groupingBy(
                        j -> j.getStatus().name(),
                        Collectors.counting()
                ));
    }

    /**
     * Get breakdown of jobs by quality level.
     *
     * @return map of quality level to count
     */
    public Map<String, Long> getQualityBreakdown() {
        List<ProcessingJob> allJobs = jobRepository.findAll();

        return allJobs.stream()
                .collect(Collectors.groupingBy(
                        j -> j.getQuality().name(),
                        Collectors.counting()
                ));
    }

    /**
     * Get recent jobs (last 10).
     *
     * @return list of recent jobs
     */
    public List<ProcessingJob> getRecentJobs(int limit) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        List<ProcessingJob> recentJobs = jobRepository.findByCreatedAtAfter(cutoffDate);

        return recentJobs.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Summary statistics DTO.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyticsSummary {
        private long totalJobs;
        private long completedJobs;
        private long failedJobs;
        private long activeJobs;
        private double successRate;
        private long avgProcessingTimeSeconds;
    }

    /**
     * Daily job count DTO.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyJobCount {
        private String date;
        private long count;
    }
}
