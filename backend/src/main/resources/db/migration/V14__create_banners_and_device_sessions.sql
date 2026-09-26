-- V14: Create Promotional Banners and User Device Sessions tables
 
CREATE TABLE IF NOT EXISTS banners (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tag VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(500),
    cta_text VARCHAR(100) NOT NULL DEFAULT 'Explore Deals',
    cta_link VARCHAR(255) NOT NULL DEFAULT '/search',
    bg_gradient VARCHAR(150) DEFAULT 'from-slate-950 via-red-950/80 to-slate-900',
    badge_color VARCHAR(150) DEFAULT 'bg-red-500/20 border-red-500/30 text-red-300',
    image_url VARCHAR(500),
    accent VARCHAR(100),
    route_info VARCHAR(150),
    promo_code VARCHAR(50),
    discount_percentage INT DEFAULT 0,
    active BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 1,
    is_ai_generated BOOLEAN DEFAULT FALSE,
    prompt_used VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_banners_active_sort (active, sort_order)
);

CREATE TABLE IF NOT EXISTS user_device_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    deviceName VARCHAR(150),
    deviceType VARCHAR(50) DEFAULT 'Desktop',
    browser VARCHAR(100),
    os VARCHAR(100),
    ipAddress VARCHAR(100),
    location VARCHAR(150) DEFAULT 'India',
    lastActiveAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    isCurrentSession BOOLEAN DEFAULT FALSE,
    sessionToken VARCHAR(255),
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_device_sessions_user (user_id)
);

-- Seed initial interactive promotional banners
INSERT INTO banners (tag, title, subtitle, cta_text, cta_link, bg_gradient, badge_color, image_url, accent, route_info, promo_code, discount_percentage, active, sort_order)
VALUES 
('AI Curated Fleet', 'Volvo 9600 Multi-Axle Luxury Sleeper', 'Memory foam berths, personal charging hubs & panoramic sunset windows.', 'Explore Luxury Fleet', '/bus-tickets/bangalore-to-chennai', 'from-slate-950 via-red-950/80 to-slate-900', 'bg-red-500/20 border-red-500/30 text-red-300', 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=1200&q=80', '₹699 onwards', 'Bangalore ⇄ Chennai', 'LUXURY20', 20, TRUE, 1),

('Festive Offer • AI Dynamic Price', 'Scenic Hill Station Holiday Express', 'Direct sleeper coaches to Ooty, Munnar, Coorg & Kodaikanal with scenic stops.', 'Book Weekend Getaway', '/bus-tickets/bangalore-to-ooty', 'from-slate-950 via-emerald-950/80 to-slate-900', 'bg-emerald-500/20 border-emerald-500/30 text-emerald-300', 'https://images.unsplash.com/photo-1570125909232-eb263c188f7e?auto=format&fit=crop&w=1200&q=80', 'Flat 20% OFF', 'Bangalore ⇄ Ooty', 'HILLSTATION', 20, TRUE, 2),

('Passenger Safety First', 'AI Smart Safe Seating For Solo Women', 'Dedicated safe zones, 24/7 live GPS telemetry & verified drivers on commercial routes.', 'Discover Safe Routes', '/bus-tickets/chennai-to-coimbatore', 'from-slate-950 via-purple-950/80 to-slate-900', 'bg-purple-500/20 border-purple-500/30 text-purple-300', 'https://images.unsplash.com/photo-1509749837427-ac94a2553d0e?auto=format&fit=crop&w=1200&q=80', '100% Verified', 'Chennai ⇄ Coimbatore', 'WOMENSAFE', 15, TRUE, 3);

