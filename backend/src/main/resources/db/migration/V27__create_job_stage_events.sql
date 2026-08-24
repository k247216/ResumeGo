CREATE TABLE job_stage_events (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    project_id BIGINT UNSIGNED NOT NULL,
    stage VARCHAR(20) NOT NULL,
    occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_job_stage_events_project (project_id, occurred_at),
    CONSTRAINT fk_job_stage_events_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_job_stage_events_project FOREIGN KEY (project_id) REFERENCES job_projects (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_job_stage_events_stage
        CHECK (stage IN ('applied', 'exam', 'interview', 'hr', 'offer', 'closed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
