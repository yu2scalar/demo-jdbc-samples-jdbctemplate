package com.example.demo_jdbc_samples.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// One line of a PlaceOrder request
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderLineDto {
    @JsonProperty("item_id")
    private Integer itemId;
    @JsonProperty("qty")
    private Integer qty;
}
