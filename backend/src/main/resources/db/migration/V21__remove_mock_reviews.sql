-- Migration V21: Remove mocked reviews and ensure ratings reflect only verified passenger reviews
DELETE FROM reviews WHERE user_name IN ('Arun Kumar R.', 'Priya Venkatesh', 'Karthik Subramanian', 'Divya N.');

