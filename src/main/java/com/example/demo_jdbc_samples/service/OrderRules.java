package com.example.demo_jdbc_samples.service;

import com.example.demo_jdbc_samples.dto.OrderLineDto;
import com.example.demo_jdbc_samples.dto.PlaceOrderRequestDto;
import com.example.demo_jdbc_samples.exception.CustomException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Business rules of OrderService (the same as in the ScalarDB version).
 */
public final class OrderRules {

    public static final String STATUS_ORDERED = "ORDERED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // Business rejections (HTTP 409). Thrown as CustomException, so never retried.
    public static final int CREDIT_LIMIT_EXCEEDED = 9800;
    public static final int OUT_OF_STOCK = 9801;
    public static final int NOT_CANCELLABLE = 9802;

    // Invalid request (HTTP 400)
    public static final int INVALID_REQUEST = 9400;

    private OrderRules() {
    }

    /** Rejects a PlaceOrder request that is empty, has a bad qty or repeats an item_id. */
    public static void validate(PlaceOrderRequestDto request) {
        if (request == null || request.getCustomerId() == null) {
            throw new CustomException("customer_id is required", INVALID_REQUEST);
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new CustomException("items must not be empty", INVALID_REQUEST);
        }
        Set<Integer> itemIds = new HashSet<>();
        for (OrderLineDto line : request.getItems()) {
            if (line == null || line.getItemId() == null) {
                throw new CustomException("item_id is required", INVALID_REQUEST);
            }
            if (line.getQty() == null || line.getQty() <= 0) {
                throw new CustomException("qty must be positive: item_id=" + line.getItemId(), INVALID_REQUEST);
            }
            if (!itemIds.add(line.getItemId())) {
                throw new CustomException("Duplicate item_id: " + line.getItemId(), INVALID_REQUEST);
            }
        }
    }

    /**
     * Indexes of the request lines in item_id order. line_no follows the request order,
     * but the item rows are read and updated in item_id order.
     */
    public static List<Integer> indexesByItemId(List<OrderLineDto> lines) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            indexes.add(i);
        }
        indexes.sort(Comparator.comparing(i -> lines.get(i).getItemId()));
        return indexes;
    }

    public static void checkCancellable(String orderId, String status) {
        if (!STATUS_ORDERED.equals(status)) {
            throw new CustomException("Order is not cancellable: order_id=" + orderId + ", status=" + status, NOT_CANCELLABLE);
        }
    }

    /** Fixed TEST-xxx value for the non-key columns filled by /init. */
    public static String testName(int id) {
        return String.format("TEST-%06d", id);
    }
}
