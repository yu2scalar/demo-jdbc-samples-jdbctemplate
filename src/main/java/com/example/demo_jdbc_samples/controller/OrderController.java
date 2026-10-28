package com.example.demo_jdbc_samples.controller;

import com.example.demo_jdbc_samples.dto.ApiResponse;
import com.example.demo_jdbc_samples.dto.OrderDetailDto;
import com.example.demo_jdbc_samples.dto.PlaceOrderRequestDto;
import com.example.demo_jdbc_samples.dto.PlaceOrderResponseDto;
import com.example.demo_jdbc_samples.dto.ResponseStatusDto;
import com.example.demo_jdbc_samples.exception.CustomException;
import com.example.demo_jdbc_samples.service.OrderService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Business logic on the Java CRUD API
@RestController
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<PlaceOrderResponseDto>> placeOrder(@RequestBody PlaceOrderRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success(orderService.placeOrder(request)));
    }

    @GetMapping("/orders/{order_id}")
    public ResponseEntity<ApiResponse<OrderDetailDto>> getOrder(@PathVariable("order_id") String order_id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrder(order_id)));
    }

    @PostMapping("/orders/{order_id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable("order_id") String order_id) {
        ResponseStatusDto status = orderService.cancelOrder(order_id);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    // method = index (default) or scan: kept for the same API as the ScalarDB version; same query with JDBC
    @GetMapping("/customers/{customer_id}/orders")
    public ResponseEntity<ApiResponse<List<OrderDetailDto>>> getOrders(
            @PathVariable("customer_id") int customer_id,
            @RequestParam(value = "method", defaultValue = "index") String method) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrdersByCustomerId(customer_id, method)));
    }

    @ExceptionHandler(value = CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleScalarDbException(CustomException ex) {
        return errorResponse(ex);
    }

    // The table controllers' mapping, plus 409 for business rejections (98xx)
    static ResponseEntity<ApiResponse<Void>> errorResponse(CustomException ex) {
        ApiResponse<Void> errorResponse = ApiResponse.error(ex.getErrorCode(), ex.getMessage());
        errorResponse.setTransactionId(ex.getTransactionId());
        return switch (ex.getErrorCode()) {
            case 9100, 9400 -> new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            case 9200, 9300 -> new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            case 9600 -> new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
            case 9700 -> new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
            case 9800, 9801, 9802 -> new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
            default -> new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        };
    }
}
