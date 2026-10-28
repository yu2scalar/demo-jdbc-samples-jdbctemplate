-- Drops the schemas with all their tables and data (PostgreSQL).
--   psql -h <host> -U <user> -d db_native -v ON_ERROR_STOP=1 -f schema/postgresql-drop.sql
DROP SCHEMA IF EXISTS customer CASCADE;
DROP SCHEMA IF EXISTS ordering CASCADE;
DROP SCHEMA IF EXISTS inventory CASCADE;
