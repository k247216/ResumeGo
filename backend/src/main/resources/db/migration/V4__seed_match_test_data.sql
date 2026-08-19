-- 职达 Sprint 1 阶段 C 种子数据：岗位匹配测试记录
-- 为 optimization_suggestions 模块提供测试数据基础

-- ============================================================
-- 匹配记录 1：全栈简历 vs 全栈 JD
-- 简历版本 3（全栈开发实习简历）→ JD 2（全栈开发实习生）
-- ============================================================
INSERT INTO job_matches (
    id, resume_version_id, job_description_id, algorithm_version,
    match_score, details_json, input_fingerprint
) VALUES (
    1, 3, 2, 'v1.0',
    82.00,
    JSON_OBJECT(
        'requiredCoverage', JSON_OBJECT(
            'matched', JSON_ARRAY('Java', 'Spring Boot', 'Vue3', 'MySQL'),
            'missing', JSON_ARRAY(),
            'total', 4,
            'rate', 1.0
        ),
        'bonusCoverage', JSON_OBJECT(
            'matched', JSON_ARRAY('Docker', 'Linux'),
            'missing', JSON_ARRAY('CI/CD', 'React'),
            'total', 4,
            'rate', 0.5
        ),
        'gaps', JSON_ARRAY(
            JSON_OBJECT('type', 'bonus_skill', 'item', 'CI/CD', 'severity', 'low'),
            JSON_OBJECT('type', 'bonus_skill', 'item', 'React', 'severity', 'low')
        ),
        'extraSkills', JSON_ARRAY('MyBatis-Plus', 'Nginx'),
        'experienceMatch', 'partial',
        'educationMatch', 'match'
    ),
    '0000000000000000000000000000000000000000000000000000000000000001'
) ON DUPLICATE KEY UPDATE
    match_score = VALUES(match_score),
    details_json = VALUES(details_json);

-- ============================================================
-- 匹配记录 2：前端简历 vs 后端 JD
-- 简历版本 2（前端开发实习简历）→ JD 1（后端开发实习生）
-- ============================================================
INSERT INTO job_matches (
    id, resume_version_id, job_description_id, algorithm_version,
    match_score, details_json, input_fingerprint
) VALUES (
    2, 2, 1, 'v1.0',
    35.00,
    JSON_OBJECT(
        'requiredCoverage', JSON_OBJECT(
            'matched', JSON_ARRAY(),
            'missing', JSON_ARRAY('Java', 'Spring Boot', 'MySQL'),
            'total', 3,
            'rate', 0.0
        ),
        'bonusCoverage', JSON_OBJECT(
            'matched', JSON_ARRAY(),
            'missing', JSON_ARRAY('Redis', 'Docker'),
            'total', 2,
            'rate', 0.0
        ),
        'gaps', JSON_ARRAY(
            JSON_OBJECT('type', 'required_skill', 'item', 'Java', 'severity', 'high'),
            JSON_OBJECT('type', 'required_skill', 'item', 'Spring Boot', 'severity', 'high'),
            JSON_OBJECT('type', 'required_skill', 'item', 'MySQL', 'severity', 'high'),
            JSON_OBJECT('type', 'bonus_skill', 'item', 'Redis', 'severity', 'low'),
            JSON_OBJECT('type', 'bonus_skill', 'item', 'Docker', 'severity', 'low')
        ),
        'extraSkills', JSON_ARRAY('Vue3', 'TypeScript', 'Element Plus', 'Pinia', 'RESTful API'),
        'experienceMatch', 'none',
        'educationMatch', 'match'
    ),
    '0000000000000000000000000000000000000000000000000000000000000002'
) ON DUPLICATE KEY UPDATE
    match_score = VALUES(match_score),
    details_json = VALUES(details_json);