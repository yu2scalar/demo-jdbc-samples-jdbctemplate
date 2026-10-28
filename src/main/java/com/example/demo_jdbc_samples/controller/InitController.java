package com.example.demo_jdbc_samples.controller;

import com.example.demo_jdbc_samples.dto.ApiResponse;
import com.example.demo_jdbc_samples.dto.InitResultDto;
import com.example.demo_jdbc_samples.exception.CustomException;
import com.example.demo_jdbc_samples.service.InitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping(value = "/init")
@RestController
public class InitController {
    @Autowired
    private InitService initService;

    // Both tables: POST /init?customers=N&items=M[&truncate=true]
    // truncate=true empties customers, items, orders and statements first.
    @PostMapping
    public ResponseEntity<ApiResponse<InitResultDto>> init(
            @RequestParam(value = "customers", defaultValue = "0") int customers,
            @RequestParam(value = "items", defaultValue = "0") int items,
            @RequestParam(value = "truncate", defaultValue = "false") boolean truncate) {
        if (truncate) {
            initService.truncateAll();
        }
        InitResultDto result = InitResultDto.builder()
            .truncated(truncate)
            .customers(initService.initCustomers(customers))
            .items(initService.initItems(items))
            .build();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/customers")
    public ResponseEntity<ApiResponse<InitResultDto>> initCustomers(@RequestParam("count") int count) {
        InitResultDto result = InitResultDto.builder().customers(initService.initCustomers(count)).build();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<InitResultDto>> initItems(@RequestParam("count") int count) {
        InitResultDto result = InitResultDto.builder().items(initService.initItems(count)).build();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @ExceptionHandler(value = CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleScalarDbException(CustomException ex) {
        return OrderController.errorResponse(ex);
    }
}
