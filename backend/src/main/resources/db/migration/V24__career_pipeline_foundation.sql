CREATE TABLE career_pipelines (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    source_project_id BIGINT UNSIGNED NULL,
    name VARCHAR(120) NOT NULL,
    company_name VARCHAR(120) NOT NULL,
    role_title VARCHAR(160) NOT NULL,
    job_description_id BIGINT UNSIGNED NULL,
    resume_version_id BIGINT UNSIGNED NULL,
    lifecycle VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    outcome VARCHAR(20) NULL,
    current_stage_id BIGINT UNSIGNED NULL,
    archived_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_career_pipelines_source_project (user_id, source_project_id),
    KEY idx_career_pipelines_user_lifecycle (user_id, lifecycle, updated_at),
    KEY idx_career_pipelines_job (job_description_id),
    KEY idx_career_pipelines_resume_version (resume_version_id),
    CONSTRAINT fk_career_pipelines_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_career_pipelines_source_project FOREIGN KEY (source_project_id) REFERENCES job_projects (id) ON DELETE SET NULL,
    CONSTRAINT fk_career_pipelines_job FOREIGN KEY (job_description_id) REFERENCES job_descriptions (id) ON DELETE SET NULL,
    CONSTRAINT fk_career_pipelines_resume_version FOREIGN KEY (resume_version_id) REFERENCES resume_versions (id) ON DELETE SET NULL,
    CONSTRAINT chk_career_pipelines_lifecycle CHECK (lifecycle IN ('ACTIVE', 'PAUSED', 'CLOSED', 'ARCHIVED')),
    CONSTRAINT chk_career_pipelines_outcome CHECK (outcome IS NULL OR outcome IN ('OFFER', 'REJECTED', 'WITHDRAWN', 'OTHER')),
    CONSTRAINT chk_career_pipelines_closed_outcome CHECK ((lifecycle = 'CLOSED' AND outcome IS NOT NULL) OR (lifecycle <> 'CLOSED' AND outcome IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pipeline_stages (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    pipeline_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(80) NOT NULL,
    position_index INT NOT NULL,
    state VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_pipeline_stages_position (pipeline_id, position_index),
    KEY idx_pipeline_stages_pipeline (pipeline_id, position_index),
    CONSTRAINT fk_pipeline_stages_pipeline FOREIGN KEY (pipeline_id) REFERENCES career_pipelines (id) ON DELETE CASCADE,
    CONSTRAINT chk_pipeline_stages_position CHECK (position_index >= 0),
    CONSTRAINT chk_pipeline_stages_state CHECK (state IN ('PENDING', 'CURRENT', 'COMPLETED', 'SKIPPED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE career_pipelines
    ADD CONSTRAINT fk_career_pipelines_current_stage
        FOREIGN KEY (current_stage_id) REFERENCES pipeline_stages (id) ON DELETE SET NULL;

CREATE TABLE pipeline_stage_transitions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    pipeline_id BIGINT UNSIGNED NOT NULL,
    from_stage_id BIGINT UNSIGNED NULL,
    to_stage_id BIGINT UNSIGNED NOT NULL,
    actor VARCHAR(20) NOT NULL,
    note VARCHAR(500) NULL,
    occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_pipeline_transitions_history (pipeline_id, occurred_at, id),
    KEY idx_pipeline_transitions_from_stage (from_stage_id),
    KEY idx_pipeline_transitions_to_stage (to_stage_id),
    CONSTRAINT fk_pipeline_transitions_pipeline FOREIGN KEY (pipeline_id) REFERENCES career_pipelines (id) ON DELETE CASCADE,
    CONSTRAINT fk_pipeline_transitions_from_stage FOREIGN KEY (from_stage_id) REFERENCES pipeline_stages (id),
    CONSTRAINT fk_pipeline_transitions_to_stage FOREIGN KEY (to_stage_id) REFERENCES pipeline_stages (id),
    CONSTRAINT chk_pipeline_transitions_actor CHECK (actor IN ('USER', 'SYSTEM', 'MIGRATION'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
