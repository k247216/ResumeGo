-- 保存“一次多轮面试”的整次复盘结果。
-- 不改变 interview_sessions.status，也不参与面试状态机转换。

ALTER TABLE interview_plans
    ADD COLUMN summary_json JSON NULL COMMENT '整次多轮面试综合复盘结果' AFTER supplement_text,
    ADD COLUMN summary_generated_at DATETIME(3) NULL COMMENT '整次复盘生成时间' AFTER summary_json,
    ADD CONSTRAINT chk_interview_plans_summary_object
        CHECK (summary_json IS NULL OR JSON_TYPE(summary_json) = 'OBJECT');
