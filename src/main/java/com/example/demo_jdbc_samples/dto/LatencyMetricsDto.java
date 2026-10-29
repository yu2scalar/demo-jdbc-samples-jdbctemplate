package com.example.demo_jdbc_samples.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Latency of successful requests in the measured window, in milliseconds
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LatencyMetricsDto {
    private Double average;
    private Long min;
    private Long max;
    private Long p50;
    private Long p95;
    private Long p99;
}
