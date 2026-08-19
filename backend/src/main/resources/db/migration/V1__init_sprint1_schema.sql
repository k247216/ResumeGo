-- 职达 Sprint 1 初始化数据库结构
-- 范围：基础用户、能力证据、简历版本、岗位 JD、评分、匹配、AI 建议和 AI 调用审计。
-- 不包含登录认证、模拟面试、PDF 导出等后续 Sprint 能力。

CREATE TABLE users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    display_name VARCHAR(100) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_invocations (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    request_id VARCHAR(64) NOT NULL,
    user_id BIGINT UNSIGNED NULL,
    feature_type VARCHAR(50) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    status VARCHAR(32) NOT NULL,
    latency_ms INT UNSIGNED NULL,
    schema_valid BOOLEAN NULL,
    input_tokens INT UNSIGNED NULL,
    output_tokens INT UNSIGNED NULL,
    error_category VARCHAR(50) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_invocations_request_id (request_id),
    KEY idx_ai_invocations_user_id (user_id),
    KEY idx_ai_invocations_feature_created_at (feature_type, created_at),
    CONSTRAINT fk_ai_invocations_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE capability_evidences (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    evidence_type VARCHAR(32) NOT NULL,
    title VARCHAR(200) NOT NULL,
    situation TEXT NULL,
    action_text TEXT NOT NULL,
    result_text TEXT NULL,
    skill_tags JSON NOT NULL,
    source_note VARCHAR(500) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    KEY idx_capability_evidences_user_id (user_id),
    KEY idx_capability_evidences_type (evidence_type),
    CONSTRAINT fk_capability_evidences_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_capability_evidences_skill_tags_array
        CHECK (JSON_TYPE(skill_tags) = 'ARRAY'),
    CONSTRAINT chk_capability_evidences_type
        CHECK (evidence_type IN ('project', 'internship', 'competition', 'skill', 'other'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE resumes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(200) NOT NULL,
    current_version_id BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    KEY idx_resumes_user_id (user_id),
    KEY idx_resumes_current_version_id (current_version_id),
    CONSTRAINT fk_resumes_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE resume_versions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    resume_id BIGINT UNSIGNED NOT NULL,
    parent_version_id BIGINT UNSIGNED NULL,
    version_no INT UNSIGNED NOT NULL,
    content_json JSON NOT NULL,
    change_summary VARCHAR(500) NULL,
    created_by_type VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_resume_versions_resume_version_no (resume_id, version_no),
    KEY idx_resume_versions_resume_id (resume_id),
    KEY idx_resume_versions_parent_version_id (parent_version_id),
    CONSTRAINT fk_resume_versions_resume
        FOREIGN KEY (resume_id) REFERENCES resumes (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_resume_versions_parent
        FOREIGN KEY (parent_version_id) REFERENCES resume_versions (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_resume_versions_created_by_type
        CHECK (created_by_type IN ('user', 'ai_suggestion', 'import'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE resumes
    ADD CONSTRAINT fk_resumes_current_version
        FOREIGN KEY (current_version_id) REFERENCES resume_versions (id)
        ON DELETE SET NULL;

CREATE TABLE resume_evidence_refs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    resume_version_id BIGINT UNSIGNED NOT NULL,
    evidence_id BIGINT UNSIGNED NOT NULL,
    section_key VARCHAR(100) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_resume_evidence_refs_version_evidence_section (resume_version_id, evidence_id, section_key),
    KEY idx_resume_evidence_refs_evidence_id (evidence_id),
    CONSTRAINT fk_resume_evidence_refs_version
        FOREIGN KEY (resume_version_id) REFERENCES resume_versions (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_resume_evidence_refs_evidence
        FOREIGN KEY (evidence_id) REFERENCES capability_evidences (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE job_descriptions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    job_title VARCHAR(200) NOT NULL,
    company_name VARCHAR(200) NULL,
    raw_text MEDIUMTEXT NOT NULL,
    parsed_json JSON NULL,
    parse_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    prompt_version VARCHAR(50) NULL,
    parse_ai_invocation_id BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_job_descriptions_user_id (user_id),
    KEY idx_job_descriptions_parse_ai_invocation_id (parse_ai_invocation_id),
    CONSTRAINT fk_job_descriptions_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_job_descriptions_parse_ai_invocation
        FOREIGN KEY (parse_ai_invocation_id) REFERENCES ai_invocations (id)
        ON DELETE SET NULL,
    CONSTRAINT chk_job_descriptions_parse_status
        CHECK (parse_status IN ('pending', 'succeeded', 'failed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE resume_assessments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    resume_version_id BIGINT UNSIGNED NOT NULL,
    rule_version VARCHAR(50) NOT NULL,
    total_score DECIMAL(5,2) NOT NULL,
    dimension_scores JSON NOT NULL,
    deductions JSON NOT NULL,
    input_fingerprint CHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_resume_assessments_input (resume_version_id, rule_version, input_fingerprint),
    KEY idx_resume_assessments_resume_version_id (resume_version_id),
    CONSTRAINT fk_resume_assessments_version
        FOREIGN KEY (resume_version_id) REFERENCES resume_versions (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_resume_assessments_total_score
        CHECK (total_score >= 0 AND total_score <= 100),
    CONSTRAINT chk_resume_assessments_dimension_scores_object
        CHECK (JSON_TYPE(dimension_scores) = 'OBJECT'),
    CONSTRAINT chk_resume_assessments_deductions_array
        CHECK (JSON_TYPE(deductions) = 'ARRAY')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE job_matches (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    resume_version_id BIGINT UNSIGNED NOT NULL,
    job_description_id BIGINT UNSIGNED NOT NULL,
    algorithm_version VARCHAR(50) NOT NULL,
    match_score DECIMAL(5,2) NOT NULL,
    details_json JSON NOT NULL,
    input_fingerprint CHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_job_matches_input (resume_version_id, job_description_id, algorithm_version, input_fingerprint),
    KEY idx_job_matches_resume_version_id (resume_version_id),
    KEY idx_job_matches_job_description_id (job_description_id),
    CONSTRAINT fk_job_matches_version
        FOREIGN KEY (resume_version_id) REFERENCES resume_versions (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_job_matches_job_description
        FOREIGN KEY (job_description_id) REFERENCES job_descriptions (id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_job_matches_match_score
        CHECK (match_score >= 0 AND match_score <= 100),
    CONSTRAINT chk_job_matches_details_object
        CHECK (JSON_TYPE(details_json) = 'OBJECT')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE optimization_suggestions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    job_match_id BIGINT UNSIGNED NOT NULL,
    resume_version_id BIGINT UNSIGNED NOT NULL,
    evidence_id BIGINT UNSIGNED NULL,
    section_key VARCHAR(100) NOT NULL,
    original_text TEXT NOT NULL,
    suggested_text TEXT NULL,
    reason_text TEXT NOT NULL,
    target_requirement TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    prompt_version VARCHAR(50) NOT NULL,
    generation_ai_invocation_id BIGINT UNSIGNED NOT NULL,
    accepted_version_id BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    decided_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    KEY idx_optimization_suggestions_job_match_id (job_match_id),
    KEY idx_optimization_suggestions_resume_version_id (resume_version_id),
    KEY idx_optimization_suggestions_evidence_id (evidence_id),
    KEY idx_optimization_suggestions_generation_ai_invocation_id (generation_ai_invocation_id),
    KEY idx_optimization_suggestions_accepted_version_id (accepted_version_id),
    CONSTRAINT fk_optimization_suggestions_job_match
        FOREIGN KEY (job_match_id) REFERENCES job_matches (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_optimization_suggestions_resume_version
        FOREIGN KEY (resume_version_id) REFERENCES resume_versions (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_optimization_suggestions_evidence
        FOREIGN KEY (evidence_id) REFERENCES capability_evidences (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_optimization_suggestions_generation_ai_invocation
        FOREIGN KEY (generation_ai_invocation_id) REFERENCES ai_invocations (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_optimization_suggestions_accepted_version
        FOREIGN KEY (accepted_version_id) REFERENCES resume_versions (id)
        ON DELETE SET NULL,
    CONSTRAINT chk_optimization_suggestions_status
        CHECK (status IN ('pending', 'accepted', 'rejected', 'evidence_required')),
    CONSTRAINT chk_optimization_suggestions_evidence_required
        CHECK (
            (status = 'evidence_required' AND suggested_text IS NULL)
            OR status <> 'evidence_required'
        )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO users (id, display_name)
VALUES (1, 'Demo User')
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);
