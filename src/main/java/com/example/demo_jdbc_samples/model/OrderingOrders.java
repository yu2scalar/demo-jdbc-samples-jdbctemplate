package com.example.demo_jdbc_samples.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderingOrders {

    public static final String NAMESPACE = "ordering";
    public static final String TABLE = "orders";
    public static final String ORDER_ID = "order_id";
    public static final String CUSTOMER_ID = "customer_id";
    public static final String CREATED_AT = "created_at";
    public static final String STATUS = "status";
    public static final String TOTAL_AMOUNT = "total_amount";

    private String orderId;
    private Integer customerId;
    private Long createdAt;
    private String status;
    private Long totalAmount;

}
