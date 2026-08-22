-- Knowledge library foundation (MySQL)
-- NOTE: schema must stay equivalent to H2 V6__knowledge_library_foundation.sql

CREATE TABLE knowledge_documents (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    source_type VARCHAR(16) NOT NULL,
    processing_status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_knowledge_documents_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX idx_knowledge_documents_user_updated ON knowledge_documents(user_id, updated_at, id);

CREATE TABLE knowledge_source_files (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_relative_path VARCHAR(512) NOT NULL,
    mime_type VARCHAR(128) NULL,
    extension VARCHAR(64) NULL,
    size_bytes BIGINT NOT NULL DEFAULT 0,
    sha256 VARCHAR(64) NOT NULL,
    availability VARCHAR(16) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_knowledge_source_file_document UNIQUE (document_id),
    CONSTRAINT uk_knowledge_source_file_sha UNIQUE (user_id, sha256),
    CONSTRAINT fk_knowledge_source_file_document FOREIGN KEY (document_id)
        REFERENCES knowledge_documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_knowledge_source_file_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
