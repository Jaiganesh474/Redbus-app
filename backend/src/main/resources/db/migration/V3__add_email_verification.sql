-- V3__add_email_verification.sql
-- Add email verification columns to users table

ALTER TABLE users 
ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN verification_token VARCHAR(100) NULL,
ADD COLUMN verification_token_expiry TIMESTAMP NULL;

-- Existing seeded users are pre-verified
UPDATE users SET email_verified = TRUE WHERE id IN (1, 2, 3);
