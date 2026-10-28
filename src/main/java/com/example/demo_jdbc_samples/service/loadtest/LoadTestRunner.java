package com.example.demo_jdbc_samples.service.loadtest;

import com.example.demo_jdbc_samples.dto.LatencyMetricsDto;
import com.example.demo_jdbc_samples.dto.LoadTestRunRequestDto;
import com.example.demo_jdbc_samples.dto.LoadTestRunResponseDto;
import com.example.demo_jdbc_samples.dto.LoadTestTimelineDto;
import com.example.demo_jdbc_samples.dto.LoadTestVerificationDto;
import com.example.demo_jdbc_samples.exception.CustomException;
import com.example.demo_jdbc_samples.service.OrderRules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Runs a LoadTestScenario on a number of threads (generate-load-test pattern).
 *
 * - Threads start one by one over the ramp-up period. A request is counted in the
 *   metrics only if it STARTED after the ramp-up (measureStart), so the low-
 *   concurrency start does not distort throughput and latency.
 * - warmup_seconds (optional) follows the ramp-up: all threads run, nothing is counted.
 * - duration_seconds is the length of the measured window; the throughput is
 *   divided by its actual length (up to the end of the last counted request).
 *   A count-based scenario (isDone / NoMoreWorkException) may end earlier.
 * - timeline: per second, by the start time of the requests.
 * - Successes and failures of the whole run (ramp-up included) are kept apart,
 *   for the scenario's database check.
 */
@Slf4j
@Service
public class LoadTestRunner {
    public static final int NOT_INITIALIZED = 9900;
    public static final int ALREADY_RUNNING = 9901;
    public static final int INTERRUPTED = 9902;

    private final Map<String, LoadTestScenario<?>> scenarios;

    // One run at a time: concurrent runs would distort each other and the check
    private final AtomicBoolean running = new AtomicBoolean(false);

    public LoadTestRunner(List<LoadTestScenario<?>> scenarios) {
        this.scenarios = scenarios.stream()
            .collect(Collectors.toMap(LoadTestScenario::name, Function.identity()));
    }

    public List<String> scenarioNames() {
        List<String> names = new ArrayList<>(scenarios.keySet());
        Collections.sort(names);
        return names;
    }

    public Map<String, Object> init(String name, Map<String, Object> params) throws CustomException {
        LoadTestScenario<?> scenario = getScenario(name);
        if (running.get()) {
            throw new CustomException("A load test is running", ALREADY_RUNNING);
        }
        return scenario.init(new LoadTestParams(params));
    }

    public LoadTestRunResponseDto run(String name, LoadTestRunRequestDto request) throws CustomException {
        LoadTestScenario<?> scenario = getScenario(name);
        int threads = positive(request.getNumberOfThreads(), "number_of_threads");
        int rampUp = request.getRampUpPeriodSeconds() != null ? request.getRampUpPeriodSeconds() : 0;
        if (rampUp < 0) {
            throw new CustomException("ramp_up_period_seconds must not be negative", OrderRules.INVALID_REQUEST);
        }
        int warmup = request.getWarmupSeconds() != null ? request.getWarmupSeconds() : 0;
        if (warmup < 0) {
            throw new CustomException("warmup_seconds must not be negative", OrderRules.INVALID_REQUEST);
        }
        int duration = positive(request.getDurationSeconds(), "duration_seconds");
        if (!running.compareAndSet(false, true)) {
            throw new CustomException("A load test is already running", ALREADY_RUNNING);
        }
        try {
            return runScenario(scenario, new LoadTestParams(request.getParams()), threads, rampUp, warmup, duration);
        } finally {
            running.set(false);
        }
    }

