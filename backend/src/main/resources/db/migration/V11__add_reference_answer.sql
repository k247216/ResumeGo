-- 职达 Sprint 2 模拟面试模块：为 interview_evaluations 表新增参考回答字段
-- 使用条件判断避免字段已存在时重复添加导致 Flyway 重试失败
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'interview_evaluations'
    AND COLUMN_NAME = 'reference_answer_json'
);

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE interview_evaluations ADD COLUMN reference_answer_json TEXT NULL COMMENT ''AI 生成的参考回答，基于候选人简历真实经历，标注仅供参考'' AFTER suggestions_json',
    'SELECT 1 AS skipped');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;