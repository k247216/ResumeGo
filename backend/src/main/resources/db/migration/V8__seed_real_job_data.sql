-- 职达 S2：真实岗位数据录入
-- 来源：招聘平台采集数据，适配 V7 扩展后的 job_descriptions 表结构
-- source_meta_json 存放岗位来源扩展信息
-- parsed_json 存放结构化解析结果
-- job_type 区分 实习/校招/社招

-- ============================================================
-- JD 1：Java 开发工程师（新浪·重庆）
-- ============================================================
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    10, 1,
    'Java 开发工程师',
    '新浪',
    '岗位职责：负责 Java 开发工程师相关的系统设计和开发工作，持续优化系统性能和用户体验，参与技术方案评审，解决技术难题。\n岗位要求：1-3 年以上相关工作经验，具备良好的沟通能力和团队协作精神，硕士及以上学历，计算机相关专业优先，具备良好的编程基础和算法能力。',
    JSON_OBJECT(
        'requiredSkills', JSON_ARRAY('TensorFlow', 'Linux', 'Python'),
        'preferredSkills', JSON_ARRAY(),
        'responsibilities', JSON_ARRAY(
            '负责 Java 开发工程师相关的系统设计和开发工作',
            '持续优化系统性能和用户体验',
            '参与技术方案评审，解决技术难题'
        ),
        'experienceRequirements', JSON_ARRAY('1-3 年以上相关工作经验'),
        'educationRequirements', JSON_ARRAY('硕士及以上学历', '计算机相关专业优先')
    ),
    'succeeded',
    'manual-import-v1',
    NULL,
    JSON_OBJECT(
        'base', '重庆',
        'salary', '9,482-15,409元/月',
        'salaryMin', 9482,
        'salaryMax', 15409,
        'salaryAvg', 12445,
        'platform', '招聘平台',
        'industry', '金融',
        'companySize', '20-99人',
        'collectedAt', '2026-02-28',
        'sourceJobId', 'JOB770487',
        'education', '硕士',
        'experience', '1-3年',
        'views', 4564,
        'applications', 112,
        'tags', JSON_ARRAY('Java', 'TensorFlow', 'Linux', 'Python')
    ),
    'social'
)
ON DUPLICATE KEY UPDATE
    job_title = VALUES(job_title),
    company_name = VALUES(company_name),
    raw_text = VALUES(raw_text),
    parsed_json = VALUES(parsed_json),
    parse_status = VALUES(parse_status),
    prompt_version = VALUES(prompt_version),
    source_meta_json = VALUES(source_meta_json),
    job_type = VALUES(job_type);

-- ============================================================
-- JD 2：媒介专员（大华股份·杭州）
-- ============================================================
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    11, 1,
    '媒介专员',
    '大华股份',
    '岗位职责：组织市场活动和品牌宣传，负责媒介专员相关的市场推广工作，制定市场推广策略和计划。\n岗位要求：具备市场策划和执行能力，具备良好的商务谈判能力，有相关行业资源者优先，大专及以上学历。',
    JSON_OBJECT(
        'requiredSkills', JSON_ARRAY('市场调研', '公关', '品牌推广', 'BD'),
        'preferredSkills', JSON_ARRAY(),
        'responsibilities', JSON_ARRAY(
            '组织市场活动和品牌宣传',
            '负责媒介专员相关的市场推广工作',
            '制定市场推广策略和计划'
        ),
        'experienceRequirements', JSON_ARRAY('1-3 年以上相关工作经验'),
        'educationRequirements', JSON_ARRAY('大专及以上学历')
    ),
    'succeeded',
    'manual-import-v1',
    NULL,
    JSON_OBJECT(
        'base', '杭州',
        'salary', '8,837-14,361元/月',
        'salaryMin', 8837,
        'salaryMax', 14361,
        'salaryAvg', 11599,
        'platform', '招聘平台',
        'industry', '制造业',
        'companySize', '100-499人',
        'collectedAt', '2026-02-11',
        'sourceJobId', 'JOB331148',
        'education', '大专',
        'experience', '1-3年',
        'views', 3863,
        'applications', 142,
        'tags', JSON_ARRAY('市场调研', '公关', '品牌推广', 'BD')
    ),
    'social'
)
ON DUPLICATE KEY UPDATE
    job_title = VALUES(job_title),
    company_name = VALUES(company_name),
    raw_text = VALUES(raw_text),
    parsed_json = VALUES(parsed_json),
    parse_status = VALUES(parse_status),
    prompt_version = VALUES(prompt_version),
    source_meta_json = VALUES(source_meta_json),
    job_type = VALUES(job_type);

