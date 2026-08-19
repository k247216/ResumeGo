-- 职达 Sprint 1 补充演示数据
-- 增加多条能力证据和简历，丰富前端演示场景

-- ============================================================
-- 能力证据 2：实习经历
-- ============================================================
INSERT INTO capability_evidences (
    id, user_id, evidence_type, title, situation, action_text, result_text, skill_tags, source_note
) VALUES (
    2, 1, 'internship',
    '某科技公司后端开发实习',
    '暑期实习期间加入支付结算团队，团队负责日均百万级交易订单的后台处理。',
    '参与账单对账接口开发，使用 Spring Boot 重构旧有对账逻辑，编写单元测试覆盖主要异常路径，协助排查线上数据库慢查询并添加索引。',
    '对账接口平均耗时由 3.2s 降至 0.8s，完整覆盖 12 类异常场景的自动化测试。',
    JSON_ARRAY('Java', 'Spring Boot', 'MySQL', 'JUnit', 'Redis'),
    'S1 演示脱敏样例'
) ON DUPLICATE KEY UPDATE
    evidence_type = VALUES(evidence_type),
    title = VALUES(title), situation = VALUES(situation), action_text = VALUES(action_text),
    result_text = VALUES(result_text), skill_tags = VALUES(skill_tags), source_note = VALUES(source_note), deleted_at = NULL;

-- ============================================================
-- 能力证据 3：竞赛经历
-- ============================================================
INSERT INTO capability_evidences (
    id, user_id, evidence_type, title, situation, action_text, result_text, skill_tags, source_note
) VALUES (
    3, 1, 'competition',
    '全国大学生软件创新大赛',
    '三人团队参加省级软件创新大赛，自选题目为"基于 AI 的校园助手"，要求在 48 小时内完成原型开发和演示答辩。',
    '担任后端开发，使用 Spring Boot 搭建 API 服务，集成通义千问 API 实现课表问答和教室推荐功能；使用 Docker Compose 统一团队开发环境。',
    '获省级二等奖，完成 6 个核心 API 并现场通过评委随机测试。',
    JSON_ARRAY('Java', 'Spring Boot', 'Docker', 'RESTful API', 'AI集成'),
    'S1 演示脱敏样例'
) ON DUPLICATE KEY UPDATE
    evidence_type = VALUES(evidence_type),
    title = VALUES(title), situation = VALUES(situation), action_text = VALUES(action_text),
    result_text = VALUES(result_text), skill_tags = VALUES(skill_tags), source_note = VALUES(source_note), deleted_at = NULL;

-- ============================================================
-- 能力证据 4：技能标签
-- ============================================================
INSERT INTO capability_evidences (
    id, user_id, evidence_type, title, situation, action_text, result_text, skill_tags, source_note
) VALUES (
    4, 1, 'skill',
    '前端开发基础能力',
    '自学 Vue3 和 TypeScript，完成课程中的前端项目作业。',
    '独立完成三个课程项目的 Web 前端页面开发，使用 Vue3 + Element Plus 实现列表、表单和状态管理。',
    NULL,
    JSON_ARRAY('Vue3', 'TypeScript', 'Element Plus', 'Pinia'),
    'S1 演示脱敏样例'
) ON DUPLICATE KEY UPDATE
    evidence_type = VALUES(evidence_type),
    title = VALUES(title), situation = VALUES(situation), action_text = VALUES(action_text),
    result_text = VALUES(result_text), skill_tags = VALUES(skill_tags), source_note = VALUES(source_note), deleted_at = NULL;

-- ============================================================
-- 简历 2：前端开发岗 + 版本
-- ============================================================
INSERT INTO resumes (id, user_id, title)
VALUES (2, 1, '前端开发实习简历')
ON DUPLICATE KEY UPDATE title = VALUES(title), deleted_at = NULL;

INSERT INTO resume_versions (
    id, resume_id, parent_version_id, version_no, content_json, change_summary, created_by_type
) VALUES (
    2, 2, NULL, 1,
    JSON_OBJECT(
        'basicInfo', JSON_OBJECT(
            'name', 'Demo User',
            'targetRole', '前端开发实习生',
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
                'description', '负责前端商品发布页面、收藏列表和搜索结果渲染，使用 Vue3 + Element Plus 完成 8 个页面开发。',
                'evidenceId', 1
            ),
            JSON_OBJECT(
                'title', 'AI 校园助手原型',
                'description', '负责前端原型页面搭建，使用 Vue3 + Pinia 管理对话状态，调用后端 AI API 实现实时问答交互。',
                'evidenceId', 3
            )
        ),
        'skills', JSON_ARRAY('Vue3', 'TypeScript', 'Element Plus', 'Pinia', 'RESTful API')
    ),
    'S1 补充演示简历 — 前端方向',
    'user'
) ON DUPLICATE KEY UPDATE
    content_json = VALUES(content_json), change_summary = VALUES(change_summary), created_by_type = VALUES(created_by_type);

UPDATE resumes SET current_version_id = 2 WHERE id = 2;

-- ============================================================
-- 证据引用：简历 2 关联到证据 1 和证据 3
-- ============================================================
INSERT INTO resume_evidence_refs (id, resume_version_id, evidence_id, section_key)
VALUES (2, 2, 1, 'projects[0]')
ON DUPLICATE KEY UPDATE section_key = VALUES(section_key);

INSERT INTO resume_evidence_refs (id, resume_version_id, evidence_id, section_key)
VALUES (3, 2, 3, 'projects[1]')
ON DUPLICATE KEY UPDATE section_key = VALUES(section_key);

