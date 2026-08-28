-- 将真实面经题集与知识库原始资料建立可追溯关系。
-- 资料删除时保留已经使用过的历史题集，但解除来源链接。
ALTER TABLE interview_question_sets
    ADD COLUMN source_document_id BIGINT UNSIGNED NULL AFTER company_icon_key,
    ADD UNIQUE KEY uk_interview_question_sets_source_document (user_id, source_document_id),
    ADD KEY idx_interview_question_sets_source_document (source_document_id),
    ADD CONSTRAINT fk_interview_question_sets_source_document
        FOREIGN KEY (source_document_id) REFERENCES knowledge_documents(id)
        ON DELETE SET NULL;
