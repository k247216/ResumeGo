ALTER TABLE job_projects
    ADD COLUMN industry VARCHAR(60) NULL AFTER stage_updated_at,
    ADD COLUMN target_role VARCHAR(120) NULL AFTER industry,
    ADD COLUMN location VARCHAR(120) NULL AFTER target_role,
    ADD COLUMN notes VARCHAR(500) NULL AFTER location;
