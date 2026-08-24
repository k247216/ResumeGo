ALTER TABLE job_projects ADD COLUMN stage VARCHAR(20) NOT NULL DEFAULT 'preparing';
ALTER TABLE job_projects ADD COLUMN stage_updated_at TIMESTAMP(3) NULL;
ALTER TABLE job_projects ADD CONSTRAINT chk_job_projects_stage
    CHECK (stage IN ('preparing', 'applied', 'exam', 'interviewing', 'offer', 'closed'));
