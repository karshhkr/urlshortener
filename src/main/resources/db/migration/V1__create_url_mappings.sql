CREATE TABLE IF NOT EXISTS url_mappings (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_code   VARCHAR(10)   NOT NULL UNIQUE,
    original_url VARCHAR(2048) NOT NULL,
    click_count  BIGINT        NOT NULL DEFAULT 0,
    expires_at   DATETIME      NULL,
    created_by   VARCHAR(100)  NULL,
    is_active    TINYINT(1)    NOT NULL DEFAULT 1,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_short_code  (short_code),
    INDEX idx_expires_at  (expires_at),
    INDEX idx_created_at  (created_at),
    INDEX idx_is_active   (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;