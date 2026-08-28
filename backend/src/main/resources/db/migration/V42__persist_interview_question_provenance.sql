-- 保存面试题目的来源引用与用户可见来源标签，确保复盘不会把原题和 AI 生成题混在一起。
ALTER TABLE interview_questions
    ADD COLUMN source_reference VARCHAR(1000) NULL AFTER source,
    ADD COLUMN provenance_label VARCHAR(120) NULL AFTER source_reference;

ALTER TABLE interview_questions
    DROP CHECK chk_interview_questions_source;

ALTER TABLE interview_questions
    ADD CONSTRAINT chk_interview_questions_source
        CHECK (source IN ('ai_generated', 'system_defined', 'manual', 'ai_follow_up'));
