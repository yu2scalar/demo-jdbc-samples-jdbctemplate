-- Run once as a role that can create databases, connected to another database
-- (for example "postgres"):
--   psql -h <host> -U <user> -d postgres -f schema/postgresql-admin.sql
-- PostgreSQL has no CREATE DATABASE IF NOT EXISTS; this fails if db_native exists.
CREATE DATABASE db_native;
