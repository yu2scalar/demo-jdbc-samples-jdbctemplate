package com.example.demo_jdbc_samples.service;

import com.example.demo_jdbc_samples.dto.OrderingOrdersDto;
import com.example.demo_jdbc_samples.dto.ResponseStatusDto;
import com.example.demo_jdbc_samples.exception.CustomException;
import com.example.demo_jdbc_samples.mapper.OrderingOrdersMapper;
import com.example.demo_jdbc_samples.model.OrderingOrders;
import com.example.demo_jdbc_samples.repository.OrderingOrdersRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * CRUD of one table, one transaction per call with retry (TransactionRunner).
 */
@Service
public class OrderingOrdersService {
    private final TransactionRunner transactionRunner;
    private final OrderingOrdersRepository ordersRepository;

    public OrderingOrdersService(TransactionRunner transactionRunner, OrderingOrdersRepository ordersRepository) {
        this.transactionRunner = transactionRunner;
        this.ordersRepository = ordersRepository;
    }

    // Create Record
    public ResponseStatusDto insertOrderingOrders(OrderingOrdersDto ordersDto) throws CustomException {
        OrderingOrders orders = OrderingOrdersMapper.mapToOrderingOrders(ordersDto);
        return transactionRunner.run(() -> {
            ordersRepository.insertOrderingOrders(orders);
            return ok();
        });
    }

    // Upsert Record
    public ResponseStatusDto upsertOrderingOrders(OrderingOrdersDto ordersDto) throws CustomException {
        OrderingOrders orders = OrderingOrdersMapper.mapToOrderingOrders(ordersDto);
        return transactionRunner.run(() -> {
            ordersRepository.upsertOrderingOrders(orders);
            return ok();
        });
    }

    // Retrieve Record
    public OrderingOrdersDto getOrderingOrders(OrderingOrdersDto ordersDto) throws CustomException {
        OrderingOrders orders = OrderingOrdersMapper.mapToOrderingOrders(ordersDto);
        return transactionRunner.run(() -> OrderingOrdersMapper.mapToOrderingOrdersDto(ordersRepository.getOrderingOrders(orders)));
    }

    // Update Record
    public ResponseStatusDto updateOrderingOrders(OrderingOrdersDto ordersDto) throws CustomException {
        OrderingOrders orders = OrderingOrdersMapper.mapToOrderingOrders(ordersDto);
        return transactionRunner.run(() -> {
            ordersRepository.updateOrderingOrders(orders);
            return ok();
        });
    }

    // Delete Record
    public ResponseStatusDto deleteOrderingOrders(OrderingOrdersDto ordersDto) throws CustomException {
        OrderingOrders orders = OrderingOrdersMapper.mapToOrderingOrders(ordersDto);
        return transactionRunner.run(() -> {
            ordersRepository.deleteOrderingOrders(orders);
            return ok();
        });
    }

    // Retrieve All Records
    public List<OrderingOrdersDto> getOrderingOrdersListAll() throws CustomException {
        return transactionRunner.run(() -> OrderingOrdersMapper.mapToOrderingOrdersDtoList(ordersRepository.getOrderingOrdersListAll()));
    }

    // Retrieve Records by Partition Key
    public List<OrderingOrdersDto> getOrderingOrdersListByPk(OrderingOrdersDto ordersDto) throws CustomException {
        OrderingOrders orders = OrderingOrdersMapper.mapToOrderingOrders(ordersDto);
        return transactionRunner.run(() -> OrderingOrdersMapper.mapToOrderingOrdersDtoList(ordersRepository.getOrderingOrdersListByPk(orders)));
    }

    private static ResponseStatusDto ok() {
        return ResponseStatusDto.builder().code(0).message("").build();
    }
}
