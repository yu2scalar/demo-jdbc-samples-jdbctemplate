package com.example.demo_jdbc_samples.service.loadtest;

import com.example.demo_jdbc_samples.dto.LoadTestVerificationDto;
import com.example.demo_jdbc_samples.dto.OrderLineDto;
import com.example.demo_jdbc_samples.dto.PlaceOrderRequestDto;
import com.example.demo_jdbc_samples.dto.PlaceOrderResponseDto;
import com.example.demo_jdbc_samples.exception.CustomException;
import com.example.demo_jdbc_samples.model.CustomerCustomers;
import com.example.demo_jdbc_samples.model.InventoryItems;
import com.example.demo_jdbc_samples.model.OrderingOrders;
import com.example.demo_jdbc_samples.model.OrderingStatements;
import com.example.demo_jdbc_samples.repository.CustomerCustomersRepository;
import com.example.demo_jdbc_samples.repository.InventoryItemsRepository;
import com.example.demo_jdbc_samples.repository.OrderingOrdersRepository;
import com.example.demo_jdbc_samples.repository.OrderingStatementsRepository;
import com.example.demo_jdbc_samples.service.InitService;
import com.example.demo_jdbc_samples.service.OrderRules;
import com.example.demo_jdbc_samples.service.OrderService;
import com.example.demo_jdbc_samples.service.TransactionRunner;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Load test of PlaceOrder (OrderService.placeOrder, JDBC).
 *
 * init: customers 1..N and items 1..M through InitService, with a large credit
 * limit and stock so that 9800 / 9801 do not distort the run.
 * run: a random customer, 1..K distinct random items, qty 1..qty_max each.
 * verify: reads credit_total and stock before and after the run by primary key and
 * checks them, and every successful order, against the successful calls. On a
 * mismatch it reads again, as the ScalarDB version (see verify()).
 */
@Slf4j
@Component
public class PlaceOrderScenario implements LoadTestScenario<PlaceOrderScenario.PlacedOrder> {
    public static final long DEFAULT_CREDIT_LIMIT = 1_000_000_000_000_000L;
    public static final int DEFAULT_STOCK = 1_000_000_000;
    // Rows read per transaction by the check
    private static final int READ_BATCH = 100;
    // The check reads again on a mismatch, up to VERIFY_READS times in all
    private static final int VERIFY_READS = 5;
    private static final long VERIFY_INTERVAL_MS = 1000;

    // One successful call
    public record PlacedOrder(String orderId, int customerId, long totalAmount, int[] itemIds, int[] qtys) {}

    private final OrderService orderService;
    private final InitService initService;
    private final TransactionRunner transactionRunner;
    private final CustomerCustomersRepository customersRepository;
    private final InventoryItemsRepository itemsRepository;
    private final OrderingOrdersRepository ordersRepository;
    private final OrderingStatementsRepository statementsRepository;

    // Set by init; 0 = not initialized (e.g. after a restart)
    private volatile int customers;
    private volatile int items;

    // Taken by beforeRun
    private long[] creditTotalsBefore;
    private long[] stocksBefore;

    public PlaceOrderScenario(OrderService orderService, InitService initService,
            TransactionRunner transactionRunner,
            CustomerCustomersRepository customersRepository, InventoryItemsRepository itemsRepository,
            OrderingOrdersRepository ordersRepository, OrderingStatementsRepository statementsRepository) {
        this.orderService = orderService;
        this.initService = initService;
        this.transactionRunner = transactionRunner;
        this.customersRepository = customersRepository;
        this.itemsRepository = itemsRepository;
        this.ordersRepository = ordersRepository;
        this.statementsRepository = statementsRepository;
    }

    @Override
    public String name() {
        return "place-order";
    }

    // params: customers, items (required), truncate (false), credit_limit (1e15), stock (1e9)
    @Override
    public Map<String, Object> init(LoadTestParams params) throws CustomException {
        int n = params.getInt("customers", 0);
        int m = params.getInt("items", 0);
        if (n < 1 || m < 1) {
            throw new CustomException("customers and items must be 1 or more", OrderRules.INVALID_REQUEST);
        }
        boolean truncate = params.getBoolean("truncate", false);
        long creditLimit = params.getLong("credit_limit", DEFAULT_CREDIT_LIMIT);
        int stock = params.getInt("stock", DEFAULT_STOCK);

        customers = 0;
        items = 0;
        if (truncate) {
            initService.truncateAll();
        }
        initService.initCustomers(n, creditLimit);
        initService.initItems(m, stock);
        customers = n;
        items = m;
        log.info("Load test {} initialized: customers={}, items={}, truncate={}", name(), n, m, truncate);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scenario", name());
        result.put("customers", n);
        result.put("items", m);
        result.put("truncated", truncate);
        result.put("credit_limit", creditLimit);
        result.put("stock", stock);
        return result;
    }

