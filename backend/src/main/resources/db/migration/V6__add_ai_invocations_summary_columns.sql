ALTER TABLE ai_invocations
    ADD COLUMN prompt_summary VARCHAR(200) NULL COMMENT 'Prompt 摘要（截断前 200 字符）' AFTER error_category,
    ADD COLUMN response_summary VARCHAR(200) NULL COMMENT 'Response 摘要（截断前 200 字符）' AFTER prompt_summary;
