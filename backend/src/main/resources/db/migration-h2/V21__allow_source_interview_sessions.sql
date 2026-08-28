-- H2 equivalent of MySQL V41.
ALTER TABLE interview_sessions ALTER COLUMN resume_version_id SET NULL;
ALTER TABLE interview_sessions ALTER COLUMN job_description_id SET NULL;
