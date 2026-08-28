CREATE TABLE pipeline_schedule_events (
    pipeline_id BIGINT UNSIGNED NOT NULL,
    schedule_event_id BIGINT UNSIGNED NOT NULL,
    linked_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (pipeline_id, schedule_event_id),
    UNIQUE KEY uk_pipeline_schedule_event (schedule_event_id),
    KEY idx_pipeline_schedule_pipeline (pipeline_id, linked_at),
    CONSTRAINT fk_pipeline_schedule_pipeline FOREIGN KEY (pipeline_id)
        REFERENCES career_pipelines (id) ON DELETE CASCADE,
    CONSTRAINT fk_pipeline_schedule_event FOREIGN KEY (schedule_event_id)
        REFERENCES schedule_events (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pipeline_interview_plans (
    pipeline_id BIGINT UNSIGNED NOT NULL,
    interview_plan_id BIGINT UNSIGNED NOT NULL,
    linked_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (pipeline_id, interview_plan_id),
    UNIQUE KEY uk_pipeline_interview_plan (interview_plan_id),
    KEY idx_pipeline_interview_pipeline (pipeline_id, linked_at),
    CONSTRAINT fk_pipeline_interview_pipeline FOREIGN KEY (pipeline_id)
        REFERENCES career_pipelines (id) ON DELETE CASCADE,
    CONSTRAINT fk_pipeline_interview_plan FOREIGN KEY (interview_plan_id)
        REFERENCES interview_plans (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
