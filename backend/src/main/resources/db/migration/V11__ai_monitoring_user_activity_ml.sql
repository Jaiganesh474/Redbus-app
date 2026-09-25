-- V11: AI Telemetry, User Activity Monitoring, and Dynamic Pricing ML

CREATE TABLE IF NOT EXISTS ai_telemetry_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(100),
    user_id BIGINT,
    request_type VARCHAR(50) NOT NULL,
    query_text TEXT,
    response_summary VARCHAR(500),
    latency_ms BIGINT NOT NULL DEFAULT 0,
    tokens_used INT DEFAULT 0,
    model_used VARCHAR(50),
    confidence_score DOUBLE DEFAULT 0.95,
    sentiment VARCHAR(30) DEFAULT 'NEUTRAL',
    intent VARCHAR(50) DEFAULT 'GENERAL_QUERY',
    is_fallback BOOLEAN DEFAULT FALSE,
    is_anomaly BOOLEAN DEFAULT FALSE,
    safety_flag VARCHAR(50) DEFAULT 'CLEAN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ai_telemetry_session (session_id),
    INDEX idx_ai_telemetry_type (request_type),
    INDEX idx_ai_telemetry_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    session_id VARCHAR(100) NOT NULL,
    ip_address VARCHAR(60),
    user_agent VARCHAR(255),
    action_type VARCHAR(50) NOT NULL,
    route_id BIGINT,
    bus_id BIGINT,
    schedule_id BIGINT,
    metadata_json TEXT,
    risk_score INT DEFAULT 0,
    is_bot BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_activity_user (user_id),
    INDEX idx_activity_session (session_id),
    INDEX idx_activity_action (action_type),
    INDEX idx_activity_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ml_dynamic_pricing_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT,
    source_city VARCHAR(100),
    destination_city VARCHAR(100),
    base_price DECIMAL(10, 2) NOT NULL,
    predicted_price DECIMAL(10, 2) NOT NULL,
    surge_multiplier DOUBLE NOT NULL DEFAULT 1.0,
    demand_factor DOUBLE NOT NULL DEFAULT 1.0,
    occupancy_rate DOUBLE DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pricing_schedule (schedule_id),
    INDEX idx_pricing_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
