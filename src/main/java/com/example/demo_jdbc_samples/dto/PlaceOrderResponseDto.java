package com.example.demo_jdbc_samples.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceOrderResponseDto {
    private String orderId;
    private Long totalAmount;
}
