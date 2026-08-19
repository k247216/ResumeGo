-- ResumeGo 产品重置：移除旧版本自动写入的个人工作区和公共岗位数据。
-- 只识别迁移脚本使用的固定简历 ID、证据来源和岗位 prompt_version；
-- 用户后来通过产品创建的简历、证据和岗位不在清理范围内。

-- 先清理依赖简历版本或种子岗位的面试数据。
DELETE FROM interview_growth_snapshots
WHERE resume_id IN (1, 2, 3)
   OR resume_version_id IN (SELECT id FROM resume_versions WHERE resume_id IN (1, 2, 3))
   OR job_description_id IN (
       SELECT id FROM job_descriptions
       WHERE prompt_version IN ('seed-v1', 'manual-import-v1', 'csv-import-v1')
   );

DELETE FROM interview_evaluations
WHERE session_id IN (
    SELECT id FROM interview_sessions
    WHERE resume_version_id IN (SELECT id FROM resume_versions WHERE resume_id IN (1, 2, 3))
       OR job_description_id IN (
           SELECT id FROM job_descriptions
           WHERE prompt_version IN ('seed-v1', 'manual-import-v1', 'csv-import-v1')
       )
);

DELETE FROM interview_answers
WHERE session_id IN (
    SELECT id FROM interview_sessions
    WHERE resume_version_id IN (SELECT id FROM resume_versions WHERE resume_id IN (1, 2, 3))
       OR job_description_id IN (
           SELECT id FROM job_descriptions
           WHERE prompt_version IN ('seed-v1', 'manual-import-v1', 'csv-import-v1')
       )
);

DELETE FROM interview_questions
WHERE session_id IN (
    SELECT id FROM interview_sessions
    WHERE resume_version_id IN (SELECT id FROM resume_versions WHERE resume_id IN (1, 2, 3))
       OR job_description_id IN (
           SELECT id FROM job_descriptions
           WHERE prompt_version IN ('seed-v1', 'manual-import-v1', 'csv-import-v1')
       )
);

DELETE FROM interview_sessions
WHERE resume_version_id IN (SELECT id FROM resume_versions WHERE resume_id IN (1, 2, 3))
   OR job_description_id IN (
       SELECT id FROM job_descriptions
       WHERE prompt_version IN ('seed-v1', 'manual-import-v1', 'csv-import-v1')
   );

DELETE FROM interview_plans
WHERE resume_version_id IN (SELECT id FROM resume_versions WHERE resume_id IN (1, 2, 3))
   OR job_description_id IN (
       SELECT id FROM job_descriptions
       WHERE prompt_version IN ('seed-v1', 'manual-import-v1', 'csv-import-v1')
   );

-- 清理诊断、匹配和建议等派生数据。
DELETE FROM optimization_suggestions
WHERE resume_version_id IN (SELECT id FROM resume_versions WHERE resume_id IN (1, 2, 3))
   OR accepted_version_id IN (SELECT id FROM resume_versions WHERE resume_id IN (1, 2, 3))
   OR job_match_id IN (
       SELECT id FROM job_matches
       WHERE resume_version_id IN (SELECT id FROM resume_versions WHERE resume_id IN (1, 2, 3))
          OR job_description_id IN (
              SELECT id FROM job_descriptions
              WHERE prompt_version IN ('seed-v1', 'manual-import-v1', 'csv-import-v1')
          )
   );

DELETE FROM job_matches
WHERE resume_version_id IN (SELECT id FROM resume_versions WHERE resume_id IN (1, 2, 3))
   OR job_description_id IN (
       SELECT id FROM job_descriptions
       WHERE prompt_version IN ('seed-v1', 'manual-import-v1', 'csv-import-v1')
   );

DELETE FROM resume_assessments
WHERE resume_version_id IN (SELECT id FROM resume_versions WHERE resume_id IN (1, 2, 3));

DELETE FROM resume_evidence_refs
WHERE resume_version_id IN (SELECT id FROM resume_versions WHERE resume_id IN (1, 2, 3))
   OR evidence_id IN (
       SELECT id FROM capability_evidences
       WHERE source_note = 'S1 演示脱敏样例'
   );

-- 解除用户手动创建简历与旧公共岗位之间的可空绑定，再删除岗位。
UPDATE resumes
SET target_job_description_id = NULL
WHERE target_job_description_id IN (
    SELECT id FROM job_descriptions
    WHERE prompt_version IN ('seed-v1', 'manual-import-v1', 'csv-import-v1')
);

DELETE FROM job_descriptions
WHERE prompt_version IN ('seed-v1', 'manual-import-v1', 'csv-import-v1');

-- 清理旧版本固定创建的三份演示简历及其版本。
UPDATE resumes
SET current_version_id = NULL
WHERE id IN (1, 2, 3);

DELETE FROM resume_versions
WHERE resume_id IN (1, 2, 3);

DELETE FROM resumes
WHERE id IN (1, 2, 3);

DELETE FROM capability_evidences
WHERE source_note = 'S1 演示脱敏样例';

-- 固定本地身份继续承担单用户数据归属，但不再以 Demo User 暴露给产品。
UPDATE users
SET display_name = '本地用户'
WHERE id = 1 AND display_name = 'Demo User';
