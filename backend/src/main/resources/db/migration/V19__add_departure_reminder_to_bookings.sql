-- V19: Add departure reminder tracking to bookings table for 1-hour automated journey alerts
ALTER TABLE bookings 
    ADD COLUMN departure_reminder_sent BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN departure_reminder_sent_at DATETIME NULL;

CREATE INDEX idx_bookings_departure_reminder ON bookings (status, departure_reminder_sent);
