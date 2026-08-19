-- 测试用 ai_invocations 表（如果不存在）
CREATE TABLE IF NOT EXISTS ai_invocations (
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
    prompt_summary VARCHAR(200) NULL,
    response_summary VARCHAR(200) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_invocations_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 测试用 users 表（如果不存在）
CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    display_name VARCHAR(100) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 测试用 resumes 表（如果不存在）
CREATE TABLE IF NOT EXISTS resumes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(200) NOT NULL,
    current_version_id BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 测试用 resume_versions 表（如果不存在）
CREATE TABLE IF NOT EXISTS resume_versions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    resume_id BIGINT UNSIGNED NOT NULL,
    parent_version_id BIGINT UNSIGNED NULL,
    version_no INT UNSIGNED NOT NULL,
    content_json JSON NOT NULL,
    change_summary VARCHAR(500) NULL,
    created_by_type VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 测试用 capability_evidences 表（如果不存在）
CREATE TABLE IF NOT EXISTS capability_evidences (
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
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 测试用 resume_evidence_refs 表（如果不存在）
CREATE TABLE IF NOT EXISTS resume_evidence_refs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    resume_version_id BIGINT UNSIGNED NOT NULL,
    evidence_id BIGINT UNSIGNED NOT NULL,
    section_key VARCHAR(100) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 测试用 job_descriptions 表（如果不存在）
CREATE TABLE IF NOT EXISTS job_descriptions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    job_title VARCHAR(200) NOT NULL,
    company_name VARCHAR(200) NULL,
    raw_text MEDIUMTEXT NOT NULL,
    parsed_json JSON NULL,
    parse_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    prompt_version VARCHAR(50) NULL,
    parse_ai_invocation_id BIGINT UNSIGNED NULL,
    source_meta_json JSON NULL,
    job_type VARCHAR(50) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 测试用 job_matches 表（如果不存在）
CREATE TABLE IF NOT EXISTS job_matches (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    resume_version_id BIGINT UNSIGNED NOT NULL,
    job_description_id BIGINT UNSIGNED NOT NULL,
    algorithm_version VARCHAR(50) NOT NULL,
    match_score DECIMAL(5,2) NOT NULL,
    details_json JSON NOT NULL,
    input_fingerprint CHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 测试用 optimization_suggestions 表（如果不存在）
CREATE TABLE IF NOT EXISTS optimization_suggestions (
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
    risk_level VARCHAR(32) NULL,
    prompt_version VARCHAR(50) NOT NULL,
    generation_ai_invocation_id BIGINT UNSIGNED NOT NULL,
    accepted_version_id BIGINT UNSIGNED NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    decided_at DATETIME(3) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 插入测试用户
INSERT IGNORE INTO users (id, display_name) VALUES (1, 'Demo User');

-- 插入测试简历和版本
INSERT IGNORE INTO resumes (id, user_id, title) VALUES (99, 1, 'Test Resume');
INSERT IGNORE INTO resume_versions (id, resume_id, version_no, content_json, created_by_type)
VALUES (99, 99, 1, '{"skills": ["Java", "Spring Boot"], "projects": []}', 'user');

-- 插入测试 JD
INSERT IGNORE INTO job_descriptions (id, user_id, job_title, raw_text, parsed_json, parse_status)
VALUES (99, 1, 'Test JD', 'Test JD raw text', '{"requiredSkills": ["Java"]}', 'succeeded');

-- 插入测试匹配记录
INSERT IGNORE INTO job_matches (id, resume_version_id, job_description_id, algorithm_version,
    match_score, details_json, input_fingerprint)
VALUES (99, 99, 99, 'v1.0', 85.00,
    '{"requiredCoverage":{"matched":["Java"],"missing":["Spring Boot"],"total":2,"rate":0.5},"gaps":[{"type":"required_skill","item":"Spring Boot","severity":"high"}]}',
    'test-fingerprint-99');

CREATE TABLE IF NOT EXISTS interview_personas (
    id BIGINT AUTO_INCREMENT,
    name VARCHAR(20) NOT NULL,
    title VARCHAR(50) NOT NULL,
    style VARCHAR(200) NOT NULL,
    avatar VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'preset',
    user_id BIGINT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
