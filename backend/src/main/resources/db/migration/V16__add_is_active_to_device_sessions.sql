-- V16: Add is_active column to user_device_sessions for tracking active and previous sessions

ALTER TABLE user_device_sessions ADD COLUMN is_active BOOLEAN DEFAULT TRUE;
