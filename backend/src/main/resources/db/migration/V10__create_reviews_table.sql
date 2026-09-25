-- Migration V10: Create reviews table for passenger bus ratings and reviews
CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bus_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    user_name VARCHAR(100) NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    tags VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_bus FOREIGN KEY (bus_id) REFERENCES buses(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_reviews_bus ON reviews(bus_id);
