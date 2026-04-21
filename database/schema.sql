-- ============================================================
-- FinTech CNIC Transaction System - Database Schema (MySQL)
-- ============================================================

CREATE DATABASE IF NOT EXISTS fintech_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fintech_db;

-- ============================================================
-- Table: users
-- Stores registered users, uniquely identified by CNIC
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    cnic          VARCHAR(15)  NOT NULL UNIQUE COMMENT 'Format: XXXXX-XXXXXXX-X',
    full_name     VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE | INACTIVE | SUSPENDED',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_status (status),
    CONSTRAINT chk_cnic_format CHECK (cnic REGEXP '^[0-9]{5}-[0-9]{7}-[0-9]$')
);

-- ============================================================
-- Table: accounts
-- One bank account per user (1:1 with users)
-- ============================================================
CREATE TABLE IF NOT EXISTS accounts (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT         NOT NULL UNIQUE,
    account_number VARCHAR(20)    NOT NULL UNIQUE COMMENT 'e.g. PKF-00100001',
    balance        DECIMAL(15,2)  NOT NULL DEFAULT 0.00,
    currency       VARCHAR(3)     NOT NULL DEFAULT 'PKR',
    updated_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_accounts_user (user_id),
    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0)
);

-- ============================================================
-- Table: transactions
-- Records all money transfers between accounts
-- ============================================================
CREATE TABLE IF NOT EXISTS transactions (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_account_id    BIGINT        NOT NULL,
    recipient_account_id BIGINT        NOT NULL,
    amount               DECIMAL(15,2) NOT NULL,
    status               VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | SUCCESS | FAILED',
    reference_number     VARCHAR(40)   NOT NULL UNIQUE COMMENT 'e.g. TXN-20240101-ABCD1234',
    description          VARCHAR(255),
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_txn_sender    FOREIGN KEY (sender_account_id)    REFERENCES accounts(id),
    CONSTRAINT fk_txn_recipient FOREIGN KEY (recipient_account_id) REFERENCES accounts(id),
    CONSTRAINT chk_txn_amount   CHECK (amount > 0),
    CONSTRAINT chk_txn_different CHECK (sender_account_id != recipient_account_id),
    INDEX idx_txn_sender    (sender_account_id),
    INDEX idx_txn_recipient (recipient_account_id),
    INDEX idx_txn_ref       (reference_number),
    INDEX idx_txn_created   (created_at)
);

-- ============================================================
-- Table: otp_logs
-- Tracks OTP generation and verification attempts
-- ============================================================
CREATE TABLE IF NOT EXISTS otp_logs (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    otp_code_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt hash - never store plain OTP',
    expires_at    DATETIME     NOT NULL,
    attempts      INT          NOT NULL DEFAULT 0,
    verified_at   DATETIME,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | VERIFIED | EXPIRED | FAILED',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_otp_user    (user_id),
    INDEX idx_otp_expires (expires_at),
    INDEX idx_otp_status  (status)
);
