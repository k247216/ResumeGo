-- Knowledge Training and Experience Simulation sessions do not require a
-- Pipeline or Resume Version. Keep the foreign keys, but allow the two
-- context references to be absent; the immutable plan snapshot is authoritative.
ALTER TABLE interview_sessions
    MODIFY COLUMN resume_version_id BIGINT UNSIGNED NULL,
    MODIFY COLUMN job_description_id BIGINT UNSIGNED NULL;
