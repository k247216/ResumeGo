-- 契约修复：日程事件此前仅通过 job_description_id 间接关联求职计划，
-- 没有录入 JD 的计划无法关联日程。增加对求职计划的直接外键。
ALTER TABLE schedule_events ADD COLUMN job_project_id BIGINT UNSIGNED NULL AFTER job_description_id;
ALTER TABLE schedule_events ADD CONSTRAINT fk_schedule_events_job_project
    FOREIGN KEY (job_project_id) REFERENCES job_projects (id) ON DELETE SET NULL;
ALTER TABLE schedule_events ADD INDEX idx_schedule_events_job_project (job_project_id);
