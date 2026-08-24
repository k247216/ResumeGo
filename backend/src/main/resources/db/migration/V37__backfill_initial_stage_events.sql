-- 契约修复：新建计划时初始「投递中」阶段此前未写入阶段历史，
-- 导致时间轴在第一次推进后只剩新阶段。为存量项目回填初始 applied 事件
-- （时间取计划创建时间，保证早于后续任何推进记录）。
INSERT INTO job_stage_events (user_id, project_id, stage, occurred_at)
SELECT jp.user_id, jp.id, 'applied', jp.created_at
FROM job_projects jp
WHERE NOT EXISTS (
    SELECT 1 FROM job_stage_events e WHERE e.project_id = jp.id AND e.stage = 'applied'
);
