package com.example.demo_jdbc_samples.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("line_no")
    private Integer lineNo;
    @JsonProperty("item_id")
    private Integer itemId;
    @JsonProperty("item_name")
    private String itemName;
    @JsonProperty("qty")
    private Integer qty;
    @JsonProperty("unit_price")
    private Long unitPrice;
    @JsonProperty("subtotal")
    private Long subtotal;
}
