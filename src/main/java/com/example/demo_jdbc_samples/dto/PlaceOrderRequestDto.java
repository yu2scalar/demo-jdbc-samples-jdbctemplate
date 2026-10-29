package com.example.demo_jdbc_samples.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// PlaceOrder request: {"customer_id":1,"items":[{"item_id":1,"qty":2}]}
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceOrderRequestDto {
    private Integer customerId;
    private List<OrderLineDto> items;
}
