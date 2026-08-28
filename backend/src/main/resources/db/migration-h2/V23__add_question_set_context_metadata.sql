-- 面经题集可选上下文，供真题演练列表展示；不从自由文本来源说明推断。
ALTER TABLE interview_question_sets ADD COLUMN company_name VARCHAR(120) NULL;
ALTER TABLE interview_question_sets ADD COLUMN target_role VARCHAR(120) NULL;
ALTER TABLE interview_question_sets ADD COLUMN company_icon_key VARCHAR(80) NULL;
