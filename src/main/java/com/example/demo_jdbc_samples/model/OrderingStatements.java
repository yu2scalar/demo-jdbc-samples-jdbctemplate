package com.example.demo_jdbc_samples.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderingStatements {

    public static final String NAMESPACE = "ordering";
    public static final String TABLE = "statements";
    public static final String ORDER_ID = "order_id";
    public static final String LINE_NO = "line_no";
    public static final String ITEM_ID = "item_id";
    public static final String QTY = "qty";
    public static final String UNIT_PRICE = "unit_price";

    private String orderId;
    private Integer lineNo;
    private Integer itemId;
    private Integer qty;
    private Long unitPrice;
}
