-- 契约修复：日程事件直接关联求职计划（与 V38 对应）。
ALTER TABLE schedule_events ADD COLUMN job_project_id BIGINT NULL;
ALTER TABLE schedule_events ADD CONSTRAINT fk_schedule_events_job_project
    FOREIGN KEY (job_project_id) REFERENCES job_projects (id) ON DELETE SET NULL;
CREATE INDEX idx_schedule_events_job_project ON schedule_events(job_project_id);
