package com.example.demo_jdbc_samples.service;

import com.example.demo_jdbc_samples.dto.OrderingStatementsDto;
import com.example.demo_jdbc_samples.dto.ResponseStatusDto;
import com.example.demo_jdbc_samples.exception.CustomException;
import com.example.demo_jdbc_samples.mapper.OrderingStatementsMapper;
import com.example.demo_jdbc_samples.model.OrderingStatements;
import com.example.demo_jdbc_samples.repository.OrderingStatementsRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * CRUD of one table, one transaction per call with retry (TransactionRunner).
 */
@Service
public class OrderingStatementsService {
    private final TransactionRunner transactionRunner;
    private final OrderingStatementsRepository statementsRepository;

    public OrderingStatementsService(TransactionRunner transactionRunner, OrderingStatementsRepository statementsRepository) {
        this.transactionRunner = transactionRunner;
        this.statementsRepository = statementsRepository;
    }

    // Create Record
    public ResponseStatusDto insertOrderingStatements(OrderingStatementsDto statementsDto) throws CustomException {
        OrderingStatements statements = OrderingStatementsMapper.mapToOrderingStatements(statementsDto);
        return transactionRunner.run(() -> {
            statementsRepository.insertOrderingStatements(statements);
            return ok();
        });
    }

    // Upsert Record
    public ResponseStatusDto upsertOrderingStatements(OrderingStatementsDto statementsDto) throws CustomException {
        OrderingStatements statements = OrderingStatementsMapper.mapToOrderingStatements(statementsDto);
        return transactionRunner.run(() -> {
            statementsRepository.upsertOrderingStatements(statements);
            return ok();
        });
    }

    // Retrieve Record
    public OrderingStatementsDto getOrderingStatements(OrderingStatementsDto statementsDto) throws CustomException {
        OrderingStatements statements = OrderingStatementsMapper.mapToOrderingStatements(statementsDto);
        return transactionRunner.run(() -> OrderingStatementsMapper.mapToOrderingStatementsDto(statementsRepository.getOrderingStatements(statements)));
    }

    // Update Record
    public ResponseStatusDto updateOrderingStatements(OrderingStatementsDto statementsDto) throws CustomException {
        OrderingStatements statements = OrderingStatementsMapper.mapToOrderingStatements(statementsDto);
        return transactionRunner.run(() -> {
            statementsRepository.updateOrderingStatements(statements);
            return ok();
        });
    }

    // Delete Record
    public ResponseStatusDto deleteOrderingStatements(OrderingStatementsDto statementsDto) throws CustomException {
        OrderingStatements statements = OrderingStatementsMapper.mapToOrderingStatements(statementsDto);
        return transactionRunner.run(() -> {
            statementsRepository.deleteOrderingStatements(statements);
            return ok();
        });
    }

    // Retrieve All Records
    public List<OrderingStatementsDto> getOrderingStatementsListAll() throws CustomException {
        return transactionRunner.run(() -> OrderingStatementsMapper.mapToOrderingStatementsDtoList(statementsRepository.getOrderingStatementsListAll()));
    }

    // Retrieve Records by Partition Key
    public List<OrderingStatementsDto> getOrderingStatementsListByPk(OrderingStatementsDto statementsDto) throws CustomException {
        OrderingStatements statements = OrderingStatementsMapper.mapToOrderingStatements(statementsDto);
        return transactionRunner.run(() -> OrderingStatementsMapper.mapToOrderingStatementsDtoList(statementsRepository.getOrderingStatementsListByPk(statements)));
    }

    private static ResponseStatusDto ok() {
        return ResponseStatusDto.builder().code(0).message("").build();
    }
}
