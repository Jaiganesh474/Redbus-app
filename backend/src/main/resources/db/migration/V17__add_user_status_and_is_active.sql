-- V17: Add is_active and status columns to users table for deactivation and soft deletion
ALTER TABLE users ADD COLUMN is_active BOOLEAN DEFAULT TRUE;
ALTER TABLE users ADD COLUMN status VARCHAR(30) DEFAULT 'ACTIVE';
