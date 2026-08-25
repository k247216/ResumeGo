-- 三模式契约：interview_plans 增加不可变 mode、契约版本与开始上下文快照；
-- 面经题集为用户独立资产（有序题目）。
-- 旧计划统一回填 ROLE_BASED 并按现有外键生成兼容快照：不复制简历/JD 正文，不伪造 Pipeline ID。

ALTER TABLE interview_plans
    ADD COLUMN mode VARCHAR(32) NOT NULL DEFAULT 'ROLE_BASED' AFTER user_id,
    ADD COLUMN context_contract_version VARCHAR(16) NOT NULL DEFAULT '1' AFTER mode,
    ADD COLUMN start_context_snapshot_json JSON NULL AFTER context_contract_version;

ALTER TABLE interview_plans
    ADD CONSTRAINT chk_interview_plans_mode
        CHECK (mode IN ('ROLE_BASED', 'KNOWLEDGE_TRAINING', 'EXPERIENCE_SIMULATION'));

-- 旧计划兼容快照：仅含模式与既有引用 ID
UPDATE interview_plans
SET start_context_snapshot_json = CONCAT(
    '{"mode":"ROLE_BASED","contextContractVersion":"1","resumeVersionId":', resume_version_id,
    ',"jobDescriptionId":', job_description_id, '}')
WHERE start_context_snapshot_json IS NULL;

-- 知识训练与面经模拟不要求岗位/简历：放宽为可空（ROLE_BASED 由校验器保证必填）
ALTER TABLE interview_plans
    MODIFY COLUMN resume_version_id BIGINT UNSIGNED NULL,
    MODIFY COLUMN job_description_id BIGINT UNSIGNED NULL;

CREATE TABLE interview_question_sets (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(120) NOT NULL,
    source_type VARCHAR(32) NOT NULL COMMENT 'USER_MANUAL/IMPORTED_EXPERIENCE/GENERATED_PRACTICE',
    source_note VARCHAR(500) NULL COMMENT '用户声明的来源说明，不做网络抓取',
    archived_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_interview_question_sets_user (user_id, id),
    CONSTRAINT fk_interview_question_sets_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_interview_question_sets_source
        CHECK (source_type IN ('USER_MANUAL', 'IMPORTED_EXPERIENCE', 'GENERATED_PRACTICE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE interview_question_set_items (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    set_id BIGINT UNSIGNED NOT NULL,
    position_index INT UNSIGNED NOT NULL,
    question_text VARCHAR(1000) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_interview_question_set_items (set_id, position_index),
    CONSTRAINT fk_interview_question_set_items_set
        FOREIGN KEY (set_id) REFERENCES interview_question_sets (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