-- ============================================================
-- 能力证据 5：开源贡献项目
-- ============================================================
INSERT INTO capability_evidences (
    id, user_id, evidence_type, title, situation, action_text, result_text, skill_tags, source_note
) VALUES (
    5, 1, 'project',
    '校园活动报名系统',
    '学生社团需要一套支持活动发布、在线报名和签到核销的管理工具，要求支持移动端适配和批量导出。',
    '独立完成全栈开发，后端使用 Spring Boot + MyBatis-Plus 实现报名和签到 API，前端使用 Vue3 + Element Plus 完成管理后台和移动端 H5 页面，部署于 Linux 服务器并通过 Nginx 反向代理。',
    '系统稳定支撑校内 23 场活动，累计处理报名 1200 余人次，活动负责人可一键导出签到表。',
    JSON_ARRAY('Java', 'Spring Boot', 'MyBatis-Plus', 'Vue3', 'MySQL', 'Nginx', 'Linux'),
    'S1 演示脱敏样例'
) ON DUPLICATE KEY UPDATE
    evidence_type = VALUES(evidence_type),
    title = VALUES(title), situation = VALUES(situation), action_text = VALUES(action_text),
    result_text = VALUES(result_text), skill_tags = VALUES(skill_tags), source_note = VALUES(source_note), deleted_at = NULL;

-- ============================================================
-- JD 2：全栈开发岗
-- ============================================================
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status, prompt_version, parse_ai_invocation_id
) VALUES (
    2, 1,
    '全栈开发实习生',
    '创新科技',
    '岗位职责：参与内部运营工具的 Web 端开发，包括后端 API 实现、管理后台前端页面、数据库建模和基础运维部署。岗位要求：熟悉 Java 和 Spring Boot，掌握 Vue3 或 React 等主流前端框架，了解 MySQL 数据库设计和 SQL 优化，有 Linux 基础操作和项目部署经验者优先；了解 Docker 或 CI/CD 流程者加分。',
    JSON_OBJECT(
        'requiredSkills', JSON_ARRAY('Java', 'Spring Boot', 'Vue3', 'MySQL'),
        'bonusSkills', JSON_ARRAY('Docker', 'CI/CD', 'Linux', 'React'),
        'responsibilities', JSON_ARRAY('后端 API 实现', '管理后台前端开发', '数据库建模', '基础运维部署'),
        'experienceRequirements', JSON_ARRAY('全栈项目经验', 'Linux 基础操作'),
        'educationRequirements', JSON_ARRAY('本科在读或以上'),
        'source', 'seed'
    ),
    'succeeded', 'seed-v1', NULL
) ON DUPLICATE KEY UPDATE
    job_title = VALUES(job_title), company_name = VALUES(company_name), raw_text = VALUES(raw_text),
    parsed_json = VALUES(parsed_json), parse_status = VALUES(parse_status), prompt_version = VALUES(prompt_version);

-- ============================================================
-- 简历 3：全栈开发岗 + 版本
-- ============================================================
INSERT INTO resumes (id, user_id, title)
VALUES (3, 1, '全栈开发实习简历')
ON DUPLICATE KEY UPDATE title = VALUES(title), deleted_at = NULL;

INSERT INTO resume_versions (
    id, resume_id, parent_version_id, version_no, content_json, change_summary, created_by_type
) VALUES (
    3, 3, NULL, 1,
    JSON_OBJECT(
        'basicInfo', JSON_OBJECT(
            'name', 'Demo User',
            'targetRole', '全栈开发实习生',
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
            ),
            JSON_OBJECT(
                'title', '校园活动报名系统',
                'description', '全栈独立开发，后端 Spring Boot + MyBatis-Plus 实现报名签到 API，前端 Vue3 + Element Plus 完成管理后台和移动端 H5，部署于 Linux 并通过 Nginx 反向代理。',
                'evidenceId', 5
            ),
            JSON_OBJECT(
                'title', 'AI 校园助手原型',
                'description', '后端使用 Spring Boot 搭建 API 服务，集成通义千问 API；前端使用 Vue3 + Pinia 管理对话状态，实现实时问答交互。',
                'evidenceId', 3
            )
        ),
        'skills', JSON_ARRAY('Java', 'Spring Boot', 'Vue3', 'MySQL', 'MyBatis-Plus', 'Nginx', 'Linux', 'Docker')
    ),
    'S1 补充演示简历 — 全栈方向',
    'user'
) ON DUPLICATE KEY UPDATE
    content_json = VALUES(content_json), change_summary = VALUES(change_summary), created_by_type = VALUES(created_by_type);

UPDATE resumes SET current_version_id = 3 WHERE id = 3;

-- ============================================================
-- 证据引用：简历 3 关联到证据 1、5、3
-- ============================================================
INSERT INTO resume_evidence_refs (id, resume_version_id, evidence_id, section_key)
VALUES (4, 3, 1, 'projects[0]')
ON DUPLICATE KEY UPDATE section_key = VALUES(section_key);

INSERT INTO resume_evidence_refs (id, resume_version_id, evidence_id, section_key)
VALUES (5, 3, 5, 'projects[1]')
ON DUPLICATE KEY UPDATE section_key = VALUES(section_key);

INSERT INTO resume_evidence_refs (id, resume_version_id, evidence_id, section_key)
VALUES (6, 3, 3, 'projects[2]')
ON DUPLICATE KEY UPDATE section_key = VALUES(section_key);
