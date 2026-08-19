-- 多轮模拟面试计划容器。
-- 说明：interview_plans 表示“一次完整面试”；interview_sessions 仍表示某位面试官的一轮面试。
-- 本迁移只建立归属关系，不改变 interview_sessions.status 的状态机含义。

CREATE TABLE interview_plans (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    resume_version_id BIGINT UNSIGNED NOT NULL,
    job_description_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(120) NOT NULL DEFAULT '多轮模拟面试',
    question_count INT UNSIGNED NOT NULL DEFAULT 5,
    persona_plan_json JSON NOT NULL COMMENT '创建时选择的面试官顺序快照',
    focus_tags_json JSON NULL COMMENT '本次面试关注点',
    supplement_text TEXT NULL COMMENT '用户补充说明',
    deleted_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_interview_plans_user_created (user_id, created_at),
    KEY idx_interview_plans_resume_version_id (resume_version_id),
    KEY idx_interview_plans_job_description_id (job_description_id),
    CONSTRAINT fk_interview_plans_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_interview_plans_resume_version
        FOREIGN KEY (resume_version_id) REFERENCES resume_versions (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_interview_plans_job_description
        FOREIGN KEY (job_description_id) REFERENCES job_descriptions (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_interview_plans_persona_plan_array
        CHECK (JSON_TYPE(persona_plan_json) = 'ARRAY'),
    CONSTRAINT chk_interview_plans_focus_tags_array
        CHECK (focus_tags_json IS NULL OR JSON_TYPE(focus_tags_json) = 'ARRAY')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='多轮模拟面试计划';

ALTER TABLE interview_sessions
    ADD COLUMN plan_id BIGINT UNSIGNED NULL COMMENT '所属多轮面试计划',
    ADD COLUMN round_order INT UNSIGNED NULL COMMENT '计划内轮次，从 1 开始',
    ADD KEY idx_interview_sessions_plan_id (plan_id),
    ADD UNIQUE KEY uk_interview_sessions_plan_round (plan_id, round_order),
    ADD CONSTRAINT fk_interview_sessions_plan
        FOREIGN KEY (plan_id) REFERENCES interview_plans (id)
        ON DELETE RESTRICT;