-- ============================================================
-- JD 3：市场专员（旷视科技·西安）
-- ============================================================
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    12, 1,
    '市场专员',
    '旷视科技',
    '岗位职责：拓展市场渠道和合作伙伴，负责市场专员相关的市场推广工作，组织市场活动和品牌宣传。\n岗位要求：硕士及以上学历，熟悉各类营销渠道和推广方式，具备市场策划和执行能力，不限市场推广经验。',
    JSON_OBJECT(
        'requiredSkills', JSON_ARRAY('品牌推广', '渠道拓展', '市场调研'),
        'preferredSkills', JSON_ARRAY(),
        'responsibilities', JSON_ARRAY(
            '拓展市场渠道和合作伙伴',
            '负责市场专员相关的市场推广工作',
            '组织市场活动和品牌宣传'
        ),
        'experienceRequirements', JSON_ARRAY('不限经验'),
        'educationRequirements', JSON_ARRAY('硕士及以上学历')
    ),
    'succeeded',
    'manual-import-v1',
    NULL,
    JSON_OBJECT(
        'base', '西安',
        'salary', '7,091-11,524元/月',
        'salaryMin', 7091,
        'salaryMax', 11524,
        'salaryAvg', 9307,
        'platform', '招聘平台',
        'industry', '医疗',
        'companySize', '1000-9999人',
        'collectedAt', '2026-02-14',
        'sourceJobId', 'JOB230889',
        'education', '硕士',
        'experience', '不限',
        'views', 3088,
        'applications', 46,
        'tags', JSON_ARRAY('品牌推广', '渠道拓展', '市场调研')
    ),
    'campus'
)
ON DUPLICATE KEY UPDATE
    job_title = VALUES(job_title),
    company_name = VALUES(company_name),
    raw_text = VALUES(raw_text),
    parsed_json = VALUES(parsed_json),
    parse_status = VALUES(parse_status),
    prompt_version = VALUES(prompt_version),
    source_meta_json = VALUES(source_meta_json),
    job_type = VALUES(job_type);

-- ============================================================
-- JD 4：运营经理（京东·天津）
-- ============================================================
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    13, 1,
    '运营经理',
    '京东',
    '岗位职责：维护用户关系，提升用户活跃度，制定运营策略和执行计划，分析运营数据，优化运营效果。\n岗位要求：具备数据分析能力和运营思维，硕士及以上学历，不限运营经验，具备良好的文案写作能力。',
    JSON_OBJECT(
        'requiredSkills', JSON_ARRAY('数据分析', '活动策划', 'SEO', 'SEM'),
        'preferredSkills', JSON_ARRAY(),
        'responsibilities', JSON_ARRAY(
            '维护用户关系，提升用户活跃度',
            '制定运营策略和执行计划',
            '分析运营数据，优化运营效果'
        ),
        'experienceRequirements', JSON_ARRAY('不限经验'),
        'educationRequirements', JSON_ARRAY('硕士及以上学历')
    ),
    'succeeded',
    'manual-import-v1',
    NULL,
    JSON_OBJECT(
        'base', '天津',
        'salary', '6,615-10,750元/月',
        'salaryMin', 6615,
        'salaryMax', 10750,
        'salaryAvg', 8682,
        'platform', '招聘平台',
        'industry', '教育',
        'companySize', '1000-9999人',
        'collectedAt', '2026-02-09',
        'sourceJobId', 'JOB488162',
        'education', '硕士',
        'experience', '不限',
        'views', 2684,
        'applications', 4562,
        'tags', JSON_ARRAY('数据分析', '活动策划', 'SEO', 'SEM')
    ),
    'campus'
)
ON DUPLICATE KEY UPDATE
    job_title = VALUES(job_title),
    company_name = VALUES(company_name),
    raw_text = VALUES(raw_text),
    parsed_json = VALUES(parsed_json),
    parse_status = VALUES(parse_status),
    prompt_version = VALUES(prompt_version),
    source_meta_json = VALUES(source_meta_json),
    job_type = VALUES(job_type);

