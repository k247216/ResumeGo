-- 简历资产谱系：与 MySQL V39 等价的 H2 迁移。
-- 现存行统一为 GENERAL、来源为空、未归档。

ALTER TABLE resumes ADD COLUMN kind VARCHAR(24) NOT NULL DEFAULT 'GENERAL';
ALTER TABLE resumes ADD COLUMN forked_from_version_id BIGINT NULL;
ALTER TABLE resumes ADD COLUMN archived_at TIMESTAMP NULL;

CREATE INDEX idx_resumes_kind ON resumes (kind);
CREATE INDEX idx_resumes_forked_from_version_id ON resumes (forked_from_version_id);

ALTER TABLE resumes ADD CONSTRAINT fk_resumes_forked_from_version
    FOREIGN KEY (forked_from_version_id) REFERENCES resume_versions (id)
    ON DELETE RESTRICT;

ALTER TABLE resumes ADD CONSTRAINT chk_resumes_kind
    CHECK (kind IN ('GENERAL', 'JOB_EXPRESSION'));

ALTER TABLE resume_versions DROP CONSTRAINT chk_resume_versions_created_by;

ALTER TABLE resume_versions ADD CONSTRAINT chk_resume_versions_created_by
    CHECK (created_by_type IN ('user', 'ai_suggestion', 'import', 'fork'));
