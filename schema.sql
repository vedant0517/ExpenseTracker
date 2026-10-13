-- =============================================================================
-- Database Schema for Personal Expense Tracker
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. MySQL Setup Script
-- -----------------------------------------------------------------------------
-- Run in MySQL Workbench or mysql CLI:
--
-- CREATE DATABASE IF NOT EXISTS expense_tracker_db;
-- USE expense_tracker_db;

CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    date DATE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_type_valid CHECK (type IN ('INCOME', 'EXPENSE'))
);

-- -----------------------------------------------------------------------------
-- 2. PostgreSQL Setup Script (Alternative)
-- -----------------------------------------------------------------------------
-- Run in psql or pgAdmin:
--
-- CREATE DATABASE expense_tracker_db;
-- \c expense_tracker_db;

-- CREATE TABLE IF NOT EXISTS transactions (
--     id SERIAL PRIMARY KEY,
--     title VARCHAR(255) NOT NULL,
--     amount DECIMAL(12, 2) NOT NULL,
--     category VARCHAR(100) NOT NULL,
--     type VARCHAR(20) NOT NULL,
--     date DATE NOT NULL,
--     description TEXT,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
--     CONSTRAINT chk_amount_positive CHECK (amount > 0),
--     CONSTRAINT chk_type_valid CHECK (type IN ('INCOME', 'EXPENSE'))
-- );
