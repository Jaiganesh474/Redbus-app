-- Add user_rating and user_review to bookings table
ALTER TABLE bookings
    ADD COLUMN user_rating INT DEFAULT NULL,
    ADD COLUMN user_review TEXT DEFAULT NULL;

