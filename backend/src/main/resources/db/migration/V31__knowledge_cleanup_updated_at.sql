-- 修复 knowledge_cleanup_jobs 缺失 updated_at（删除清理 job 状态更新报 BadSqlGrammar）
ALTER TABLE knowledge_cleanup_jobs ADD COLUMN updated_at TIMESTAMP(3) NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3);
