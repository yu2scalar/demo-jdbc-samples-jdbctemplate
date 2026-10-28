-- Run once as a DBA (Oracle 19c or later). Tested on Oracle Database 23ai Free.
--   sqlplus <dba-user>/<password>@//<host>:1521/<service> @schema/oracle-admin.sql
-- In Oracle a schema is a user. The three schemas are schema-only accounts
-- (no password, no login); the tables are created and used by a DBA-level user.
-- Assumes the tablespace USERS exists. Users that already exist are skipped.
SET SERVEROUTPUT ON
DECLARE
  PROCEDURE create_schema(p_name VARCHAR2) IS
  BEGIN
    EXECUTE IMMEDIATE 'CREATE USER ' || p_name || ' NO AUTHENTICATION DEFAULT TABLESPACE USERS QUOTA UNLIMITED ON USERS';
  EXCEPTION
    WHEN OTHERS THEN
      IF SQLCODE = -1920 THEN  -- ORA-01920: user name conflicts with another user or role name
        DBMS_OUTPUT.PUT_LINE('skipped (exists): ' || p_name);
      ELSE
        RAISE;
      END IF;
  END;
BEGIN
  create_schema('customer');
  create_schema('ordering');
  create_schema('inventory');
END;
/
