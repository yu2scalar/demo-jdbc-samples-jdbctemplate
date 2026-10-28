package com.example.demo_jdbc_samples.mapper;

import com.example.demo_jdbc_samples.model.OrderingStatements;
import com.example.demo_jdbc_samples.dto.OrderingStatementsDto;
import java.util.ArrayList;
import java.util.List;
import org.modelmapper.ModelMapper;

public class OrderingStatementsMapper {
    private static final ModelMapper modelMapper = new ModelMapper();

    // Convert Model to DTO
    public static OrderingStatementsDto mapToOrderingStatementsDto(OrderingStatements statements) {
        return modelMapper.map(statements, OrderingStatementsDto.class);
    }

    // Convert DTO to Model
    public static OrderingStatements mapToOrderingStatements(OrderingStatementsDto statementsDto) {
        return modelMapper.map(statementsDto, OrderingStatements.class);
    }

    // Convert Model List to DTO List
    public static List<OrderingStatementsDto> mapToOrderingStatementsDtoList(List<OrderingStatements> statementsList) {
        List<OrderingStatementsDto> statementsDtoList = new ArrayList<>();
        for (OrderingStatements statements : statementsList) {
            statementsDtoList.add(mapToOrderingStatementsDto(statements));
        }
        return statementsDtoList;
    }
}