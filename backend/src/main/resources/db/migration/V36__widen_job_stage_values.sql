ALTER TABLE job_projects DROP CHECK chk_job_projects_stage;
ALTER TABLE job_projects ADD CONSTRAINT chk_job_projects_stage
    CHECK (stage IN ('applied', 'exam', 'interview', 'hr', 'offer', 'pool', 'screened_out', 'rejected', 'closed'));
ALTER TABLE job_stage_events DROP CHECK chk_job_stage_events_stage;
ALTER TABLE job_stage_events ADD CONSTRAINT chk_job_stage_events_stage
    CHECK (stage IN ('applied', 'exam', 'interview', 'hr', 'offer', 'pool', 'screened_out', 'rejected', 'closed'));
