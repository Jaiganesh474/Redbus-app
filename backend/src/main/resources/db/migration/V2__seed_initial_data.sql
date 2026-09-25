-- V2__seed_initial_data.sql
-- Initial seed data for users, buses, seats, routes, route_seats, and RAG knowledge base

-- Users:
-- admin@redbus.com / admin123 ($2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW)
-- operator@zingbus.com / operator123 ($2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW)
-- user@example.com / user123 ($2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW)
INSERT INTO users (id, name, email, password_hash, phone, role) VALUES
(1, 'RedBus Admin', 'admin@redbus.com', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW', '+91 9876543210', 'ROLE_ADMIN'),
(2, 'Zingbus Operator', 'operator@zingbus.com', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW', '+91 9876543211', 'ROLE_OPERATOR'),
(3, 'Rahul Sharma', 'user@example.com', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW', '+91 9876543212', 'ROLE_USER');

-- Buses
INSERT INTO buses (id, operator_name, bus_type, total_seats, amenities, rating) VALUES
(1, 'IntrCity SmartBus', 'AC Sleeper (2+1)', 30, 'WiFi,Charging Point,Water Bottle,Blanket,Live Tracking,Emergency Exit', 4.8),
(2, 'Zingbus Plus', 'Volvo Multi-Axle AC Sleeper (2+1)', 30, 'WiFi,Charging Point,Blanket,Snacks,Reading Light,CCTV', 4.7),
(3, 'Orange Travels', 'BharatBenz AC Sleeper (2+1)', 30, 'Charging Point,Water Bottle,Pillow,Blanket,Movie Screen', 4.6),
(4, 'SRS Travels', 'Scania Multi-Axle AC Seater (2+2)', 40, 'Charging Point,Reading Light,Emergency Exit,Luggage Storage', 4.3),
(5, 'KSRTC Airavat Club Class', 'Volvo Multi-Axle AC Seater (2+2)', 40, 'Water Bottle,Charging Point,Air Suspension,Emergency Exit', 4.5);

-- Seats for Bus 1, 2, 3 (30 seats: 15 Lower Deck, 15 Upper Deck - 2+1 Sleeper layout)
-- Bus 1 seats
INSERT INTO seats (bus_id, seat_number, seat_type, deck, row_num, col_num) VALUES
(1, 'L1', 'SLEEPER', 'LOWER', 1, 1), (1, 'L2', 'SLEEPER', 'LOWER', 1, 2), (1, 'L3', 'SLEEPER', 'LOWER', 1, 3),
(1, 'L4', 'SLEEPER', 'LOWER', 2, 1), (1, 'L5', 'SLEEPER', 'LOWER', 2, 2), (1, 'L6', 'SLEEPER', 'LOWER', 2, 3),
(1, 'L7', 'SLEEPER', 'LOWER', 3, 1), (1, 'L8', 'SLEEPER', 'LOWER', 3, 2), (1, 'L9', 'SLEEPER', 'LOWER', 3, 3),
(1, 'L10', 'SLEEPER', 'LOWER', 4, 1), (1, 'L11', 'SLEEPER', 'LOWER', 4, 2), (1, 'L12', 'SLEEPER', 'LOWER', 4, 3),
(1, 'L13', 'SLEEPER', 'LOWER', 5, 1), (1, 'L14', 'SLEEPER', 'LOWER', 5, 2), (1, 'L15', 'SLEEPER', 'LOWER', 5, 3),
(1, 'U1', 'SLEEPER', 'UPPER', 1, 1), (1, 'U2', 'SLEEPER', 'UPPER', 1, 2), (1, 'U3', 'SLEEPER', 'UPPER', 1, 3),
(1, 'U4', 'SLEEPER', 'UPPER', 2, 1), (1, 'U5', 'SLEEPER', 'UPPER', 2, 2), (1, 'U6', 'SLEEPER', 'UPPER', 2, 3),
(1, 'U7', 'SLEEPER', 'UPPER', 3, 1), (1, 'U8', 'SLEEPER', 'UPPER', 3, 2), (1, 'U9', 'SLEEPER', 'UPPER', 3, 3),
(1, 'U10', 'SLEEPER', 'UPPER', 4, 1), (1, 'U11', 'SLEEPER', 'UPPER', 4, 2), (1, 'U12', 'SLEEPER', 'UPPER', 4, 3),
(1, 'U13', 'SLEEPER', 'UPPER', 5, 1), (1, 'U14', 'SLEEPER', 'UPPER', 5, 2), (1, 'U15', 'SLEEPER', 'UPPER', 5, 3);

-- Bus 2 seats
INSERT INTO seats (bus_id, seat_number, seat_type, deck, row_num, col_num) VALUES
(2, 'L1', 'SLEEPER', 'LOWER', 1, 1), (2, 'L2', 'SLEEPER', 'LOWER', 1, 2), (2, 'L3', 'SLEEPER', 'LOWER', 1, 3),
(2, 'L4', 'SLEEPER', 'LOWER', 2, 1), (2, 'L5', 'SLEEPER', 'LOWER', 2, 2), (2, 'L6', 'SLEEPER', 'LOWER', 2, 3),
(2, 'L7', 'SLEEPER', 'LOWER', 3, 1), (2, 'L8', 'SLEEPER', 'LOWER', 3, 2), (2, 'L9', 'SLEEPER', 'LOWER', 3, 3),
(2, 'L10', 'SLEEPER', 'LOWER', 4, 1), (2, 'L11', 'SLEEPER', 'LOWER', 4, 2), (2, 'L12', 'SLEEPER', 'LOWER', 4, 3),
(2, 'L13', 'SLEEPER', 'LOWER', 5, 1), (2, 'L14', 'SLEEPER', 'LOWER', 5, 2), (2, 'L15', 'SLEEPER', 'LOWER', 5, 3),
(2, 'U1', 'SLEEPER', 'UPPER', 1, 1), (2, 'U2', 'SLEEPER', 'UPPER', 1, 2), (2, 'U3', 'SLEEPER', 'UPPER', 1, 3),
(2, 'U4', 'SLEEPER', 'UPPER', 2, 1), (2, 'U5', 'SLEEPER', 'UPPER', 2, 2), (2, 'U6', 'SLEEPER', 'UPPER', 2, 3),
(2, 'U7', 'SLEEPER', 'UPPER', 3, 1), (2, 'U8', 'SLEEPER', 'UPPER', 3, 2), (2, 'U9', 'SLEEPER', 'UPPER', 3, 3),
(2, 'U10', 'SLEEPER', 'UPPER', 4, 1), (2, 'U11', 'SLEEPER', 'UPPER', 4, 2), (2, 'U12', 'SLEEPER', 'UPPER', 4, 3),
(2, 'U13', 'SLEEPER', 'UPPER', 5, 1), (2, 'U14', 'SLEEPER', 'UPPER', 5, 2), (2, 'U15', 'SLEEPER', 'UPPER', 5, 3);

-- Bus 4 seats (40 Seater 2+2)
INSERT INTO seats (bus_id, seat_number, seat_type, deck, row_num, col_num) VALUES
(4, 'S1', 'SEATER', 'LOWER', 1, 1), (4, 'S2', 'SEATER', 'LOWER', 1, 2), (4, 'S3', 'SEATER', 'LOWER', 1, 3), (4, 'S4', 'SEATER', 'LOWER', 1, 4),
(4, 'S5', 'SEATER', 'LOWER', 2, 1), (4, 'S6', 'SEATER', 'LOWER', 2, 2), (4, 'S7', 'SEATER', 'LOWER', 2, 3), (4, 'S8', 'SEATER', 'LOWER', 2, 4),
(4, 'S9', 'SEATER', 'LOWER', 3, 1), (4, 'S10', 'SEATER', 'LOWER', 3, 2), (4, 'S11', 'SEATER', 'LOWER', 3, 3), (4, 'S12', 'SEATER', 'LOWER', 3, 4),
(4, 'S13', 'SEATER', 'LOWER', 4, 1), (4, 'S14', 'SEATER', 'LOWER', 4, 2), (4, 'S15', 'SEATER', 'LOWER', 4, 3), (4, 'S16', 'SEATER', 'LOWER', 4, 4),
(4, 'S17', 'SEATER', 'LOWER', 5, 1), (4, 'S18', 'SEATER', 'LOWER', 5, 2), (4, 'S19', 'SEATER', 'LOWER', 5, 3), (4, 'S20', 'SEATER', 'LOWER', 5, 4);

-- Routes
-- Dynamic dates around current date and upcoming dates
INSERT INTO routes (id, bus_id, source_city, destination_city, departure_time, arrival_time, travel_date, base_price, duration_hours, boarding_points, dropping_points) VALUES
-- Bangalore to Chennai
(1, 1, 'Bangalore', 'Chennai', '21:30:00', '05:00:00', CURRENT_DATE(), 850.00, 7.5, 'Madiwala (21:00), Silk Board (21:15), Electronic City (21:30)', 'Koyambedu (04:30), Guindy (05:00), Tambaram (05:30)'),
(2, 2, 'Bangalore', 'Chennai', '22:15:00', '05:45:00', CURRENT_DATE(), 920.00, 7.5, 'Majestic (21:30), Shantinagar (21:50), Indiranagar (22:15)', 'Poonamallee (05:00), Koyambedu (05:30), Central (05:45)'),
(3, 1, 'Bangalore', 'Chennai', '21:30:00', '05:00:00', DATE_ADD(CURRENT_DATE(), INTERVAL 1 DAY), 890.00, 7.5, 'Madiwala (21:00), Silk Board (21:15), Electronic City (21:30)', 'Koyambedu (04:30), Guindy (05:00), Tambaram (05:30)'),
(4, 2, 'Bangalore', 'Chennai', '22:15:00', '05:45:00', DATE_ADD(CURRENT_DATE(), INTERVAL 1 DAY), 950.00, 7.5, 'Majestic (21:30), Shantinagar (21:50), Indiranagar (22:15)', 'Poonamallee (05:00), Koyambedu (05:30), Central (05:45)'),
(5, 4, 'Bangalore', 'Chennai', '06:00:00', '13:00:00', DATE_ADD(CURRENT_DATE(), INTERVAL 1 DAY), 600.00, 7.0, 'Majestic (05:30), Indiranagar (06:00), KR Puram (06:20)', 'Koyambedu (12:30), Central (13:00)'),

-- Chennai to Bangalore
(6, 1, 'Chennai', 'Bangalore', '22:00:00', '05:30:00', DATE_ADD(CURRENT_DATE(), INTERVAL 1 DAY), 850.00, 7.5, 'Koyambedu (21:30), Guindy (22:00)', 'Electronic City (04:45), Silk Board (05:00), Madiwala (05:30)'),

-- Mumbai to Pune
(7, 2, 'Mumbai', 'Pune', '07:00:00', '10:30:00', DATE_ADD(CURRENT_DATE(), INTERVAL 1 DAY), 450.00, 3.5, 'Dadar (06:30), Chembur (07:00), Vashi (07:30)', 'Wakad (09:45), Chandani Chowk (10:15), Swargate (10:30)'),
(8, 4, 'Mumbai', 'Pune', '18:00:00', '21:45:00', DATE_ADD(CURRENT_DATE(), INTERVAL 1 DAY), 420.00, 3.75, 'Borivali (17:00), Dadar (17:30), Vashi (18:00)', 'Hinjewadi (21:00), Wakad (21:15), Pune Station (21:45)'),

-- Delhi to Jaipur
(9, 1, 'Delhi', 'Jaipur', '06:30:00', '12:00:00', DATE_ADD(CURRENT_DATE(), INTERVAL 1 DAY), 550.00, 5.5, 'Kashmere Gate ISBT (06:00), Dhaula Kuan (06:30), IFFCO Chowk Gurgaon (07:15)', 'Sindhi Camp (11:30), 200 Feet Bypass (12:00)'),
(10, 2, 'Delhi', 'Jaipur', '23:00:00', '04:30:00', DATE_ADD(CURRENT_DATE(), INTERVAL 1 DAY), 750.00, 5.5, 'Kashmere Gate ISBT (22:30), Mahipalpur (23:00), Gurgaon (23:45)', 'Sindhi Camp (04:00), Narayan Singh Circle (04:30)'),

-- Hyderabad to Bangalore
(11, 3, 'Hyderabad', 'Bangalore', '21:00:00', '06:30:00', DATE_ADD(CURRENT_DATE(), INTERVAL 1 DAY), 1100.00, 9.5, 'Ameerpet (20:15), Lakdikapul (20:45), Shamshabad (21:30)', 'Hebbal (05:45), Majestic (06:15), Madiwala (06:30)');

-- Route seats generation for Route 1 (Bus 1)
INSERT INTO route_seats (route_id, seat_id, status, gender_restriction)
SELECT 1, s.id, 'AVAILABLE', 'NONE' FROM seats s WHERE s.bus_id = 1;

-- Route seats generation for Route 3 (Bus 1)
INSERT INTO route_seats (route_id, seat_id, status, gender_restriction)
SELECT 3, s.id, 'AVAILABLE', 'NONE' FROM seats s WHERE s.bus_id = 1;

-- Route seats generation for Route 4 (Bus 2)
INSERT INTO route_seats (route_id, seat_id, status, gender_restriction)
SELECT 4, s.id, 'AVAILABLE', 'NONE' FROM seats s WHERE s.bus_id = 2;

-- Mark a few sample seats as booked or female reserved for realistic preview
UPDATE route_seats SET status = 'BOOKED' WHERE route_id = 3 AND seat_id IN (SELECT id FROM seats WHERE bus_id = 1 AND seat_number IN ('L1', 'L2', 'U5'));
UPDATE route_seats SET gender_restriction = 'FEMALE' WHERE route_id = 3 AND seat_id IN (SELECT id FROM seats WHERE bus_id = 1 AND seat_number IN ('L3', 'L6'));

-- Knowledge Base Documents for RAG & FAQ
INSERT INTO kb_documents (id, title, source_type, content) VALUES
(1, 'Cancellation and Refund Policy', 'POLICY', 'Passengers can cancel tickets up to 2 hours before scheduled departure. Cancellation charges: More than 24 hours before departure: 10% deduction (90% refund). Between 12 to 24 hours: 25% deduction (75% refund). Between 2 to 12 hours: 50% deduction (50% refund). Less than 2 hours or after departure: No refund. Refunds are credited to original payment method within 3-5 business days.'),
(2, 'Luggage and Baggage Rules', 'POLICY', 'Each passenger is permitted up to 15 kg of personal luggage free of charge. Excess baggage will be charged at operator discretion (usually Rs 20 per kg). Prohibited items include flammable substances, explosives, firearms, contraband, and dangerous goods. Luggage must be properly tagged at boarding.'),
(3, 'Boarding and Identification Guidelines', 'POLICY', 'Passengers must arrive at the boarding point at least 15 minutes prior to scheduled departure. A valid government-issued photo ID (Aadhaar Card, Driving License, Passport, or Voter ID) along with the digital M-Ticket / SMS / E-Ticket is required for verification before boarding.'),
(4, 'Child and Infant Fare Policy', 'POLICY', 'Children aged 5 and above require an individual seat and full ticket fare. Children below 5 years of age may travel free of charge if sharing a seat with an accompanying adult, provided valid age proof is shown upon request.'),
(5, 'Pet Travel Policy', 'POLICY', 'Pets and animals are strictly not permitted on regular passenger buses for safety, hygiene, and passenger comfort reasons, unless specified as a private chartered service with prior operator written consent.');

-- KB Chunks for Retrieval
INSERT INTO kb_chunks (document_id, chunk_text, chunk_index) VALUES
(1, 'Cancellation policy: Cancellations over 24h prior to departure incur a 10% cancellation fee. Cancellations between 12-24h incur 25%. Cancellations between 2-12h incur 50%. Under 2h is non-refundable. Refunds take 3-5 working days.', 0),
(2, 'Luggage allowance: 15kg free per passenger. Overweight baggage costs approx Rs 20/kg. Prohibited items include hazardous materials, weapons, and flammables.', 0),
(3, 'Boarding requirements: Arrive 15 minutes prior to bus departure. Valid Govt Photo ID (Aadhaar, DL, Passport) and e-ticket/SMS are mandatory.', 0),
(4, 'Child ticket policy: Children 5 years and older require full tickets. Children under 5 travel free without an allocated seat.', 0),
(5, 'Pet policy: Pets are not allowed on regular commercial buses due to hygiene and safety standards.', 0);
