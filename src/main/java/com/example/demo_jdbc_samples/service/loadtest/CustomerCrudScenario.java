package com.example.demo_jdbc_samples.service.loadtest;

import com.example.demo_jdbc_samples.dto.LoadTestVerificationDto;
import com.example.demo_jdbc_samples.exception.CustomException;
import com.example.demo_jdbc_samples.model.CustomerCustomers;
import com.example.demo_jdbc_samples.service.CustomerCrudService;
import com.example.demo_jdbc_samples.service.InitService;
import com.example.demo_jdbc_samples.repository.CustomerCustomersRepository;
import com.example.demo_jdbc_samples.service.OrderRules;
import com.example.demo_jdbc_samples.service.TransactionRunner;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * CRUD load test on customer.customers (W1-W4).
 *
 * init: truncates customers (unless truncate=false) and remembers record_count.
 * run, by params.workload:
 * - insert (W1): customer_id 1..record_count in order, rows_per_tx per transaction;
 *   stops when all are inserted (count-based).
 * - rmw (W2 read_ratio=50, W3 read_ratio=95): each call is a read-only search with
 *   probability read_ratio %, otherwise search + credit_total + 1 on one row found.
 * - read (W4): read-only search.
 * The search picks a random customer_id in 1..record_count and searches its name
 * (TEST-%06d, as InitService) with a SELECT on name: exact (name = ?) or prefix
 * (the last prefix_digits digits replaced by %, up to 10^prefix_digits rows).
 */
@Slf4j
@Component
public class CustomerCrudScenario implements LoadTestScenario<CustomerCrudScenario.Call> {
    public static final int DEFAULT_RECORD_COUNT = 100_000;
    // Rows read per transaction by the check
    private static final int READ_BATCH = 100;
    // The check reads again on a mismatch, up to VERIFY_READS times in all
    private static final int VERIFY_READS = 5;
    private static final long VERIFY_INTERVAL_MS = 1000;

    // One successful call: inserted customer_id from..to, or rows updated by RMW
    public record Call(int from, int to, int updated) {}

    private final CustomerCrudService crudService;
    private final CustomerCustomersRepository customersRepository;
    private final TransactionRunner transactionRunner;

    // Set by init; 0 = not initialized (e.g. after a restart)
    private volatile int recordCount;
    // Last customer_id handed out by the insert workload
    private final AtomicInteger lastInsertedKey = new AtomicInteger();

    // Set by beforeRun
    private volatile String workload;
    private long creditTotalBefore;
    private final AtomicLong searches = new AtomicLong();
    private final AtomicLong rowsFound = new AtomicLong();
    private final AtomicLong zeroHits = new AtomicLong();
    private final AtomicLong unexpectedHits = new AtomicLong();

