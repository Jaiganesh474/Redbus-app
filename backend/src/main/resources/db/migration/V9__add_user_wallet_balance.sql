-- Add wallet_balance to users table
ALTER TABLE users ADD COLUMN wallet_balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00;

-- Add wallet_amount_used to bookings table
ALTER TABLE bookings ADD COLUMN wallet_amount_used DECIMAL(10, 2) DEFAULT 0.00;
