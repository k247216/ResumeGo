CREATE TABLE job_projects (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    job_description_id BIGINT UNSIGNED NULL,
    resume_version_id BIGINT UNSIGNED NULL,
    archived_at DATETIME(3) NULL,
    deleted_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_job_projects_user_status (user_id, status, deleted_at),
    KEY idx_job_projects_job_description (job_description_id),
    KEY idx_job_projects_resume_version (resume_version_id),
    CONSTRAINT fk_job_projects_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_job_projects_job_description
        FOREIGN KEY (job_description_id) REFERENCES job_descriptions (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_job_projects_resume_version
        FOREIGN KEY (resume_version_id) REFERENCES resume_versions (id)
        ON DELETE SET NULL,
    CONSTRAINT chk_job_projects_status
        CHECK (status IN ('active', 'archived'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
