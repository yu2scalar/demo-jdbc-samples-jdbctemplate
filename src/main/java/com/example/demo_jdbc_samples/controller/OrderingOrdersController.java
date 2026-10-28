package com.example.demo_jdbc_samples.controller;

import com.example.demo_jdbc_samples.service.OrderingOrdersService;
import com.example.demo_jdbc_samples.dto.OrderingOrdersDto;
import com.example.demo_jdbc_samples.dto.ApiResponse;
import com.example.demo_jdbc_samples.dto.ResponseStatusDto;
import com.example.demo_jdbc_samples.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@RequestMapping(value = "/ordering-orders")
@RestController
public class OrderingOrdersController {
    @Autowired
    private OrderingOrdersService ordersService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> insertOrderingOrders(@RequestBody OrderingOrdersDto ordersDto) {
        ResponseStatusDto status = ordersService.insertOrderingOrders(ordersDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @PostMapping("/upsert")
    public ResponseEntity<ApiResponse<Void>> upsertOrderingOrders(@RequestBody OrderingOrdersDto ordersDto) {
        ResponseStatusDto status = ordersService.upsertOrderingOrders(ordersDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/{order_id}")
    public ResponseEntity<ApiResponse<OrderingOrdersDto>> getOrderingOrders(@PathVariable("order_id") String order_id) {
        OrderingOrdersDto ordersDto = OrderingOrdersDto.builder()
            .orderId(order_id)
            .build();
        OrderingOrdersDto result = ordersService.getOrderingOrders(ordersDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateOrderingOrders(@RequestBody OrderingOrdersDto ordersDto) {
        ResponseStatusDto status = ordersService.updateOrderingOrders(ordersDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @DeleteMapping("/{order_id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrderingOrders(@PathVariable("order_id") String order_id) {
        OrderingOrdersDto ordersDto = OrderingOrdersDto.builder()
            .orderId(order_id)
            .build();
        ResponseStatusDto status = ordersService.deleteOrderingOrders(ordersDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/scan-by-pk/{order_id}")
    public ResponseEntity<ApiResponse<List<OrderingOrdersDto>>> getOrderingOrdersByPk(@PathVariable("order_id") String order_id) {
        OrderingOrdersDto ordersDto = OrderingOrdersDto.builder()
            .orderId(order_id)
            .build();
        List<OrderingOrdersDto> result = ordersService.getOrderingOrdersListByPk(ordersDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/scan-all")
    public ResponseEntity<ApiResponse<List<OrderingOrdersDto>>> getOrderingOrdersListAll() {
        List<OrderingOrdersDto> result = ordersService.getOrderingOrdersListAll();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @ExceptionHandler(value = CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleScalarDbException(CustomException ex) {
        ApiResponse<Void> errorResponse = ApiResponse.error(ex.getErrorCode(), ex.getMessage());
        errorResponse.setTransactionId(ex.getTransactionId());
        return switch (ex.getErrorCode()) {
            case 9100, 9400 -> new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            case 9200, 9300 -> new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
            case 9600 -> new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
            case 9700 -> new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
            default -> new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        };
    }
}