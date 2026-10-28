package com.example.demo_jdbc_samples.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// One second of a load test run, by the start time of the requests
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadTestTimelineDto {
    // Seconds from the start (0 = first second)
    @JsonProperty("second")
    private Integer second;
    // ramp-up / warmup / measure / drain
    @JsonProperty("phase")
    private String phase;
    @JsonProperty("requests")
    private Long requests;
    @JsonProperty("successes")
    private Long successes;
    @JsonProperty("failures")
    private Long failures;
    // Latency of the successful requests in milliseconds
    @JsonProperty("p50")
    private Long p50;
    @JsonProperty("p99")
    private Long p99;
    // Most threads running at once during the second
    @JsonProperty("active_threads")
    private Integer activeThreads;
}
