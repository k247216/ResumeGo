-- Knowledge import & extraction (MySQL)
-- NOTE: schema must stay equivalent to H2 V7__knowledge_import_extraction.sql
-- All id/user_id/document_id/source_file_id follow the existing MySQL convention:
-- BIGINT UNSIGNED, tables use utf8mb4_unicode_ci to match users.id FK type.

CREATE TABLE knowledge_extracted_contents (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    content LONGTEXT NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_knowledge_extracted_document UNIQUE (document_id),
    CONSTRAINT fk_knowledge_extracted_document FOREIGN KEY (document_id)
        REFERENCES knowledge_documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_knowledge_extracted_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE knowledge_import_jobs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    source_file_id BIGINT UNSIGNED NOT NULL,
    job_status VARCHAR(16) NOT NULL,
    error_code VARCHAR(32) NULL,
    started_at TIMESTAMP(3) NULL,
    finished_at TIMESTAMP(3) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_knowledge_import_job_document UNIQUE (document_id),
    CONSTRAINT fk_knowledge_import_job_document FOREIGN KEY (document_id)
        REFERENCES knowledge_documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_knowledge_import_job_source FOREIGN KEY (source_file_id)
        REFERENCES knowledge_source_files (id) ON DELETE CASCADE,
    CONSTRAINT fk_knowledge_import_job_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX idx_knowledge_import_jobs_user_status ON knowledge_import_jobs(user_id, job_status);
