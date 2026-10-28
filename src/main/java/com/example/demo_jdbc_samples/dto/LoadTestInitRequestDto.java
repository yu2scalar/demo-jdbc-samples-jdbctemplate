package com.example.demo_jdbc_samples.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// POST /load-test/{scenario}/init
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoadTestInitRequestDto {
    // Scenario parameters
    @JsonProperty("params")
    private Map<String, Object> params;
}
