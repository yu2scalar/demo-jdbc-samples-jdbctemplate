package com.example.demo_jdbc_samples.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// POST /load-test/{scenario}/run
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadTestRunRequestDto {
    private Integer numberOfThreads;
    // Threads start over this period; requests started in it are not counted
    private Integer rampUpPeriodSeconds;
    // After the ramp-up, all threads run this long without being counted
    private Integer warmupSeconds;
    // Length of the measured window (total time = ramp-up + warmup + duration).
    // For a count-based scenario it is the upper limit.
    private Integer durationSeconds;
    // Scenario parameters
    private Map<String, Object> params;
}
