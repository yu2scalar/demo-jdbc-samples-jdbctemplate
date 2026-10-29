package com.example.demo_jdbc_samples.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Result of POST /load-test/{scenario}/run
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoadTestRunResponseDto {
    private String scenario;

    // --- Measured window only (requests started during ramp-up are excluded) ---
    private Long totalRequests;
    private Long successfulRequests;
    private Long failedRequests;
    private Double errorRate;
    // All requests (success + failure) per second
    private Double throughputPerSecond;
    private Double successPerSecond;
    private LatencyMetricsDto latencyMs;
    // Failures by error code (-1 = not a CustomException)
    private Map<Integer, Long> failuresByCode;
    // Transaction re-attempts; null if the scenario does not count attempts
    private Long retryCount;

    // --- What was excluded ---
    private Integer numberOfThreads;
    private Integer rampUpPeriodSeconds;
    private Integer warmupSeconds;
    // Actual length of the measured window; the throughput is divided by this
    private Double measuredDurationSeconds;
    // Ramp-up, warmup and drain included
    private Double totalElapsedSeconds;
    // Requests started during the ramp-up or warmup
    private Long excludedRampUpRequests;

    // Per second from the start, ramp-up and warmup included
    private List<LoadTestTimelineDto> timeline;

    private Map<String, Object> params;

    // --- Whole run (ramp-up included) ---
    private LoadTestVerificationDto verification;
}
