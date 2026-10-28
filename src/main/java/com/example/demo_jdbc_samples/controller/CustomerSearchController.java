package com.example.demo_jdbc_samples.controller;

import com.example.demo_jdbc_samples.dto.ApiResponse;
import com.example.demo_jdbc_samples.dto.CustomerCustomersDto;
import com.example.demo_jdbc_samples.exception.CustomException;
import com.example.demo_jdbc_samples.mapper.CustomerCustomersMapper;
import com.example.demo_jdbc_samples.service.CustomerCrudService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Customers by name: SELECT ... WHERE name = ? (exact) or name LIKE '?%' (prefix)
@RestController
public class CustomerSearchController {
    @Autowired
    private CustomerCrudService customerCrudService;

    @GetMapping("/customer-customers/search")
    public ResponseEntity<ApiResponse<List<CustomerCustomersDto>>> search(
            @RequestParam("name") String name,
            @RequestParam(value = "match", defaultValue = "exact") String match,
            @RequestParam(value = "limit", defaultValue = "1000") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
            CustomerCustomersMapper.mapToCustomerCustomersDtoList(customerCrudService.search(name, match, limit))));
    }

    @ExceptionHandler(value = CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleScalarDbException(CustomException ex) {
        return OrderController.errorResponse(ex);
    }
}