    private <R> LoadTestRunResponseDto runScenario(LoadTestScenario<R> scenario, LoadTestParams params,
            int threads, int rampUp, int warmup, int duration) throws CustomException {
        scenario.beforeRun(params);
        log.info("Starting load test {}: threads={}, rampUp={}s, warmup={}s, duration={}s, params={}",
            scenario.name(), threads, rampUp, warmup, duration, params.asMap());

        // Measured window only
        ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();
        AtomicLong successCount = new AtomicLong();
        AtomicLong failureCount = new AtomicLong();
        Map<Integer, AtomicLong> failuresByCode = new ConcurrentHashMap<>();
        AtomicLong lastCountedEndMillis = new AtomicLong();
        AtomicLong excludedRampUpRequests = new AtomicLong();
        // Whole run, for the database check
        ConcurrentLinkedQueue<R> allSuccesses = new ConcurrentLinkedQueue<>();
        Map<Integer, AtomicLong> allFailuresByCode = new ConcurrentHashMap<>();
        AtomicLong allCalls = new AtomicLong();

        long t0 = System.currentTimeMillis();
        long warmupStart = t0 + rampUp * 1000L;
        long measureStart = warmupStart + warmup * 1000L;
        long testEnd = measureStart + duration * 1000L;
        // Per second by request start time; the extra seconds cover the drain
        Bucket[] timeline = new Bucket[rampUp + warmup + duration + 32];
        for (int s = 0; s < timeline.length; s++) {
            timeline[s] = new Bucket();
        }
        AtomicInteger activeThreads = new AtomicInteger();
        AtomicInteger finishedThreads = new AtomicInteger();
        // The last thread starts right at measureStart
        long rampUpIntervalMs = threads > 1 ? (rampUp * 1000L) / (threads - 1) : 0;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                if (threadIndex > 0 && rampUpIntervalMs > 0) {
                    try {
                        Thread.sleep(threadIndex * rampUpIntervalMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                activeThreads.incrementAndGet();
                try {
                    Random random = new Random();
                    while (System.currentTimeMillis() < testEnd && !scenario.isDone()
                            && !Thread.currentThread().isInterrupted()) {
                        long startMillis = System.currentTimeMillis();
                        long startNanos = System.nanoTime();
                        R record = null;
                        int errorCode = 0;
                        try {
                            record = scenario.execute(random, params);
                        } catch (LoadTestScenario.NoMoreWorkException e) {
                            break;
                        } catch (CustomException e) {
                            errorCode = e.getErrorCode() != null ? e.getErrorCode() : -1;
                        } catch (Exception e) {
                            errorCode = -1;
                            log.debug("Request failed: {}", e.getMessage());
                        }
                        boolean ok = errorCode == 0;
                        long endMillis = System.currentTimeMillis();
                        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;

                        allCalls.incrementAndGet();
                        Bucket bucket = timeline[(int) Math.min(timeline.length - 1, Math.max(0, (startMillis - t0) / 1000))];
                        bucket.add(ok, latencyMs, activeThreads.get());
                        if (ok) {
                            if (record != null) {
                                allSuccesses.add(record);
                            }
                        } else {
                            allFailuresByCode.computeIfAbsent(errorCode, k -> new AtomicLong()).incrementAndGet();
                        }

                        // Counted only if the request started in the measured window
                        if (startMillis >= measureStart) {
                            if (ok) {
                                successCount.incrementAndGet();
                                latencies.add(latencyMs);
                            } else {
                                failureCount.incrementAndGet();
                                failuresByCode.computeIfAbsent(errorCode, k -> new AtomicLong()).incrementAndGet();
                            }
                            lastCountedEndMillis.accumulateAndGet(endMillis, Math::max);
                        } else {
                            excludedRampUpRequests.incrementAndGet();
                        }
                    }
                } finally {
                    activeThreads.decrementAndGet();
                    finishedThreads.incrementAndGet();
                }
            });
        }

        // Snapshot the attempt counter when the measured window opens (or when a
        // count-based run has already finished)
        sleepUntil(measureStart, () -> finishedThreads.get() == threads);
        long attemptsAtMeasureStart = scenario.attemptCount();
        long callsAtMeasureStart = allCalls.get();

        executor.shutdown();
        try {
            if (!executor.awaitTermination(rampUp + warmup + duration + 30L, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw new CustomException("Load test was interrupted", INTERRUPTED);
        }
        long wallClockEnd = System.currentTimeMillis();

        long totalRequests = successCount.get() + failureCount.get();
        double measuredSeconds = totalRequests > 0
            ? Math.max(0.0, (lastCountedEndMillis.get() - measureStart) / 1000.0) : 0.0;
        Long retryCount = null;
        if (attemptsAtMeasureStart >= 0) {
            // Requests started before measureStart may still retry after it; the
            // difference of attempts minus calls is close enough for a summary
            long attempts = scenario.attemptCount() - attemptsAtMeasureStart;
            long calls = allCalls.get() - callsAtMeasureStart;
            retryCount = Math.max(0L, attempts - calls);
        }

        log.info("Load test {} completed: {} requests in {}s ({} excluded during ramp-up and warmup); checking the database",
            scenario.name(), totalRequests, measuredSeconds, excludedRampUpRequests.get());
        long verifyStart = System.nanoTime();
        LoadTestVerificationDto verification = scenario.verify(new ArrayList<>(allSuccesses), toSortedMap(allFailuresByCode));
        verification.setElapsedSeconds((System.nanoTime() - verifyStart) / 1_000_000_000.0);

        return LoadTestRunResponseDto.builder()
            .scenario(scenario.name())
            .totalRequests(totalRequests)
            .successfulRequests(successCount.get())
            .failedRequests(failureCount.get())
            .errorRate(totalRequests > 0 ? failureCount.get() * 100.0 / totalRequests : 0.0)
            .throughputPerSecond(measuredSeconds > 0 ? totalRequests / measuredSeconds : 0.0)
            .successPerSecond(measuredSeconds > 0 ? successCount.get() / measuredSeconds : 0.0)
            .latencyMs(latencyMetrics(latencies))
            .failuresByCode(toSortedMap(failuresByCode))
            .retryCount(retryCount)
            .numberOfThreads(threads)
            .rampUpPeriodSeconds(rampUp)
            .warmupSeconds(warmup)
            .measuredDurationSeconds(measuredSeconds)
            .totalElapsedSeconds((wallClockEnd - t0) / 1000.0)
            .excludedRampUpRequests(excludedRampUpRequests.get())
            .timeline(toTimeline(timeline, t0, warmupStart, measureStart, testEnd))
            .params(params.asMap())
            .verification(verification)
            .build();
    }

    private LoadTestScenario<?> getScenario(String name) {
        LoadTestScenario<?> scenario = scenarios.get(name);
        if (scenario == null) {
            throw new CustomException("Unknown scenario: " + name + " (available: " + scenarioNames() + ")",
                OrderRules.INVALID_REQUEST);
        }
        return scenario;
    }

    private static int positive(Integer value, String name) {
        if (value == null || value < 1) {
            throw new CustomException(name + " must be 1 or more", OrderRules.INVALID_REQUEST);
        }
        return value;
    }

    private static Map<Integer, Long> toSortedMap(Map<Integer, AtomicLong> counts) {
        Map<Integer, Long> sorted = new TreeMap<>();
        counts.forEach((code, count) -> sorted.put(code, count.get()));
        return sorted;
    }

    // One second of the timeline
    private static class Bucket {
        final AtomicLong successes = new AtomicLong();
        final AtomicLong failures = new AtomicLong();
        final ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();
        final AtomicInteger maxActiveThreads = new AtomicInteger();

        void add(boolean ok, long latencyMs, int activeThreads) {
            if (ok) {
                successes.incrementAndGet();
                latencies.add(latencyMs);
            } else {
                failures.incrementAndGet();
            }
            maxActiveThreads.accumulateAndGet(activeThreads, Math::max);
        }
    }

    private static List<LoadTestTimelineDto> toTimeline(Bucket[] buckets, long t0, long warmupStart,
            long measureStart, long testEnd) {
        int last = -1;
        for (int s = 0; s < buckets.length; s++) {
            if (buckets[s].successes.get() + buckets[s].failures.get() > 0) {
                last = s;
            }
        }
        List<LoadTestTimelineDto> timeline = new ArrayList<>();
        for (int s = 0; s <= last; s++) {
            Bucket b = buckets[s];
            long secondStart = t0 + s * 1000L;
            String phase = secondStart < warmupStart ? "ramp-up"
                : secondStart < measureStart ? "warmup"
                : secondStart < testEnd ? "measure" : "drain";
            List<Long> sorted = new ArrayList<>(b.latencies);
            Collections.sort(sorted);
            timeline.add(LoadTestTimelineDto.builder()
                .second(s)
                .phase(phase)
                .requests(b.successes.get() + b.failures.get())
                .successes(b.successes.get())
                .failures(b.failures.get())
                .p50(sorted.isEmpty() ? 0L : percentile(sorted, 50))
                .p99(sorted.isEmpty() ? 0L : percentile(sorted, 99))
                .activeThreads(b.maxActiveThreads.get())
                .build());
        }
        return timeline;
    }

    private static void sleepUntil(long targetMillis, BooleanSupplier finished) {
        long remaining = targetMillis - System.currentTimeMillis();
        while (remaining > 0 && !finished.getAsBoolean()) {
            try {
                Thread.sleep(Math.min(remaining, 200L));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            remaining = targetMillis - System.currentTimeMillis();
        }
    }

    private static LatencyMetricsDto latencyMetrics(ConcurrentLinkedQueue<Long> latencies) {
        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);
        if (sorted.isEmpty()) {
            return LatencyMetricsDto.builder().average(0.0).min(0L).max(0L).p50(0L).p95(0L).p99(0L).build();
        }
        long sum = 0;
        for (long latency : sorted) {
            sum += latency;
        }
        return LatencyMetricsDto.builder()
            .average((double) sum / sorted.size())
            .min(sorted.get(0))
            .max(sorted.get(sorted.size() - 1))
            .p50(percentile(sorted, 50))
            .p95(percentile(sorted, 95))
            .p99(percentile(sorted, 99))
            .build();
    }

    private static long percentile(List<Long> sorted, int percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
}
