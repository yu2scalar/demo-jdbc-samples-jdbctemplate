package com.example.demo_jdbc_samples.dto;

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
    private Integer itemId;
    private Integer qty;
}