    // params: items_per_order_max (K, 1), qty_max (1)
    @Override
    public void beforeRun(LoadTestParams params) throws CustomException {
        if (customers == 0 || items == 0) {
            throw new CustomException("Not initialized: call /load-test/" + name() + "/init first",
                LoadTestRunner.NOT_INITIALIZED);
        }
        int k = params.getInt("items_per_order_max", 1);
        // Statements are read back with the repository's scan limit (100)
        int kLimit = Math.min(items, statementsRepository.getScanLimit());
        if (k < 1 || k > kLimit) {
            throw new CustomException("items_per_order_max must be 1.." + kLimit + ": " + k, OrderRules.INVALID_REQUEST);
        }
        if (params.getInt("qty_max", 1) < 1) {
            throw new CustomException("qty_max must be 1 or more", OrderRules.INVALID_REQUEST);
        }
        creditTotalsBefore = readCreditTotals();
        stocksBefore = readStocks();
    }

    @Override
    public PlacedOrder execute(Random random, LoadTestParams params) throws Exception {
        int k = params.getInt("items_per_order_max", 1);
        int qtyMax = params.getInt("qty_max", 1);
        int customerId = 1 + random.nextInt(customers);
        int lineCount = 1 + random.nextInt(k);
        Set<Integer> itemIds = new LinkedHashSet<>();
        while (itemIds.size() < lineCount) {
            itemIds.add(1 + random.nextInt(items));
        }
        List<OrderLineDto> lines = new ArrayList<>();
        for (int itemId : itemIds) {
            lines.add(OrderLineDto.builder().itemId(itemId).qty(1 + random.nextInt(qtyMax)).build());
        }

        PlaceOrderResponseDto response = orderService.placeOrder(
            PlaceOrderRequestDto.builder().customerId(customerId).items(lines).build());

        int[] ids = new int[lines.size()];
        int[] qtys = new int[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            ids[i] = lines.get(i).getItemId();
            qtys[i] = lines.get(i).getQty();
        }
        return new PlacedOrder(response.getOrderId(), customerId, response.getTotalAmount(), ids, qtys);
    }

    @Override
    public long attemptCount() {
        return orderService.getAttemptCount();
    }

    @Override
    public LoadTestVerificationDto verify(List<PlacedOrder> successes, Map<Integer, Long> failuresByCode)
            throws CustomException {
        long[] expectedCredit = new long[customers + 1];
        long[] expectedConsumed = new long[items + 1];
        for (PlacedOrder order : successes) {
            expectedCredit[order.customerId()] += order.totalAmount();
            for (int i = 0; i < order.itemIds().length; i++) {
                expectedConsumed[order.itemIds()[i]] += order.qtys()[i];
            }
        }

        // Read again on a mismatch, as the ScalarDB version (there, records left PREPARED are
        // recovered lazily). With JDBC a committed transaction is visible at once, so
        // first_read_* are expected to be 0.
        Map<String, Object> checks = check(successes, expectedCredit, expectedConsumed);
        Map<String, Object> firstRead = checks;
        int reads = 1;
        while (!isConsistent(checks) && reads < VERIFY_READS) {
            sleep(VERIFY_INTERVAL_MS);
            checks = check(successes, expectedCredit, expectedConsumed);
            reads++;
        }
        boolean consistent = isConsistent(checks);
        checks.put("reads", reads);
        // Rows that were not yet recovered at the first read (0 = consistent at once)
        checks.put("first_read_customer_mismatches", firstRead.get("customer_mismatches"));
        checks.put("first_read_item_mismatches", firstRead.get("item_mismatches"));
        checks.put("first_read_orders_missing", firstRead.get("orders_missing"));
        checks.put("first_read_orders_mismatched", firstRead.get("orders_mismatched"));

        String note = null;
        if (failuresByCode.getOrDefault(9200, 0L) > 0) {
            note = "9200 (transaction status unknown) occurred " + failuresByCode.get(9200)
                + " times: those orders may have been committed, so a mismatch is not necessarily an error";
        }
        return LoadTestVerificationDto.builder().consistent(consistent).checks(checks).note(note).build();
    }

