-- V12__add_trip_guarantee_and_service_fee_to_bookings.sql
-- Add trip guarantee and AI service fee columns to bookings table

ALTER TABLE bookings
    ADD COLUMN has_trip_guarantee BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN trip_guarantee_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN service_fee DECIMAL(10, 2) NOT NULL DEFAULT 0.00;
