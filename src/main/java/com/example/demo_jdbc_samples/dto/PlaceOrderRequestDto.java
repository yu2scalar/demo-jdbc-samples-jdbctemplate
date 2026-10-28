package com.example.demo_jdbc_samples.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("customer_id")
    private Integer customerId;
    @JsonProperty("items")
    private List<OrderLineDto> items;
}
