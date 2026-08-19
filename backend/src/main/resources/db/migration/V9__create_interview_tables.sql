-- 职达 Sprint 2 模拟面试模块数据库结构
-- 范围：面试会话、问题、回答、AI 评估四张表，支撑完整的模拟面试流程。
-- 关联：users（考生）、resume_versions（简历版本）、job_descriptions（目标岗位）

CREATE TABLE interview_sessions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    resume_version_id BIGINT UNSIGNED NOT NULL,
    job_description_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'READY',
    current_question_index INT UNSIGNED NOT NULL DEFAULT 0,
    total_questions INT UNSIGNED NOT NULL DEFAULT 3,
    started_at DATETIME(3) NULL,
    completed_at DATETIME(3) NULL,
    summary_json JSON NULL COMMENT 'AI 生成的面试总结报告（整体表现、亮点、待改进领域等）',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_interview_sessions_user_id (user_id),
    KEY idx_interview_sessions_resume_version_id (resume_version_id),
    KEY idx_interview_sessions_job_description_id (job_description_id),
    KEY idx_interview_sessions_status (status),
    CONSTRAINT fk_interview_sessions_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_interview_sessions_resume_version
        FOREIGN KEY (resume_version_id) REFERENCES resume_versions (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_interview_sessions_job_description
        FOREIGN KEY (job_description_id) REFERENCES job_descriptions (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_interview_sessions_status
        CHECK (status IN ('READY', 'ASKING', 'WAITING_ANSWER', 'EVALUATING', 'SUMMARIZING', 'COMPLETED', 'FAILED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE interview_questions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    session_id BIGINT UNSIGNED NOT NULL,
    question_index INT UNSIGNED NOT NULL,
    question_text TEXT NOT NULL,
    question_type VARCHAR(50) NULL,
    target_skill VARCHAR(100) NULL,
    source VARCHAR(50) NOT NULL DEFAULT 'ai_generated',
    generation_ai_invocation_id BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_interview_questions_session_index (session_id, question_index),
    KEY idx_interview_questions_session_id (session_id),
    CONSTRAINT fk_interview_questions_session
        FOREIGN KEY (session_id) REFERENCES interview_sessions (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_interview_questions_question_type
        CHECK (question_type IN ('behavioral', 'technical', 'situational', 'background', 'other')),
    CONSTRAINT chk_interview_questions_source
        CHECK (source IN ('ai_generated', 'system_defined', 'manual')),
    CONSTRAINT fk_interview_questions_generation_ai_invocation
        FOREIGN KEY (generation_ai_invocation_id) REFERENCES ai_invocations (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE interview_answers (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    session_id BIGINT UNSIGNED NOT NULL,
    question_id BIGINT UNSIGNED NOT NULL,
    answer_text TEXT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_interview_answers_question (question_id),
    KEY idx_interview_answers_session_id (session_id),
    CONSTRAINT fk_interview_answers_session
        FOREIGN KEY (session_id) REFERENCES interview_sessions (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_interview_answers_question
        FOREIGN KEY (question_id) REFERENCES interview_questions (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE interview_evaluations (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    session_id BIGINT UNSIGNED NOT NULL,
    question_id BIGINT UNSIGNED NOT NULL,
    answer_id BIGINT UNSIGNED NOT NULL,
    score_json JSON NULL COMMENT '多维评分，如 {"relevance":8,"depth":7,"expression":8,"overall":7.7}',
    strengths_json JSON NULL COMMENT '回答亮点列表，数组格式',
    weaknesses_json JSON NULL COMMENT '回答不足列表，数组格式',
    suggestions_json JSON NULL COMMENT '改进建议列表，数组格式',
    evaluation_ai_invocation_id BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_interview_evaluations_answer (answer_id),
    KEY idx_interview_evaluations_session_id (session_id),
    KEY idx_interview_evaluations_question_id (question_id),
    CONSTRAINT fk_interview_evaluations_session
        FOREIGN KEY (session_id) REFERENCES interview_sessions (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_interview_evaluations_question
        FOREIGN KEY (question_id) REFERENCES interview_questions (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_interview_evaluations_answer
        FOREIGN KEY (answer_id) REFERENCES interview_answers (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_interview_evaluations_score_object
        CHECK (score_json IS NULL OR JSON_TYPE(score_json) = 'OBJECT'),
    CONSTRAINT chk_interview_evaluations_strengths_array
        CHECK (strengths_json IS NULL OR JSON_TYPE(strengths_json) = 'ARRAY'),
    CONSTRAINT chk_interview_evaluations_weaknesses_array
        CHECK (weaknesses_json IS NULL OR JSON_TYPE(weaknesses_json) = 'ARRAY'),
    CONSTRAINT chk_interview_evaluations_suggestions_array
        CHECK (suggestions_json IS NULL OR JSON_TYPE(suggestions_json) = 'ARRAY'),
    CONSTRAINT fk_interview_evaluations_evaluation_ai_invocation
        FOREIGN KEY (evaluation_ai_invocation_id) REFERENCES ai_invocations (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
