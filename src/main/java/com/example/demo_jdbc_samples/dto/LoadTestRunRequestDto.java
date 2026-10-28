package com.example.demo_jdbc_samples.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("number_of_threads")
    private Integer numberOfThreads;
    // Threads start over this period; requests started in it are not counted
    @JsonProperty("ramp_up_period_seconds")
    private Integer rampUpPeriodSeconds;
    // After the ramp-up, all threads run this long without being counted
    @JsonProperty("warmup_seconds")
    private Integer warmupSeconds;
    // Length of the measured window (total time = ramp-up + warmup + duration).
    // For a count-based scenario it is the upper limit.
    @JsonProperty("duration_seconds")
    private Integer durationSeconds;
    // Scenario parameters
    @JsonProperty("params")
    private Map<String, Object> params;
}