-- ============================================================
-- JD 5：Python 开发工程师（搜狐·杭州）
-- ============================================================
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    14, 1,
    'Python 开发工程师',
    '搜狐',
    '岗位职责：持续优化系统性能和用户体验，编写高质量、可维护的代码，参与需求分析、系统设计、编码实现和测试。\n岗位要求：有大型项目经验者优先，熟悉相关技术栈和开发工具，具备良好的编程基础和算法能力，5-10年以上相关工作经验。',
    JSON_OBJECT(
        'requiredSkills', JSON_ARRAY('Jenkins', '深度学习', '机器学习', 'Docker', 'Linux'),
        'preferredSkills', JSON_ARRAY(),
        'responsibilities', JSON_ARRAY(
            '持续优化系统性能和用户体验',
            '编写高质量、可维护的代码',
            '参与需求分析、系统设计、编码实现和测试'
        ),
        'experienceRequirements', JSON_ARRAY('5-10年以上相关工作经验'),
        'educationRequirements', JSON_ARRAY('本科及以上学历')
    ),
    'succeeded',
    'manual-import-v1',
    NULL,
    JSON_OBJECT(
        'base', '杭州',
        'salary', '27,176-44,162元/月',
        'salaryMin', 27176,
        'salaryMax', 44162,
        'salaryAvg', 35669,
        'platform', '招聘平台',
        'industry', '其他',
        'companySize', '100-499人',
        'collectedAt', '2026-02-26',
        'sourceJobId', 'JOB-SOHU-PYTHON',
        'education', '本科',
        'experience', '5-10年',
        'views', 1988,
        'applications', 35,
        'tags', JSON_ARRAY('Python', 'Jenkins', '深度学习', '机器学习', 'Docker', 'Linux')
    ),
    'social'
)
ON DUPLICATE KEY UPDATE
    job_title = VALUES(job_title),
    company_name = VALUES(company_name),
    raw_text = VALUES(raw_text),
    parsed_json = VALUES(parsed_json),
    parse_status = VALUES(parse_status),
    prompt_version = VALUES(prompt_version),
    source_meta_json = VALUES(source_meta_json),
    job_type = VALUES(job_type);

-- ============================================================
-- JD 6：数据分析师（百度·南京）
-- ============================================================
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    15, 1,
    '数据分析师',
    '百度',
    '岗位职责：参与技术方案评审，解决技术难题，参与需求分析、系统设计、编码实现和测试，编写高质量、可维护的代码。\n岗位要求：熟悉相关技术栈和开发工具，有大型项目经验者优先，具备良好的沟通能力和团队协作精神，10年以上相关工作经验。',
    JSON_OBJECT(
        'requiredSkills', JSON_ARRAY('JavaScript', 'Django', 'TypeScript', '深度学习', '数据结构'),
        'preferredSkills', JSON_ARRAY(),
        'responsibilities', JSON_ARRAY(
            '参与技术方案评审，解决技术难题',
            '参与需求分析、系统设计、编码实现和测试',
            '编写高质量、可维护的代码'
        ),
        'experienceRequirements', JSON_ARRAY('10年以上相关工作经验'),
        'educationRequirements', JSON_ARRAY('大专及以上学历')
    ),
    'succeeded',
    'manual-import-v1',
    NULL,
    JSON_OBJECT(
        'base', '南京',
        'salary', '18,879-30,679元/月',
        'salaryMin', 18879,
        'salaryMax', 30679,
        'salaryAvg', 24779,
        'platform', '招聘平台',
        'industry', '教育',
        'companySize', '20-99人',
        'collectedAt', '2026-02-07',
        'sourceJobId', 'JOB-BAIDU-ANALYST',
        'education', '大专',
        'experience', '10年以上',
        'views', 4921,
        'applications', 114,
        'tags', JSON_ARRAY('数据分析', 'JavaScript', 'Django', 'TypeScript', '深度学习', '数据结构')
    ),
    'social'
)
ON DUPLICATE KEY UPDATE
    job_title = VALUES(job_title),
    company_name = VALUES(company_name),
    raw_text = VALUES(raw_text),
    parsed_json = VALUES(parsed_json),
    parse_status = VALUES(parse_status),
    prompt_version = VALUES(prompt_version),
    source_meta_json = VALUES(source_meta_json),
    job_type = VALUES(job_type);

