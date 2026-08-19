CREATE TABLE ai_provider_profiles (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    protocol_type VARCHAR(32) NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    default_model VARCHAR(120) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    last_tested_at DATETIME(3) NULL,
    last_test_status VARCHAR(32) NULL,
    last_test_message VARCHAR(200) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_ai_provider_profiles_user (user_id, is_default, updated_at),
    CONSTRAINT fk_ai_provider_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_ai_provider_profiles_protocol CHECK (protocol_type IN ('openai-compatible', 'anthropic', 'gemini')),
    CONSTRAINT chk_ai_provider_profiles_test_status CHECK (last_test_status IS NULL OR last_test_status IN ('success', 'failed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
