-- V7__fix_schedule_route_nullable.sql
-- Modify route_id in schedules table to be nullable and add source_city, destination_city, boarding_points, dropping_points

ALTER TABLE schedules
    MODIFY COLUMN route_id BIGINT NULL,
    ADD COLUMN source_city VARCHAR(100) NULL,
    ADD COLUMN destination_city VARCHAR(100) NULL,
    ADD COLUMN boarding_points TEXT NULL,
    ADD COLUMN dropping_points TEXT NULL;
