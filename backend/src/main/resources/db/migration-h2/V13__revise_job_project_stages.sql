ALTER TABLE job_projects DROP CONSTRAINT chk_job_projects_stage;
UPDATE job_projects SET stage = 'applied' WHERE stage IN ('preparing', 'interviewing');
ALTER TABLE job_projects ALTER COLUMN stage SET DEFAULT 'applied';
ALTER TABLE job_projects ADD CONSTRAINT chk_job_projects_stage
    CHECK (stage IN ('applied', 'exam', 'interview', 'hr', 'offer', 'closed'));