    public CustomerCrudScenario(CustomerCrudService crudService, CustomerCustomersRepository customersRepository,
            TransactionRunner transactionRunner) {
        this.crudService = crudService;
        this.customersRepository = customersRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    public String name() {
        return "customer-crud";
    }

    // params: record_count (100,000), truncate (true). With truncate=false the rows are
    // assumed to be there already (e.g. after a restart), so the insert workload has nothing to do.
    @Override
    public Map<String, Object> init(LoadTestParams params) throws CustomException {
        int n = params.getInt("record_count", DEFAULT_RECORD_COUNT);
        if (n < 1 || n > 999_999) {
            throw new CustomException("record_count must be 1..999999 (TEST-%06d)", OrderRules.INVALID_REQUEST);
        }
        boolean truncate = params.getBoolean("truncate", true);
        recordCount = 0;
        if (truncate) {
            String table = CustomerCustomers.NAMESPACE + "." + CustomerCustomers.TABLE;
            log.info("Truncating {}", table);
            try {
                transactionRunner.runOnce(() -> {
                    customersRepository.truncate();
                    return null;
                });
            } catch (RuntimeException e) {
                throw new CustomException("Failed to truncate " + table + ": " + e.getMessage(), e, 9500);
            }
        }
        lastInsertedKey.set(truncate ? 0 : n);
        recordCount = n;
        log.info("Load test {} initialized: record_count={}, truncate={}", name(), n, truncate);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scenario", name());
        result.put("record_count", n);
        result.put("truncated", truncate);
        return result;
    }

    // params: workload (insert / rmw / read), rows_per_tx (1), match (exact / prefix),
    // prefix_digits (1), read_ratio (rmw: 50)
    @Override
    public void beforeRun(LoadTestParams params) throws CustomException {
        if (recordCount == 0) {
            throw new CustomException("Not initialized: call /load-test/" + name() + "/init first",
                LoadTestRunner.NOT_INITIALIZED);
        }
        String w = params.asMap().getOrDefault("workload", "").toString();
        searches.set(0);
        rowsFound.set(0);
        zeroHits.set(0);
        unexpectedHits.set(0);
        switch (w) {
            case "insert" -> {
                if (params.getInt("rows_per_tx", 1) < 1) {
                    throw new CustomException("rows_per_tx must be 1 or more", OrderRules.INVALID_REQUEST);
                }
                if (lastInsertedKey.get() >= recordCount) {
                    throw new CustomException("All " + recordCount + " rows are already inserted: call init to start over",
                        OrderRules.INVALID_REQUEST);
                }
            }
            case "rmw", "read" -> {
                String match = params.asMap().getOrDefault("match", CustomerCrudService.EXACT).toString();
                if (!CustomerCrudService.EXACT.equals(match) && !CustomerCrudService.PREFIX.equals(match)) {
                    throw new CustomException("match must be exact or prefix: " + match, OrderRules.INVALID_REQUEST);
                }
                int d = params.getInt("prefix_digits", 1);
                if (d < 1 || d > 5) {
                    throw new CustomException("prefix_digits must be 1..5", OrderRules.INVALID_REQUEST);
                }
                int ratio = params.getInt("read_ratio", "read".equals(w) ? 100 : 50);
                if (ratio < 0 || ratio > 100) {
                    throw new CustomException("read_ratio must be 0..100", OrderRules.INVALID_REQUEST);
                }
                creditTotalBefore = "rmw".equals(w) ? readCreditTotalSum() : 0;
            }
            default -> throw new CustomException("workload must be insert, rmw or read: " + w, OrderRules.INVALID_REQUEST);
        }
        workload = w;
    }

    @Override
    public Call execute(Random random, LoadTestParams params) throws Exception {
        if ("insert".equals(workload)) {
            int rowsPerTx = params.getInt("rows_per_tx", 1);
            int from = lastInsertedKey.getAndAdd(rowsPerTx) + 1;
            if (from > recordCount) {
                throw new NoMoreWorkException();
            }
            int to = Math.min(from + rowsPerTx - 1, recordCount);
            crudService.insertRange(from, to, InitService.CREDIT_LIMIT);
            return new Call(from, to, 0);
        }

        String match = params.asMap().getOrDefault("match", CustomerCrudService.EXACT).toString();
        int d = params.getInt("prefix_digits", 1);
        int ratio = params.getInt("read_ratio", "read".equals(workload) ? 100 : 50);
        String name = OrderRules.testName(1 + random.nextInt(recordCount));
        boolean prefix = CustomerCrudService.PREFIX.equals(match);
        String value = prefix ? name.substring(0, name.length() - d) : name;
        int limit = prefix ? (int) Math.pow(10, d) : 0;

        if (random.nextInt(100) < ratio) {
            List<CustomerCustomers> found = crudService.search(value, match, limit);
            countHits(found.size(), prefix, limit);
            return null;
        }
        int updated = crudService.readModifyWrite(value, match, limit, random::nextInt);
        searches.incrementAndGet();
        if (updated == 0) {
            zeroHits.incrementAndGet();
        }
        return new Call(0, 0, updated);
    }

    private void countHits(int rows, boolean prefix, int limit) {
        searches.incrementAndGet();
        rowsFound.addAndGet(rows);
        if (rows == 0) {
            zeroHits.incrementAndGet();
        } else if (prefix ? rows > limit : rows != 1) {
            unexpectedHits.incrementAndGet();
        }
    }

    @Override
    public boolean isDone() {
        return "insert".equals(workload) && lastInsertedKey.get() >= recordCount;
    }

    @Override
    public long attemptCount() {
        return crudService.getAttemptCount();
    }

    @Override
    public LoadTestVerificationDto verify(List<Call> successes, Map<Integer, Long> failuresByCode)
            throws CustomException {
        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("workload", workload);
        boolean consistent;
        if ("insert".equals(workload)) {
            consistent = verifyInsert(successes, checks);
        } else {
            checks.put("searches", searches.get());
            checks.put("rows_found_by_read_only_searches", rowsFound.get());
            // Missing names (failed inserts in W1) are counted here, not as failures
            checks.put("zero_hits", zeroHits.get());
            checks.put("unexpected_hits", unexpectedHits.get());
            consistent = unexpectedHits.get() == 0;
            if ("rmw".equals(workload)) {
                consistent &= verifyRmw(successes, checks);
            }
        }
        String note = null;
        if (failuresByCode.getOrDefault(9200, 0L) > 0) {
            note = "9200 (transaction status unknown) occurred " + failuresByCode.get(9200)
                + " times: those calls may have been committed, so a mismatch is not necessarily an error";
        }
        return LoadTestVerificationDto.builder().consistent(consistent).checks(checks).note(note).build();
    }

    // Every inserted id is there with its name and credit_total 0, and the table has no other rows
    private boolean verifyInsert(List<Call> successes, Map<String, Object> checks) throws CustomException {
        boolean[] expected = new boolean[recordCount + 1];
        long expectedRows = 0;
        for (Call call : successes) {
            for (int id = call.from(); id <= call.to(); id++) {
                expected[id] = true;
                expectedRows++;
            }
        }
        long[] result = null;
        for (int read = 1; read <= VERIFY_READS; read++) {
            result = checkInserted(expected);
            checks.put("reads", read);
            if (read == 1) {
                checks.put("first_read_missing", result[1]);
            }
            if (result[1] == 0 && result[2] == 0 && result[3] == 0) {
                break;
            }
            sleep(VERIFY_INTERVAL_MS);
        }
        checks.put("expected_rows", expectedRows);
        checks.put("actual_rows", result[0]);
        checks.put("missing", result[1]);
        checks.put("unexpected_rows", result[2]);
        checks.put("wrong_values", result[3]);
        return result[0] == expectedRows && result[1] == 0 && result[2] == 0 && result[3] == 0;
    }

    // [rows, missing, unexpected, wrong values]
    private long[] checkInserted(boolean[] expected) throws CustomException {
        boolean[] seen = new boolean[recordCount + 1];
        long[] result = new long[4];
        scanAll((id, name, creditTotal) -> {
            result[0]++;
            if (id < 1 || id > recordCount || !expected[id]) {
                result[2]++;
                return;
            }
            seen[id] = true;
            if (!OrderRules.testName(id).equals(name) || creditTotal != 0) {
                result[3]++;
            }
        });
        for (int id = 1; id <= recordCount; id++) {
            if (expected[id] && !seen[id]) {
                result[1]++;
            }
        }
        return result;
    }

    // The sum of credit_total grew by the number of rows updated
    private boolean verifyRmw(List<Call> successes, Map<String, Object> checks) throws CustomException {
        long expected = 0;
        for (Call call : successes) {
            expected += call.updated();
        }
        long actual = 0;
        for (int read = 1; read <= VERIFY_READS; read++) {
            actual = readCreditTotalSum() - creditTotalBefore;
            checks.put("reads", read);
            if (read == 1) {
                checks.put("first_read_credit_total_increase", actual);
            }
            if (actual == expected) {
                break;
            }
            sleep(VERIFY_INTERVAL_MS);
        }
        checks.put("rows_updated", expected);
        checks.put("credit_total_increase", actual);
        return actual == expected;
    }

    private long readCreditTotalSum() throws CustomException {
        long[] sum = new long[1];
        scanAll((id, name, creditTotal) -> sum[0] += creditTotal);
        return sum[0];
    }

    @FunctionalInterface
    private interface RowVisitor {
        void visit(int id, String name, long creditTotal);
    }

    // Reads the whole table in one transaction
    private void scanAll(RowVisitor visitor) throws CustomException {
        List<CustomerCustomers> rows;
        try {
            rows = transactionRunner.runOnce(customersRepository::getAllRows);
        } catch (RuntimeException e) {
            throw new CustomException("Load test check failed: " + e.getMessage(), e, 9500);
        }
        for (CustomerCustomers r : rows) {
            visitor.visit(r.getCustomerId(), r.getName(), r.getCreditTotal());
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException("Load test check was interrupted", LoadTestRunner.INTERRUPTED);
        }
    }
}
