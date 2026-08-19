-- 职达 Sprint 1 演示种子数据
-- 只提供基础闭环的输入数据：用户、能力证据、简历、简历版本、证据引用和岗位 JD。
-- 不预置评分、匹配、AI 建议结果，避免污染 AI 禁飞区和业务逻辑演示。

INSERT INTO users (id, display_name)
VALUES (1, 'Demo User')
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);

INSERT INTO capability_evidences (
    id,
    user_id,
    evidence_type,
    title,
    situation,
    action_text,
    result_text,
    skill_tags,
    source_note
)
VALUES (
    1,
    1,
    'project',
    '校园二手交易小程序',
    '课程实践中需要完成一个面向学生的二手物品发布与交易原型，要求支持商品浏览、发布和基础订单流转。',
    '负责商品发布、关键词搜索、收藏列表和订单状态管理模块；参与设计 RESTful API，并使用 MySQL 保存商品、收藏和订单数据。',
    '完成 8 个核心页面和主要后端接口，使用约 200 条测试商品数据完成联调演示。',
    JSON_ARRAY('Java', 'Spring Boot', 'MySQL', 'Vue3', 'RESTful API'),
    'S1 演示脱敏样例'
)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    situation = VALUES(situation),
    action_text = VALUES(action_text),
    result_text = VALUES(result_text),
    skill_tags = VALUES(skill_tags),
    source_note = VALUES(source_note),
    deleted_at = NULL;

INSERT INTO resumes (
    id,
    user_id,
    title
)
VALUES (
    1,
    1,
    '后端开发实习简历'
)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    deleted_at = NULL;

INSERT INTO resume_versions (
    id,
    resume_id,
    parent_version_id,
    version_no,
    content_json,
    change_summary,
    created_by_type
)
VALUES (
    1,
    1,
    NULL,
    1,
    JSON_OBJECT(
        'basicInfo', JSON_OBJECT(
            'name', 'Demo User',
            'targetRole', '后端开发实习生',
            'phone', '',
            'email', ''
        ),
        'education', JSON_ARRAY(
            JSON_OBJECT(
                'school', '武汉大学',
                'major', '软件工程',
                'degree', '本科',
                'period', '2024-2028'
            )
        ),
        'projects', JSON_ARRAY(
            JSON_OBJECT(
                'title', '校园二手交易小程序',
                'description', '负责商品发布、关键词搜索、收藏列表和订单状态管理模块，参与 RESTful API 设计并使用 MySQL 保存核心数据。',
                'evidenceId', 1
            )
        ),
        'skills', JSON_ARRAY('Java', 'Spring Boot', 'MySQL', 'Vue3', 'RESTful API')
    ),
    'S1 初始演示简历版本',
    'user'
)
ON DUPLICATE KEY UPDATE
    content_json = VALUES(content_json),
    change_summary = VALUES(change_summary),
    created_by_type = VALUES(created_by_type);

UPDATE resumes
SET current_version_id = 1
WHERE id = 1;

INSERT INTO resume_evidence_refs (
    id,
    resume_version_id,
    evidence_id,
    section_key
)
VALUES (
    1,
    1,
    1,
    'projects[0]'
)
ON DUPLICATE KEY UPDATE
    section_key = VALUES(section_key);

INSERT INTO job_descriptions (
    id,
    user_id,
    job_title,
    company_name,
    raw_text,
    parsed_json,
    parse_status,
    prompt_version,
    parse_ai_invocation_id
)
VALUES (
    1,
    1,
    '后端开发实习生',
    '示例科技',
    '岗位职责：参与后端业务接口开发，完成接口设计、数据库表设计和基础联调。岗位要求：熟悉 Java，了解 Spring Boot 和 MySQL，理解 RESTful API，有课程项目或实习项目经验优先；了解前端 Vue 或能够进行简单前后端联调者优先。',
    JSON_OBJECT(
        'requiredSkills', JSON_ARRAY('Java', 'Spring Boot', 'MySQL', 'RESTful API'),
        'bonusSkills', JSON_ARRAY('Vue', '前后端联调'),
        'responsibilities', JSON_ARRAY('后端业务接口开发', '接口设计', '数据库表设计', '基础联调'),
        'experienceRequirements', JSON_ARRAY('课程项目或实习项目经验'),
        'educationRequirements', JSON_ARRAY('本科在读或以上'),
        'source', 'seed'
    ),
    'succeeded',
    'seed-v1',
    NULL
)
ON DUPLICATE KEY UPDATE
    job_title = VALUES(job_title),
    company_name = VALUES(company_name),
    raw_text = VALUES(raw_text),
    parsed_json = VALUES(parsed_json),
    parse_status = VALUES(parse_status),
    prompt_version = VALUES(prompt_version),
    parse_ai_invocation_id = VALUES(parse_ai_invocation_id);
