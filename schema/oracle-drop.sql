-- Drops the tables with their data (Oracle). The schema users stay. Tested on Oracle Database 23ai Free.
--   sqlplus <dba-user>/<password>@//<host>:1521/<service> @schema/oracle-drop.sql
SET SERVEROUTPUT ON
DECLARE
  PROCEDURE drop_table(p_name VARCHAR2) IS
  BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE ' || p_name || ' PURGE';
  EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE = -942 THEN  -- ORA-00942: table or view does not exist
        DBMS_OUTPUT.PUT_LINE('skipped (not found): ' || p_name);
      ELSE
        RAISE;
      END IF;
  END;
BEGIN
  drop_table('customer.customers');
  drop_table('ordering.orders');
  drop_table('ordering.statements');
  drop_table('inventory.items');
END;
/
