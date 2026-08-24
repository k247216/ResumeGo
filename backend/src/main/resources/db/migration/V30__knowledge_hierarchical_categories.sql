-- Hierarchical library categories (MySQL)
-- NOTE: schema must stay equivalent to H2 V10__knowledge_hierarchical_categories.sql
-- parent_id 与 id 同为 BIGINT UNSIGNED，自引用 FK；禁止级联删除子树。

ALTER TABLE knowledge_categories ADD COLUMN parent_id BIGINT UNSIGNED NULL;
ALTER TABLE knowledge_categories
    ADD CONSTRAINT fk_knowledge_category_parent FOREIGN KEY (parent_id)
    REFERENCES knowledge_categories (id) ON DELETE RESTRICT;
