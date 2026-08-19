-- S1 AI 建议安全标记补丁
-- 支持将疑似编造事实的建议保存为 high_risk，并记录 risk_level。

ALTER TABLE optimization_suggestions
    ADD COLUMN risk_level VARCHAR(32) NULL AFTER status;

ALTER TABLE optimization_suggestions
    DROP CHECK chk_optimization_suggestions_status;

ALTER TABLE optimization_suggestions
    ADD CONSTRAINT chk_optimization_suggestions_status
        CHECK (status IN ('pending', 'accepted', 'rejected', 'evidence_required', 'high_risk'));
