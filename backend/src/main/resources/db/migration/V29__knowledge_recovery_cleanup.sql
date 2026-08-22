-- Knowledge recovery & cleanup (MySQL)
-- NOTE: schema must stay equivalent to H2 V9__knowledge_recovery_cleanup.sql

ALTER TABLE knowledge_source_files ADD COLUMN staging_relative_path VARCHAR(512) NULL;

CREATE TABLE knowledge_delete_confirmations (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    document_id BIGINT UNSIGNED NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(3) NOT NULL,
    consumed_at TIMESTAMP(3) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_knowledge_delete_confirm_document UNIQUE (document_id),
    CONSTRAINT fk_knowledge_delete_confirm_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_knowledge_delete_confirm_document FOREIGN KEY (document_id)
        REFERENCES knowledge_documents (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE knowledge_cleanup_jobs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    document_id BIGINT UNSIGNED NOT NULL,
    document_title VARCHAR(120) NOT NULL,
    source_relative_path VARCHAR(512) NULL,
    job_status VARCHAR(16) NOT NULL,
    error_code VARCHAR(32) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    started_at TIMESTAMP(3) NULL,
    finished_at TIMESTAMP(3) NULL,
    CONSTRAINT fk_knowledge_cleanup_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_knowledge_cleanup_user_status ON knowledge_cleanup_jobs(user_id, job_status);
