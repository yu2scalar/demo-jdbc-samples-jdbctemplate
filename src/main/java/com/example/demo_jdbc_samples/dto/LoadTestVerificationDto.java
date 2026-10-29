package com.example.demo_jdbc_samples.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Database check after a run, against every successful call (ramp-up included)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoadTestVerificationDto {
    private Boolean consistent;
    // Scenario-specific expected / actual values
    private Map<String, Object> checks;
    private String note;
    // Time spent reading for the check (not part of the measured values)
    private Double elapsedSeconds;
}
