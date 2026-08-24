-- 简历资产谱系：种类（通用/岗位表达）、fork 来源版本、归档时间。
-- 现存行统一为 GENERAL、来源为空、未归档；不修改或删除任何历史字段。

ALTER TABLE resumes
    ADD COLUMN kind VARCHAR(24) NOT NULL DEFAULT 'GENERAL' AFTER title,
    ADD COLUMN forked_from_version_id BIGINT UNSIGNED NULL AFTER current_version_id,
    ADD COLUMN archived_at DATETIME(3) NULL AFTER deleted_at,
    ADD KEY idx_resumes_kind (kind),
    ADD KEY idx_resumes_forked_from_version_id (forked_from_version_id),
    ADD CONSTRAINT fk_resumes_forked_from_version
        FOREIGN KEY (forked_from_version_id) REFERENCES resume_versions (id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT chk_resumes_kind
        CHECK (kind IN ('GENERAL', 'JOB_EXPRESSION'));

ALTER TABLE resume_versions DROP CHECK chk_resume_versions_created_by_type;

ALTER TABLE resume_versions
    ADD CONSTRAINT chk_resume_versions_created_by_type
        CHECK (created_by_type IN ('user', 'ai_suggestion', 'import', 'fork'));
