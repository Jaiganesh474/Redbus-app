-- V13__add_refund_workflow_and_operator_wallet_ledger.sql
-- Add stagewise refund tracking, destination, and operator wallet ledger

ALTER TABLE bookings
    ADD COLUMN refund_status VARCHAR(30) NOT NULL DEFAULT 'NONE',
    ADD COLUMN refund_destination VARCHAR(30) NOT NULL DEFAULT 'WALLET',
    ADD COLUMN refund_stage VARCHAR(30) NOT NULL DEFAULT 'NONE',
    ADD COLUMN refund_requested_at DATETIME NULL,
    ADD COLUMN refund_approved_at DATETIME NULL;

CREATE TABLE IF NOT EXISTS operator_wallet_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id BIGINT NOT NULL,
    booking_id BIGINT NULL,
    pnr VARCHAR(50) NULL,
    type VARCHAR(30) NOT NULL, -- 'CREDIT_TICKET_FARE', 'DEBIT_REFUND_AUDIT', 'PAYOUT_WITHDRAWAL'
    amount DECIMAL(10, 2) NOT NULL,
    balance_after DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    description VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_op_wallet_op (operator_id),
    INDEX idx_op_wallet_pnr (pnr)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
