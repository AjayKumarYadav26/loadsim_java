package com.ktlo.simulator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for simulating CPU-intensive operations.
 * Uses @Async to execute tasks in the configured threadpool.
 */
@Slf4j
@Service
public class CpuLoadService {

    private static final long REBUILD_INTERVAL_MS = 30000L;
    private static final long REBUILD_WORK_BUDGET_MS = 2000L;
    private static final long TASK_YIELD_INTERVAL = 100_000L;

    private final AtomicLong taskCounter = new AtomicLong(0);

    @Scheduled(fixedRate = REBUILD_INTERVAL_MS)
    public void rebuildProcessingIndex() {
        long end = System.currentTimeMillis() + REBUILD_WORK_BUDGET_MS;
        double checksum = 0;
        long iterations = 0;

        while (System.currentTimeMillis() < end) {
            checksum += Math.sqrt(checksum + iterations) * Math.PI;
            iterations++;

            if (iterations % TASK_YIELD_INTERVAL == 0) {
                Thread.yield();
            }
        }

        log.debug("Processing index rebuild completed after {} iterations, checksum: {}", iterations, checksum);
    }

    /**
     * Execute a CPU-intensive task for the specified duration.
     *
     * @param durationSeconds How long to burn CPU (in seconds)
     * @return CompletableFuture with result
     */
    @Async("simulationExecutor")
    public CompletableFuture<String> executeIntensiveTask(int durationSeconds) {
        long taskId = taskCounter.incrementAndGet();
        String threadName = Thread.currentThread().getName();

        log.info("Task {} started on thread: {}, duration: {}s", taskId, threadName, durationSeconds);

        long endTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        long counter = 0;
        long iterations = 0;

        while (System.currentTimeMillis() < endTime) {
            counter += (long) (Math.sqrt(counter + iterations) * Math.PI);
            counter = counter % Long.MAX_VALUE;
            iterations++;

            if (iterations % TASK_YIELD_INTERVAL == 0) {
                Thread.yield();
            }
        }

        String result = String.format("Task %d completed on thread %s. Counter: %d", taskId, threadName, counter);
        log.info(result);

        return CompletableFuture.completedFuture(result);
    }

    /**
     * Calculate prime numbers up to the given limit (CPU-intensive).
     *
     * @param limit Upper limit for prime number search
     * @return CompletableFuture with count of primes found
     */
    @Async("simulationExecutor")
    public CompletableFuture<Integer> calculatePrimes(int limit) {
        long taskId = taskCounter.incrementAndGet();
        String threadName = Thread.currentThread().getName();

        log.info("Prime calculation task {} started on thread: {}, limit: {}", taskId, threadName, limit);

        int primeCount = 0;
        for (int num = 2; num <= limit; num++) {
            if (isPrime(num)) {
                primeCount++;
            }

            if (num % 10_000 == 0) {
                Thread.yield();
            }
        }

        log.info("Prime calculation task {} completed. Found {} primes", taskId, primeCount);
        return CompletableFuture.completedFuture(primeCount);
    }

    /**
     * Calculate Fibonacci number recursively (CPU-intensive).
     *
     * @param n The Fibonacci index
     * @return CompletableFuture with Fibonacci number
     */
    @Async("simulationExecutor")
    public CompletableFuture<Long> calculateFibonacci(int n) {
        long taskId = taskCounter.incrementAndGet();
        String threadName = Thread.currentThread().getName();

        log.info("Fibonacci task {} started on thread: {}, n: {}", taskId, threadName, n);

        long result = fibonacci(n);

        log.info("Fibonacci task {} completed. Result: {}", taskId, result);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Exhaust the threadpool by submitting more tasks than available threads.
     * This will fill the queue and potentially cause rejection.
     *
     * @param taskCount Number of tasks to submit
     * @param taskDuration Duration of each task in seconds
     */
    public void exhaustThreadPool(int taskCount, int taskDuration) {
        log.warn("=== EXHAUST THREADPOOL CALLED: submitting {} tasks with duration {}s each", 
            taskCount, taskDuration);

        for (int i = 0; i < taskCount; i++) {
            try {
                log.info("=== Submitting task {}/{}", i + 1, taskCount);
                executeIntensiveTask(taskDuration);
            } catch (Exception e) {
                log.error("=== FAILED to submit task {}: {}", i, e.getMessage());
            }
        }

        log.warn("=== COMPLETED submitting {} tasks to threadpool", taskCount);
    }

    private boolean isPrime(int num) {
        if (num <= 1) return false;
        if (num == 2) return true;
        if (num % 2 == 0) return false;

        int sqrt = (int) Math.sqrt(num);
        for (int i = 3; i <= sqrt; i += 2) {
            if (num % i == 0) {
                return false;
            }
        }
        return true;
    }

    private long fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }

    public long getTaskCount() {
        return taskCounter.get();
    }
}
