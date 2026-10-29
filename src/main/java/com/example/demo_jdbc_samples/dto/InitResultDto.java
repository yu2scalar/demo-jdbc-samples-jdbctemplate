package com.example.demo_jdbc_samples.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Number of rows upserted by /init, and whether the tables were truncated first
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitResultDto {
    private Integer customers;
    private Integer items;
    // Only set by POST /init
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean truncated;
}
