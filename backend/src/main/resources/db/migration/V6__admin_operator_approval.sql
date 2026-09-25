-- V6__admin_operator_approval.sql
-- Seed default verified admin redbus@admin.in and support operator approval workflow

-- 1. Insert or update default system administrator (Password: 123456)
INSERT INTO users (name, email, password_hash, phone, role, email_verified, avatar_url, gender)
VALUES (
    'RedBus Administrator',
    'redbus@admin.in',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
    '+91 9999999999',
    'ROLE_ADMIN',
    TRUE,
    'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150',
    'MALE'
)
ON DUPLICATE KEY UPDATE
    name = 'RedBus Administrator',
    password_hash = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
    role = 'ROLE_ADMIN',
    email_verified = TRUE;

-- 2. Ensure operators table defaults new registrations to PENDING
ALTER TABLE operators MODIFY COLUMN status VARCHAR(30) NOT NULL DEFAULT 'PENDING';
