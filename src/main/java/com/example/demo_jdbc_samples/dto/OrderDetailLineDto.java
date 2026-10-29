package com.example.demo_jdbc_samples.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// One statement line of an order, with the item name
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailLineDto {
    private Integer lineNo;
    private Integer itemId;
    private String itemName;
    private Integer qty;
    private Long unitPrice;
    private Long subtotal;
}
