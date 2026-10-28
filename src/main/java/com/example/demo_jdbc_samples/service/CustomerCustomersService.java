package com.example.demo_jdbc_samples.service;

import com.example.demo_jdbc_samples.dto.CustomerCustomersDto;
import com.example.demo_jdbc_samples.dto.ResponseStatusDto;
import com.example.demo_jdbc_samples.exception.CustomException;
import com.example.demo_jdbc_samples.mapper.CustomerCustomersMapper;
import com.example.demo_jdbc_samples.model.CustomerCustomers;
import com.example.demo_jdbc_samples.repository.CustomerCustomersRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * CRUD of one table, one transaction per call with retry (TransactionRunner).
 */
@Service
public class CustomerCustomersService {
    private final TransactionRunner transactionRunner;
    private final CustomerCustomersRepository customersRepository;

    public CustomerCustomersService(TransactionRunner transactionRunner, CustomerCustomersRepository customersRepository) {
        this.transactionRunner = transactionRunner;
        this.customersRepository = customersRepository;
    }

    // Create Record
    public ResponseStatusDto insertCustomerCustomers(CustomerCustomersDto customersDto) throws CustomException {
        CustomerCustomers customers = CustomerCustomersMapper.mapToCustomerCustomers(customersDto);
        return transactionRunner.run(() -> {
            customersRepository.insertCustomerCustomers(customers);
            return ok();
        });
    }

    // Upsert Record
    public ResponseStatusDto upsertCustomerCustomers(CustomerCustomersDto customersDto) throws CustomException {
        CustomerCustomers customers = CustomerCustomersMapper.mapToCustomerCustomers(customersDto);
        return transactionRunner.run(() -> {
            customersRepository.upsertCustomerCustomers(customers);
            return ok();
        });
    }

    // Retrieve Record
    public CustomerCustomersDto getCustomerCustomers(CustomerCustomersDto customersDto) throws CustomException {
        CustomerCustomers customers = CustomerCustomersMapper.mapToCustomerCustomers(customersDto);
        return transactionRunner.run(() -> CustomerCustomersMapper.mapToCustomerCustomersDto(customersRepository.getCustomerCustomers(customers)));
    }

    // Update Record
    public ResponseStatusDto updateCustomerCustomers(CustomerCustomersDto customersDto) throws CustomException {
        CustomerCustomers customers = CustomerCustomersMapper.mapToCustomerCustomers(customersDto);
        return transactionRunner.run(() -> {
            customersRepository.updateCustomerCustomers(customers);
            return ok();
        });
    }

    // Delete Record
    public ResponseStatusDto deleteCustomerCustomers(CustomerCustomersDto customersDto) throws CustomException {
        CustomerCustomers customers = CustomerCustomersMapper.mapToCustomerCustomers(customersDto);
        return transactionRunner.run(() -> {
            customersRepository.deleteCustomerCustomers(customers);
            return ok();
        });
    }

    // Retrieve All Records
    public List<CustomerCustomersDto> getCustomerCustomersListAll() throws CustomException {
        return transactionRunner.run(() -> CustomerCustomersMapper.mapToCustomerCustomersDtoList(customersRepository.getCustomerCustomersListAll()));
    }

    // Retrieve Records by Partition Key
    public List<CustomerCustomersDto> getCustomerCustomersListByPk(CustomerCustomersDto customersDto) throws CustomException {
        CustomerCustomers customers = CustomerCustomersMapper.mapToCustomerCustomers(customersDto);
        return transactionRunner.run(() -> CustomerCustomersMapper.mapToCustomerCustomersDtoList(customersRepository.getCustomerCustomersListByPk(customers)));
    }

    private static ResponseStatusDto ok() {
        return ResponseStatusDto.builder().code(0).message("").build();
    }
}
