package com.example.demo_jdbc_samples.service;

import com.example.demo_jdbc_samples.exception.CustomException;
import com.example.demo_jdbc_samples.model.CustomerCustomers;
import com.example.demo_jdbc_samples.repository.CustomerCustomersRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntUnaryOperator;
import org.springframework.stereotype.Service;

/**
 * Customers by name, plus read-modify-write and insert, for the customer-crud load test.
 *
 * The search is a SELECT on name (the ScalarDB version uses ScanAll + where):
 * - exact:  name = value
 * - prefix: name LIKE 'value%'
 * There is one plain index on name, as in the ScalarDB version.
 */
@Service
public class CustomerCrudService {
    public static final String EXACT = "exact";
    public static final String PREFIX = "prefix";
    // Load test only (W5, W6): by the primary key customer_id instead of the name
    public static final String KEY = "key";

    // Transaction attempts including retries; the load test reads the difference
    private final AtomicLong attemptCount = new AtomicLong();

    private final TransactionRunner transactionRunner;
    private final CustomerCustomersRepository customersRepository;

    public CustomerCrudService(TransactionRunner transactionRunner, CustomerCustomersRepository customersRepository) {
        this.transactionRunner = transactionRunner;
        this.customersRepository = customersRepository;
    }

    // Search by name in one transaction. limit 0 = no limit
    public List<CustomerCustomers> search(String value, String match, int limit) throws CustomException {
        boolean prefix = checkSearch(value, match);
        return transactionRunner.run(() -> customersRepository.searchByName(value, prefix, limit), attemptCount);
    }

    /**
     * Search by name, then add 1 to credit_total of one of the rows found, in one transaction.
     * chooser picks the index of the row to update from the number of rows found.
     * Returns the number of rows updated (0 if nothing was found).
     *
     * The search does not lock; the chosen row is read again with SELECT ... FOR UPDATE and
     * credit_total is computed from that read, so a concurrent update is not lost. (Oracle
     * does not allow FOR UPDATE with FETCH FIRST, and only the updated row needs a lock.)
     */
    public int readModifyWrite(String value, String match, int limit, IntUnaryOperator chooser)
            throws CustomException {
        boolean prefix = checkSearch(value, match);
        return transactionRunner.run(() -> {
            List<CustomerCustomers> found = customersRepository.searchByName(value, prefix, limit);
            if (found.isEmpty()) {
                return 0;
            }
            CustomerCustomers customer = customersRepository.getCustomerCustomersForUpdate(
                found.get(chooser.applyAsInt(found.size())));
            customer.setCreditTotal(customer.getCreditTotal() + 1);
            customersRepository.updateCustomerCustomers(customer);
            return 1;
        }, attemptCount);
    }

    // Read one row by the primary key in one transaction: 0 or 1 rows (load test W6)
    public List<CustomerCustomers> readByKey(int customerId) throws CustomException {
        return transactionRunner.run(() -> {
            CustomerCustomers customer = getOrNull(customerId, false);
            return customer == null ? List.of() : List.of(customer);
        }, attemptCount);
    }

    /**
     * Read the row by the primary key with SELECT ... FOR UPDATE, then add 1 to credit_total,
     * in one transaction (load test W5). Returns the number of rows updated (0 if there is no row).
     */
    public int readModifyWriteByKey(int customerId) throws CustomException {
        return transactionRunner.run(() -> {
            CustomerCustomers customer = getOrNull(customerId, true);
            if (customer == null) {
                return 0;
            }
            customer.setCreditTotal(customer.getCreditTotal() + 1);
            customersRepository.updateCustomerCustomers(customer);
            return 1;
        }, attemptCount);
    }

    // The row, or null if there is none (the repository's 9700)
    private CustomerCustomers getOrNull(int customerId, boolean forUpdate) {
        CustomerCustomers key = CustomerCustomers.builder().customerId(customerId).build();
        try {
            return forUpdate ? customersRepository.getCustomerCustomersForUpdate(key)
                : customersRepository.getCustomerCustomers(key);
        } catch (CustomException e) {
            if (Integer.valueOf(9700).equals(e.getErrorCode())) {
                return null;
            }
            throw e;
        }
    }

    // Insert customer_id from..to in one transaction (name by OrderRules.testName, credit_total 0)
    public int insertRange(int from, int to, long creditLimit) throws CustomException {
        return transactionRunner.run(() -> {
            for (int id = from; id <= to; id++) {
                customersRepository.insertCustomerCustomers(CustomerCustomers.builder()
                    .customerId(id)
                    .name(OrderRules.testName(id))
                    .creditLimit(creditLimit)
                    .creditTotal(0L)
                    .build());
            }
            return to - from + 1;
        }, attemptCount);
    }

    public long getAttemptCount() {
        return attemptCount.get();
    }

    // true = prefix
    private static boolean checkSearch(String value, String match) {
        if (value == null || value.isEmpty()) {
            throw new CustomException("name must not be empty", OrderRules.INVALID_REQUEST);
        }
        if (EXACT.equals(match)) {
            return false;
        }
        if (PREFIX.equals(match)) {
            return true;
        }
        throw new CustomException("match must be exact or prefix: " + match, OrderRules.INVALID_REQUEST);
    }
}
