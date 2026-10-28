package com.example.demo_jdbc_samples.mapper;

import com.example.demo_jdbc_samples.model.CustomerCustomers;
import com.example.demo_jdbc_samples.dto.CustomerCustomersDto;
import java.util.ArrayList;
import java.util.List;
import org.modelmapper.ModelMapper;

public class CustomerCustomersMapper {
    private static final ModelMapper modelMapper = new ModelMapper();

    // Convert Model to DTO
    public static CustomerCustomersDto mapToCustomerCustomersDto(CustomerCustomers customers) {
        return modelMapper.map(customers, CustomerCustomersDto.class);
    }

    // Convert DTO to Model
    public static CustomerCustomers mapToCustomerCustomers(CustomerCustomersDto customersDto) {
        return modelMapper.map(customersDto, CustomerCustomers.class);
    }

    // Convert Model List to DTO List
    public static List<CustomerCustomersDto> mapToCustomerCustomersDtoList(List<CustomerCustomers> customersList) {
        List<CustomerCustomersDto> customersDtoList = new ArrayList<>();
        for (CustomerCustomers customers : customersList) {
            customersDtoList.add(mapToCustomerCustomersDto(customers));
        }
        return customersDtoList;
    }
}