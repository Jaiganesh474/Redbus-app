-- V4: Add profile avatar, gender, password reset, saved travellers, and seat gender tracking

ALTER TABLE users 
    ADD COLUMN avatar_url VARCHAR(255) NULL,
    ADD COLUMN gender VARCHAR(20) NULL,
    ADD COLUMN password_reset_token VARCHAR(20) NULL,
    ADD COLUMN password_reset_expiry DATETIME NULL;

CREATE TABLE IF NOT EXISTS saved_travellers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    gender VARCHAR(20) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

ALTER TABLE seats
    ADD COLUMN booked_gender VARCHAR(20) NULL;

ALTER TABLE route_seats
    ADD COLUMN booked_gender VARCHAR(20) NULL;
