-- Schemas, tables and indexes for the native JDBC sample (PostgreSQL).
--   psql -h <host> -U <user> -d db_native -v ON_ERROR_STOP=1 -f schema/postgresql.sql
-- Objects that already exist are skipped, so it can be run again.
--
-- The schema names are the ScalarDB namespaces, and the columns, primary keys and
-- indexes are those of the ScalarDB version (schema/schema.json there). The types are
-- what ScalarDB creates on PostgreSQL: TEXT in a key or an index is VARCHAR(10485760).

CREATE SCHEMA IF NOT EXISTS customer;
CREATE SCHEMA IF NOT EXISTS ordering;
CREATE SCHEMA IF NOT EXISTS inventory;

CREATE TABLE IF NOT EXISTS customer.customers (
  customer_id  INT NOT NULL,
  name         VARCHAR(10485760),
  credit_limit BIGINT,
  credit_total BIGINT,
  PRIMARY KEY (customer_id)
);
CREATE INDEX IF NOT EXISTS index_customer_customers_name ON customer.customers (name);

CREATE TABLE IF NOT EXISTS ordering.orders (
  order_id     VARCHAR(10485760) NOT NULL,
  customer_id  INT,
  created_at   BIGINT,
  status       TEXT,
  total_amount BIGINT,
  PRIMARY KEY (order_id)
);
CREATE INDEX IF NOT EXISTS index_ordering_orders_customer_id ON ordering.orders (customer_id);
CREATE INDEX IF NOT EXISTS index_ordering_orders_created_at ON ordering.orders (created_at);

CREATE TABLE IF NOT EXISTS ordering.statements (
  order_id   VARCHAR(10485760) NOT NULL,
  line_no    INT NOT NULL,
  item_id    INT,
  qty        INT,
  unit_price BIGINT,
  PRIMARY KEY (order_id, line_no)
);
CREATE INDEX IF NOT EXISTS index_ordering_statements_item_id ON ordering.statements (item_id);

CREATE TABLE IF NOT EXISTS inventory.items (
  item_id INT NOT NULL,
  name    VARCHAR(10485760),
  price   BIGINT,
  stock   INT,
  PRIMARY KEY (item_id)
);
CREATE INDEX IF NOT EXISTS index_inventory_items_name ON inventory.items (name);
