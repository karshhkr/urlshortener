CREATE TABLE IF NOT EXISTS click_events (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_code VARCHAR(10)  NOT NULL,
    clicked_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_agent VARCHAR(255) NULL,
    referrer   VARCHAR(512) NULL,

    INDEX idx_click_code_time (short_code, clicked_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;