-- ============================================================
-- JD 7：UX 设计（滴滴·北京）
-- ============================================================
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    16, 1,
    'UX 设计',
    '滴滴',
    '岗位职责：根据需求进行创意设计和视觉表现，负责UX设计相关的设计工作，持续优化设计质量和用户体验。\n岗位要求：熟练使用设计软件和工具，具备良好的沟通理解能力，有优秀作品集者优先，不限设计经验。',
    JSON_OBJECT(
        'requiredSkills', JSON_ARRAY('Illustrator', 'After Effects', 'C4D', '3D Max', 'Photoshop'),
        'preferredSkills', JSON_ARRAY(),
        'responsibilities', JSON_ARRAY(
            '根据需求进行创意设计和视觉表现',
            '负责UX设计相关的设计工作',
            '持续优化设计质量和用户体验'
        ),
        'experienceRequirements', JSON_ARRAY('不限经验'),
        'educationRequirements', JSON_ARRAY('不限')
    ),
    'succeeded',
    'manual-import-v1',
    NULL,
    JSON_OBJECT(
        'base', '北京',
        'salary', '6,597-10,720元/月',
        'salaryMin', 6597,
        'salaryMax', 10720,
        'salaryAvg', 8658,
        'platform', '招聘平台',
        'industry', '医疗',
        'companySize', '20-99人',
        'collectedAt', '2026-03-05',
        'sourceJobId', 'JOB-DIDI-UX',
        'education', '不限',
        'experience', '不限',
        'views', 1996,
        'applications', 78,
        'tags', JSON_ARRAY('UX设计', 'Illustrator', 'After Effects', 'C4D', '3D Max', 'Photoshop')
    ),
    'campus'
)
ON DUPLICATE KEY UPDATE
    job_title = VALUES(job_title),
    company_name = VALUES(company_name),
    raw_text = VALUES(raw_text),
    parsed_json = VALUES(parsed_json),
    parse_status = VALUES(parse_status),
    prompt_version = VALUES(prompt_version),
    source_meta_json = VALUES(source_meta_json),
    job_type = VALUES(job_type);

-- ============================================================
-- JD 8：活动运营（新浪·郑州）
-- ============================================================
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    17, 1,
    '活动运营',
    '新浪',
    '岗位职责：制定运营策略和执行计划，分析运营数据，优化运营效果，负责活动运营相关的运营工作。\n岗位要求：博士及以上学历，有成功运营案例者优先，应届生以上运营经验，熟悉新媒体运营和内容策划。',
    JSON_OBJECT(
        'requiredSkills', JSON_ARRAY('SEO', '内容策划', '社群运营', '用户增长'),
        'preferredSkills', JSON_ARRAY(),
        'responsibilities', JSON_ARRAY(
            '制定运营策略和执行计划',
            '分析运营数据，优化运营效果',
            '负责活动运营相关的运营工作'
        ),
        'experienceRequirements', JSON_ARRAY('应届生'),
        'educationRequirements', JSON_ARRAY('博士及以上学历')
    ),
    'succeeded',
    'manual-import-v1',
    NULL,
    JSON_OBJECT(
        'base', '郑州',
        'salary', '6,957-11,305元/月',
        'salaryMin', 6957,
        'salaryMax', 11305,
        'salaryAvg', 9131,
        'platform', '招聘平台',
        'industry', '其他',
        'companySize', '100-499人',
        'collectedAt', '2026-02-15',
        'sourceJobId', 'JOB-SINA-OPER',
        'education', '博士',
        'experience', '应届生',
        'views', 4762,
        'applications', 78,
        'tags', JSON_ARRAY('活动运营', 'SEO', '内容策划', '社群运营', '用户增长')
    ),
    'campus'
)
ON DUPLICATE KEY UPDATE
    job_title = VALUES(job_title),
    company_name = VALUES(company_name),
    raw_text = VALUES(raw_text),
    parsed_json = VALUES(parsed_json),
    parse_status = VALUES(parse_status),
    prompt_version = VALUES(prompt_version),
    source_meta_json = VALUES(source_meta_json),
    job_type = VALUES(job_type);

