-- Tables and indexes for the native JDBC sample (Oracle 19c or later). Tested on Oracle Database 23ai Free.
--   sqlplus <dba-user>/<password>@//<host>:1521/<service> @schema/oracle.sql
-- Run schema/oracle-admin.sql first. Objects that already exist are skipped.
--
-- The columns, primary keys and indexes are those of the ScalarDB version. The types are
-- what ScalarDB creates on Oracle: TEXT in a key or an index is VARCHAR2(128), other TEXT
-- is VARCHAR2(4000), INT is NUMBER(10) and BIGINT is NUMBER(19).
SET SERVEROUTPUT ON
DECLARE
  PROCEDURE run(p_sql VARCHAR2) IS
  BEGIN
    EXECUTE IMMEDIATE p_sql;
  EXCEPTION
    WHEN OTHERS THEN
      -- ORA-00955: name is already used by an existing object
      -- ORA-01408: such column list already indexed
      IF SQLCODE IN (-955, -1408) THEN
        DBMS_OUTPUT.PUT_LINE('skipped (exists): ' || SUBSTR(p_sql, 1, 60));
      ELSE
        RAISE;
      END IF;
  END;
BEGIN
  run('CREATE TABLE customer.customers (
         customer_id  NUMBER(10) NOT NULL,
         name         VARCHAR2(128),
         credit_limit NUMBER(19),
         credit_total NUMBER(19),
         PRIMARY KEY (customer_id))');
  run('CREATE INDEX customer.index_customer_customers_name ON customer.customers (name)');

  run('CREATE TABLE ordering.orders (
         order_id     VARCHAR2(128) NOT NULL,
         customer_id  NUMBER(10),
         created_at   NUMBER(19),
         status       VARCHAR2(4000),
         total_amount NUMBER(19),
         PRIMARY KEY (order_id))');
  run('CREATE INDEX ordering.index_ordering_orders_customer_id ON ordering.orders (customer_id)');
  run('CREATE INDEX ordering.index_ordering_orders_created_at ON ordering.orders (created_at)');

  run('CREATE TABLE ordering.statements (
         order_id   VARCHAR2(128) NOT NULL,
         line_no    NUMBER(10) NOT NULL,
         item_id    NUMBER(10),
         qty        NUMBER(10),
         unit_price NUMBER(19),
         PRIMARY KEY (order_id, line_no))');
  run('CREATE INDEX ordering.index_ordering_statements_item_id ON ordering.statements (item_id)');

  run('CREATE TABLE inventory.items (
         item_id NUMBER(10) NOT NULL,
         name    VARCHAR2(128),
         price   NUMBER(19),
         stock   NUMBER(10),
         PRIMARY KEY (item_id))');
  run('CREATE INDEX inventory.index_inventory_items_name ON inventory.items (name)');
END;
/
