CREATE TABLE pipeline_schedule_events (
    pipeline_id BIGINT NOT NULL,
    schedule_event_id BIGINT NOT NULL,
    linked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (pipeline_id, schedule_event_id),
    CONSTRAINT uk_pipeline_schedule_event UNIQUE (schedule_event_id),
    CONSTRAINT fk_pipeline_schedule_pipeline FOREIGN KEY (pipeline_id)
        REFERENCES career_pipelines (id) ON DELETE CASCADE,
    CONSTRAINT fk_pipeline_schedule_event FOREIGN KEY (schedule_event_id)
        REFERENCES schedule_events (id) ON DELETE CASCADE
);
CREATE INDEX idx_pipeline_schedule_pipeline ON pipeline_schedule_events(pipeline_id, linked_at);

CREATE TABLE pipeline_interview_plans (
    pipeline_id BIGINT NOT NULL,
    interview_plan_id BIGINT NOT NULL,
    linked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (pipeline_id, interview_plan_id),
    CONSTRAINT uk_pipeline_interview_plan UNIQUE (interview_plan_id),
    CONSTRAINT fk_pipeline_interview_pipeline FOREIGN KEY (pipeline_id)
        REFERENCES career_pipelines (id) ON DELETE CASCADE,
    CONSTRAINT fk_pipeline_interview_plan FOREIGN KEY (interview_plan_id)
        REFERENCES interview_plans (id) ON DELETE CASCADE
);
CREATE INDEX idx_pipeline_interview_pipeline ON pipeline_interview_plans(pipeline_id, linked_at);
