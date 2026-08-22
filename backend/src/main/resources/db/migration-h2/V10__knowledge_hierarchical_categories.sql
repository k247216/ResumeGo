-- Hierarchical library categories (H2)
-- NOTE: schema must stay equivalent to MySQL V30__knowledge_hierarchical_categories.sql
-- 现有分类全部成为根节点（parent_id 默认 NULL）；不允许级联删除子树（ON DELETE RESTRICT）。

ALTER TABLE knowledge_categories ADD COLUMN parent_id BIGINT NULL;
ALTER TABLE knowledge_categories
    ADD CONSTRAINT fk_knowledge_category_parent FOREIGN KEY (parent_id)
    REFERENCES knowledge_categories (id) ON DELETE RESTRICT;
