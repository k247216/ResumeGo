-- Knowledge classification & keyword search (MySQL)
-- NOTE: schema must stay equivalent to H2 V8__knowledge_classification_search.sql
-- All id/user_id/document_id/category_id/tag_id follow BIGINT UNSIGNED + utf8mb4_unicode_ci.

CREATE TABLE knowledge_categories (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(40) NOT NULL,
    normalized_name VARCHAR(40) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_knowledge_category_user_name UNIQUE (user_id, normalized_name),
    CONSTRAINT fk_knowledge_category_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE knowledge_tags (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(40) NOT NULL,
    normalized_name VARCHAR(40) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_knowledge_tag_user_name UNIQUE (user_id, normalized_name),
    CONSTRAINT fk_knowledge_tag_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE knowledge_document_categories (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT UNSIGNED NOT NULL,
    category_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_knowledge_doc_category_document UNIQUE (document_id),
    CONSTRAINT uk_knowledge_doc_category_pair UNIQUE (document_id, category_id),
    CONSTRAINT fk_knowledge_doc_category_document FOREIGN KEY (document_id)
        REFERENCES knowledge_documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_knowledge_doc_category_category FOREIGN KEY (category_id)
        REFERENCES knowledge_categories (id) ON DELETE CASCADE,
    CONSTRAINT fk_knowledge_doc_category_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE knowledge_document_tags (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT UNSIGNED NOT NULL,
    tag_id BIGINT UNSIGNED NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_knowledge_doc_tag_pair UNIQUE (document_id, tag_id),
    CONSTRAINT fk_knowledge_doc_tag_document FOREIGN KEY (document_id)
        REFERENCES knowledge_documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_knowledge_doc_tag_tag FOREIGN KEY (tag_id)
        REFERENCES knowledge_tags (id) ON DELETE CASCADE,
    CONSTRAINT fk_knowledge_doc_tag_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
