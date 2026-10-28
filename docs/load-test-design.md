# Load test (simple benchmark)

A load test runs one API on several threads inside this application and returns
throughput, latency and a database check. The common part (`LoadTestRunner`) and
the per-API part (`LoadTestScenario`) are separate, so a new API needs only one
new scenario class.

Services are called directly, so HTTP is not part of the measured values.
The runner, the scenarios, their parameters and the response are the same as in the
ScalarDB version (demo-scalardb-samples); only the database access is JDBC.

## Endpoints

| Method | Path | Body |
|---|---|---|
| GET | `/load-test/scenarios` | – |
| POST | `/load-test/{scenario}/init` | `{"params": {...}}` |
| POST | `/load-test/{scenario}/run` | `{"number_of_threads": 8, "ramp_up_period_seconds": 5, "warmup_seconds": 0, "duration_seconds": 30, "params": {...}}` |

Error codes:

| Code | HTTP | Meaning |
|---|---|---|
| 9400 | 400 | Bad parameter or unknown scenario |
| 9900 | 409 | `run` before `init` (also after a restart: `init` is kept in memory) |
| 9901 | 409 | Another run is in progress (one run at a time) |
| 9902 | 500 | Interrupted |

## Measurement (LoadTestRunner)

```
 t0                  measureStart                     testEnd
 |<--- ramp-up ------>|<------- duration ------------>|
 | threads start      |      all threads running      |
 | not counted        |          counted              |
```

- Threads start one by one over `ramp_up_period_seconds`. Then all threads run for
  `warmup_seconds` (optional, default 0) without being counted. A request is counted only
  if it **started** after the ramp-up and warmup. Total time = ramp-up + warmup + duration.
- A count-based scenario (`customer-crud` insert) stops when its work is done
  (`isDone()` / `NoMoreWorkException`); `duration_seconds` is then only an upper limit.
- The throughput is divided by the actual measured window
  (`measured_duration_seconds`, up to the end of the last counted request).
- `retry_count` = transaction attempts − calls in the measured window. It is read from
  the scenario (`OrderService.getAttemptCount()` for PlaceOrder). With JDBC, only deadlocks,
  serialization failures and lock timeouts are retried (`JdbcRetryConfig`).
- `failures_by_code`: failures in the measured window by error code
  (-1 = not a `CustomException`).
- Check that the ramp-up is excluded: `total_elapsed_seconds` ≈ ramp-up + warmup + duration,
  and `measured_duration_seconds` ≈ duration.
- `timeline`: one entry per second from the start, by request start time: `phase`
  (ramp-up / warmup / measure / drain), `requests`, `successes`, `failures`, `p50`, `p99`
  (successful requests) and `active_threads`. Use it to see when the values settle, and
  choose the ramp-up and warmup from that.
- Successes and failures of the **whole run** (ramp-up included) are kept separately
  for the database check.

## Scenario: `place-order`

`init` params:

| Name | Default | |
|---|---|---|
| `customers` | (required) | customer_id 1..N |
| `items` | (required) | item_id 1..M |
| `truncate` | false | Empty customers, items, orders and statements first (`InitService.truncateAll`, `TRUNCATE TABLE`) |
| `credit_limit` | 10^15 | Large, so that 9800 does not distort the run |
| `stock` | 10^9 | Large, so that 9801 does not distort the run |

`run` params:

| Name | Default | |
|---|---|---|
| `items_per_order_max` | 1 | K: each order has 1..K distinct random items (K ≤ min(M, 100)) |
| `qty_max` | 1 | Each line has qty 1..qty_max |

Each call: a random customer, 1..K distinct random items, then `OrderService.placeOrder`.
With M = 1 (or a small M), all orders write the same item rows. This shows contention.
The item and customer rows are locked with `SELECT ... FOR UPDATE`, so concurrent orders
of the same item wait for each other (no failures, longer latency). In the ScalarDB
version they conflict at commit and are retried or fail.

### Database check (`verification`)

- Before the run, `credit_total` of customers 1..N and `stock` of items 1..M are read by
  primary key, 100 rows per transaction.
- After the run, the same rows are read again. Then:
  - each customer's increase of credit_total = the total amount of its successful orders
  - each item's decrease of stock = the qty of its successful order lines
  - every successful order_id exists, with the same total_amount and number of statements
- **On a mismatch, it waits 1 second and reads again, up to 5 reads in all**, as in the
  ScalarDB version (there, records left PREPARED by a committed transaction are recovered
  lazily, and a read in READ_COMMITTED can return the before-image until then). With
  JDBC a committed transaction is visible at once, so one read is expected.
- `checks.reads` is the number of reads. `checks.first_read_*` are the mismatches at the
  first read.
- If 9200 (commit outcome unknown) occurred, `note` says so: those orders may have
  been committed.
