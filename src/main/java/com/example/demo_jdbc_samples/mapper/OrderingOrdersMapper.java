package com.example.demo_jdbc_samples.mapper;

import com.example.demo_jdbc_samples.model.OrderingOrders;
import com.example.demo_jdbc_samples.dto.OrderingOrdersDto;
import java.util.ArrayList;
import java.util.List;
import org.modelmapper.ModelMapper;

public class OrderingOrdersMapper {
    private static final ModelMapper modelMapper = new ModelMapper();

    // Convert Model to DTO
    public static OrderingOrdersDto mapToOrderingOrdersDto(OrderingOrders orders) {
        return modelMapper.map(orders, OrderingOrdersDto.class);
    }

    // Convert DTO to Model
    public static OrderingOrders mapToOrderingOrders(OrderingOrdersDto ordersDto) {
        return modelMapper.map(ordersDto, OrderingOrders.class);
    }

    // Convert Model List to DTO List
    public static List<OrderingOrdersDto> mapToOrderingOrdersDtoList(List<OrderingOrders> ordersList) {
        List<OrderingOrdersDto> ordersDtoList = new ArrayList<>();
        for (OrderingOrders orders : ordersList) {
            ordersDtoList.add(mapToOrderingOrdersDto(orders));
        }
        return ordersDtoList;
    }
}