-- ============================================================
-- JD 9：数据工程师（云从科技·重庆）
-- ============================================================
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    18, 1,
    '数据工程师',
    '云从科技',
    '岗位职责：编写高质量、可维护的代码，持续优化系统性能和用户体验，负责数据工程师相关的系统设计和开发工作。\n岗位要求：硕士及以上学历，计算机相关专业优先，不限相关工作经验，有大型项目经验者优先，具备良好的编程基础和算法能力。',
    JSON_OBJECT(
        'requiredSkills', JSON_ARRAY('Git', 'JavaScript', 'PyTorch', 'Python', 'Linux', '算法'),
        'preferredSkills', JSON_ARRAY(),
        'responsibilities', JSON_ARRAY(
            '编写高质量、可维护的代码',
            '持续优化系统性能和用户体验',
            '负责数据工程师相关的系统设计和开发工作'
        ),
        'experienceRequirements', JSON_ARRAY('不限经验'),
        'educationRequirements', JSON_ARRAY('硕士及以上学历', '计算机相关专业优先')
    ),
    'succeeded',
    'manual-import-v1',
    NULL,
    JSON_OBJECT(
        'base', '重庆',
        'salary', '7,795-12,668元/月',
        'salaryMin', 7795,
        'salaryMax', 12668,
        'salaryAvg', 10231,
        'platform', '招聘平台',
        'industry', '能源',
        'companySize', '100-499人',
        'collectedAt', '2026-02-08',
        'sourceJobId', 'JOB-YUNCANG-DE',
        'education', '硕士',
        'experience', '不限',
        'views', 2307,
        'applications', 152,
        'tags', JSON_ARRAY('数据工程', 'Git', 'JavaScript', 'PyTorch', 'Python', 'Linux', '算法')
    ),
    'campus'
)
ON DUPLICATE KEY UPDATE
    job_title = VALUES(job_title),
    company_name = VALUES(company_name),
    raw_text = VALUES(raw_text),
    parsed_json = VALUES(parsed_json),
    parse_status = VALUES(parse_status),
    prompt_version = VALUES(prompt_version),
    source_meta_json = VALUES(source_meta_json),
    job_type = VALUES(job_type);

-- ============================================================
-- JD 10：用户研究（拼多多·西安）
-- ============================================================
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    19, 1,
    '用户研究',
    '拼多多',
    '岗位职责：负责用户研究的产品规划和设计，进行用户调研和需求分析，协调开发、设计、测试等团队资源。\n岗位要求：熟练使用Axure、Figma等设计工具，有相关行业产品经验者优先，具备良好的产品思维和用户洞察力，具备优秀的沟通协调能力。',
    JSON_OBJECT(
        'requiredSkills', JSON_ARRAY('需求分析', 'Axure', '原型设计', '数据分析', '用户研究', 'PRD文档'),
        'preferredSkills', JSON_ARRAY('Figma'),
        'responsibilities', JSON_ARRAY(
            '负责用户研究的产品规划和设计',
            '进行用户调研和需求分析',
            '协调开发、设计、测试等团队资源'
        ),
        'experienceRequirements', JSON_ARRAY('3-5年相关经验'),
        'educationRequirements', JSON_ARRAY('博士及以上学历')
    ),
    'succeeded',
    'manual-import-v1',
    NULL,
    JSON_OBJECT(
        'base', '西安',
        'salary', '15,058-24,470元/月',
        'salaryMin', 15058,
        'salaryMax', 24470,
        'salaryAvg', 19764,
        'platform', '招聘平台',
        'industry', '房地产',
        'companySize', '1000-9999人',
        'collectedAt', '2026-02-04',
        'sourceJobId', 'JOB-PDD-UR',
        'education', '博士',
        'experience', '3-5年',
        'views', 3380,
        'applications', 199,
        'tags', JSON_ARRAY('用户研究', '需求分析', 'Axure', '原型设计', '数据分析', 'PRD文档')
    ),
    'social'
)
ON DUPLICATE KEY UPDATE
    job_title = VALUES(job_title),
    company_name = VALUES(company_name),
    raw_text = VALUES(raw_text),
    parsed_json = VALUES(parsed_json),
    parse_status = VALUES(parse_status),
    prompt_version = VALUES(prompt_version),
    source_meta_json = VALUES(source_meta_json),
    job_type = VALUES(job_type);
