# demo-jdbc-samples-jdbctemplate

The native JDBC (Spring `JdbcTemplate`) version of
[demo-scalardb-samples](https://github.com/yu2scalar/demo-scalardb-samples), for comparing
the two. It has the same REST API, JSON and error codes, so the same `curl` commands work
on both:

- **CRUD** for four tables (`customer.customers`, `inventory.items`, `ordering.orders`,
  `ordering.statements`)
- **Business logic** from scalardb-sample: PlaceOrder, GetOrder, GetOrders, plus CancelOrder
  (`/orders`)
- **Load tests** (simple benchmarks) that run inside the application:
  `place-order` and `customer-crud` (W1-W4). See [docs/load-test-design.md](docs/load-test-design.md).

It runs on **PostgreSQL** (profile `postgresql`, default) or **Oracle** (profile `oracle`,
tested on Oracle Database 23ai Free). The ScalarDB version's SQL API endpoints (`*-sql`, `/generic-sql`)
are not here: this version is SQL already.

## How it differs from the ScalarDB version

- Each request is one local transaction (`DataSourceTransactionManager`) in
  **READ COMMITTED**, retried on deadlocks and serialization failures with the same
  `retry.*` settings.
- Rows that are read, checked and written back are read with **`SELECT ... FOR UPDATE`**
  (PlaceOrder and CancelOrder: the items in item_id order, then the customer; CancelOrder
  also the order; the read-modify-write of `customer-crud`: the row it updates). Without
  the lock, two transactions in READ COMMITTED could both read the same stock and one
  update would be lost. ScalarDB detects that conflict at commit and fails one
  transaction (it is retried); here the second transaction waits for the first.
- `GET /customers/{id}/orders?method=index|scan` accepts both values; both run the same
  query (`WHERE customer_id = ?`).
- A failed commit returns 9200 (the outcome is unknown), as UnknownTransactionStatusException
  does in the ScalarDB version. `transactionId` is never set.
- An insert of an existing key returns 9300 right away (not retried).

## Prerequisites

- JDK 25 (the Gradle toolchain uses Java 25)
- PostgreSQL 15 or later, or Oracle 19c or later
- `psql` (or `sqlplus`) to create the tables

## 1. Clone and build

```bash
git clone <repository-url>
cd demo-jdbc-samples-jdbctemplate
./gradlew build -x test
ls build/libs/demo-jdbc-samples-jdbctemplate-0.0.1-SNAPSHOT.jar
```

`./gradlew build` with the test needs the database: the test starts the application.

## 2. Create the schemas and tables

The schema names are the ScalarDB namespaces (`customer`, `ordering`, `inventory`), and
the columns, primary keys and indexes are those of the ScalarDB version, with the types
ScalarDB creates. Use a database of its own, not the one ScalarDB uses, because the
names are the same.

| File | What it does |
|---|---|
| `schema/postgresql-admin.sql` | Creates the database `db_native` (once) |
| `schema/postgresql.sql` | Creates the schemas, tables and indexes. Existing ones are skipped |
| `schema/postgresql-drop.sql` | Drops the schemas with their tables and data |
| `schema/oracle-admin.sql` | Creates the schema users `customer`, `ordering`, `inventory` (once, as a DBA) |
| `schema/oracle.sql` | Creates the tables and indexes. Existing ones are skipped |
| `schema/oracle-drop.sql` | Drops the tables with their data (the schema users stay) |

PostgreSQL:

```bash
psql -h <host> -U <user> -d postgres  -f schema/postgresql-admin.sql
psql -h <host> -U <user> -d db_native -v ON_ERROR_STOP=1 -f schema/postgresql.sql
# to start again from empty tables
psql -h <host> -U <user> -d db_native -v ON_ERROR_STOP=1 -f schema/postgresql-drop.sql
psql -h <host> -U <user> -d db_native -v ON_ERROR_STOP=1 -f schema/postgresql.sql
```

`postgresql-admin.sql` fails if `db_native` exists (PostgreSQL has no
`CREATE DATABASE IF NOT EXISTS`).

Oracle (tested on Oracle Database 23ai Free). For now, run the scripts and the application as a DBA-level
user, because the tables are in three schemas. `oracle-admin.sql` assumes the tablespace
`USERS`.

```bash
sqlplus <dba-user>/<password>@//<host>:1521/<service> @schema/oracle-admin.sql
sqlplus <dba-user>/<password>@//<host>:1521/<service> @schema/oracle.sql
sqlplus <dba-user>/<password>@//<host>:1521/<service> @schema/oracle-drop.sql
```

To empty the tables without dropping them, use `POST /init?truncate=true`.

## 3. Configuration

The connection settings contain passwords, so the real files are not in git
(`.gitignore`). Create them from the examples, then edit them (replace every `<host>`,
`<user>`, `<password>` and `<service>`):

```bash
cp src/main/resources/application-postgresql.properties.example src/main/resources/application-postgresql.properties
cp src/main/resources/application-oracle.properties.example src/main/resources/application-oracle.properties
```

They are packed into the jar, so rebuild the jar after editing them. (Or pass the values
on the command line instead, e.g. `--spring.datasource.password=...`.)

`src/main/resources/application.properties` holds the settings common to both databases:

- `spring.profiles.default=postgresql`
- `spring.datasource.hikari.transaction-isolation=TRANSACTION_READ_COMMITTED`
- `spring.datasource.hikari.maximum-pool-size=20` (can be overridden per database; keep it
  larger than the number of load-test threads, and within the database's connection limit)
- the retry settings (`retry.*`, the same values as the ScalarDB version)

At startup the log shows the database and the SQL dialect in use, e.g.
`Database: PostgreSQL 16.15 -> dialect POSTGRESQL`.

## 4. Run

```bash
# PostgreSQL (default)
java -Xmx1g -jar build/libs/demo-jdbc-samples-jdbctemplate-0.0.1-SNAPSHOT.jar

# Oracle
java -Xmx1g -jar build/libs/demo-jdbc-samples-jdbctemplate-0.0.1-SNAPSHOT.jar --spring.profiles.active=oracle
```

- Port: `--server.port=18091` (default 8080, the same as the ScalarDB version: change it
  to run both at once)
- In the background:

```bash
setsid nohup java -Xmx1g -jar build/libs/demo-jdbc-samples-jdbctemplate-0.0.1-SNAPSHOT.jar \
  > app.log 2>&1 < /dev/null &
grep -m1 'Started' app.log     # the line shows the process ID
kill <process ID>
```

## 5. Check that it works

- Swagger UI: http://localhost:8080/swagger-ui.html

```bash
curl -s -X POST 'http://localhost:8080/init?customers=10&items=10'
curl -s -X POST http://localhost:8080/orders -H 'Content-Type: application/json' \
  -d '{"customer_id":1,"items":[{"item_id":1,"qty":2}]}'
curl -s http://localhost:8080/orders/<order_id>
curl -s 'http://localhost:8080/customer-customers/search?name=TEST-000001'
curl -s http://localhost:8080/load-test/scenarios
```

## 6. Load tests

The same scenarios, parameters and response as the ScalarDB version. Details:
[docs/load-test-design.md](docs/load-test-design.md).

**Run one load test at a time, with no other load on the same database server.** `init`
truncates tables, and the database check assumes that nothing else writes to them.

```bash
B=http://localhost:8080/load-test/place-order
curl -s -X POST $B/init -H 'Content-Type: application/json' \
  -d '{"params":{"customers":1000,"items":1000,"truncate":true}}'
curl -s -X POST $B/run -H 'Content-Type: application/json' \
  -d '{"number_of_threads":8,"ramp_up_period_seconds":5,"duration_seconds":30,
       "params":{"items_per_order_max":3,"qty_max":5}}'

C=http://localhost:8080/load-test/customer-crud
curl -s -X POST $C/init -H 'Content-Type: application/json' -d '{"params":{"record_count":100000}}'
# W1: Insert-only (stops when all rows are in)
curl -s -X POST $C/run -H 'Content-Type: application/json' \
  -d '{"number_of_threads":8,"ramp_up_period_seconds":5,"duration_seconds":3600,"params":{"workload":"insert"}}'
# W2 (rmw 50) / W3 (rmw 95) / W4 (read)
curl -s -X POST $C/run -H 'Content-Type: application/json' \
  -d '{"number_of_threads":8,"ramp_up_period_seconds":10,"duration_seconds":60,
       "params":{"workload":"rmw","read_ratio":50,"match":"exact"}}'
```

## Notes

- `POST /init` upserts (PostgreSQL: `INSERT ... ON CONFLICT DO UPDATE`, Oracle: `MERGE`).
  Orders are kept unless `truncate=true`.
- Each failed transaction attempt is logged at ERROR with a stack trace, as in the
  ScalarDB version.
- The index on `name` is a plain B-tree, as in the ScalarDB version. With a non-C
  collation (e.g. `en_US.utf8`), PostgreSQL does not use it for `LIKE 'x%'`.