    private Map<String, Object> check(List<PlacedOrder> successes, long[] expectedCredit, long[] expectedConsumed)
            throws CustomException {
        long[] creditTotalsAfter = readCreditTotals();
        long[] stocksAfter = readStocks();
        long expectedCreditSum = 0, actualCreditSum = 0, customerMismatches = 0;
        for (int c = 1; c <= customers; c++) {
            long actual = creditTotalsAfter[c] - creditTotalsBefore[c];
            expectedCreditSum += expectedCredit[c];
            actualCreditSum += actual;
            if (actual != expectedCredit[c]) {
                customerMismatches++;
            }
        }
        long expectedConsumedSum = 0, actualConsumedSum = 0, itemMismatches = 0;
        for (int i = 1; i <= items; i++) {
            long actual = stocksBefore[i] - stocksAfter[i];
            expectedConsumedSum += expectedConsumed[i];
            actualConsumedSum += actual;
            if (actual != expectedConsumed[i]) {
                itemMismatches++;
            }
        }
        long[] orderCheck = checkOrders(successes);

        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("successful_orders", (long) successes.size());
        checks.put("expected_credit_total_increase", expectedCreditSum);
        checks.put("actual_credit_total_increase", actualCreditSum);
        checks.put("customer_mismatches", customerMismatches);
        checks.put("expected_stock_consumed", expectedConsumedSum);
        checks.put("actual_stock_consumed", actualConsumedSum);
        checks.put("item_mismatches", itemMismatches);
        checks.put("orders_missing", orderCheck[0]);
        checks.put("orders_mismatched", orderCheck[1]);
        return checks;
    }

    private static boolean isConsistent(Map<String, Object> checks) {
        return (long) checks.get("customer_mismatches") == 0 && (long) checks.get("item_mismatches") == 0
            && (long) checks.get("orders_missing") == 0 && (long) checks.get("orders_mismatched") == 0;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException("Load test check was interrupted", LoadTestRunner.INTERRUPTED);
        }
    }

    @FunctionalInterface
    private interface Read {
        void run(int id);
    }

    // Reads ids 1..count, READ_BATCH per transaction
    private void readAll(int count, Read read) throws CustomException {
        for (int from = 1; from <= count; from += READ_BATCH) {
            int start = from;
            int end = Math.min(from + READ_BATCH - 1, count);
            try {
                transactionRunner.runOnce(() -> {
                    for (int id = start; id <= end; id++) {
                        read.run(id);
                    }
                    return null;
                });
            } catch (CustomException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new CustomException("Load test check failed: " + e.getMessage(), e, 9500);
            }
        }
    }

    private long[] readCreditTotals() throws CustomException {
        long[] values = new long[customers + 1];
        readAll(customers, id -> values[id] = customersRepository.getCustomerCustomers(
            CustomerCustomers.builder().customerId(id).build()).getCreditTotal());
        return values;
    }

    private long[] readStocks() throws CustomException {
        long[] values = new long[items + 1];
        readAll(items, id -> values[id] = itemsRepository.getInventoryItems(
            InventoryItems.builder().itemId(id).build()).getStock());
        return values;
    }

    // [missing, mismatched]: order not found / total_amount or number of statements differs
    private long[] checkOrders(List<PlacedOrder> successes) throws CustomException {
        long[] result = new long[2];
        readAll(successes.size(), index -> {
            PlacedOrder expected = successes.get(index - 1);
            OrderingOrders order;
            try {
                order = ordersRepository.getOrderingOrders(
                    OrderingOrders.builder().orderId(expected.orderId()).build());
            } catch (CustomException e) {
                result[0]++;
                return;
            }
            List<OrderingStatements> statements = statementsRepository.getOrderingStatementsListByPk(
                OrderingStatements.builder().orderId(expected.orderId()).build());
            if (order.getTotalAmount() != expected.totalAmount()
                    || statements.size() != expected.itemIds().length) {
                result[1]++;
            }
        });
        return result;
    }
}
