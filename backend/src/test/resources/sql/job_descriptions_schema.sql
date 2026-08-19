-- H2-compatible schema for job_descriptions tests
CREATE TABLE IF NOT EXISTS job_descriptions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    job_title VARCHAR(200) NOT NULL,
    company_name VARCHAR(200) NULL,
    raw_text CLOB NOT NULL,
    parsed_json CLOB NULL,
    parse_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    prompt_version VARCHAR(50) NULL,
    parse_ai_invocation_id BIGINT NULL,
    source_meta_json CLOB NULL,
    job_type VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_job_descriptions_parse_status
        CHECK (parse_status IN ('pending', 'succeeded', 'failed'))
);

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
