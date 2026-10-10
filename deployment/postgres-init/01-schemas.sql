-- PostgreSQL creates the inventory database before running this file.
-- Flyway also creates these schemas, but creating them here makes the
-- database layout explicit and allows the services to start consistently.
CREATE SCHEMA IF NOT EXISTS user_service;
CREATE SCHEMA IF NOT EXISTS inventory_service;
CREATE SCHEMA IF NOT EXISTS notification_service;
