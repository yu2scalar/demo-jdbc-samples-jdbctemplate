package com.example.demo_jdbc_samples.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("scenario")
    private String scenario;

    // --- Measured window only (requests started during ramp-up are excluded) ---
    @JsonProperty("total_requests")
    private Long totalRequests;
    @JsonProperty("successful_requests")
    private Long successfulRequests;
    @JsonProperty("failed_requests")
    private Long failedRequests;
    @JsonProperty("error_rate")
    private Double errorRate;
    // All requests (success + failure) per second
    @JsonProperty("throughput_per_second")
    private Double throughputPerSecond;
    @JsonProperty("success_per_second")
    private Double successPerSecond;
    @JsonProperty("latency_ms")
    private LatencyMetricsDto latencyMs;
    // Failures by error code (-1 = not a CustomException)
    @JsonProperty("failures_by_code")
    private Map<Integer, Long> failuresByCode;
    // Transaction re-attempts; null if the scenario does not count attempts
    @JsonProperty("retry_count")
    private Long retryCount;

    // --- What was excluded ---
    @JsonProperty("number_of_threads")
    private Integer numberOfThreads;
    @JsonProperty("ramp_up_period_seconds")
    private Integer rampUpPeriodSeconds;
    @JsonProperty("warmup_seconds")
    private Integer warmupSeconds;
    // Actual length of the measured window; the throughput is divided by this
    @JsonProperty("measured_duration_seconds")
    private Double measuredDurationSeconds;
    // Ramp-up, warmup and drain included
    @JsonProperty("total_elapsed_seconds")
    private Double totalElapsedSeconds;
    // Requests started during the ramp-up or warmup
    @JsonProperty("excluded_ramp_up_requests")
    private Long excludedRampUpRequests;

    // Per second from the start, ramp-up and warmup included
    @JsonProperty("timeline")
    private List<LoadTestTimelineDto> timeline;

    @JsonProperty("params")
    private Map<String, Object> params;

    // --- Whole run (ramp-up included) ---
    @JsonProperty("verification")
    private LoadTestVerificationDto verification;
}
