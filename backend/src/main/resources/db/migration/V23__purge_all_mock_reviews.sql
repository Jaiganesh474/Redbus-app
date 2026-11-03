-- Migration V23: Purge all mock/seeded reviews so ONLY real passenger reviews from completed journeys exist
DELETE FROM reviews WHERE user_id IS NULL OR user_name IN (
    'Arun Kumar R.', 'Priya Venkatesh', 'Karthik Subramanian', 'Divya N.',
    'Sri sahettya S', 'Thabresh khan', 'Meenakshi Sundaram', 'Ananya Sharma',
    'Venkatesh Babu', 'Kavitha Ramesh', 'Rajeshwari M.', 'Verified Passenger'
);

-- Recalculate all bus ratings dynamically based only on remaining real passenger reviews
UPDATE buses b
SET b.rating = COALESCE(
    (SELECT ROUND(AVG(r.rating), 1) FROM reviews r WHERE r.bus_id = b.id),
    0.0
);
