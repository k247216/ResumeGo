CREATE TABLE schedule_events (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(120) NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    start_time DATETIME(3) NOT NULL,
    end_time DATETIME(3) NULL,
    notes VARCHAR(1000) NULL,
    job_description_id BIGINT UNSIGNED NULL,
    deleted_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_schedule_events_user_time (user_id, start_time, deleted_at),
    KEY idx_schedule_events_job_description (job_description_id),
    CONSTRAINT fk_schedule_events_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_schedule_events_job_description
        FOREIGN KEY (job_description_id) REFERENCES job_descriptions (id)
        ON DELETE SET NULL,
    CONSTRAINT chk_schedule_events_type
        CHECK (event_type IN ('interview', 'exam', 'followup', 'other'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
