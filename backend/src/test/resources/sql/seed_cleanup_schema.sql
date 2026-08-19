DROP ALL OBJECTS;

CREATE TABLE users (id BIGINT PRIMARY KEY, display_name VARCHAR(100) NOT NULL);
CREATE TABLE capability_evidences (id BIGINT PRIMARY KEY, source_note VARCHAR(500));
CREATE TABLE resumes (id BIGINT PRIMARY KEY, current_version_id BIGINT, target_job_description_id BIGINT);
CREATE TABLE resume_versions (id BIGINT PRIMARY KEY, resume_id BIGINT NOT NULL);
CREATE TABLE resume_evidence_refs (id BIGINT PRIMARY KEY, resume_version_id BIGINT NOT NULL, evidence_id BIGINT NOT NULL);
CREATE TABLE job_descriptions (id BIGINT PRIMARY KEY, prompt_version VARCHAR(50));
CREATE TABLE resume_assessments (id BIGINT PRIMARY KEY, resume_version_id BIGINT NOT NULL);
CREATE TABLE job_matches (id BIGINT PRIMARY KEY, resume_version_id BIGINT NOT NULL, job_description_id BIGINT NOT NULL);
CREATE TABLE optimization_suggestions (
    id BIGINT PRIMARY KEY,
    job_match_id BIGINT NOT NULL,
    resume_version_id BIGINT NOT NULL,
    accepted_version_id BIGINT
);
CREATE TABLE interview_plans (
    id BIGINT PRIMARY KEY,
    resume_version_id BIGINT NOT NULL,
    job_description_id BIGINT NOT NULL
);
CREATE TABLE interview_sessions (
    id BIGINT PRIMARY KEY,
    plan_id BIGINT,
    resume_version_id BIGINT NOT NULL,
    job_description_id BIGINT NOT NULL
);
CREATE TABLE interview_questions (id BIGINT PRIMARY KEY, session_id BIGINT NOT NULL);
CREATE TABLE interview_answers (id BIGINT PRIMARY KEY, session_id BIGINT NOT NULL, question_id BIGINT NOT NULL);
CREATE TABLE interview_evaluations (
    id BIGINT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    answer_id BIGINT NOT NULL
);
CREATE TABLE interview_growth_snapshots (
    id BIGINT PRIMARY KEY,
    resume_id BIGINT NOT NULL,
    resume_version_id BIGINT NOT NULL,
    job_description_id BIGINT NOT NULL,
    interview_plan_id BIGINT NOT NULL
);

INSERT INTO users VALUES (1, 'Demo User');

INSERT INTO capability_evidences VALUES (1, 'S1 演示脱敏样例');
INSERT INTO capability_evidences VALUES (50, '用户手动录入');

INSERT INTO resumes VALUES (1, 1, 10);
INSERT INTO resumes VALUES (50, 50, 50);
INSERT INTO resume_versions VALUES (1, 1);
INSERT INTO resume_versions VALUES (50, 50);
INSERT INTO resume_evidence_refs VALUES (1, 1, 1);
INSERT INTO resume_evidence_refs VALUES (50, 50, 50);

INSERT INTO job_descriptions VALUES (10, 'manual-import-v1');
INSERT INTO job_descriptions VALUES (50, NULL);

INSERT INTO resume_assessments VALUES (1, 1);
INSERT INTO resume_assessments VALUES (50, 50);
INSERT INTO job_matches VALUES (1, 1, 10);
INSERT INTO job_matches VALUES (50, 50, 50);
INSERT INTO optimization_suggestions VALUES (1, 1, 1, NULL);
INSERT INTO optimization_suggestions VALUES (50, 50, 50, NULL);

INSERT INTO interview_plans VALUES (1, 1, 10);
INSERT INTO interview_plans VALUES (50, 50, 50);
INSERT INTO interview_sessions VALUES (1, 1, 1, 10);
INSERT INTO interview_sessions VALUES (50, 50, 50, 50);
INSERT INTO interview_questions VALUES (1, 1);
INSERT INTO interview_questions VALUES (50, 50);
INSERT INTO interview_answers VALUES (1, 1, 1);
INSERT INTO interview_answers VALUES (50, 50, 50);
INSERT INTO interview_evaluations VALUES (1, 1, 1, 1);
INSERT INTO interview_evaluations VALUES (50, 50, 50, 50);
INSERT INTO interview_growth_snapshots VALUES (1, 1, 1, 10, 1);
INSERT INTO interview_growth_snapshots VALUES (50, 50, 50, 50, 50);
