-- H2-compatible schema for ai_invocations tests
CREATE TABLE IF NOT EXISTS ai_invocations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_id VARCHAR(64) NOT NULL,
    user_id BIGINT NULL,
    feature_type VARCHAR(50) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    status VARCHAR(32) NOT NULL,
    latency_ms INT NULL,
    schema_valid BOOLEAN NULL,
    input_tokens INT NULL,
    output_tokens INT NULL,
    error_category VARCHAR(50) NULL,
    prompt_summary VARCHAR(200) NULL,
    response_summary VARCHAR(200) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
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
