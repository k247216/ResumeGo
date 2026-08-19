-- 公司偏好 Profile 预留表。
-- 说明：用于后续把公开资料/人工整理的大厂岗位偏好接入 AI 建议 Prompt。
-- 本表不参与简历评分、岗位匹配排序，也不作为录用概率判断依据。

CREATE TABLE company_profiles (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    company_name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,
    source_type VARCHAR(40) NOT NULL DEFAULT 'experience_based' COMMENT 'official / dataset / experience_based / manual',
    source_note VARCHAR(500) NULL COMMENT '资料来源说明，经验型偏好必须显式标注',
    preference_tags JSON NOT NULL COMMENT '公司偏好标签，如业务结果、工程稳定性、快速迭代',
    writing_style VARCHAR(500) NULL COMMENT '简历表达风格建议',
    interview_focus JSON NULL COMMENT '面试关注点标签',
    resume_advice_rules JSON NULL COMMENT '简历表达建议规则，供后续 AI Prompt 使用',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_company_profiles_normalized_name (normalized_name),
    KEY idx_company_profiles_enabled (enabled),
    CONSTRAINT chk_company_profiles_source_type
        CHECK (source_type IN ('official', 'dataset', 'experience_based', 'manual')),
    CONSTRAINT chk_company_profiles_preference_tags_array
        CHECK (JSON_TYPE(preference_tags) = 'ARRAY'),
    CONSTRAINT chk_company_profiles_interview_focus_array
        CHECK (interview_focus IS NULL OR JSON_TYPE(interview_focus) = 'ARRAY'),
    CONSTRAINT chk_company_profiles_resume_rules_array
        CHECK (resume_advice_rules IS NULL OR JSON_TYPE(resume_advice_rules) = 'ARRAY')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公司岗位偏好 Profile';

INSERT INTO company_profiles (
    company_name,
    normalized_name,
    source_type,
    source_note,
    preference_tags,
    writing_style,
    interview_focus,
    resume_advice_rules
) VALUES
(
    '字节跳动',
    'bytedance',
    'experience_based',
    '经验型偏好，仅用于演示和后续资料整理入口；上线前需补充公开资料或人工审核来源。',
    JSON_ARRAY('业务结果', '数据指标', '快速迭代', '工程效率'),
    '建议突出个人动作、结果指标和迭代速度，避免只罗列技术名词。',
    JSON_ARRAY('项目深挖', '业务理解', '数据结果', '系统设计'),
    JSON_ARRAY('补充量化结果', '说明技术动作对业务指标的影响', '突出快速定位和迭代过程')
),
(
    '腾讯',
    'tencent',
    'experience_based',
    '经验型偏好，仅用于演示和后续资料整理入口；上线前需补充公开资料或人工审核来源。',
    JSON_ARRAY('工程稳定性', '用户规模', '协作沟通', '质量保障'),
    '建议突出稳定性、协作角色、用户规模和质量保障实践。',
    JSON_ARRAY('工程质量', '团队协作', '场景理解', '稳定性设计'),
    JSON_ARRAY('补充稳定性和可用性措施', '说明团队协作边界', '突出用户规模或真实使用场景')
),
(
    '阿里巴巴',
    'alibaba',
    'experience_based',
    '经验型偏好，仅用于演示和后续资料整理入口；上线前需补充公开资料或人工审核来源。',
    JSON_ARRAY('业务理解', '架构思考', '复杂系统', '沉淀复盘'),
    '建议强调复杂问题拆解、架构取舍和业务价值沉淀。',
    JSON_ARRAY('架构设计', '业务抽象', '复杂系统治理', '复盘能力'),
    JSON_ARRAY('说明问题背景和业务约束', '补充架构取舍依据', '体现复盘和方法沉淀')
),
(
    '美团',
    'meituan',
    'experience_based',
    '经验型偏好，仅用于演示和后续资料整理入口；上线前需补充公开资料或人工审核来源。',
    JSON_ARRAY('履约链路', '高并发', '成本效率', '问题闭环'),
    '建议突出链路效率、成本收益、并发处理和问题闭环能力。',
    JSON_ARRAY('高并发场景', '链路优化', '成本意识', '问题排查'),
    JSON_ARRAY('补充链路指标和优化前后对比', '说明并发或性能瓶颈', '突出成本效率和问题闭环')
);
