-- V5__multi_operator_marketplace.sql
-- Multi-operator marketplace schema, operator management, fleet, schedules, and analytics tracking

-- 1. Clean all seeded bus, route, and seat inventory per zero-seed requirement
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE booking_passengers;
TRUNCATE TABLE payments;
TRUNCATE TABLE bookings;
TRUNCATE TABLE route_seats;
TRUNCATE TABLE routes;
TRUNCATE TABLE seats;
TRUNCATE TABLE buses;
SET FOREIGN_KEY_CHECKS = 1;

-- 2. Create operators table
CREATE TABLE IF NOT EXISTS operators (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    company_name VARCHAR(150) NOT NULL,
    contact_person VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    kyc_doc_url VARCHAR(255) NULL,
    bank_account_ref VARCHAR(100) NULL,
    commission_rate DECIMAL(5, 2) DEFAULT 10.00,
    status VARCHAR(30) NOT NULL DEFAULT 'APPROVED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_operators_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Enhance buses table
ALTER TABLE buses
    ADD COLUMN operator_id BIGINT NULL,
    ADD COLUMN registration_number VARCHAR(50) NULL,
    ADD COLUMN photo_urls TEXT NULL,
    ADD COLUMN active BOOLEAN DEFAULT TRUE;

CREATE INDEX idx_buses_operator ON buses (operator_id);

-- 4. Enhance routes table
ALTER TABLE routes
    ADD COLUMN operator_id BIGINT NULL,
    ADD COLUMN distance_km DECIMAL(6, 1) NULL,
    ADD COLUMN duration_minutes INT NULL;

CREATE INDEX idx_routes_operator ON routes (operator_id);

-- 5. Create recurring schedules table
CREATE TABLE IF NOT EXISTS schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL,
    bus_id BIGINT NOT NULL,
    departure_time TIME NOT NULL,
    arrival_time TIME NOT NULL,
    operating_days VARCHAR(100) NOT NULL DEFAULT 'DAILY',
    base_price DECIMAL(10, 2) NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_schedules_operator FOREIGN KEY (operator_id) REFERENCES operators (id) ON DELETE CASCADE,
    CONSTRAINT fk_schedules_route FOREIGN KEY (route_id) REFERENCES routes (id) ON DELETE CASCADE,
    CONSTRAINT fk_schedules_bus FOREIGN KEY (bus_id) REFERENCES buses (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_schedules_operator ON schedules (operator_id);

-- 6. Enhance bookings table with operator tracking and commission
ALTER TABLE bookings
    ADD COLUMN operator_id BIGINT NULL,
    ADD COLUMN commission_amount DECIMAL(10, 2) DEFAULT 0.00;

CREATE INDEX idx_bookings_operator ON bookings (operator_id);

-- 7. Seed Operator profile for existing operator user
INSERT INTO operators (id, user_id, company_name, contact_person, email, phone, commission_rate, status)
VALUES (1, 2, 'Zingbus Mobility', 'Fleet Operations Manager', 'operator@zingbus.com', '+91 9876543211', 10.00, 'APPROVED');