- The check assumes that nothing else writes to these tables during the run.
- `verification.elapsed_seconds` is the time spent on the check. It is not part of the
  measured values.

## Scenario: `customer-crud` (W1-W4)

On `customer.customers` only. Run in this order on the same data:

| | `params` of run |
|---|---|
| W1: Insert-only | `{"workload": "insert"}` (+ `rows_per_tx`, default 1) |
| W2: 50% Read / 50% Update (RMW) | `{"workload": "rmw", "read_ratio": 50, "match": "exact"}` |
| W3: 95% Read / 5% Update (RMW) | `{"workload": "rmw", "read_ratio": 95, "match": "exact"}` |
| W4: 100% Read | `{"workload": "read", "match": "exact"}` |

- `init` params: `record_count` (default 100,000, at most 999,999) and `truncate`
  (default true: truncates `customer.customers` only). With `truncate: false` the rows are
  assumed to be there already (e.g. after a restart): only the count is remembered.
- W1 inserts customer_id 1..record_count in order (one shared counter for all threads),
  name `TEST-%06d`, credit_limit 1,000,000, credit_total 0, and stops when all are in.
  A failed insert leaves its id missing. Use a short ramp-up: rows inserted during it are
  kept but not counted.
- Read: a random customer_id in 1..record_count gives the name. It is searched with a
  `SELECT` on name (`CustomerCrudService.search`; the ScalarDB version uses ScanAll + where):
  - `match: exact`: `name = 'TEST-001234'` (1 row)
  - `match: prefix`: the last `prefix_digits` (1..5, default 1) digits become `%`, e.g.
    `name LIKE 'TEST-00123%'` (up to 10^prefix_digits rows; `FETCH FIRST` that many)
- RMW: the same search (not locked), then the chosen row is read again by primary key
  with `SELECT ... FOR UPDATE`, and credit_total + 1 is written, in the same transaction
  (`CustomerCrudService.readModifyWrite`). The second read under the lock keeps a
  concurrent update from being lost. (Oracle does not allow `FOR UPDATE` with
  `FETCH FIRST`, and only the updated row needs the lock.)
- The index on name is one plain B-tree, as in the ScalarDB version (which also has one
  on `before_name` and adds `OR before_name = ?` to the condition).
- `verification`:
  - insert: every inserted id is there with its name and credit_total 0, and no other rows
    (the whole table in one SELECT, read again on a mismatch)
  - rmw: the sum of credit_total grew by the number of rows updated (the whole table
    before and after, read again on a mismatch)
  - read / rmw: `searches`, `rows_found_by_read_only_searches`, `zero_hits` (missing ids)
    and `unexpected_hits` (exact: not 1 row; prefix: more than 10^prefix_digits)
- The same search is available as REST:
  `GET /customer-customers/search?name=TEST-000123&match=exact|prefix&limit=1000`
  (for prefix, `name` is the prefix without `%`).

## Example

```bash
B=http://localhost:8080/load-test/place-order
curl -s -X POST $B/init -H 'Content-Type: application/json' \
  -d '{"params":{"customers":1000,"items":1000,"truncate":true}}'
curl -s -X POST $B/run -H 'Content-Type: application/json' \
  -d '{"number_of_threads":8,"ramp_up_period_seconds":5,"duration_seconds":30,
       "params":{"items_per_order_max":3,"qty_max":5}}'

C=http://localhost:8080/load-test/customer-crud
curl -s -X POST $C/init -H 'Content-Type: application/json' -d '{"params":{"record_count":100000}}'
# W1 (stops when all rows are in; duration_seconds is the upper limit)
curl -s -X POST $C/run -H 'Content-Type: application/json' \
  -d '{"number_of_threads":8,"ramp_up_period_seconds":5,"duration_seconds":3600,"params":{"workload":"insert"}}'
# W2 / W3 / W4
curl -s -X POST $C/run -H 'Content-Type: application/json' \
  -d '{"number_of_threads":8,"ramp_up_period_seconds":10,"duration_seconds":60,
       "params":{"workload":"rmw","read_ratio":50,"match":"exact"}}'
```

Notes:
- Each failed attempt is logged at ERROR with a stack trace (as in the other services).
  With much contention, the logging can affect the measured values.
- Run with no other load: `truncate` and the database check assume that.

## Adding a scenario

1. Implement `LoadTestScenario<R>` as a Spring bean (`@Component`). `R` is what
   `execute` keeps for each successful call.
2. `name()` is the path name. The runner finds all scenario beans by themselves.
3. `beforeRun` checks the params and that `init` was called (9900). It takes the snapshot
   for `verify`.
4. `attemptCount()` returns -1 if the target service does not count attempts
   (`retry_count` is then omitted).
