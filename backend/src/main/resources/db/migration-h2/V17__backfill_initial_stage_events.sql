-- 契约修复：为存量项目回填初始「投递中」阶段历史（与 V37 对应）。
INSERT INTO job_stage_events (user_id, project_id, stage, occurred_at)
SELECT jp.user_id, jp.id, 'applied', jp.created_at
FROM job_projects jp
WHERE NOT EXISTS (
    SELECT 1 FROM job_stage_events e WHERE e.project_id = jp.id AND e.stage = 'applied'
);
