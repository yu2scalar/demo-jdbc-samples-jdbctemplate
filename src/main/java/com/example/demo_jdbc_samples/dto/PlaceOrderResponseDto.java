package com.example.demo_jdbc_samples.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceOrderResponseDto {
    @JsonProperty("order_id")
    private String orderId;
    @JsonProperty("total_amount")
    private Long totalAmount;
}
