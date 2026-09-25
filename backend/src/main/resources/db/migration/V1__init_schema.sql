-- V1__init_schema.sql
-- Core database schema for RedBus clone platform

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(30) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS buses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_name VARCHAR(150) NOT NULL,
    bus_type VARCHAR(100) NOT NULL, -- e.g. 'AC Sleeper 2+1', 'Volvo Multi-Axle AC Seater 2+2', 'Non-AC Seater'
    total_seats INT NOT NULL,
    amenities TEXT, -- Comma-separated or JSON: 'WiFi,Charging Point,Water Bottle,Blanket,Reading Light'
    rating DECIMAL(2, 1) DEFAULT 4.5,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS routes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bus_id BIGINT NOT NULL,
    source_city VARCHAR(100) NOT NULL,
    destination_city VARCHAR(100) NOT NULL,
    departure_time TIME NOT NULL,
    arrival_time TIME NOT NULL,
    travel_date DATE NOT NULL,
    base_price DECIMAL(10, 2) NOT NULL,
    duration_hours DECIMAL(4, 1) DEFAULT 6.5,
    boarding_points TEXT, -- e.g. 'Madiwala (21:00), Electronic City (21:30), Silk Board (21:45)'
    dropping_points TEXT, -- e.g. 'Koyambedu (05:30), Guindy (06:00), Tambaram (06:30)'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_routes_bus FOREIGN KEY (bus_id) REFERENCES buses (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_routes_search ON routes (source_city, destination_city, travel_date);
CREATE INDEX idx_routes_cities ON routes (source_city, destination_city);

CREATE TABLE IF NOT EXISTS seats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bus_id BIGINT NOT NULL,
    seat_number VARCHAR(10) NOT NULL,
    seat_type VARCHAR(20) NOT NULL, -- 'SEATER', 'SLEEPER'
    deck VARCHAR(10) NOT NULL DEFAULT 'LOWER', -- 'LOWER', 'UPPER'
    row_num INT NOT NULL DEFAULT 1,
    col_num INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_seats_bus FOREIGN KEY (bus_id) REFERENCES buses (id) ON DELETE CASCADE,
    CONSTRAINT uk_bus_seat UNIQUE (bus_id, seat_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS route_seats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', -- 'AVAILABLE', 'LOCKED', 'BOOKED'
    gender_restriction VARCHAR(10) DEFAULT 'NONE', -- 'NONE', 'FEMALE', 'MALE'
    lock_expiry TIMESTAMP NULL,
    locked_by_user_id BIGINT NULL,
    price_override DECIMAL(10, 2) NULL,
    CONSTRAINT fk_route_seats_route FOREIGN KEY (route_id) REFERENCES routes (id) ON DELETE CASCADE,
    CONSTRAINT fk_route_seats_seat FOREIGN KEY (seat_id) REFERENCES seats (id) ON DELETE CASCADE,
    CONSTRAINT uk_route_seat UNIQUE (route_id, seat_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_route_seats_status ON route_seats (route_id, status);

CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    route_id BIGINT NOT NULL,
    pnr VARCHAR(50) NOT NULL UNIQUE,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT', -- 'PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'REFUNDED'
    boarding_point VARCHAR(255),
    dropping_point VARCHAR(255),
    contact_email VARCHAR(150) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    cancellation_reason VARCHAR(255),
    refund_amount DECIMAL(10, 2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_route FOREIGN KEY (route_id) REFERENCES routes (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_bookings_user ON bookings (user_id);
CREATE INDEX idx_bookings_pnr ON bookings (pnr);

CREATE TABLE IF NOT EXISTS booking_passengers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    gender VARCHAR(10) NOT NULL,
    seat_number VARCHAR(10) NOT NULL,
    CONSTRAINT fk_passengers_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT fk_passengers_seat FOREIGN KEY (seat_id) REFERENCES seats (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'INITIATED', -- 'INITIATED', 'SUCCESS', 'FAILED', 'REFUNDED'
    method VARCHAR(50) DEFAULT 'RAZORPAY',
    razorpay_order_id VARCHAR(100),
    razorpay_payment_id VARCHAR(100),
    razorpay_signature VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_search_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    source_city VARCHAR(100) NOT NULL,
    destination_city VARCHAR(100) NOT NULL,
    search_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_sessions (
    id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL, -- 'user', 'assistant', 'system'
    content TEXT NOT NULL,
    tool_calls TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_session FOREIGN KEY (session_id) REFERENCES chat_sessions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS kb_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    source_type VARCHAR(50) NOT NULL DEFAULT 'POLICY', -- 'POLICY', 'FAQ', 'TERMS', 'ROUTE_GUIDE'
    content TEXT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS kb_chunks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding TEXT NULL, -- Stored as JSON array or text for embedding matching
    chunk_index INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_kb_chunks_doc FOREIGN KEY (document_id) REFERENCES kb_documents (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
