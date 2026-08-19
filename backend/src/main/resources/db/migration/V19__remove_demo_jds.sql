-- V19: 移除早期演示 JD（id=1 后端开发实习生、id=2 全栈开发实习生）
-- 说明：V2/V3 播种的演示 JD 使用硬编码 parsed_json，非真实岗位数据；
--       后续 V8/V10 已有 210 条真实解析岗位，演示 JD 不再需要。
--       遵循 Flyway 不可变原则：旧版本保留历史记录，V19 执行清理。

-- 先解除可空引用，再删从表（FK 关联），最后删主表，按依赖链逐层清理。
-- 注意：interview_* 外键均为 RESTRICT，因此要先删评价、回答、问题，再删 session/plan。
UPDATE resumes
SET target_job_description_id = NULL
WHERE target_job_description_id IN (1, 2);

DELETE FROM interview_growth_snapshots
WHERE job_description_id IN (1, 2);

DELETE FROM interview_evaluations
WHERE session_id IN (SELECT id FROM interview_sessions WHERE job_description_id IN (1, 2));

DELETE FROM interview_answers
WHERE session_id IN (SELECT id FROM interview_sessions WHERE job_description_id IN (1, 2));

DELETE FROM interview_questions
WHERE session_id IN (SELECT id FROM interview_sessions WHERE job_description_id IN (1, 2));

DELETE FROM interview_sessions
WHERE job_description_id IN (1, 2);

DELETE FROM interview_plans
WHERE job_description_id IN (1, 2);

DELETE FROM optimization_suggestions WHERE job_match_id IN (SELECT id FROM job_matches WHERE job_description_id IN (1, 2));
DELETE FROM job_matches WHERE job_description_id IN (1, 2);
DELETE FROM job_descriptions WHERE id IN (1, 2);
