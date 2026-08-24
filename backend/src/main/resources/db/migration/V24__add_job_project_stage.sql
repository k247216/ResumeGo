ALTER TABLE job_projects
    ADD COLUMN stage VARCHAR(20) NOT NULL DEFAULT 'preparing' AFTER status,
    ADD COLUMN stage_updated_at DATETIME(3) NULL AFTER archived_at,
    ADD CONSTRAINT chk_job_projects_stage
        CHECK (stage IN ('preparing', 'applied', 'exam', 'interviewing', 'offer', 'closed'));
