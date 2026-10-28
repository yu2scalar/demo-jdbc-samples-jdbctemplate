package com.example.demo_jdbc_samples.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("customers")
    private Integer customers;
    @JsonProperty("items")
    private Integer items;
    // Only set by POST /init
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("truncated")
    private Boolean truncated;
}
