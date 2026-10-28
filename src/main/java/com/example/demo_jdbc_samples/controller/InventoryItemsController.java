package com.example.demo_jdbc_samples.controller;

import com.example.demo_jdbc_samples.service.InventoryItemsService;
import com.example.demo_jdbc_samples.dto.InventoryItemsDto;
import com.example.demo_jdbc_samples.dto.ApiResponse;
import com.example.demo_jdbc_samples.dto.ResponseStatusDto;
import com.example.demo_jdbc_samples.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@RequestMapping(value = "/inventory-items")
@RestController
public class InventoryItemsController {
    @Autowired
    private InventoryItemsService itemsService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> insertInventoryItems(@RequestBody InventoryItemsDto itemsDto) {
        ResponseStatusDto status = itemsService.insertInventoryItems(itemsDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @PostMapping("/upsert")
    public ResponseEntity<ApiResponse<Void>> upsertInventoryItems(@RequestBody InventoryItemsDto itemsDto) {
        ResponseStatusDto status = itemsService.upsertInventoryItems(itemsDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/{item_id}")
    public ResponseEntity<ApiResponse<InventoryItemsDto>> getInventoryItems(@PathVariable("item_id") Integer item_id) {
        InventoryItemsDto itemsDto = InventoryItemsDto.builder()
            .itemId(item_id)
            .build();
        InventoryItemsDto result = itemsService.getInventoryItems(itemsDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateInventoryItems(@RequestBody InventoryItemsDto itemsDto) {
        ResponseStatusDto status = itemsService.updateInventoryItems(itemsDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @DeleteMapping("/{item_id}")
    public ResponseEntity<ApiResponse<Void>> deleteInventoryItems(@PathVariable("item_id") Integer item_id) {
        InventoryItemsDto itemsDto = InventoryItemsDto.builder()
            .itemId(item_id)
            .build();
        ResponseStatusDto status = itemsService.deleteInventoryItems(itemsDto);
        return ResponseEntity.ok(ApiResponse.fromResponseStatus(status));
    }

    @GetMapping("/scan-by-pk/{item_id}")
    public ResponseEntity<ApiResponse<List<InventoryItemsDto>>> getInventoryItemsByPk(@PathVariable("item_id") Integer item_id) {
        InventoryItemsDto itemsDto = InventoryItemsDto.builder()
            .itemId(item_id)
            .build();
        List<InventoryItemsDto> result = itemsService.getInventoryItemsListByPk(itemsDto);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/scan-all")
    public ResponseEntity<ApiResponse<List<InventoryItemsDto>>> getInventoryItemsListAll() {
        List<InventoryItemsDto> result = itemsService.getInventoryItemsListAll();
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