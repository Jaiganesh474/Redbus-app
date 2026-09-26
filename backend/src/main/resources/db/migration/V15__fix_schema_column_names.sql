-- V15: Fix and reconcile user_device_sessions and banners schema for Hibernate validation

-- Recreate user_device_sessions table with exact snake_case columns
DROP TABLE IF EXISTS user_device_sessions;

CREATE TABLE user_device_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    device_name VARCHAR(150),
    device_type VARCHAR(50) DEFAULT 'Desktop',
    browser VARCHAR(100),
    os VARCHAR(100),
    ip_address VARCHAR(100),
    location VARCHAR(150) DEFAULT 'India',
    last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_current_session BOOLEAN DEFAULT FALSE,
    session_token VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_device_sessions_user (user_id)
);
