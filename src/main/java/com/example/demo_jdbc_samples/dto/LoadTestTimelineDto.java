package com.example.demo_jdbc_samples.dto;

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
    private Integer second;
    // ramp-up / warmup / measure / drain
    private String phase;
    private Long requests;
    private Long successes;
    private Long failures;
    // Latency of the successful requests in milliseconds
    private Long p50;
    private Long p99;
    // Most threads running at once during the second
    private Integer activeThreads;
}
