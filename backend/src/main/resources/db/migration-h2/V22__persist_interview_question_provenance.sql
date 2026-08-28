-- H2 equivalent of MySQL V42.
ALTER TABLE interview_questions ADD COLUMN source_reference VARCHAR(1000) NULL;
ALTER TABLE interview_questions ADD COLUMN provenance_label VARCHAR(120) NULL;

ALTER TABLE interview_questions DROP CONSTRAINT chk_interview_questions_source;
ALTER TABLE interview_questions ADD CONSTRAINT chk_interview_questions_source
    CHECK (source IN ('ai_generated', 'system_defined', 'manual', 'ai_follow_up'));
