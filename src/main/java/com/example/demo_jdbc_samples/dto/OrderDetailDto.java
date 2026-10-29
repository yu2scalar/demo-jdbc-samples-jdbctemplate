package com.example.demo_jdbc_samples.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// GetOrder / GetOrders response: an order with its statements
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailDto {
    private String orderId;
    private Integer customerId;
    private Long createdAt;
    private String status;
    private Long totalAmount;
    private List<OrderDetailLineDto> items;
}
