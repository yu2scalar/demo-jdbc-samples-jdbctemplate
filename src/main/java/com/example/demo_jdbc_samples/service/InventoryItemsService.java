package com.example.demo_jdbc_samples.service;

import com.example.demo_jdbc_samples.dto.InventoryItemsDto;
import com.example.demo_jdbc_samples.dto.ResponseStatusDto;
import com.example.demo_jdbc_samples.exception.CustomException;
import com.example.demo_jdbc_samples.mapper.InventoryItemsMapper;
import com.example.demo_jdbc_samples.model.InventoryItems;
import com.example.demo_jdbc_samples.repository.InventoryItemsRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * CRUD of one table, one transaction per call with retry (TransactionRunner).
 */
@Service
public class InventoryItemsService {
    private final TransactionRunner transactionRunner;
    private final InventoryItemsRepository itemsRepository;

    public InventoryItemsService(TransactionRunner transactionRunner, InventoryItemsRepository itemsRepository) {
        this.transactionRunner = transactionRunner;
        this.itemsRepository = itemsRepository;
    }

    // Create Record
    public ResponseStatusDto insertInventoryItems(InventoryItemsDto itemsDto) throws CustomException {
        InventoryItems items = InventoryItemsMapper.mapToInventoryItems(itemsDto);
        return transactionRunner.run(() -> {
            itemsRepository.insertInventoryItems(items);
            return ok();
        });
    }

    // Upsert Record
    public ResponseStatusDto upsertInventoryItems(InventoryItemsDto itemsDto) throws CustomException {
        InventoryItems items = InventoryItemsMapper.mapToInventoryItems(itemsDto);
        return transactionRunner.run(() -> {
            itemsRepository.upsertInventoryItems(items);
            return ok();
        });
    }

    // Retrieve Record
    public InventoryItemsDto getInventoryItems(InventoryItemsDto itemsDto) throws CustomException {
        InventoryItems items = InventoryItemsMapper.mapToInventoryItems(itemsDto);
        return transactionRunner.run(() -> InventoryItemsMapper.mapToInventoryItemsDto(itemsRepository.getInventoryItems(items)));
    }

    // Update Record
    public ResponseStatusDto updateInventoryItems(InventoryItemsDto itemsDto) throws CustomException {
        InventoryItems items = InventoryItemsMapper.mapToInventoryItems(itemsDto);
        return transactionRunner.run(() -> {
            itemsRepository.updateInventoryItems(items);
            return ok();
        });
    }

    // Delete Record
    public ResponseStatusDto deleteInventoryItems(InventoryItemsDto itemsDto) throws CustomException {
        InventoryItems items = InventoryItemsMapper.mapToInventoryItems(itemsDto);
        return transactionRunner.run(() -> {
            itemsRepository.deleteInventoryItems(items);
            return ok();
        });
    }

    // Retrieve All Records
    public List<InventoryItemsDto> getInventoryItemsListAll() throws CustomException {
        return transactionRunner.run(() -> InventoryItemsMapper.mapToInventoryItemsDtoList(itemsRepository.getInventoryItemsListAll()));
    }

    // Retrieve Records by Partition Key
    public List<InventoryItemsDto> getInventoryItemsListByPk(InventoryItemsDto itemsDto) throws CustomException {
        InventoryItems items = InventoryItemsMapper.mapToInventoryItems(itemsDto);
        return transactionRunner.run(() -> InventoryItemsMapper.mapToInventoryItemsDtoList(itemsRepository.getInventoryItemsListByPk(items)));
    }

    private static ResponseStatusDto ok() {
        return ResponseStatusDto.builder().code(0).message("").build();
    }
}
