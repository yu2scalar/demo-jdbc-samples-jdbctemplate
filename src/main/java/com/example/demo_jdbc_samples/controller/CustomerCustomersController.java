package com.example.demo_jdbc_samples.controller;

import com.example.demo_jdbc_samples.service.CustomerCustomersService;
import com.example.demo_jdbc_samples.dto.CustomerCustomersDto;
import com.example.demo_jdbc_samples.dto.ApiResponse;
import com.example.demo_jdbc_samples.dto.ResponseStatusDto;
import com.example.demo_jdbc_samples.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@RequestMapping(value = "/customer-customers")
@RestController
public class CustomerCustomersController {
    @Autowired
    private CustomerCustomersService customersService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> insertCustomerCustomers(@RequestBody CustomerCustomersDto customersDto) {
        ResponseStatusDto status = customersService.insertCustomerCustomers(customersDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @PostMapping("/upsert")
    public ResponseEntity<ApiResponse<Void>> upsertCustomerCustomers(@RequestBody CustomerCustomersDto customersDto) {
        ResponseStatusDto status = customersService.upsertCustomerCustomers(customersDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/{customer_id}")
    public ResponseEntity<ApiResponse<CustomerCustomersDto>> getCustomerCustomers(@PathVariable("customer_id") Integer customer_id) {
        CustomerCustomersDto customersDto = CustomerCustomersDto.builder()
            .customerId(customer_id)
            .build();
        CustomerCustomersDto result = customersService.getCustomerCustomers(customersDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateCustomerCustomers(@RequestBody CustomerCustomersDto customersDto) {
        ResponseStatusDto status = customersService.updateCustomerCustomers(customersDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @DeleteMapping("/{customer_id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomerCustomers(@PathVariable("customer_id") Integer customer_id) {
        CustomerCustomersDto customersDto = CustomerCustomersDto.builder()
            .customerId(customer_id)
            .build();
        ResponseStatusDto status = customersService.deleteCustomerCustomers(customersDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/scan-by-pk/{customer_id}")
    public ResponseEntity<ApiResponse<List<CustomerCustomersDto>>> getCustomerCustomersByPk(@PathVariable("customer_id") Integer customer_id) {
        CustomerCustomersDto customersDto = CustomerCustomersDto.builder()
            .customerId(customer_id)
            .build();
        List<CustomerCustomersDto> result = customersService.getCustomerCustomersListByPk(customersDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/scan-all")
    public ResponseEntity<ApiResponse<List<CustomerCustomersDto>>> getCustomerCustomersListAll() {
        List<CustomerCustomersDto> result = customersService.getCustomerCustomersListAll();
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