-- 职达 S2：岗位来源扩展信息 + 岗位类型
-- source_meta_json 存放真实岗位扩展信息（base/salary/platform/sourceUrl/industry/companySize/collectedAt/tags）
-- job_type 用于区分实习/校招/社招，方便前端工作台筛选
ALTER TABLE job_descriptions
    ADD COLUMN source_meta_json JSON NULL COMMENT '岗位来源扩展信息' AFTER parse_ai_invocation_id,
    ADD COLUMN job_type VARCHAR(50) NULL COMMENT '岗位类型：internship/campus/social' AFTER source_meta_json;
