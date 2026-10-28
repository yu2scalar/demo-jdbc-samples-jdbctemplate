package com.example.demo_jdbc_samples.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("order_id")
    private String orderId;
    @JsonProperty("customer_id")
    private Integer customerId;
    @JsonProperty("created_at")
    private Long createdAt;
    @JsonProperty("status")
    private String status;
    @JsonProperty("total_amount")
    private Long totalAmount;
    @JsonProperty("items")
    private List<OrderDetailLineDto> items;
}
