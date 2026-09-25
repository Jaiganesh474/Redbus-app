-- V8__create_coupons_and_booking_free_cancellation.sql
-- Create coupons table and extend bookings table with coupon and free cancellation fields

CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    discount_percentage DECIMAL(5, 2) NOT NULL DEFAULT 2.00,
    max_discount_amount DECIMAL(10, 2) NULL,
    min_booking_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    operator_id BIGINT NULL,
    operator_name VARCHAR(150) NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    usage_limit INT NOT NULL DEFAULT 1000,
    times_used INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_coupons_code (code),
    INDEX idx_coupons_operator (operator_id),
    CONSTRAINT fk_coupons_operator FOREIGN KEY (operator_id) REFERENCES users(id) ON DELETE SET NULL
);

ALTER TABLE bookings
    ADD COLUMN coupon_code VARCHAR(50) NULL,
    ADD COLUMN discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN has_free_cancellation BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN free_cancellation_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00;

-- Seed instant live coupons
INSERT INTO coupons (code, discount_percentage, max_discount_amount, min_booking_amount, operator_id, operator_name, valid_from, valid_to, usage_limit, times_used, is_active)
VALUES 
('SAVE2', 2.00, 100.00, 100.00, NULL, 'redBus Official', '2026-01-01', '2027-12-31', 10000, 0, TRUE),
('FIRSTBUS', 15.00, 200.00, 300.00, NULL, 'redBus Official', '2026-01-01', '2027-12-31', 5000, 0, TRUE),
('RBTRIP', 10.00, 150.00, 500.00, NULL, 'redBus Official', '2026-01-01', '2027-12-31', 5000, 0, TRUE);
