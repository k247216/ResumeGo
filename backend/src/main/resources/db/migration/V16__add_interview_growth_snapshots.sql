-- 面试成长趋势快照。
-- 说明：快照只固化 AI 评价后的回答表现维度，用于成长趋势展示；
-- 不参与简历评分、岗位匹配排序，也不控制面试状态机。

CREATE TABLE interview_growth_snapshots (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    resume_id BIGINT UNSIGNED NOT NULL,
    resume_version_id BIGINT UNSIGNED NOT NULL,
    job_description_id BIGINT UNSIGNED NOT NULL,
    interview_plan_id BIGINT UNSIGNED NOT NULL,
    clarity_score DECIMAL(4,1) NOT NULL,
    relevance_score DECIMAL(4,1) NOT NULL,
    depth_score DECIMAL(4,1) NOT NULL,
    accuracy_score DECIMAL(4,1) NOT NULL,
    overall_score DECIMAL(4,1) NOT NULL,
    weak_points_json JSON NULL,
    summary_text VARCHAR(500) NULL,
    completed_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_interview_growth_plan (interview_plan_id),
    KEY idx_interview_growth_user_resume_job (user_id, resume_id, job_description_id, completed_at),
    KEY idx_interview_growth_resume_version (resume_version_id),
    CONSTRAINT fk_interview_growth_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_interview_growth_resume
        FOREIGN KEY (resume_id) REFERENCES resumes (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_interview_growth_resume_version
        FOREIGN KEY (resume_version_id) REFERENCES resume_versions (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_interview_growth_job
        FOREIGN KEY (job_description_id) REFERENCES job_descriptions (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_interview_growth_plan
        FOREIGN KEY (interview_plan_id) REFERENCES interview_plans (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_interview_growth_weak_points_array
        CHECK (weak_points_json IS NULL OR JSON_TYPE(weak_points_json) = 'ARRAY')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试成长趋势快照';
