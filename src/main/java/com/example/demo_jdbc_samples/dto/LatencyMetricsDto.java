package com.example.demo_jdbc_samples.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("average")
    private Double average;
    @JsonProperty("min")
    private Long min;
    @JsonProperty("max")
    private Long max;
    @JsonProperty("p50")
    private Long p50;
    @JsonProperty("p95")
    private Long p95;
    @JsonProperty("p99")
    private Long p99;
}
