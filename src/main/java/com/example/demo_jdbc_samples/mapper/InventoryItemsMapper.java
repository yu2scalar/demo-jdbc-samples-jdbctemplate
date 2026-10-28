package com.example.demo_jdbc_samples.mapper;

import com.example.demo_jdbc_samples.model.InventoryItems;
import com.example.demo_jdbc_samples.dto.InventoryItemsDto;
import java.util.ArrayList;
import java.util.List;
import org.modelmapper.ModelMapper;

public class InventoryItemsMapper {
    private static final ModelMapper modelMapper = new ModelMapper();

    // Convert Model to DTO
    public static InventoryItemsDto mapToInventoryItemsDto(InventoryItems items) {
        return modelMapper.map(items, InventoryItemsDto.class);
    }

    // Convert DTO to Model
    public static InventoryItems mapToInventoryItems(InventoryItemsDto itemsDto) {
        return modelMapper.map(itemsDto, InventoryItems.class);
    }

    // Convert Model List to DTO List
    public static List<InventoryItemsDto> mapToInventoryItemsDtoList(List<InventoryItems> itemsList) {
        List<InventoryItemsDto> itemsDtoList = new ArrayList<>();
        for (InventoryItems items : itemsList) {
            itemsDtoList.add(mapToInventoryItemsDto(items));
        }
        return itemsDtoList;
    }
}