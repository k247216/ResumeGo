-- 为每份简历保存当前目标岗位，支撑“岗位 → 简历 → 评分/匹配/面试”的稳定上下文。

ALTER TABLE resumes
    ADD COLUMN target_job_description_id BIGINT UNSIGNED NULL AFTER current_version_id,
    ADD KEY idx_resumes_target_job_description_id (target_job_description_id),
    ADD CONSTRAINT fk_resumes_target_job_description
        FOREIGN KEY (target_job_description_id) REFERENCES job_descriptions (id)
        ON DELETE SET NULL;
