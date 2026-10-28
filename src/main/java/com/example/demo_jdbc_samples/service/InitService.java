package com.example.demo_jdbc_samples.service;

import com.example.demo_jdbc_samples.exception.CustomException;
import com.example.demo_jdbc_samples.model.CustomerCustomers;
import com.example.demo_jdbc_samples.model.InventoryItems;
import com.example.demo_jdbc_samples.repository.CustomerCustomersRepository;
import com.example.demo_jdbc_samples.repository.InventoryItemsRepository;
import com.example.demo_jdbc_samples.repository.OrderingOrdersRepository;
import com.example.demo_jdbc_samples.repository.OrderingStatementsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Initial data for the business logic (LoadInitialData of scalardb-sample), with the
 * number of rows given by the caller. Keys are 1..count; the other columns are fixed
 * values. Existing rows are overwritten (upsert). Rows are written BATCH_SIZE per
 * transaction. truncateAll() empties the four tables beforehand (POST /init?truncate=true).
 */
@Slf4j
@Service
public class InitService {
    public static final int BATCH_SIZE = 100;
    public static final long CREDIT_LIMIT = 1_000_000L;
    public static final long PRICE = 100L;
    public static final int STOCK = 1_000_000;

    private final TransactionRunner transactionRunner;
    private final CustomerCustomersRepository customersRepository;
    private final InventoryItemsRepository itemsRepository;
    private final OrderingOrdersRepository ordersRepository;
    private final OrderingStatementsRepository statementsRepository;

    public InitService(TransactionRunner transactionRunner,
            CustomerCustomersRepository customersRepository, InventoryItemsRepository itemsRepository,
            OrderingOrdersRepository ordersRepository, OrderingStatementsRepository statementsRepository) {
        this.transactionRunner = transactionRunner;
        this.customersRepository = customersRepository;
        this.itemsRepository = itemsRepository;
        this.ordersRepository = ordersRepository;
        this.statementsRepository = statementsRepository;
    }

    // Empties the four tables with TRUNCATE TABLE. Run it while nothing else uses them
    // (Oracle commits a TRUNCATE at once; PostgreSQL waits for other transactions).
    public void truncateAll() throws CustomException {
        truncate("ordering.statements", statementsRepository::truncate);
        truncate("ordering.orders", ordersRepository::truncate);
        truncate("customer.customers", customersRepository::truncate);
        truncate("inventory.items", itemsRepository::truncate);
    }

    private void truncate(String name, Runnable truncate) throws CustomException {
        log.info("Truncating {}", name);
        try {
            transactionRunner.runOnce(() -> {
                truncate.run();
                return null;
            });
        } catch (RuntimeException e) {
            throw new CustomException("Failed to truncate " + name + ": " + e.getMessage(), e, 9500);
        }
        log.info("Truncated {}", name);
    }

    // customer_id 1..count, credit_limit 1,000,000, credit_total 0
    public int initCustomers(int count) throws CustomException {
        return initCustomers(count, CREDIT_LIMIT);
    }

    // As above with the given credit_limit (the load test uses a large one)
    public int initCustomers(int count, long creditLimit) throws CustomException {
        checkCount(count);
        for (int from = 1; from <= count; from += BATCH_SIZE) {
            int start = from;
            int end = Math.min(from + BATCH_SIZE - 1, count);
            transactionRunner.run(() -> {
                for (int id = start; id <= end; id++) {
                    customersRepository.upsertCustomerCustomers(CustomerCustomers.builder()
                        .customerId(id)
                        .name(OrderRules.testName(id))
                        .creditLimit(creditLimit)
                        .creditTotal(0L)
                        .build());
                }
                return null;
            });
        }
        return count;
    }

    // item_id 1..count, price 100, stock 1,000,000
    public int initItems(int count) throws CustomException {
        return initItems(count, STOCK);
    }

    // As above with the given stock (the load test uses a large one)
    public int initItems(int count, int stock) throws CustomException {
        checkCount(count);
        for (int from = 1; from <= count; from += BATCH_SIZE) {
            int start = from;
            int end = Math.min(from + BATCH_SIZE - 1, count);
            transactionRunner.run(() -> {
                for (int id = start; id <= end; id++) {
                    itemsRepository.upsertInventoryItems(InventoryItems.builder()
                        .itemId(id)
                        .name(OrderRules.testName(id))
                        .price(PRICE)
                        .stock(stock)
                        .build());
                }
                return null;
            });
        }
        return count;
    }

    private static void checkCount(int count) {
        if (count < 0) {
            throw new CustomException("count must not be negative: " + count, OrderRules.INVALID_REQUEST);
        }
    }
}
