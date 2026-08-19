-- H2-compatible schema and seed data for job_matches tests
-- Covers: AC-01~06, 5 fixed samples, B-01~10, I-01~04, V-01~04
-- Uses H2 CLOB for JSON columns; CHECK constraints relaxed for H2 compatibility

DROP TABLE IF EXISTS job_matches;
DROP TABLE IF EXISTS resume_versions;
DROP TABLE IF EXISTS resumes;
DROP TABLE IF EXISTS job_descriptions;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    display_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE resumes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    current_version_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE resume_versions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    resume_id BIGINT NOT NULL,
    parent_version_id BIGINT NULL,
    version_no INT NOT NULL,
    content_json CLOB NOT NULL,
    change_summary VARCHAR(500) NULL,
    created_by_type VARCHAR(32) NOT NULL DEFAULT 'user',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (resume_id) REFERENCES resumes(id)
);

CREATE TABLE job_descriptions (
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
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE job_matches (
    id BIGINT NOT NULL AUTO_INCREMENT,
    resume_version_id BIGINT NOT NULL,
    job_description_id BIGINT NOT NULL,
    algorithm_version VARCHAR(50) NOT NULL,
    match_score INT NOT NULL,
    details_json CLOB NOT NULL,
    input_fingerprint CHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_job_matches_input (resume_version_id, job_description_id, algorithm_version, input_fingerprint),
    FOREIGN KEY (resume_version_id) REFERENCES resume_versions(id),
    FOREIGN KEY (job_description_id) REFERENCES job_descriptions(id)
);

-- ============================================================
-- 用户
-- ============================================================
INSERT INTO users (id, display_name) VALUES (1, 'Demo User');
INSERT INTO users (id, display_name) VALUES (999, 'Other User');

-- ============================================================
-- 简历版本种子数据
-- ============================================================

-- r1/v1: 正常简历（Java 后端方向）—— AC 验收 & 样例 1 基准
INSERT INTO resumes (id, user_id, title, current_version_id) VALUES (1, 1, '后端开发简历', 1);
INSERT INTO resume_versions (id, resume_id, version_no, content_json, created_by_type) VALUES (
    1, 1, 1,
    '{"skills":["Java","Spring Boot","MySQL","Git"],"education":[{"school":"武汉大学","major":"软件工程","degree":"本科"}],"projects":[{"title":"校园二手交易小程序","description":"负责商品发布、搜索、订单管理模块，使用 Spring Boot + MySQL"}]}',
    'user'
);
UPDATE resumes SET current_version_id = 1 WHERE id = 1;

-- r2/v2: 空技能简历 —— B-01
INSERT INTO resumes (id, user_id, title, current_version_id) VALUES (2, 1, '空技能简历', 2);
INSERT INTO resume_versions (id, resume_id, version_no, content_json, created_by_type) VALUES (
    2, 2, 1,
    '{"skills":[],"education":[{"school":"武汉大学","major":"软件工程","degree":"本科"}],"projects":[]}',
    'user'
);
UPDATE resumes SET current_version_id = 2 WHERE id = 2;

-- r3/v3: 空教育简历 —— B-06
INSERT INTO resumes (id, user_id, title, current_version_id) VALUES (3, 1, '空教育简历', 3);
INSERT INTO resume_versions (id, resume_id, version_no, content_json, created_by_type) VALUES (
    3, 3, 1,
    '{"skills":["Java","MySQL"],"education":[],"projects":[]}',
    'user'
);
UPDATE resumes SET current_version_id = 3 WHERE id = 3;

-- r4/v4: 空经历简历 —— B-03
INSERT INTO resumes (id, user_id, title, current_version_id) VALUES (4, 1, '空经历简历', 4);
INSERT INTO resume_versions (id, resume_id, version_no, content_json, created_by_type) VALUES (
    4, 4, 1,
    '{"skills":["Java","Spring Boot","MySQL"],"education":[{"school":"武汉大学","major":"软件工程","degree":"本科"}],"projects":[]}',
    'user'
);
UPDATE resumes SET current_version_id = 4 WHERE id = 4;

-- r5/v5: 别名技能简历 —— §5.1 样例 5
INSERT INTO resumes (id, user_id, title, current_version_id) VALUES (5, 1, '别名技能简历', 5);
INSERT INTO resume_versions (id, resume_id, version_no, content_json, created_by_type) VALUES (
    5, 5, 1,
    '{"skills":["K8s","Node.js","React.js","PostgreSQL"],"education":[{"school":"武汉大学","major":"计算机科学","degree":"本科"}],"projects":[{"title":"全栈项目","description":"4 年全栈开发经验"}]}',
    'user'
);
UPDATE resumes SET current_version_id = 5 WHERE id = 5;

-- r6/v6: 其他用户的简历 —— V-04
INSERT INTO resumes (id, user_id, title, current_version_id) VALUES (6, 999, '其他用户简历', 6);
INSERT INTO resume_versions (id, resume_id, version_no, content_json, created_by_type) VALUES (
    6, 6, 1,
    '{"skills":["Go","Python"],"education":[],"projects":[]}',
    'user'
);
UPDATE resumes SET current_version_id = 6 WHERE id = 6;

-- r7/v7: 大小写/空白技能简历 —— B-08
INSERT INTO resumes (id, user_id, title, current_version_id) VALUES (7, 1, '大小写简历', 7);
INSERT INTO resume_versions (id, resume_id, version_no, content_json, created_by_type) VALUES (
    7, 7, 1,
    '{"skills":["  JAVA  ","spring boot","mysql"],"education":[],"projects":[]}',
    'user'
);
UPDATE resumes SET current_version_id = 7 WHERE id = 7;

-- ============================================================
-- JD 种子数据
-- ============================================================

-- jd1: 正常 JD（后端开发实习生）—— AC 验收基准
INSERT INTO job_descriptions (id, user_id, job_title, raw_text, parsed_json, parse_status) VALUES (
    1, 1, '后端开发实习生',
    '岗位要求：熟悉 Java、Spring Boot、MySQL',
    '{"requiredSkills":["Java","Spring Boot","MySQL"],"preferredSkills":["Redis","Docker"],"responsibilities":["后端接口开发"],"experienceRequirements":["有项目经验"],"educationRequirements":["本科及以上"]}',
    'succeeded'
);

-- jd2: 全空要求 JD —— B-02
INSERT INTO job_descriptions (id, user_id, job_title, raw_text, parsed_json, parse_status) VALUES (
    2, 1, '全空要求JD',
    '无具体要求。',
    '{"requiredSkills":[],"preferredSkills":[],"responsibilities":[],"experienceRequirements":[],"educationRequirements":[]}',
    'succeeded'
);

-- jd3: 未解析 JD —— V-03
INSERT INTO job_descriptions (id, user_id, job_title, raw_text, parsed_json, parse_status) VALUES (
    3, 1, '未解析JD',
    '待解析的 JD 正文，至少 20 字。',
    NULL,
    'pending'
);

-- jd4: 5 个必备技能 JD —— B-04/B-05 边界（r1 覆盖 3/5 = 60%）
INSERT INTO job_descriptions (id, user_id, job_title, raw_text, parsed_json, parse_status) VALUES (
    4, 1, '高级后端开发',
    '要求：Java, Spring Boot, MySQL, Redis, Docker',
    '{"requiredSkills":["Java","Spring Boot","MySQL","Redis","Docker"],"preferredSkills":[],"responsibilities":[],"experienceRequirements":[],"educationRequirements":[]}',
    'succeeded'
);

-- jd5: 别名技能 JD —— §5.1 样例 5
INSERT INTO job_descriptions (id, user_id, job_title, raw_text, parsed_json, parse_status) VALUES (
    5, 1, '云原生开发工程师',
    '要求：Kubernetes, Node, React, Postgres',
    '{"requiredSkills":["Kubernetes","Node","React","Postgres"],"preferredSkills":["TypeScript","Docker"],"responsibilities":["云原生应用开发"],"experienceRequirements":["3 年以上全栈开发"],"educationRequirements":["本科及以上","计算机相关"]}',
    'succeeded'
);

-- jd6: 无法解析的学历要求 —— B-07
INSERT INTO job_descriptions (id, user_id, job_title, raw_text, parsed_json, parse_status) VALUES (
    6, 1, '特殊学历JD',
    '学历要求：优秀院校优先',
    '{"requiredSkills":["Java"],"preferredSkills":[],"responsibilities":[],"experienceRequirements":[],"educationRequirements":["优秀院校优先"]}',
    'succeeded'
);

-- jd7: 含重复技能 JD —— B-09（JSON 层面去重由算法处理）
INSERT INTO job_descriptions (id, user_id, job_title, raw_text, parsed_json, parse_status) VALUES (
    7, 1, '重复技能JD',
    '要求：Go, Go, Go',
    '{"requiredSkills":["Go","Go"],"preferredSkills":[],"responsibilities":[],"experienceRequirements":[],"educationRequirements":[]}',
    'succeeded'
);

-- jd8: 应届生 JD —— fresh grad 测试
INSERT INTO job_descriptions (id, user_id, job_title, raw_text, parsed_json, parse_status) VALUES (
    8, 1, '应届生JD',
    '面向应届毕业生，要求 Java',
    '{"requiredSkills":["Java"],"preferredSkills":[],"responsibilities":[],"experienceRequirements":["应届生"],"educationRequirements":["本科及以上"]}',
    'succeeded'
);

-- r8/v8: 今年毕业的应届生简历 —— fresh grad positive
INSERT INTO resumes (id, user_id, title, current_version_id) VALUES (8, 1, '应届生简历', 8);
INSERT INTO resume_versions (id, resume_id, version_no, content_json, created_by_type) VALUES (
    8, 8, 1,
    '{"skills":["Java"],"education":[{"school":"武汉大学","major":"软件工程","degree":"本科","period":"2022-2026"}],"projects":[{"title":"毕业设计","description":"基于Spring Boot的在线商城"}]}',
    'user'
);
UPDATE resumes SET current_version_id = 8 WHERE id = 8;

-- r9/v9: 非应届生（毕业年份不是今年）
INSERT INTO resumes (id, user_id, title, current_version_id) VALUES (9, 1, '非应届生简历', 9);
INSERT INTO resume_versions (id, resume_id, version_no, content_json, created_by_type) VALUES (
    9, 9, 1,
    '{"skills":["Java"],"education":[{"school":"武汉大学","major":"软件工程","degree":"本科","period":"2020-2024"}],"projects":[{"title":"企业项目","description":"3年Java后端开发经验"}]}',
    'user'
);
UPDATE resumes SET current_version_id = 9 WHERE id = 9;

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
