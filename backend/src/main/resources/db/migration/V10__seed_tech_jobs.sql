-- 职达 S2：从 CSV 导入 200 条技术岗位
-- 来源：jobs.csv，筛选 job_category='技术'
-- 去重：已排除 V8 等已有 migration 中已导入的 sourceJobId
-- 重复执行策略：ON DUPLICATE KEY UPDATE（按主键 id 幂等）

-- JD 1: 后端开发工程师（平安科技·青岛）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    30, 1,
    '后端开发工程师',
    '平安科技',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
编写高质量、可维护的代码
岗位要求：具备良好的沟通能力和团队协作精神
有大型项目经验者优先
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["大数据", "Hadoop", "Scikit-learn", "Git", "React", "MySQL"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验\\n编写高质量、可维护的代码"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n有大型项目经验者优先\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "青岛", "salary": "5066-8233元/月", "salaryMin": 5066, "salaryMax": 8233, "salaryAvg": 6649, "platform": "招聘平台", "industry": "物流", "companySize": "20-99人", "collectedAt": "2026-02-16", "sourceJobId": "JOB380746", "education": "不限", "experience": "不限", "views": 3609, "applications": 154, "tags": ["大数据", "Hadoop", "Scikit-learn", "Git", "React", "MySQL"]}',
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

-- JD 2: Python开发工程师（搜狐·杭州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    31, 1,
    'Python开发工程师',
    '搜狐',
    '岗位职责：持续优化系统性能和用户体验
编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
岗位要求：有大型项目经验者优先
熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力
5-10年以上相关工作经验',
    '{"requiredSkills": ["Jenkins", "深度学习", "机器学习", "Docker", "Linux"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["有大型项目经验者优先\\n熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力\\n5-10年以上相关工作经验"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "杭州", "salary": "27176-44162元/月", "salaryMin": 27176, "salaryMax": 44162, "salaryAvg": 35669, "platform": "招聘平台", "industry": "其他", "companySize": "100-499人", "collectedAt": "2026-02-26", "sourceJobId": "JOB931450", "education": "本科", "experience": "5-10年", "views": 1988, "applications": 35, "tags": ["Jenkins", "深度学习", "机器学习", "Docker", "Linux"]}',
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

-- JD 3: Java开发工程师（华为·西安）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    32, 1,
    'Java开发工程师',
    '华为',
    '岗位职责：编写高质量、可维护的代码
参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
岗位要求：熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力
不限以上相关工作经验
硕士及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["高并发", "Java", "TensorFlow"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力\\n不限以上相关工作经验\\n硕士及以上学历，计算机相关专业优先"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "西安", "salary": "8972-14580元/月", "salaryMin": 8972, "salaryMax": 14580, "salaryAvg": 11776, "platform": "招聘平台", "industry": "物流", "companySize": "10000人以上", "collectedAt": "2026-02-26", "sourceJobId": "JOB387636", "education": "硕士", "experience": "不限", "views": 4013, "applications": 34, "tags": ["高并发", "Java", "TensorFlow"]}',
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

-- JD 4: 数据分析师（百度·南京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    33, 1,
    '数据分析师',
    '百度',
    '岗位职责：参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
岗位要求：熟悉相关技术栈和开发工具
有大型项目经验者优先
具备良好的沟通能力和团队协作精神
10年以上以上相关工作经验',
    '{"requiredSkills": ["JavaScript", "Django", "TypeScript", "深度学习", "数据结构"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神\\n10年以上以上相关工作经验"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "南京", "salary": "18879-30679元/月", "salaryMin": 18879, "salaryMax": 30679, "salaryAvg": 24779, "platform": "招聘平台", "industry": "教育", "companySize": "20-99人", "collectedAt": "2026-02-07", "sourceJobId": "JOB962050", "education": "大专", "experience": "10年以上", "views": 4921, "applications": 114, "tags": ["JavaScript", "Django", "TypeScript", "深度学习", "数据结构"]}',
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

-- JD 5: 数据工程师（云从科技·重庆）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    34, 1,
    '数据工程师',
    '云从科技',
    '岗位职责：编写高质量、可维护的代码
持续优化系统性能和用户体验
负责数据工程师相关的系统设计和开发工作
岗位要求：硕士及以上学历，计算机相关专业优先
不限以上相关工作经验
有大型项目经验者优先
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Git", "JavaScript", "PyTorch", "Python", "Linux", "算法"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n持续优化系统性能和用户体验\\n负责数据工程师相关的系统设计和开发工作"], "experienceRequirements": ["硕士及以上学历，计算机相关专业优先\\n不限以上相关工作经验\\n有大型项目经验者优先\\n具备良好的编程基础和算法能力"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "重庆", "salary": "7795-12668元/月", "salaryMin": 7795, "salaryMax": 12668, "salaryAvg": 10231, "platform": "招聘平台", "industry": "能源", "companySize": "100-499人", "collectedAt": "2026-02-08", "sourceJobId": "JOB560485", "education": "硕士", "experience": "不限", "views": 2307, "applications": 152, "tags": ["Git", "JavaScript", "PyTorch", "Python", "Linux", "算法"]}',
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

-- JD 6: 数据工程师（平安科技·上海）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    35, 1,
    '数据工程师',
    '平安科技',
    '岗位职责：参与技术方案评审，解决技术难题
持续优化系统性能和用户体验
负责数据工程师相关的系统设计和开发工作
岗位要求：3-5年以上相关工作经验
具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["Pandas", "高并发", "深度学习"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n持续优化系统性能和用户体验\\n负责数据工程师相关的系统设计和开发工作"], "experienceRequirements": ["3-5年以上相关工作经验\\n具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "上海", "salary": "23749-38592元/月", "salaryMin": 23749, "salaryMax": 38592, "salaryAvg": 31170, "platform": "招聘平台", "industry": "房地产", "companySize": "0-20人", "collectedAt": "2026-02-10", "sourceJobId": "JOB676818", "education": "硕士", "experience": "3-5年", "views": 4183, "applications": 165, "tags": ["Pandas", "高并发", "深度学习"]}',
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

-- JD 7: 后端开发工程师（优酷·杭州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    36, 1,
    '后端开发工程师',
    '优酷',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
参与技术方案评审，解决技术难题
岗位要求：具备良好的沟通能力和团队协作精神
不限及以上学历，计算机相关专业优先
熟悉相关技术栈和开发工具
有大型项目经验者优先',
    '{"requiredSkills": ["Java", "Scikit-learn", "PyTorch"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n不限及以上学历，计算机相关专业优先\\n熟悉相关技术栈和开发工具\\n有大型项目经验者优先"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "杭州", "salary": "8437-13710元/月", "salaryMin": 8437, "salaryMax": 13710, "salaryAvg": 11073, "platform": "招聘平台", "industry": "物流", "companySize": "500-999人", "collectedAt": "2026-02-27", "sourceJobId": "JOB563513", "education": "不限", "experience": "1-3年", "views": 1029, "applications": 123, "tags": ["Java", "Scikit-learn", "PyTorch"]}',
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

-- JD 8: Java开发工程师（滴滴·杭州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    37, 1,
    'Java开发工程师',
    '滴滴',
    '岗位职责：持续优化系统性能和用户体验
参与技术方案评审，解决技术难题
编写高质量、可维护的代码
岗位要求：有大型项目经验者优先
应届生以上相关工作经验
熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Kubernetes", "Git", "Docker", "Pandas"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n参与技术方案评审", "解决技术难题\\n编写高质量、可维护的代码"], "experienceRequirements": ["有大型项目经验者优先\\n应届生以上相关工作经验\\n熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "杭州", "salary": "16007-26011元/月", "salaryMin": 16007, "salaryMax": 26011, "salaryAvg": 21009, "platform": "招聘平台", "industry": "能源", "companySize": "20-99人", "collectedAt": "2026-03-03", "sourceJobId": "JOB246566", "education": "博士", "experience": "应届生", "views": 4108, "applications": 162, "tags": ["Kubernetes", "Git", "Docker", "Pandas"]}',
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

-- JD 9: Java开发工程师（美团·成都）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    38, 1,
    'Java开发工程师',
    '美团',
    '岗位职责：参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
岗位要求：具备良好的沟通能力和团队协作精神
熟悉相关技术栈和开发工具
应届生以上相关工作经验
硕士及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["Git", "TensorFlow", "深度学习", "微服务", "算法", "Python"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n熟悉相关技术栈和开发工具\\n应届生以上相关工作经验\\n硕士及以上学历，计算机相关专业优先"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "成都", "salary": "6213-10097元/月", "salaryMin": 6213, "salaryMax": 10097, "salaryAvg": 8155, "platform": "招聘平台", "industry": "其他", "companySize": "1000-9999人", "collectedAt": "2026-02-20", "sourceJobId": "JOB573866", "education": "硕士", "experience": "应届生", "views": 947, "applications": 92, "tags": ["Git", "TensorFlow", "深度学习", "微服务", "算法", "Python"]}',
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

-- JD 10: 机器学习工程师（搜狐·天津）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    39, 1,
    '机器学习工程师',
    '搜狐',
    '岗位职责：编写高质量、可维护的代码
参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
岗位要求：具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神
本科及以上学历，计算机相关专业优先
有大型项目经验者优先',
    '{"requiredSkills": ["机器学习", "Spark", "PyTorch", "Vue", "Docker", "分布式系统"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神\\n本科及以上学历，计算机相关专业优先\\n有大型项目经验者优先"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "天津", "salary": "6499-10561元/月", "salaryMin": 6499, "salaryMax": 10561, "salaryAvg": 8530, "platform": "招聘平台", "industry": "物流", "companySize": "10000人以上", "collectedAt": "2026-02-14", "sourceJobId": "JOB849014", "education": "本科", "experience": "应届生", "views": 2841, "applications": 21, "tags": ["机器学习", "Spark", "PyTorch", "Vue", "Docker", "分布式系统"]}',
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

-- JD 11: 机器学习工程师（360·天津）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    40, 1,
    '机器学习工程师',
    '360',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
持续优化系统性能和用户体验
岗位要求：有大型项目经验者优先
熟悉相关技术栈和开发工具
3-5年以上相关工作经验
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["MongoDB", "JavaScript", "TensorFlow", "Pandas"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码\\n持续优化系统性能和用户体验"], "experienceRequirements": ["有大型项目经验者优先\\n熟悉相关技术栈和开发工具\\n3-5年以上相关工作经验\\n具备良好的编程基础和算法能力"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "天津", "salary": "23888-38818元/月", "salaryMin": 23888, "salaryMax": 38818, "salaryAvg": 31353, "platform": "招聘平台", "industry": "其他", "companySize": "10000人以上", "collectedAt": "2026-02-12", "sourceJobId": "JOB445705", "education": "博士", "experience": "3-5年", "views": 794, "applications": 84, "tags": ["MongoDB", "JavaScript", "TensorFlow", "Pandas"]}',
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

-- JD 12: 算法工程师（蚂蚁集团·青岛）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    41, 1,
    '算法工程师',
    '蚂蚁集团',
    '岗位职责：参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
岗位要求：熟悉相关技术栈和开发工具
有大型项目经验者优先
大专及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Redis", "Hadoop", "分布式系统"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n有大型项目经验者优先\\n大专及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "青岛", "salary": "5700-9263元/月", "salaryMin": 5700, "salaryMax": 9263, "salaryAvg": 7481, "platform": "招聘平台", "industry": "教育", "companySize": "20-99人", "collectedAt": "2026-03-06", "sourceJobId": "JOB335445", "education": "大专", "experience": "不限", "views": 2196, "applications": 60, "tags": ["Redis", "Hadoop", "分布式系统"]}',
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

-- JD 13: 算法工程师（蚂蚁集团·广州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    42, 1,
    '算法工程师',
    '蚂蚁集团',
    '岗位职责：参与技术方案评审，解决技术难题
持续优化系统性能和用户体验
编写高质量、可维护的代码
岗位要求：10年以上以上相关工作经验
有大型项目经验者优先
大专及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["深度学习", "MongoDB", "分布式系统", "Scikit-learn", "Vue"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n持续优化系统性能和用户体验\\n编写高质量、可维护的代码"], "experienceRequirements": ["10年以上以上相关工作经验\\n有大型项目经验者优先\\n大专及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "广州", "salary": "22218-36105元/月", "salaryMin": 22218, "salaryMax": 36105, "salaryAvg": 29161, "platform": "招聘平台", "industry": "互联网", "companySize": "20-99人", "collectedAt": "2026-02-10", "sourceJobId": "JOB926823", "education": "大专", "experience": "10年以上", "views": 3849, "applications": 14, "tags": ["深度学习", "MongoDB", "分布式系统", "Scikit-learn", "Vue"]}',
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

-- JD 14: Python开发工程师（海尔·杭州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    43, 1,
    'Python开发工程师',
    '海尔',
    '岗位职责：负责Python开发工程师相关的系统设计和开发工作
参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
岗位要求：有大型项目经验者优先
具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力
博士及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["分布式系统", "TensorFlow", "JavaScript", "React", "MongoDB", "Scikit-learn"], "preferredSkills": [], "responsibilities": ["负责Python开发工程师相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力\\n博士及以上学历，计算机相关专业优先"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "杭州", "salary": "15428-25071元/月", "salaryMin": 15428, "salaryMax": 25071, "salaryAvg": 20249, "platform": "招聘平台", "industry": "房地产", "companySize": "1000-9999人", "collectedAt": "2026-02-06", "sourceJobId": "JOB741674", "education": "博士", "experience": "不限", "views": 4616, "applications": 168, "tags": ["分布式系统", "TensorFlow", "JavaScript", "React", "MongoDB", "Scikit-learn"]}',
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

-- JD 15: 测试工程师（新浪·青岛）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    44, 1,
    '测试工程师',
    '新浪',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
负责测试工程师相关的系统设计和开发工作
编写高质量、可维护的代码
岗位要求：1-3年以上相关工作经验
熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神
有大型项目经验者优先',
    '{"requiredSkills": ["MongoDB", "NumPy", "Jenkins", "TypeScript", "Pandas", "数据结构"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n负责测试工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码"], "experienceRequirements": ["1-3年以上相关工作经验\\n熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神\\n有大型项目经验者优先"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "青岛", "salary": "5949-9668元/月", "salaryMin": 5949, "salaryMax": 9668, "salaryAvg": 7808, "platform": "招聘平台", "industry": "金融", "companySize": "0-20人", "collectedAt": "2026-02-14", "sourceJobId": "JOB354235", "education": "不限", "experience": "1-3年", "views": 1733, "applications": 198, "tags": ["MongoDB", "NumPy", "Jenkins", "TypeScript", "Pandas", "数据结构"]}',
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

-- JD 16: 机器学习工程师（比亚迪·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    45, 1,
    '机器学习工程师',
    '比亚迪',
    '岗位职责：参与技术方案评审，解决技术难题
持续优化系统性能和用户体验
编写高质量、可维护的代码
岗位要求：1-3年以上相关工作经验
有大型项目经验者优先
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["微服务", "Kubernetes", "JavaScript", "Docker", "Spark"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n持续优化系统性能和用户体验\\n编写高质量、可维护的代码"], "experienceRequirements": ["1-3年以上相关工作经验\\n有大型项目经验者优先\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "13457-21868元/月", "salaryMin": 13457, "salaryMax": 21868, "salaryAvg": 17662, "platform": "招聘平台", "industry": "零售", "companySize": "100-499人", "collectedAt": "2026-02-08", "sourceJobId": "JOB212214", "education": "本科", "experience": "1-3年", "views": 760, "applications": 172, "tags": ["微服务", "Kubernetes", "JavaScript", "Docker", "Spark"]}',
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

-- JD 17: 技术总监（顺丰·上海）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    46, 1,
    '技术总监',
    '顺丰',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
编写高质量、可维护的代码
岗位要求：熟悉相关技术栈和开发工具
硕士及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Hadoop", "高并发", "MongoDB", "Java"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验\\n编写高质量、可维护的代码"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n硕士及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "上海", "salary": "50057-81343元/月", "salaryMin": 50057, "salaryMax": 81343, "salaryAvg": 65700, "platform": "招聘平台", "industry": "互联网", "companySize": "10000人以上", "collectedAt": "2026-02-16", "sourceJobId": "JOB384263", "education": "硕士", "experience": "10年以上", "views": 2069, "applications": 150, "tags": ["Hadoop", "高并发", "MongoDB", "Java"]}',
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

-- JD 18: Java开发工程师（美团·上海）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    47, 1,
    'Java开发工程师',
    '美团',
    '岗位职责：参与技术方案评审，解决技术难题
负责Java开发工程师相关的系统设计和开发工作
编写高质量、可维护的代码
岗位要求：具备良好的沟通能力和团队协作精神
10年以上以上相关工作经验
有大型项目经验者优先
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["高并发", "Kubernetes", "Java", "Redis", "Linux"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n负责Java开发工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n10年以上以上相关工作经验\\n有大型项目经验者优先\\n具备良好的编程基础和算法能力"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "上海", "salary": "40114-65185元/月", "salaryMin": 40114, "salaryMax": 65185, "salaryAvg": 52649, "platform": "招聘平台", "industry": "其他", "companySize": "1000-9999人", "collectedAt": "2026-02-22", "sourceJobId": "JOB527865", "education": "硕士", "experience": "10年以上", "views": 1488, "applications": 40, "tags": ["高并发", "Kubernetes", "Java", "Redis", "Linux"]}',
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

-- JD 19: 数据工程师（美团·上海）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    48, 1,
    '数据工程师',
    '美团',
    '岗位职责：持续优化系统性能和用户体验
参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
岗位要求：熟悉相关技术栈和开发工具
硕士及以上学历，计算机相关专业优先
有大型项目经验者优先
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Redis", "MongoDB", "Git", "微服务", "Spring Boot"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n硕士及以上学历，计算机相关专业优先\\n有大型项目经验者优先\\n具备良好的编程基础和算法能力"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "上海", "salary": "26048-42329元/月", "salaryMin": 26048, "salaryMax": 42329, "salaryAvg": 34188, "platform": "招聘平台", "industry": "教育", "companySize": "0-20人", "collectedAt": "2026-02-12", "sourceJobId": "JOB461724", "education": "硕士", "experience": "5-10年", "views": 3806, "applications": 86, "tags": ["Redis", "MongoDB", "Git", "微服务", "Spring Boot"]}',
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

-- JD 20: 架构师（华为·成都）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    49, 1,
    '架构师',
    '华为',
    '岗位职责：参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
负责架构师相关的系统设计和开发工作
岗位要求：具备良好的沟通能力和团队协作精神
大专及以上学历，计算机相关专业优先
5-10年以上相关工作经验
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["Linux", "Pandas", "Jenkins", "Spark", "NumPy", "Git"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试\\n负责架构师相关的系统设计和开发工作"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n大专及以上学历，计算机相关专业优先\\n5-10年以上相关工作经验\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "成都", "salary": "12926-21004元/月", "salaryMin": 12926, "salaryMax": 21004, "salaryAvg": 16965, "platform": "招聘平台", "industry": "房地产", "companySize": "20-99人", "collectedAt": "2026-03-05", "sourceJobId": "JOB749498", "education": "大专", "experience": "5-10年", "views": 2351, "applications": 62, "tags": ["Linux", "Pandas", "Jenkins", "Spark", "NumPy", "Git"]}',
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

-- JD 21: 后端开发工程师（中通·南京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    50, 1,
    '后端开发工程师',
    '中通',
    '岗位职责：参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
岗位要求：大专及以上学历，计算机相关专业优先
1-3年以上相关工作经验
熟悉相关技术栈和开发工具
有大型项目经验者优先',
    '{"requiredSkills": ["Java", "Spark", "NumPy", "MongoDB", "分布式系统", "TypeScript"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验"], "experienceRequirements": ["大专及以上学历，计算机相关专业优先\\n1-3年以上相关工作经验\\n熟悉相关技术栈和开发工具\\n有大型项目经验者优先"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "南京", "salary": "9100-14788元/月", "salaryMin": 9100, "salaryMax": 14788, "salaryAvg": 11944, "platform": "招聘平台", "industry": "能源", "companySize": "20-99人", "collectedAt": "2026-02-11", "sourceJobId": "JOB199807", "education": "大专", "experience": "1-3年", "views": 939, "applications": 31, "tags": ["Java", "Spark", "NumPy", "MongoDB", "分布式系统", "TypeScript"]}',
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

-- JD 22: 深度学习工程师（大华股份·重庆）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    51, 1,
    '深度学习工程师',
    '大华股份',
    '岗位职责：持续优化系统性能和用户体验
编写高质量、可维护的代码
参与技术方案评审，解决技术难题
岗位要求：博士及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力
3-5年以上相关工作经验
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["MySQL", "Scikit-learn", "JavaScript", "Django"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["博士及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力\\n3-5年以上相关工作经验\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "重庆", "salary": "20252-32910元/月", "salaryMin": 20252, "salaryMax": 32910, "salaryAvg": 26581, "platform": "招聘平台", "industry": "教育", "companySize": "100-499人", "collectedAt": "2026-02-15", "sourceJobId": "JOB888996", "education": "博士", "experience": "3-5年", "views": 4274, "applications": 53, "tags": ["MySQL", "Scikit-learn", "JavaScript", "Django"]}',
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

-- JD 23: 后端开发工程师（依图科技·北京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    52, 1,
    '后端开发工程师',
    '依图科技',
    '岗位职责：参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
岗位要求：3-5年以上相关工作经验
熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力
有大型项目经验者优先',
    '{"requiredSkills": ["深度学习", "微服务", "Hadoop"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验"], "experienceRequirements": ["3-5年以上相关工作经验\\n熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力\\n有大型项目经验者优先"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "北京", "salary": "27036-43933元/月", "salaryMin": 27036, "salaryMax": 43933, "salaryAvg": 35484, "platform": "招聘平台", "industry": "教育", "companySize": "20-99人", "collectedAt": "2026-02-08", "sourceJobId": "JOB146177", "education": "博士", "experience": "3-5年", "views": 738, "applications": 151, "tags": ["深度学习", "微服务", "Hadoop"]}',
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

-- JD 24: 机器学习工程师（新浪·天津）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    53, 1,
    '机器学习工程师',
    '新浪',
    '岗位职责：编写高质量、可维护的代码
持续优化系统性能和用户体验
负责机器学习工程师相关的系统设计和开发工作
岗位要求：具备良好的沟通能力和团队协作精神
熟悉相关技术栈和开发工具
大专及以上学历，计算机相关专业优先
1-3年以上相关工作经验',
    '{"requiredSkills": ["JavaScript", "大数据", "Git", "高并发", "Redis", "数据结构"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n持续优化系统性能和用户体验\\n负责机器学习工程师相关的系统设计和开发工作"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n熟悉相关技术栈和开发工具\\n大专及以上学历，计算机相关专业优先\\n1-3年以上相关工作经验"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "天津", "salary": "5950-9668元/月", "salaryMin": 5950, "salaryMax": 9668, "salaryAvg": 7809, "platform": "招聘平台", "industry": "互联网", "companySize": "1000-9999人", "collectedAt": "2026-02-28", "sourceJobId": "JOB490917", "education": "大专", "experience": "1-3年", "views": 2488, "applications": 149, "tags": ["JavaScript", "大数据", "Git", "高并发", "Redis", "数据结构"]}',
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

-- JD 25: 前端开发工程师（海康威视·南京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    54, 1,
    '前端开发工程师',
    '海康威视',
    '岗位职责：参与技术方案评审，解决技术难题
编写高质量、可维护的代码
负责前端开发工程师相关的系统设计和开发工作
岗位要求：博士及以上学历，计算机相关专业优先
3-5年以上相关工作经验
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["Spark", "数据结构", "Spring Boot", "Git", "Redis"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n编写高质量、可维护的代码\\n负责前端开发工程师相关的系统设计和开发工作"], "experienceRequirements": ["博士及以上学历，计算机相关专业优先\\n3-5年以上相关工作经验\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "南京", "salary": "25237-41010元/月", "salaryMin": 25237, "salaryMax": 41010, "salaryAvg": 33123, "platform": "招聘平台", "industry": "能源", "companySize": "0-20人", "collectedAt": "2026-02-12", "sourceJobId": "JOB262959", "education": "博士", "experience": "3-5年", "views": 430, "applications": 60, "tags": ["Spark", "数据结构", "Spring Boot", "Git", "Redis"]}',
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

-- JD 26: 算法工程师（阿里巴巴·杭州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    55, 1,
    '算法工程师',
    '阿里巴巴',
    '岗位职责：编写高质量、可维护的代码
参与技术方案评审，解决技术难题
负责算法工程师相关的系统设计和开发工作
岗位要求：熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神
不限以上相关工作经验
大专及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["算法", "Linux", "Vue", "Hadoop", "Jenkins", "Kubernetes"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题\\n负责算法工程师相关的系统设计和开发工作"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神\\n不限以上相关工作经验\\n大专及以上学历，计算机相关专业优先"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "杭州", "salary": "7478-12152元/月", "salaryMin": 7478, "salaryMax": 12152, "salaryAvg": 9815, "platform": "招聘平台", "industry": "房地产", "companySize": "0-20人", "collectedAt": "2026-02-20", "sourceJobId": "JOB826847", "education": "大专", "experience": "不限", "views": 703, "applications": 13, "tags": ["算法", "Linux", "Vue", "Hadoop", "Jenkins", "Kubernetes"]}',
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

-- JD 27: 算法工程师（蚂蚁集团·杭州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    56, 1,
    '算法工程师',
    '蚂蚁集团',
    '岗位职责：负责算法工程师相关的系统设计和开发工作
编写高质量、可维护的代码
参与技术方案评审，解决技术难题
岗位要求：具备良好的沟通能力和团队协作精神
熟悉相关技术栈和开发工具
大专及以上学历，计算机相关专业优先
应届生以上相关工作经验',
    '{"requiredSkills": ["算法", "JavaScript", "深度学习", "Spark"], "preferredSkills": [], "responsibilities": ["负责算法工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n熟悉相关技术栈和开发工具\\n大专及以上学历，计算机相关专业优先\\n应届生以上相关工作经验"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "杭州", "salary": "5137-8347元/月", "salaryMin": 5137, "salaryMax": 8347, "salaryAvg": 6742, "platform": "招聘平台", "industry": "房地产", "companySize": "20-99人", "collectedAt": "2026-02-04", "sourceJobId": "JOB409234", "education": "大专", "experience": "应届生", "views": 1451, "applications": 160, "tags": ["算法", "JavaScript", "深度学习", "Spark"]}',
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

-- JD 28: 架构师（商汤科技·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    57, 1,
    '架构师',
    '商汤科技',
    '岗位职责：编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
负责架构师相关的系统设计和开发工作
岗位要求：熟悉相关技术栈和开发工具
有大型项目经验者优先
具备良好的沟通能力和团队协作精神
3-5年以上相关工作经验',
    '{"requiredSkills": ["大数据", "算法", "JavaScript", "Docker", "分布式系统"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试\\n负责架构师相关的系统设计和开发工作"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神\\n3-5年以上相关工作经验"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "17988-29231元/月", "salaryMin": 17988, "salaryMax": 29231, "salaryAvg": 23609, "platform": "招聘平台", "industry": "其他", "companySize": "500-999人", "collectedAt": "2026-02-08", "sourceJobId": "JOB148118", "education": "本科", "experience": "3-5年", "views": 3259, "applications": 181, "tags": ["大数据", "算法", "JavaScript", "Docker", "分布式系统"]}',
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

-- JD 29: 技术总监（京东·武汉）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    58, 1,
    '技术总监',
    '京东',
    '岗位职责：持续优化系统性能和用户体验
参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
岗位要求：具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神
博士及以上学历，计算机相关专业优先
应届生以上相关工作经验',
    '{"requiredSkills": ["NumPy", "高并发", "Pandas"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码"], "experienceRequirements": ["具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神\\n博士及以上学历，计算机相关专业优先\\n应届生以上相关工作经验"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "武汉", "salary": "8780-14268元/月", "salaryMin": 8780, "salaryMax": 14268, "salaryAvg": 11524, "platform": "招聘平台", "industry": "零售", "companySize": "100-499人", "collectedAt": "2026-02-13", "sourceJobId": "JOB611833", "education": "博士", "experience": "应届生", "views": 2136, "applications": 128, "tags": ["NumPy", "高并发", "Pandas"]}',
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

-- JD 30: 运维工程师（宁德时代·长沙）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    59, 1,
    '运维工程师',
    '宁德时代',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
负责运维工程师相关的系统设计和开发工作
岗位要求：具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力
硕士及以上学历，计算机相关专业优先
1-3年以上相关工作经验',
    '{"requiredSkills": ["TypeScript", "PyTorch", "高并发", "Pandas", "算法", "深度学习"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题\\n负责运维工程师相关的系统设计和开发工作"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力\\n硕士及以上学历，计算机相关专业优先\\n1-3年以上相关工作经验"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "长沙", "salary": "11529-18736元/月", "salaryMin": 11529, "salaryMax": 18736, "salaryAvg": 15132, "platform": "招聘平台", "industry": "零售", "companySize": "0-20人", "collectedAt": "2026-02-22", "sourceJobId": "JOB980155", "education": "硕士", "experience": "1-3年", "views": 3512, "applications": 180, "tags": ["TypeScript", "PyTorch", "高并发", "Pandas", "算法", "深度学习"]}',
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

-- JD 31: Python开发工程师（阿里巴巴·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    60, 1,
    'Python开发工程师',
    '阿里巴巴',
    '岗位职责：参与技术方案评审，解决技术难题
负责Python开发工程师相关的系统设计和开发工作
持续优化系统性能和用户体验
岗位要求：具备良好的沟通能力和团队协作精神
熟悉相关技术栈和开发工具
不限及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Pandas", "Scikit-learn", "数据结构", "Spring Boot"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n负责Python开发工程师相关的系统设计和开发工作\\n持续优化系统性能和用户体验"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n熟悉相关技术栈和开发工具\\n不限及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "33391-54260元/月", "salaryMin": 33391, "salaryMax": 54260, "salaryAvg": 43825, "platform": "招聘平台", "industry": "金融", "companySize": "10000人以上", "collectedAt": "2026-02-13", "sourceJobId": "JOB638857", "education": "不限", "experience": "10年以上", "views": 1359, "applications": 11, "tags": ["Pandas", "Scikit-learn", "数据结构", "Spring Boot"]}',
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

-- JD 32: 数据工程师（携程·天津）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    61, 1,
    '数据工程师',
    '携程',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
编写高质量、可维护的代码
岗位要求：具备良好的沟通能力和团队协作精神
博士及以上学历，计算机相关专业优先
5-10年以上相关工作经验
有大型项目经验者优先',
    '{"requiredSkills": ["Java", "Django", "Python", "React", "TensorFlow", "分布式系统"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验\\n编写高质量、可维护的代码"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n博士及以上学历，计算机相关专业优先\\n5-10年以上相关工作经验\\n有大型项目经验者优先"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "天津", "salary": "26288-42718元/月", "salaryMin": 26288, "salaryMax": 42718, "salaryAvg": 34503, "platform": "招聘平台", "industry": "教育", "companySize": "1000-9999人", "collectedAt": "2026-03-01", "sourceJobId": "JOB855242", "education": "博士", "experience": "5-10年", "views": 2215, "applications": 51, "tags": ["Java", "Django", "Python", "React", "TensorFlow", "分布式系统"]}',
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

-- JD 33: 前端开发工程师（360·武汉）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    62, 1,
    '前端开发工程师',
    '360',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
编写高质量、可维护的代码
岗位要求：具备良好的沟通能力和团队协作精神
熟悉相关技术栈和开发工具
3-5年以上相关工作经验
有大型项目经验者优先',
    '{"requiredSkills": ["PyTorch", "MySQL", "Pandas", "Kubernetes", "数据结构"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题\\n编写高质量、可维护的代码"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n熟悉相关技术栈和开发工具\\n3-5年以上相关工作经验\\n有大型项目经验者优先"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "武汉", "salary": "11450-18606元/月", "salaryMin": 11450, "salaryMax": 18606, "salaryAvg": 15028, "platform": "招聘平台", "industry": "房地产", "companySize": "1000-9999人", "collectedAt": "2026-02-17", "sourceJobId": "JOB479867", "education": "不限", "experience": "3-5年", "views": 4645, "applications": 128, "tags": ["PyTorch", "MySQL", "Pandas", "Kubernetes", "数据结构"]}',
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

-- JD 34: 数据分析师（大疆创新·天津）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    63, 1,
    '数据分析师',
    '大疆创新',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
参与技术方案评审，解决技术难题
岗位要求：有大型项目经验者优先
应届生以上相关工作经验
具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Docker", "Spring Boot", "大数据", "TensorFlow", "MySQL"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["有大型项目经验者优先\\n应届生以上相关工作经验\\n具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "天津", "salary": "6769-11000元/月", "salaryMin": 6769, "salaryMax": 11000, "salaryAvg": 8884, "platform": "招聘平台", "industry": "物流", "companySize": "20-99人", "collectedAt": "2026-02-27", "sourceJobId": "JOB401244", "education": "硕士", "experience": "应届生", "views": 2274, "applications": 57, "tags": ["Docker", "Spring Boot", "大数据", "TensorFlow", "MySQL"]}',
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

-- JD 35: 数据工程师（360·杭州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    64, 1,
    '数据工程师',
    '360',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
持续优化系统性能和用户体验
岗位要求：熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神
硕士及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Java", "机器学习", "分布式系统"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码\\n持续优化系统性能和用户体验"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神\\n硕士及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "杭州", "salary": "31694-51504元/月", "salaryMin": 31694, "salaryMax": 51504, "salaryAvg": 41599, "platform": "招聘平台", "industry": "制造业", "companySize": "20-99人", "collectedAt": "2026-02-05", "sourceJobId": "JOB943797", "education": "硕士", "experience": "5-10年", "views": 4129, "applications": 198, "tags": ["Java", "机器学习", "分布式系统"]}',
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

-- JD 36: 前端开发工程师（拼多多·北京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    65, 1,
    '前端开发工程师',
    '拼多多',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
负责前端开发工程师相关的系统设计和开发工作
参与技术方案评审，解决技术难题
岗位要求：不限及以上学历，计算机相关专业优先
熟悉相关技术栈和开发工具
应届生以上相关工作经验
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["React", "NumPy", "Git", "算法", "Java"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n负责前端开发工程师相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["不限及以上学历，计算机相关专业优先\\n熟悉相关技术栈和开发工具\\n应届生以上相关工作经验\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "北京", "salary": "8847-14377元/月", "salaryMin": 8847, "salaryMax": 14377, "salaryAvg": 11612, "platform": "招聘平台", "industry": "制造业", "companySize": "0-20人", "collectedAt": "2026-02-21", "sourceJobId": "JOB436219", "education": "不限", "experience": "应届生", "views": 2652, "applications": 132, "tags": ["React", "NumPy", "Git", "算法", "Java"]}',
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

-- JD 37: 后端开发工程师（携程·南京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    66, 1,
    '后端开发工程师',
    '携程',
    '岗位职责：负责后端开发工程师相关的系统设计和开发工作
持续优化系统性能和用户体验
参与需求分析、系统设计、编码实现和测试
岗位要求：熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力
10年以上以上相关工作经验
有大型项目经验者优先',
    '{"requiredSkills": ["Hadoop", "MongoDB", "Vue", "Scikit-learn", "算法"], "preferredSkills": [], "responsibilities": ["负责后端开发工程师相关的系统设计和开发工作\\n持续优化系统性能和用户体验\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力\\n10年以上以上相关工作经验\\n有大型项目经验者优先"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "南京", "salary": "19416-31551元/月", "salaryMin": 19416, "salaryMax": 31551, "salaryAvg": 25483, "platform": "招聘平台", "industry": "房地产", "companySize": "500-999人", "collectedAt": "2026-02-06", "sourceJobId": "JOB873449", "education": "大专", "experience": "10年以上", "views": 526, "applications": 175, "tags": ["Hadoop", "MongoDB", "Vue", "Scikit-learn", "算法"]}',
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

-- JD 38: 后端开发工程师（阿里巴巴·天津）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    67, 1,
    '后端开发工程师',
    '阿里巴巴',
    '岗位职责：负责后端开发工程师相关的系统设计和开发工作
持续优化系统性能和用户体验
编写高质量、可维护的代码
岗位要求：有大型项目经验者优先
熟悉相关技术栈和开发工具
3-5年以上相关工作经验
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Kubernetes", "微服务", "Django"], "preferredSkills": [], "responsibilities": ["负责后端开发工程师相关的系统设计和开发工作\\n持续优化系统性能和用户体验\\n编写高质量、可维护的代码"], "experienceRequirements": ["有大型项目经验者优先\\n熟悉相关技术栈和开发工具\\n3-5年以上相关工作经验\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "天津", "salary": "17367-28221元/月", "salaryMin": 17367, "salaryMax": 28221, "salaryAvg": 22794, "platform": "招聘平台", "industry": "制造业", "companySize": "20-99人", "collectedAt": "2026-02-10", "sourceJobId": "JOB328367", "education": "博士", "experience": "3-5年", "views": 833, "applications": 68, "tags": ["Kubernetes", "微服务", "Django"]}',
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

-- JD 39: Java开发工程师（蚂蚁集团·广州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    68, 1,
    'Java开发工程师',
    '蚂蚁集团',
    '岗位职责：编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
岗位要求：具备良好的编程基础和算法能力
不限及以上学历，计算机相关专业优先
有大型项目经验者优先
3-5年以上相关工作经验',
    '{"requiredSkills": ["Java", "Docker", "Pandas", "Scikit-learn"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验"], "experienceRequirements": ["具备良好的编程基础和算法能力\\n不限及以上学历，计算机相关专业优先\\n有大型项目经验者优先\\n3-5年以上相关工作经验"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "广州", "salary": "12474-20271元/月", "salaryMin": 12474, "salaryMax": 20271, "salaryAvg": 16372, "platform": "招聘平台", "industry": "房地产", "companySize": "500-999人", "collectedAt": "2026-02-06", "sourceJobId": "JOB847078", "education": "不限", "experience": "3-5年", "views": 341, "applications": 147, "tags": ["Java", "Docker", "Pandas", "Scikit-learn"]}',
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

-- JD 40: 运维工程师（海康威视·郑州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    69, 1,
    '运维工程师',
    '海康威视',
    '岗位职责：负责运维工程师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
岗位要求：具备良好的编程基础和算法能力
有大型项目经验者优先
本科及以上学历，计算机相关专业优先
10年以上以上相关工作经验',
    '{"requiredSkills": ["Docker", "深度学习", "微服务", "Scikit-learn", "Spring Boot"], "preferredSkills": [], "responsibilities": ["负责运维工程师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码"], "experienceRequirements": ["具备良好的编程基础和算法能力\\n有大型项目经验者优先\\n本科及以上学历，计算机相关专业优先\\n10年以上以上相关工作经验"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "郑州", "salary": "21490-34922元/月", "salaryMin": 21490, "salaryMax": 34922, "salaryAvg": 28206, "platform": "招聘平台", "industry": "互联网", "companySize": "500-999人", "collectedAt": "2026-02-25", "sourceJobId": "JOB755843", "education": "本科", "experience": "10年以上", "views": 4250, "applications": 145, "tags": ["Docker", "深度学习", "微服务", "Scikit-learn", "Spring Boot"]}',
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

-- JD 41: Python开发工程师（云从科技·天津）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    70, 1,
    'Python开发工程师',
    '云从科技',
    '岗位职责：编写高质量、可维护的代码
持续优化系统性能和用户体验
参与技术方案评审，解决技术难题
岗位要求：熟悉相关技术栈和开发工具
有大型项目经验者优先
硕士及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["分布式系统", "Vue", "Spring Boot"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n持续优化系统性能和用户体验\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n有大型项目经验者优先\\n硕士及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "天津", "salary": "16491-26798元/月", "salaryMin": 16491, "salaryMax": 26798, "salaryAvg": 21644, "platform": "招聘平台", "industry": "其他", "companySize": "10000人以上", "collectedAt": "2026-03-02", "sourceJobId": "JOB439658", "education": "硕士", "experience": "3-5年", "views": 4044, "applications": 44, "tags": ["分布式系统", "Vue", "Spring Boot"]}',
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

-- JD 42: DevOps工程师（格力·广州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    71, 1,
    'DevOps工程师',
    '格力',
    '岗位职责：持续优化系统性能和用户体验
编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
岗位要求：5-10年以上相关工作经验
大专及以上学历，计算机相关专业优先
有大型项目经验者优先
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Spring Boot", "MongoDB", "Hadoop", "Jenkins"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["5-10年以上相关工作经验\\n大专及以上学历，计算机相关专业优先\\n有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "广州", "salary": "20419-33181元/月", "salaryMin": 20419, "salaryMax": 33181, "salaryAvg": 26800, "platform": "招聘平台", "industry": "物流", "companySize": "0-20人", "collectedAt": "2026-02-11", "sourceJobId": "JOB492393", "education": "大专", "experience": "5-10年", "views": 399, "applications": 122, "tags": ["Spring Boot", "MongoDB", "Hadoop", "Jenkins"]}',
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

-- JD 43: 算法工程师（宁德时代·西安）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    72, 1,
    '算法工程师',
    '宁德时代',
    '岗位职责：参与技术方案评审，解决技术难题
负责算法工程师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
岗位要求：3-5年以上相关工作经验
具备良好的沟通能力和团队协作精神
熟悉相关技术栈和开发工具
有大型项目经验者优先',
    '{"requiredSkills": ["Django", "PyTorch", "Spark", "深度学习", "Vue", "Jenkins"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n负责算法工程师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["3-5年以上相关工作经验\\n具备良好的沟通能力和团队协作精神\\n熟悉相关技术栈和开发工具\\n有大型项目经验者优先"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "西安", "salary": "8887-14441元/月", "salaryMin": 8887, "salaryMax": 14441, "salaryAvg": 11664, "platform": "招聘平台", "industry": "房地产", "companySize": "500-999人", "collectedAt": "2026-02-16", "sourceJobId": "JOB935461", "education": "大专", "experience": "3-5年", "views": 2903, "applications": 144, "tags": ["Django", "PyTorch", "Spark", "深度学习", "Vue", "Jenkins"]}',
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

-- JD 44: 深度学习工程师（优酷·西安）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    73, 1,
    '深度学习工程师',
    '优酷',
    '岗位职责：编写高质量、可维护的代码
负责深度学习工程师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
岗位要求：博士及以上学历，计算机相关专业优先
1-3年以上相关工作经验
熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Java", "NumPy", "深度学习"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n负责深度学习工程师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["博士及以上学历，计算机相关专业优先\\n1-3年以上相关工作经验\\n熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "西安", "salary": "14326-23280元/月", "salaryMin": 14326, "salaryMax": 23280, "salaryAvg": 18803, "platform": "招聘平台", "industry": "零售", "companySize": "0-20人", "collectedAt": "2026-02-16", "sourceJobId": "JOB711033", "education": "博士", "experience": "1-3年", "views": 3329, "applications": 165, "tags": ["Java", "NumPy", "深度学习"]}',
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

-- JD 45: 后端开发工程师（宁德时代·南京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    74, 1,
    '后端开发工程师',
    '宁德时代',
    '岗位职责：负责后端开发工程师相关的系统设计和开发工作
参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
岗位要求：具备良好的沟通能力和团队协作精神
熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力
不限以上相关工作经验',
    '{"requiredSkills": ["大数据", "MongoDB", "深度学习"], "preferredSkills": [], "responsibilities": ["负责后端开发工程师相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力\\n不限以上相关工作经验"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "南京", "salary": "5800-9425元/月", "salaryMin": 5800, "salaryMax": 9425, "salaryAvg": 7612, "platform": "招聘平台", "industry": "制造业", "companySize": "1000-9999人", "collectedAt": "2026-02-23", "sourceJobId": "JOB739705", "education": "大专", "experience": "不限", "views": 2513, "applications": 79, "tags": ["大数据", "MongoDB", "深度学习"]}',
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

-- JD 46: 运维工程师（当当网·青岛）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    75, 1,
    '运维工程师',
    '当当网',
    '岗位职责：持续优化系统性能和用户体验
负责运维工程师相关的系统设计和开发工作
编写高质量、可维护的代码
岗位要求：具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神
不限及以上学历，计算机相关专业优先
不限以上相关工作经验',
    '{"requiredSkills": ["Python", "Scikit-learn", "Jenkins", "TensorFlow", "Vue"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n负责运维工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码"], "experienceRequirements": ["具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神\\n不限及以上学历，计算机相关专业优先\\n不限以上相关工作经验"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "青岛", "salary": "4998-8122元/月", "salaryMin": 4998, "salaryMax": 8122, "salaryAvg": 6560, "platform": "招聘平台", "industry": "其他", "companySize": "10000人以上", "collectedAt": "2026-02-19", "sourceJobId": "JOB275645", "education": "不限", "experience": "不限", "views": 775, "applications": 37, "tags": ["Python", "Scikit-learn", "Jenkins", "TensorFlow", "Vue"]}',
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

-- JD 47: 架构师（华为·长沙）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    76, 1,
    '架构师',
    '华为',
    '岗位职责：持续优化系统性能和用户体验
参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
岗位要求：有大型项目经验者优先
3-5年以上相关工作经验
熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["高并发", "MySQL", "Java", "算法", "TypeScript"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码"], "experienceRequirements": ["有大型项目经验者优先\\n3-5年以上相关工作经验\\n熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "长沙", "salary": "14773-24006元/月", "salaryMin": 14773, "salaryMax": 24006, "salaryAvg": 19389, "platform": "招聘平台", "industry": "制造业", "companySize": "1000-9999人", "collectedAt": "2026-02-09", "sourceJobId": "JOB630155", "education": "硕士", "experience": "3-5年", "views": 1536, "applications": 71, "tags": ["高并发", "MySQL", "Java", "算法", "TypeScript"]}',
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

-- JD 48: 测试工程师（海康威视·北京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    77, 1,
    '测试工程师',
    '海康威视',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
持续优化系统性能和用户体验
岗位要求：应届生以上相关工作经验
大专及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Pandas", "Git", "Docker"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码\\n持续优化系统性能和用户体验"], "experienceRequirements": ["应届生以上相关工作经验\\n大专及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "北京", "salary": "5552-9022元/月", "salaryMin": 5552, "salaryMax": 9022, "salaryAvg": 7287, "platform": "招聘平台", "industry": "能源", "companySize": "0-20人", "collectedAt": "2026-03-04", "sourceJobId": "JOB674213", "education": "大专", "experience": "应届生", "views": 565, "applications": 157, "tags": ["Pandas", "Git", "Docker"]}',
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

-- JD 49: DevOps工程师（快手·郑州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    78, 1,
    'DevOps工程师',
    '快手',
    '岗位职责：负责DevOps工程师相关的系统设计和开发工作
编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
岗位要求：有大型项目经验者优先
熟悉相关技术栈和开发工具
5-10年以上相关工作经验
不限及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["Java", "Vue", "Hadoop"], "preferredSkills": [], "responsibilities": ["负责DevOps工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["有大型项目经验者优先\\n熟悉相关技术栈和开发工具\\n5-10年以上相关工作经验\\n不限及以上学历，计算机相关专业优先"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "郑州", "salary": "11771-19129元/月", "salaryMin": 11771, "salaryMax": 19129, "salaryAvg": 15450, "platform": "招聘平台", "industry": "物流", "companySize": "0-20人", "collectedAt": "2026-03-02", "sourceJobId": "JOB239849", "education": "不限", "experience": "5-10年", "views": 2319, "applications": 138, "tags": ["Java", "Vue", "Hadoop"]}',
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

-- JD 50: 数据工程师（宁德时代·重庆）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    79, 1,
    '数据工程师',
    '宁德时代',
    '岗位职责：负责数据工程师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
岗位要求：不限及以上学历，计算机相关专业优先
有大型项目经验者优先
熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["微服务", "Pandas", "Spark", "NumPy", "大数据"], "preferredSkills": [], "responsibilities": ["负责数据工程师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["不限及以上学历，计算机相关专业优先\\n有大型项目经验者优先\\n熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "重庆", "salary": "5638-9162元/月", "salaryMin": 5638, "salaryMax": 9162, "salaryAvg": 7400, "platform": "招聘平台", "industry": "物流", "companySize": "100-499人", "collectedAt": "2026-02-25", "sourceJobId": "JOB969856", "education": "不限", "experience": "不限", "views": 730, "applications": 32, "tags": ["微服务", "Pandas", "Spark", "NumPy", "大数据"]}',
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

-- JD 51: Java开发工程师（蚂蚁集团·郑州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    80, 1,
    'Java开发工程师',
    '蚂蚁集团',
    '岗位职责：持续优化系统性能和用户体验
参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
岗位要求：具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神
10年以上以上相关工作经验
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["Scikit-learn", "微服务", "MongoDB", "Jenkins", "TensorFlow"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神\\n10年以上以上相关工作经验\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "郑州", "salary": "20430-33199元/月", "salaryMin": 20430, "salaryMax": 33199, "salaryAvg": 26814, "platform": "招聘平台", "industry": "其他", "companySize": "500-999人", "collectedAt": "2026-03-01", "sourceJobId": "JOB298244", "education": "本科", "experience": "10年以上", "views": 2229, "applications": 63, "tags": ["Scikit-learn", "微服务", "MongoDB", "Jenkins", "TensorFlow"]}',
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

-- JD 52: 运维工程师（优酷·西安）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    81, 1,
    '运维工程师',
    '优酷',
    '岗位职责：参与技术方案评审，解决技术难题
编写高质量、可维护的代码
持续优化系统性能和用户体验
岗位要求：不限及以上学历，计算机相关专业优先
熟悉相关技术栈和开发工具
有大型项目经验者优先
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["微服务", "TensorFlow", "算法", "Hadoop"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n编写高质量、可维护的代码\\n持续优化系统性能和用户体验"], "experienceRequirements": ["不限及以上学历，计算机相关专业优先\\n熟悉相关技术栈和开发工具\\n有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "西安", "salary": "11102-18041元/月", "salaryMin": 11102, "salaryMax": 18041, "salaryAvg": 14571, "platform": "招聘平台", "industry": "制造业", "companySize": "500-999人", "collectedAt": "2026-02-13", "sourceJobId": "JOB263633", "education": "不限", "experience": "3-5年", "views": 4613, "applications": 34, "tags": ["微服务", "TensorFlow", "算法", "Hadoop"]}',
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

-- JD 53: 数据分析师（商汤科技·苏州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    82, 1,
    '数据分析师',
    '商汤科技',
    '岗位职责：持续优化系统性能和用户体验
参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
岗位要求：5-10年以上相关工作经验
本科及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["PyTorch", "Scikit-learn", "Spring Boot"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["5-10年以上相关工作经验\\n本科及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "苏州", "salary": "19617-31878元/月", "salaryMin": 19617, "salaryMax": 31878, "salaryAvg": 25747, "platform": "招聘平台", "industry": "零售", "companySize": "0-20人", "collectedAt": "2026-02-07", "sourceJobId": "JOB423047", "education": "本科", "experience": "5-10年", "views": 3749, "applications": 178, "tags": ["PyTorch", "Scikit-learn", "Spring Boot"]}',
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

-- JD 54: 机器学习工程师（平安科技·武汉）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    83, 1,
    '机器学习工程师',
    '平安科技',
    '岗位职责：参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
岗位要求：熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力
有大型项目经验者优先
5-10年以上相关工作经验',
    '{"requiredSkills": ["Hadoop", "Pandas", "Docker"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力\\n有大型项目经验者优先\\n5-10年以上相关工作经验"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "武汉", "salary": "17049-27705元/月", "salaryMin": 17049, "salaryMax": 27705, "salaryAvg": 22377, "platform": "招聘平台", "industry": "其他", "companySize": "1000-9999人", "collectedAt": "2026-03-06", "sourceJobId": "JOB804179", "education": "本科", "experience": "5-10年", "views": 2431, "applications": 93, "tags": ["Hadoop", "Pandas", "Docker"]}',
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

-- JD 55: 前端开发工程师（海尔·杭州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    84, 1,
    '前端开发工程师',
    '海尔',
    '岗位职责：负责前端开发工程师相关的系统设计和开发工作
编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
岗位要求：硕士及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["Vue", "Jenkins", "NumPy", "Scikit-learn", "Django", "Java"], "preferredSkills": [], "responsibilities": ["负责前端开发工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["硕士及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "杭州", "salary": "11290-18347元/月", "salaryMin": 11290, "salaryMax": 18347, "salaryAvg": 14818, "platform": "招聘平台", "industry": "制造业", "companySize": "0-20人", "collectedAt": "2026-02-19", "sourceJobId": "JOB128143", "education": "硕士", "experience": "不限", "views": 4733, "applications": 116, "tags": ["Vue", "Jenkins", "NumPy", "Scikit-learn", "Django", "Java"]}',
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

-- JD 56: 技术总监（哔哩哔哩·苏州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    85, 1,
    '技术总监',
    '哔哩哔哩',
    '岗位职责：编写高质量、可维护的代码
持续优化系统性能和用户体验
参与需求分析、系统设计、编码实现和测试
岗位要求：10年以上以上相关工作经验
具备良好的沟通能力和团队协作精神
有大型项目经验者优先
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["JavaScript", "React", "高并发"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n持续优化系统性能和用户体验\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["10年以上以上相关工作经验\\n具备良好的沟通能力和团队协作精神\\n有大型项目经验者优先\\n具备良好的编程基础和算法能力"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "苏州", "salary": "29239-47514元/月", "salaryMin": 29239, "salaryMax": 47514, "salaryAvg": 38376, "platform": "招聘平台", "industry": "零售", "companySize": "20-99人", "collectedAt": "2026-02-16", "sourceJobId": "JOB201191", "education": "本科", "experience": "10年以上", "views": 659, "applications": 30, "tags": ["JavaScript", "React", "高并发"]}',
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

-- JD 57: Python开发工程师（中通·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    86, 1,
    'Python开发工程师',
    '中通',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
负责Python开发工程师相关的系统设计和开发工作
岗位要求：5-10年以上相关工作经验
熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神
有大型项目经验者优先',
    '{"requiredSkills": ["Docker", "Spring Boot", "MongoDB"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题\\n负责Python开发工程师相关的系统设计和开发工作"], "experienceRequirements": ["5-10年以上相关工作经验\\n熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神\\n有大型项目经验者优先"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "43055-69964元/月", "salaryMin": 43055, "salaryMax": 69964, "salaryAvg": 56509, "platform": "招聘平台", "industry": "教育", "companySize": "0-20人", "collectedAt": "2026-02-10", "sourceJobId": "JOB809381", "education": "博士", "experience": "5-10年", "views": 4867, "applications": 156, "tags": ["Docker", "Spring Boot", "MongoDB"]}',
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

-- JD 58: 深度学习工程师（大疆创新·天津）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    87, 1,
    '深度学习工程师',
    '大疆创新',
    '岗位职责：参与技术方案评审，解决技术难题
编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
岗位要求：具备良好的沟通能力和团队协作精神
熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力
不限以上相关工作经验',
    '{"requiredSkills": ["Jenkins", "深度学习", "高并发", "Vue", "机器学习", "Python"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力\\n不限以上相关工作经验"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "天津", "salary": "11273-18319元/月", "salaryMin": 11273, "salaryMax": 18319, "salaryAvg": 14796, "platform": "招聘平台", "industry": "能源", "companySize": "10000人以上", "collectedAt": "2026-02-24", "sourceJobId": "JOB901123", "education": "博士", "experience": "不限", "views": 603, "applications": 12, "tags": ["Jenkins", "深度学习", "高并发", "Vue", "机器学习", "Python"]}',
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

-- JD 59: 前端开发工程师（中通·郑州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    88, 1,
    '前端开发工程师',
    '中通',
    '岗位职责：负责前端开发工程师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
岗位要求：3-5年以上相关工作经验
具备良好的编程基础和算法能力
硕士及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["TypeScript", "Linux", "Docker", "大数据", "Hadoop", "高并发"], "preferredSkills": [], "responsibilities": ["负责前端开发工程师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验"], "experienceRequirements": ["3-5年以上相关工作经验\\n具备良好的编程基础和算法能力\\n硕士及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "郑州", "salary": "14364-23341元/月", "salaryMin": 14364, "salaryMax": 23341, "salaryAvg": 18852, "platform": "招聘平台", "industry": "零售", "companySize": "0-20人", "collectedAt": "2026-03-04", "sourceJobId": "JOB719145", "education": "硕士", "experience": "3-5年", "views": 2597, "applications": 34, "tags": ["TypeScript", "Linux", "Docker", "大数据", "Hadoop", "高并发"]}',
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

-- JD 60: 架构师（京东·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    89, 1,
    '架构师',
    '京东',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
编写高质量、可维护的代码
岗位要求：10年以上以上相关工作经验
硕士及以上学历，计算机相关专业优先
熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Git", "NumPy", "Java", "数据结构", "Python"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题\\n编写高质量、可维护的代码"], "experienceRequirements": ["10年以上以上相关工作经验\\n硕士及以上学历，计算机相关专业优先\\n熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "33721-54796元/月", "salaryMin": 33721, "salaryMax": 54796, "salaryAvg": 44258, "platform": "招聘平台", "industry": "房地产", "companySize": "100-499人", "collectedAt": "2026-02-14", "sourceJobId": "JOB542882", "education": "硕士", "experience": "10年以上", "views": 2603, "applications": 85, "tags": ["Git", "NumPy", "Java", "数据结构", "Python"]}',
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

-- JD 61: Python开发工程师（华为·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    90, 1,
    'Python开发工程师',
    '华为',
    '岗位职责：编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
岗位要求：具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力
有大型项目经验者优先
本科及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["Git", "Jenkins", "高并发", "算法", "React"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力\\n有大型项目经验者优先\\n本科及以上学历，计算机相关专业优先"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "33097-53784元/月", "salaryMin": 33097, "salaryMax": 53784, "salaryAvg": 43440, "platform": "招聘平台", "industry": "房地产", "companySize": "1000-9999人", "collectedAt": "2026-03-04", "sourceJobId": "JOB703779", "education": "本科", "experience": "10年以上", "views": 3495, "applications": 99, "tags": ["Git", "Jenkins", "高并发", "算法", "React"]}',
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

-- JD 62: Java开发工程师（联想·重庆）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    91, 1,
    'Java开发工程师',
    '联想',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
持续优化系统性能和用户体验
岗位要求：有大型项目经验者优先
熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神
不限及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["Django", "NumPy", "Spring Boot"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码\\n持续优化系统性能和用户体验"], "experienceRequirements": ["有大型项目经验者优先\\n熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神\\n不限及以上学历，计算机相关专业优先"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "重庆", "salary": "14241-23142元/月", "salaryMin": 14241, "salaryMax": 23142, "salaryAvg": 18691, "platform": "招聘平台", "industry": "物流", "companySize": "10000人以上", "collectedAt": "2026-02-21", "sourceJobId": "JOB694797", "education": "不限", "experience": "5-10年", "views": 1792, "applications": 158, "tags": ["Django", "NumPy", "Spring Boot"]}',
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

-- JD 63: 数据工程师（联想·杭州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    92, 1,
    '数据工程师',
    '联想',
    '岗位职责：参与技术方案评审，解决技术难题
持续优化系统性能和用户体验
负责数据工程师相关的系统设计和开发工作
岗位要求：有大型项目经验者优先
不限以上相关工作经验
本科及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Python", "Git", "Docker", "NumPy", "深度学习", "Jenkins"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n持续优化系统性能和用户体验\\n负责数据工程师相关的系统设计和开发工作"], "experienceRequirements": ["有大型项目经验者优先\\n不限以上相关工作经验\\n本科及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "杭州", "salary": "10843-17620元/月", "salaryMin": 10843, "salaryMax": 17620, "salaryAvg": 14231, "platform": "招聘平台", "industry": "金融", "companySize": "500-999人", "collectedAt": "2026-03-04", "sourceJobId": "JOB292037", "education": "本科", "experience": "不限", "views": 1768, "applications": 77, "tags": ["Python", "Git", "Docker", "NumPy", "深度学习", "Jenkins"]}',
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

-- JD 64: 机器学习工程师（滴滴·上海）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    93, 1,
    '机器学习工程师',
    '滴滴',
    '岗位职责：编写高质量、可维护的代码
持续优化系统性能和用户体验
参与技术方案评审，解决技术难题
岗位要求：大专及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神
有大型项目经验者优先
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["TypeScript", "Git", "Pandas", "算法", "TensorFlow", "MySQL"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n持续优化系统性能和用户体验\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["大专及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神\\n有大型项目经验者优先\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "上海", "salary": "22372-36355元/月", "salaryMin": 22372, "salaryMax": 36355, "salaryAvg": 29363, "platform": "招聘平台", "industry": "房地产", "companySize": "1000-9999人", "collectedAt": "2026-02-11", "sourceJobId": "JOB696152", "education": "大专", "experience": "10年以上", "views": 3891, "applications": 50, "tags": ["TypeScript", "Git", "Pandas", "算法", "TensorFlow", "MySQL"]}',
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

-- JD 65: 数据分析师（海尔·上海）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    94, 1,
    '数据分析师',
    '海尔',
    '岗位职责：参与技术方案评审，解决技术难题
编写高质量、可维护的代码
负责数据分析师相关的系统设计和开发工作
岗位要求：1-3年以上相关工作经验
有大型项目经验者优先
大专及以上学历，计算机相关专业优先
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["Redis", "PyTorch", "分布式系统", "Hadoop", "Docker", "NumPy"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n编写高质量、可维护的代码\\n负责数据分析师相关的系统设计和开发工作"], "experienceRequirements": ["1-3年以上相关工作经验\\n有大型项目经验者优先\\n大专及以上学历，计算机相关专业优先\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "上海", "salary": "8364-13592元/月", "salaryMin": 8364, "salaryMax": 13592, "salaryAvg": 10978, "platform": "招聘平台", "industry": "房地产", "companySize": "500-999人", "collectedAt": "2026-03-03", "sourceJobId": "JOB118785", "education": "大专", "experience": "1-3年", "views": 230, "applications": 183, "tags": ["Redis", "PyTorch", "分布式系统", "Hadoop", "Docker", "NumPy"]}',
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

-- JD 66: 后端开发工程师（搜狐·南京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    95, 1,
    '后端开发工程师',
    '搜狐',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
持续优化系统性能和用户体验
岗位要求：本科及以上学历，计算机相关专业优先
有大型项目经验者优先
不限以上相关工作经验
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["JavaScript", "Redis", "数据结构", "Git"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题\\n持续优化系统性能和用户体验"], "experienceRequirements": ["本科及以上学历，计算机相关专业优先\\n有大型项目经验者优先\\n不限以上相关工作经验\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "南京", "salary": "8242-13393元/月", "salaryMin": 8242, "salaryMax": 13393, "salaryAvg": 10817, "platform": "招聘平台", "industry": "能源", "companySize": "500-999人", "collectedAt": "2026-02-11", "sourceJobId": "JOB297364", "education": "本科", "experience": "不限", "views": 3747, "applications": 129, "tags": ["JavaScript", "Redis", "数据结构", "Git"]}',
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

-- JD 67: 架构师（哔哩哔哩·南京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    96, 1,
    '架构师',
    '哔哩哔哩',
    '岗位职责：参与技术方案评审，解决技术难题
持续优化系统性能和用户体验
参与需求分析、系统设计、编码实现和测试
岗位要求：有大型项目经验者优先
具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力
不限及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["Python", "MongoDB", "Java", "TypeScript", "Scikit-learn", "NumPy"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n持续优化系统性能和用户体验\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力\\n不限及以上学历，计算机相关专业优先"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "南京", "salary": "7158-11631元/月", "salaryMin": 7158, "salaryMax": 11631, "salaryAvg": 9394, "platform": "招聘平台", "industry": "其他", "companySize": "100-499人", "collectedAt": "2026-02-19", "sourceJobId": "JOB594392", "education": "不限", "experience": "1-3年", "views": 3374, "applications": 129, "tags": ["Python", "MongoDB", "Java", "TypeScript", "Scikit-learn", "NumPy"]}',
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

-- JD 68: 深度学习工程师（新浪·广州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    97, 1,
    '深度学习工程师',
    '新浪',
    '岗位职责：持续优化系统性能和用户体验
编写高质量、可维护的代码
负责深度学习工程师相关的系统设计和开发工作
岗位要求：有大型项目经验者优先
具备良好的沟通能力和团队协作精神
大专及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["微服务", "Docker", "数据结构", "Python", "大数据"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n编写高质量、可维护的代码\\n负责深度学习工程师相关的系统设计和开发工作"], "experienceRequirements": ["有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神\\n大专及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "广州", "salary": "26514-43086元/月", "salaryMin": 26514, "salaryMax": 43086, "salaryAvg": 34800, "platform": "招聘平台", "industry": "制造业", "companySize": "500-999人", "collectedAt": "2026-03-05", "sourceJobId": "JOB551538", "education": "大专", "experience": "10年以上", "views": 3686, "applications": 122, "tags": ["微服务", "Docker", "数据结构", "Python", "大数据"]}',
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

-- JD 69: Python开发工程师（滴滴·长沙）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    98, 1,
    'Python开发工程师',
    '滴滴',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
负责Python开发工程师相关的系统设计和开发工作
岗位要求：有大型项目经验者优先
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Vue", "Scikit-learn", "TypeScript"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码\\n负责Python开发工程师相关的系统设计和开发工作"], "experienceRequirements": ["有大型项目经验者优先\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "长沙", "salary": "16823-27338元/月", "salaryMin": 16823, "salaryMax": 27338, "salaryAvg": 22080, "platform": "招聘平台", "industry": "互联网", "companySize": "1000-9999人", "collectedAt": "2026-02-19", "sourceJobId": "JOB982478", "education": "不限", "experience": "10年以上", "views": 913, "applications": 44, "tags": ["Vue", "Scikit-learn", "TypeScript"]}',
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

-- JD 70: 数据分析师（网易·长沙）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    99, 1,
    '数据分析师',
    '网易',
    '岗位职责：持续优化系统性能和用户体验
编写高质量、可维护的代码
参与技术方案评审，解决技术难题
岗位要求：有大型项目经验者优先
具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神
大专及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["Linux", "Kubernetes", "大数据", "Pandas", "NumPy"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["有大型项目经验者优先\\n具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神\\n大专及以上学历，计算机相关专业优先"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "长沙", "salary": "5168-8399元/月", "salaryMin": 5168, "salaryMax": 8399, "salaryAvg": 6783, "platform": "招聘平台", "industry": "互联网", "companySize": "1000-9999人", "collectedAt": "2026-02-08", "sourceJobId": "JOB555157", "education": "大专", "experience": "不限", "views": 2391, "applications": 140, "tags": ["Linux", "Kubernetes", "大数据", "Pandas", "NumPy"]}',
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

-- JD 71: 算法工程师（360·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    100, 1,
    '算法工程师',
    '360',
    '岗位职责：持续优化系统性能和用户体验
参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
岗位要求：具备良好的沟通能力和团队协作精神
有大型项目经验者优先
熟悉相关技术栈和开发工具
博士及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["Spring Boot", "Django", "数据结构"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n有大型项目经验者优先\\n熟悉相关技术栈和开发工具\\n博士及以上学历，计算机相关专业优先"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "29662-48201元/月", "salaryMin": 29662, "salaryMax": 48201, "salaryAvg": 38931, "platform": "招聘平台", "industry": "金融", "companySize": "500-999人", "collectedAt": "2026-02-26", "sourceJobId": "JOB508881", "education": "博士", "experience": "3-5年", "views": 596, "applications": 86, "tags": ["Spring Boot", "Django", "数据结构"]}',
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

-- JD 72: 技术总监（京东·北京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    101, 1,
    '技术总监',
    '京东',
    '岗位职责：负责技术总监相关的系统设计和开发工作
持续优化系统性能和用户体验
编写高质量、可维护的代码
岗位要求：博士及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具
3-5年以上相关工作经验',
    '{"requiredSkills": ["Python", "Redis", "PyTorch", "Scikit-learn", "Linux", "JavaScript"], "preferredSkills": [], "responsibilities": ["负责技术总监相关的系统设计和开发工作\\n持续优化系统性能和用户体验\\n编写高质量、可维护的代码"], "experienceRequirements": ["博士及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具\\n3-5年以上相关工作经验"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "北京", "salary": "38159-62009元/月", "salaryMin": 38159, "salaryMax": 62009, "salaryAvg": 50084, "platform": "招聘平台", "industry": "互联网", "companySize": "20-99人", "collectedAt": "2026-03-03", "sourceJobId": "JOB189689", "education": "博士", "experience": "3-5年", "views": 3467, "applications": 64, "tags": ["Python", "Redis", "PyTorch", "Scikit-learn", "Linux", "JavaScript"]}',
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

-- JD 73: 深度学习工程师（华为·长沙）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    102, 1,
    '深度学习工程师',
    '华为',
    '岗位职责：参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
负责深度学习工程师相关的系统设计和开发工作
岗位要求：10年以上以上相关工作经验
本科及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["Docker", "Linux", "Jenkins", "算法", "PyTorch"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试\\n负责深度学习工程师相关的系统设计和开发工作"], "experienceRequirements": ["10年以上以上相关工作经验\\n本科及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "长沙", "salary": "16472-26768元/月", "salaryMin": 16472, "salaryMax": 26768, "salaryAvg": 21620, "platform": "招聘平台", "industry": "能源", "companySize": "10000人以上", "collectedAt": "2026-02-25", "sourceJobId": "JOB847948", "education": "本科", "experience": "10年以上", "views": 4888, "applications": 116, "tags": ["Docker", "Linux", "Jenkins", "算法", "PyTorch"]}',
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

-- JD 74: 数据分析师（快手·重庆）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    103, 1,
    '数据分析师',
    '快手',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
持续优化系统性能和用户体验
岗位要求：具备良好的沟通能力和团队协作精神
大专及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力
有大型项目经验者优先',
    '{"requiredSkills": ["Pandas", "Hadoop", "JavaScript", "NumPy", "算法"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题\\n持续优化系统性能和用户体验"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n大专及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力\\n有大型项目经验者优先"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "重庆", "salary": "6265-10181元/月", "salaryMin": 6265, "salaryMax": 10181, "salaryAvg": 8223, "platform": "招聘平台", "industry": "能源", "companySize": "100-499人", "collectedAt": "2026-02-10", "sourceJobId": "JOB696236", "education": "大专", "experience": "1-3年", "views": 961, "applications": 134, "tags": ["Pandas", "Hadoop", "JavaScript", "NumPy", "算法"]}',
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

-- JD 75: 算法工程师（网易·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    104, 1,
    '算法工程师',
    '网易',
    '岗位职责：编写高质量、可维护的代码
参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
岗位要求：熟悉相关技术栈和开发工具
有大型项目经验者优先
具备良好的编程基础和算法能力
1-3年以上相关工作经验',
    '{"requiredSkills": ["高并发", "机器学习", "算法"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n有大型项目经验者优先\\n具备良好的编程基础和算法能力\\n1-3年以上相关工作经验"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "23381-37995元/月", "salaryMin": 23381, "salaryMax": 37995, "salaryAvg": 30688, "platform": "招聘平台", "industry": "互联网", "companySize": "20-99人", "collectedAt": "2026-02-19", "sourceJobId": "JOB604201", "education": "博士", "experience": "1-3年", "views": 2621, "applications": 169, "tags": ["高并发", "机器学习", "算法"]}',
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

-- JD 76: 数据分析师（依图科技·青岛）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    105, 1,
    '数据分析师',
    '依图科技',
    '岗位职责：编写高质量、可维护的代码
参与技术方案评审，解决技术难题
持续优化系统性能和用户体验
岗位要求：有大型项目经验者优先
具备良好的编程基础和算法能力
大专及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["数据结构", "Linux", "Spark", "MySQL"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题\\n持续优化系统性能和用户体验"], "experienceRequirements": ["有大型项目经验者优先\\n具备良好的编程基础和算法能力\\n大专及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "青岛", "salary": "3623-5887元/月", "salaryMin": 3623, "salaryMax": 5887, "salaryAvg": 4755, "platform": "招聘平台", "industry": "教育", "companySize": "1000-9999人", "collectedAt": "2026-02-22", "sourceJobId": "JOB716886", "education": "大专", "experience": "应届生", "views": 3573, "applications": 193, "tags": ["数据结构", "Linux", "Spark", "MySQL"]}',
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

-- JD 77: 运维工程师（海康威视·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    106, 1,
    '运维工程师',
    '海康威视',
    '岗位职责：负责运维工程师相关的系统设计和开发工作
编写高质量、可维护的代码
持续优化系统性能和用户体验
岗位要求：熟悉相关技术栈和开发工具
本科及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["高并发", "Scikit-learn", "Vue"], "preferredSkills": [], "responsibilities": ["负责运维工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码\\n持续优化系统性能和用户体验"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n本科及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "16689-27120元/月", "salaryMin": 16689, "salaryMax": 27120, "salaryAvg": 21904, "platform": "招聘平台", "industry": "其他", "companySize": "500-999人", "collectedAt": "2026-02-24", "sourceJobId": "JOB310701", "education": "本科", "experience": "3-5年", "views": 343, "applications": 154, "tags": ["高并发", "Scikit-learn", "Vue"]}',
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

-- JD 78: 算法工程师（滴滴·武汉）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    107, 1,
    '算法工程师',
    '滴滴',
    '岗位职责：持续优化系统性能和用户体验
负责算法工程师相关的系统设计和开发工作
参与技术方案评审，解决技术难题
岗位要求：熟悉相关技术栈和开发工具
博士及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力
有大型项目经验者优先',
    '{"requiredSkills": ["PyTorch", "Spring Boot", "NumPy", "React", "高并发"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n负责算法工程师相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n博士及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力\\n有大型项目经验者优先"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "武汉", "salary": "12941-21029元/月", "salaryMin": 12941, "salaryMax": 21029, "salaryAvg": 16985, "platform": "招聘平台", "industry": "房地产", "companySize": "10000人以上", "collectedAt": "2026-02-17", "sourceJobId": "JOB742349", "education": "博士", "experience": "不限", "views": 3924, "applications": 48, "tags": ["PyTorch", "Spring Boot", "NumPy", "React", "高并发"]}',
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

-- JD 79: 数据工程师（搜狐·天津）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    108, 1,
    '数据工程师',
    '搜狐',
    '岗位职责：负责数据工程师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
岗位要求：熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力
1-3年以上相关工作经验
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["TypeScript", "Linux", "NumPy"], "preferredSkills": [], "responsibilities": ["负责数据工程师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力\\n1-3年以上相关工作经验\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "天津", "salary": "6980-11343元/月", "salaryMin": 6980, "salaryMax": 11343, "salaryAvg": 9161, "platform": "招聘平台", "industry": "教育", "companySize": "20-99人", "collectedAt": "2026-02-26", "sourceJobId": "JOB567965", "education": "不限", "experience": "1-3年", "views": 4001, "applications": 176, "tags": ["TypeScript", "Linux", "NumPy"]}',
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

-- JD 80: 机器学习工程师（小米·郑州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    109, 1,
    '机器学习工程师',
    '小米',
    '岗位职责：负责机器学习工程师相关的系统设计和开发工作
参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
岗位要求：不限及以上学历，计算机相关专业优先
有大型项目经验者优先
10年以上以上相关工作经验
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["高并发", "PyTorch", "NumPy", "Vue"], "preferredSkills": [], "responsibilities": ["负责机器学习工程师相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["不限及以上学历，计算机相关专业优先\\n有大型项目经验者优先\\n10年以上以上相关工作经验\\n具备良好的编程基础和算法能力"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "郑州", "salary": "14213-23096元/月", "salaryMin": 14213, "salaryMax": 23096, "salaryAvg": 18654, "platform": "招聘平台", "industry": "金融", "companySize": "20-99人", "collectedAt": "2026-02-28", "sourceJobId": "JOB612295", "education": "不限", "experience": "10年以上", "views": 1420, "applications": 27, "tags": ["高并发", "PyTorch", "NumPy", "Vue"]}',
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

-- JD 81: 数据分析师（旷视科技·南京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    110, 1,
    '数据分析师',
    '旷视科技',
    '岗位职责：负责数据分析师相关的系统设计和开发工作
编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
岗位要求：熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神
博士及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["分布式系统", "深度学习", "高并发", "数据结构"], "preferredSkills": [], "responsibilities": ["负责数据分析师相关的系统设计和开发工作\\n编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神\\n博士及以上学历，计算机相关专业优先"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "南京", "salary": "15369-24975元/月", "salaryMin": 15369, "salaryMax": 24975, "salaryAvg": 20172, "platform": "招聘平台", "industry": "零售", "companySize": "0-20人", "collectedAt": "2026-03-06", "sourceJobId": "JOB470453", "education": "博士", "experience": "1-3年", "views": 4759, "applications": 105, "tags": ["分布式系统", "深度学习", "高并发", "数据结构"]}',
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

-- JD 82: 深度学习工程师（腾讯·上海）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    111, 1,
    '深度学习工程师',
    '腾讯',
    '岗位职责：负责深度学习工程师相关的系统设计和开发工作
编写高质量、可维护的代码
持续优化系统性能和用户体验
岗位要求：有大型项目经验者优先
具备良好的沟通能力和团队协作精神
博士及以上学历，计算机相关专业优先
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["React", "Django", "数据结构"], "preferredSkills": [], "responsibilities": ["负责深度学习工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码\\n持续优化系统性能和用户体验"], "experienceRequirements": ["有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神\\n博士及以上学历，计算机相关专业优先\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "上海", "salary": "52137-84723元/月", "salaryMin": 52137, "salaryMax": 84723, "salaryAvg": 68430, "platform": "招聘平台", "industry": "互联网", "companySize": "0-20人", "collectedAt": "2026-02-04", "sourceJobId": "JOB886897", "education": "博士", "experience": "5-10年", "views": 971, "applications": 143, "tags": ["React", "Django", "数据结构"]}',
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

-- JD 83: 数据工程师（腾讯·郑州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    112, 1,
    '数据工程师',
    '腾讯',
    '岗位职责：参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
负责数据工程师相关的系统设计和开发工作
岗位要求：熟悉相关技术栈和开发工具
1-3年以上相关工作经验
具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["JavaScript", "Django", "Java", "TensorFlow"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试\\n负责数据工程师相关的系统设计和开发工作"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n1-3年以上相关工作经验\\n具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "郑州", "salary": "5671-9216元/月", "salaryMin": 5671, "salaryMax": 9216, "salaryAvg": 7443, "platform": "招聘平台", "industry": "零售", "companySize": "20-99人", "collectedAt": "2026-02-21", "sourceJobId": "JOB709859", "education": "不限", "experience": "1-3年", "views": 2863, "applications": 118, "tags": ["JavaScript", "Django", "Java", "TensorFlow"]}',
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

-- JD 84: 后端开发工程师（顺丰·杭州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    113, 1,
    '后端开发工程师',
    '顺丰',
    '岗位职责：编写高质量、可维护的代码
持续优化系统性能和用户体验
参与需求分析、系统设计、编码实现和测试
岗位要求：具备良好的沟通能力和团队协作精神
10年以上以上相关工作经验
具备良好的编程基础和算法能力
有大型项目经验者优先',
    '{"requiredSkills": ["Python", "Spark", "MongoDB", "JavaScript", "Redis", "Java"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n持续优化系统性能和用户体验\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n10年以上以上相关工作经验\\n具备良好的编程基础和算法能力\\n有大型项目经验者优先"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "杭州", "salary": "20424-33190元/月", "salaryMin": 20424, "salaryMax": 33190, "salaryAvg": 26807, "platform": "招聘平台", "industry": "其他", "companySize": "0-20人", "collectedAt": "2026-02-11", "sourceJobId": "JOB769399", "education": "大专", "experience": "10年以上", "views": 3015, "applications": 38, "tags": ["Python", "Spark", "MongoDB", "JavaScript", "Redis", "Java"]}',
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

-- JD 85: 技术总监（旷视科技·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    114, 1,
    '技术总监',
    '旷视科技',
    '岗位职责：参与技术方案评审，解决技术难题
编写高质量、可维护的代码
持续优化系统性能和用户体验
岗位要求：具备良好的沟通能力和团队协作精神
博士及以上学历，计算机相关专业优先
有大型项目经验者优先
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["深度学习", "Linux", "Kubernetes", "Redis"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n编写高质量、可维护的代码\\n持续优化系统性能和用户体验"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n博士及以上学历，计算机相关专业优先\\n有大型项目经验者优先\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "47100-76538元/月", "salaryMin": 47100, "salaryMax": 76538, "salaryAvg": 61819, "platform": "招聘平台", "industry": "能源", "companySize": "20-99人", "collectedAt": "2026-03-04", "sourceJobId": "JOB452139", "education": "博士", "experience": "5-10年", "views": 234, "applications": 162, "tags": ["深度学习", "Linux", "Kubernetes", "Redis"]}',
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

-- JD 86: 数据工程师（滴滴·重庆）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    115, 1,
    '数据工程师',
    '滴滴',
    '岗位职责：参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
负责数据工程师相关的系统设计和开发工作
岗位要求：熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神
有大型项目经验者优先
不限及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["Pandas", "Redis", "高并发"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试\\n负责数据工程师相关的系统设计和开发工作"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神\\n有大型项目经验者优先\\n不限及以上学历，计算机相关专业优先"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "重庆", "salary": "19268-31310元/月", "salaryMin": 19268, "salaryMax": 31310, "salaryAvg": 25289, "platform": "招聘平台", "industry": "物流", "companySize": "10000人以上", "collectedAt": "2026-02-28", "sourceJobId": "JOB531257", "education": "不限", "experience": "10年以上", "views": 3946, "applications": 36, "tags": ["Pandas", "Redis", "高并发"]}',
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

-- JD 87: 运维工程师（小米·长沙）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    116, 1,
    '运维工程师',
    '小米',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
负责运维工程师相关的系统设计和开发工作
岗位要求：本科及以上学历，计算机相关专业优先
有大型项目经验者优先
具备良好的沟通能力和团队协作精神
10年以上以上相关工作经验',
    '{"requiredSkills": ["分布式系统", "数据结构", "Scikit-learn", "MySQL", "Spark", "深度学习"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码\\n负责运维工程师相关的系统设计和开发工作"], "experienceRequirements": ["本科及以上学历，计算机相关专业优先\\n有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神\\n10年以上以上相关工作经验"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "长沙", "salary": "23478-38153元/月", "salaryMin": 23478, "salaryMax": 38153, "salaryAvg": 30815, "platform": "招聘平台", "industry": "其他", "companySize": "10000人以上", "collectedAt": "2026-03-06", "sourceJobId": "JOB342189", "education": "本科", "experience": "10年以上", "views": 4960, "applications": 44, "tags": ["分布式系统", "数据结构", "Scikit-learn", "MySQL", "Spark", "深度学习"]}',
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

-- JD 88: 数据工程师（海康威视·天津）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    117, 1,
    '数据工程师',
    '海康威视',
    '岗位职责：持续优化系统性能和用户体验
负责数据工程师相关的系统设计和开发工作
参与技术方案评审，解决技术难题
岗位要求：具备良好的编程基础和算法能力
有大型项目经验者优先
熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["微服务", "Vue", "Hadoop", "Redis", "Java", "大数据"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n负责数据工程师相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["具备良好的编程基础和算法能力\\n有大型项目经验者优先\\n熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "天津", "salary": "6205-10083元/月", "salaryMin": 6205, "salaryMax": 10083, "salaryAvg": 8144, "platform": "招聘平台", "industry": "零售", "companySize": "500-999人", "collectedAt": "2026-02-15", "sourceJobId": "JOB533843", "education": "大专", "experience": "1-3年", "views": 4576, "applications": 6, "tags": ["微服务", "Vue", "Hadoop", "Redis", "Java", "大数据"]}',
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

-- JD 89: 后端开发工程师（携程·重庆）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    118, 1,
    '后端开发工程师',
    '携程',
    '岗位职责：负责后端开发工程师相关的系统设计和开发工作
持续优化系统性能和用户体验
参与技术方案评审，解决技术难题
岗位要求：具备良好的沟通能力和团队协作精神
有大型项目经验者优先
熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Scikit-learn", "Redis", "NumPy", "PyTorch", "算法"], "preferredSkills": [], "responsibilities": ["负责后端开发工程师相关的系统设计和开发工作\\n持续优化系统性能和用户体验\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n有大型项目经验者优先\\n熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "重庆", "salary": "23934-38893元/月", "salaryMin": 23934, "salaryMax": 38893, "salaryAvg": 31413, "platform": "招聘平台", "industry": "医疗", "companySize": "20-99人", "collectedAt": "2026-02-25", "sourceJobId": "JOB511970", "education": "硕士", "experience": "5-10年", "views": 2054, "applications": 48, "tags": ["Scikit-learn", "Redis", "NumPy", "PyTorch", "算法"]}',
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

-- JD 90: 架构师（优酷·北京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    119, 1,
    '架构师',
    '优酷',
    '岗位职责：编写高质量、可维护的代码
参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
岗位要求：有大型项目经验者优先
具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["数据结构", "React", "TypeScript", "Redis"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["有大型项目经验者优先\\n具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "北京", "salary": "9952-16172元/月", "salaryMin": 9952, "salaryMax": 16172, "salaryAvg": 13062, "platform": "招聘平台", "industry": "能源", "companySize": "20-99人", "collectedAt": "2026-02-19", "sourceJobId": "JOB214699", "education": "不限", "experience": "不限", "views": 2176, "applications": 98, "tags": ["数据结构", "React", "TypeScript", "Redis"]}',
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

-- JD 91: 架构师（蚂蚁集团·成都）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    120, 1,
    '架构师',
    '蚂蚁集团',
    '岗位职责：负责架构师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
岗位要求：不限以上相关工作经验
有大型项目经验者优先
具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["TypeScript", "MySQL", "Pandas", "机器学习", "算法", "Scikit-learn"], "preferredSkills": [], "responsibilities": ["负责架构师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["不限以上相关工作经验\\n有大型项目经验者优先\\n具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "成都", "salary": "10584-17199元/月", "salaryMin": 10584, "salaryMax": 17199, "salaryAvg": 13891, "platform": "招聘平台", "industry": "其他", "companySize": "1000-9999人", "collectedAt": "2026-03-06", "sourceJobId": "JOB581035", "education": "硕士", "experience": "不限", "views": 2101, "applications": 152, "tags": ["TypeScript", "MySQL", "Pandas", "机器学习", "算法", "Scikit-learn"]}',
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

-- JD 92: Python开发工程师（格力·重庆）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    121, 1,
    'Python开发工程师',
    '格力',
    '岗位职责：参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
负责Python开发工程师相关的系统设计和开发工作
岗位要求：有大型项目经验者优先
不限以上相关工作经验
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["机器学习", "React", "深度学习", "算法"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试\\n负责Python开发工程师相关的系统设计和开发工作"], "experienceRequirements": ["有大型项目经验者优先\\n不限以上相关工作经验\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "重庆", "salary": "5909-9602元/月", "salaryMin": 5909, "salaryMax": 9602, "salaryAvg": 7755, "platform": "招聘平台", "industry": "互联网", "companySize": "10000人以上", "collectedAt": "2026-02-25", "sourceJobId": "JOB447401", "education": "不限", "experience": "不限", "views": 152, "applications": 129, "tags": ["机器学习", "React", "深度学习", "算法"]}',
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

-- JD 93: 深度学习工程师（腾讯·南京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    122, 1,
    '深度学习工程师',
    '腾讯',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
参与技术方案评审，解决技术难题
岗位要求：硕士及以上学历，计算机相关专业优先
不限以上相关工作经验
熟悉相关技术栈和开发工具
有大型项目经验者优先',
    '{"requiredSkills": ["MySQL", "Spring Boot", "MongoDB", "数据结构", "Python", "React"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["硕士及以上学历，计算机相关专业优先\\n不限以上相关工作经验\\n熟悉相关技术栈和开发工具\\n有大型项目经验者优先"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "南京", "salary": "11488-18668元/月", "salaryMin": 11488, "salaryMax": 18668, "salaryAvg": 15078, "platform": "招聘平台", "industry": "零售", "companySize": "0-20人", "collectedAt": "2026-02-09", "sourceJobId": "JOB831400", "education": "硕士", "experience": "不限", "views": 4719, "applications": 161, "tags": ["MySQL", "Spring Boot", "MongoDB", "数据结构", "Python", "React"]}',
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

-- JD 94: 算法工程师（美团·上海）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    123, 1,
    '算法工程师',
    '美团',
    '岗位职责：负责算法工程师相关的系统设计和开发工作
参与技术方案评审，解决技术难题
编写高质量、可维护的代码
岗位要求：10年以上以上相关工作经验
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具
有大型项目经验者优先',
    '{"requiredSkills": ["MySQL", "算法", "Python", "TypeScript"], "preferredSkills": [], "responsibilities": ["负责算法工程师相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题\\n编写高质量、可维护的代码"], "experienceRequirements": ["10年以上以上相关工作经验\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具\\n有大型项目经验者优先"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "上海", "salary": "57591-93585元/月", "salaryMin": 57591, "salaryMax": 93585, "salaryAvg": 75588, "platform": "招聘平台", "industry": "物流", "companySize": "1000-9999人", "collectedAt": "2026-02-25", "sourceJobId": "JOB837244", "education": "博士", "experience": "10年以上", "views": 1965, "applications": 10, "tags": ["MySQL", "算法", "Python", "TypeScript"]}',
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

-- JD 95: DevOps工程师（依图科技·长沙）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    124, 1,
    'DevOps工程师',
    '依图科技',
    '岗位职责：参与技术方案评审，解决技术难题
持续优化系统性能和用户体验
编写高质量、可维护的代码
岗位要求：10年以上以上相关工作经验
本科及以上学历，计算机相关专业优先
熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Python", "Kubernetes", "数据结构", "Hadoop", "NumPy", "React"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n持续优化系统性能和用户体验\\n编写高质量、可维护的代码"], "experienceRequirements": ["10年以上以上相关工作经验\\n本科及以上学历，计算机相关专业优先\\n熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "长沙", "salary": "17481-28408元/月", "salaryMin": 17481, "salaryMax": 28408, "salaryAvg": 22944, "platform": "招聘平台", "industry": "互联网", "companySize": "10000人以上", "collectedAt": "2026-02-06", "sourceJobId": "JOB158045", "education": "本科", "experience": "10年以上", "views": 3112, "applications": 95, "tags": ["Python", "Kubernetes", "数据结构", "Hadoop", "NumPy", "React"]}',
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

-- JD 96: 数据工程师（商汤科技·北京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    125, 1,
    '数据工程师',
    '商汤科技',
    '岗位职责：负责数据工程师相关的系统设计和开发工作
编写高质量、可维护的代码
参与技术方案评审，解决技术难题
岗位要求：具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力
博士及以上学历，计算机相关专业优先
应届生以上相关工作经验',
    '{"requiredSkills": ["Linux", "Python", "TensorFlow", "Spark"], "preferredSkills": [], "responsibilities": ["负责数据工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力\\n博士及以上学历，计算机相关专业优先\\n应届生以上相关工作经验"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "北京", "salary": "14080-22881元/月", "salaryMin": 14080, "salaryMax": 22881, "salaryAvg": 18480, "platform": "招聘平台", "industry": "能源", "companySize": "500-999人", "collectedAt": "2026-03-01", "sourceJobId": "JOB585201", "education": "博士", "experience": "应届生", "views": 2213, "applications": 75, "tags": ["Linux", "Python", "TensorFlow", "Spark"]}',
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

-- JD 97: 深度学习工程师（中兴通讯·南京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    126, 1,
    '深度学习工程师',
    '中兴通讯',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
负责深度学习工程师相关的系统设计和开发工作
岗位要求：具备良好的编程基础和算法能力
10年以上以上相关工作经验
熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["分布式系统", "TensorFlow", "Redis", "Hadoop"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码\\n负责深度学习工程师相关的系统设计和开发工作"], "experienceRequirements": ["具备良好的编程基础和算法能力\\n10年以上以上相关工作经验\\n熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "南京", "salary": "21641-35167元/月", "salaryMin": 21641, "salaryMax": 35167, "salaryAvg": 28404, "platform": "招聘平台", "industry": "制造业", "companySize": "100-499人", "collectedAt": "2026-02-11", "sourceJobId": "JOB936979", "education": "不限", "experience": "10年以上", "views": 1045, "applications": 182, "tags": ["分布式系统", "TensorFlow", "Redis", "Hadoop"]}',
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

-- JD 98: 算法工程师（滴滴·青岛）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    127, 1,
    '算法工程师',
    '滴滴',
    '岗位职责：参与技术方案评审，解决技术难题
持续优化系统性能和用户体验
编写高质量、可维护的代码
岗位要求：熟悉相关技术栈和开发工具
有大型项目经验者优先
具备良好的编程基础和算法能力
10年以上以上相关工作经验',
    '{"requiredSkills": ["算法", "Hadoop", "Docker"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n持续优化系统性能和用户体验\\n编写高质量、可维护的代码"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n有大型项目经验者优先\\n具备良好的编程基础和算法能力\\n10年以上以上相关工作经验"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "青岛", "salary": "29669-48212元/月", "salaryMin": 29669, "salaryMax": 48212, "salaryAvg": 38940, "platform": "招聘平台", "industry": "互联网", "companySize": "10000人以上", "collectedAt": "2026-02-12", "sourceJobId": "JOB407643", "education": "硕士", "experience": "10年以上", "views": 1202, "applications": 192, "tags": ["算法", "Hadoop", "Docker"]}',
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

-- JD 99: 运维工程师（京东·长沙）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    128, 1,
    '运维工程师',
    '京东',
    '岗位职责：负责运维工程师相关的系统设计和开发工作
编写高质量、可维护的代码
持续优化系统性能和用户体验
岗位要求：熟悉相关技术栈和开发工具
本科及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["MySQL", "Git", "Scikit-learn", "算法", "机器学习"], "preferredSkills": [], "responsibilities": ["负责运维工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码\\n持续优化系统性能和用户体验"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n本科及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "长沙", "salary": "6124-9952元/月", "salaryMin": 6124, "salaryMax": 9952, "salaryAvg": 8038, "platform": "招聘平台", "industry": "教育", "companySize": "100-499人", "collectedAt": "2026-02-08", "sourceJobId": "JOB942678", "education": "本科", "experience": "不限", "views": 4672, "applications": 174, "tags": ["MySQL", "Git", "Scikit-learn", "算法", "机器学习"]}',
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

-- JD 100: DevOps工程师（蚂蚁集团·武汉）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    129, 1,
    'DevOps工程师',
    '蚂蚁集团',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
参与技术方案评审，解决技术难题
岗位要求：不限以上相关工作经验
有大型项目经验者优先
熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Scikit-learn", "Python", "机器学习"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["不限以上相关工作经验\\n有大型项目经验者优先\\n熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "武汉", "salary": "5501-8939元/月", "salaryMin": 5501, "salaryMax": 8939, "salaryAvg": 7220, "platform": "招聘平台", "industry": "能源", "companySize": "100-499人", "collectedAt": "2026-02-27", "sourceJobId": "JOB857311", "education": "大专", "experience": "不限", "views": 2477, "applications": 46, "tags": ["Scikit-learn", "Python", "机器学习"]}',
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

-- JD 101: Java开发工程师（滴滴·杭州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    130, 1,
    'Java开发工程师',
    '滴滴',
    '岗位职责：负责Java开发工程师相关的系统设计和开发工作
编写高质量、可维护的代码
参与技术方案评审，解决技术难题
岗位要求：具备良好的沟通能力和团队协作精神
1-3年以上相关工作经验
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["TypeScript", "Python", "PyTorch", "Git", "Vue"], "preferredSkills": [], "responsibilities": ["负责Java开发工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n1-3年以上相关工作经验\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "杭州", "salary": "10808-17564元/月", "salaryMin": 10808, "salaryMax": 17564, "salaryAvg": 14186, "platform": "招聘平台", "industry": "物流", "companySize": "1000-9999人", "collectedAt": "2026-03-06", "sourceJobId": "JOB593348", "education": "本科", "experience": "1-3年", "views": 1678, "applications": 75, "tags": ["TypeScript", "Python", "PyTorch", "Git", "Vue"]}',
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

-- JD 102: 测试工程师（海康威视·成都）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    131, 1,
    '测试工程师',
    '海康威视',
    '岗位职责：编写高质量、可维护的代码
持续优化系统性能和用户体验
负责测试工程师相关的系统设计和开发工作
岗位要求：熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神
1-3年以上相关工作经验
大专及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["Scikit-learn", "大数据", "TypeScript", "Vue", "Hadoop"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n持续优化系统性能和用户体验\\n负责测试工程师相关的系统设计和开发工作"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神\\n1-3年以上相关工作经验\\n大专及以上学历，计算机相关专业优先"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "成都", "salary": "6100-9912元/月", "salaryMin": 6100, "salaryMax": 9912, "salaryAvg": 8006, "platform": "招聘平台", "industry": "零售", "companySize": "500-999人", "collectedAt": "2026-02-20", "sourceJobId": "JOB835354", "education": "大专", "experience": "1-3年", "views": 3839, "applications": 156, "tags": ["Scikit-learn", "大数据", "TypeScript", "Vue", "Hadoop"]}',
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

-- JD 103: 前端开发工程师（美团·广州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    132, 1,
    '前端开发工程师',
    '美团',
    '岗位职责：编写高质量、可维护的代码
负责前端开发工程师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
岗位要求：5-10年以上相关工作经验
具备良好的编程基础和算法能力
本科及以上学历，计算机相关专业优先
有大型项目经验者优先',
    '{"requiredSkills": ["Pandas", "深度学习", "高并发", "数据结构"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n负责前端开发工程师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["5-10年以上相关工作经验\\n具备良好的编程基础和算法能力\\n本科及以上学历，计算机相关专业优先\\n有大型项目经验者优先"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "广州", "salary": "23761-38612元/月", "salaryMin": 23761, "salaryMax": 38612, "salaryAvg": 31186, "platform": "招聘平台", "industry": "医疗", "companySize": "10000人以上", "collectedAt": "2026-02-17", "sourceJobId": "JOB422465", "education": "本科", "experience": "5-10年", "views": 3328, "applications": 183, "tags": ["Pandas", "深度学习", "高并发", "数据结构"]}',
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

-- JD 104: 前端开发工程师（格力·成都）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    133, 1,
    '前端开发工程师',
    '格力',
    '岗位职责：参与技术方案评审，解决技术难题
持续优化系统性能和用户体验
负责前端开发工程师相关的系统设计和开发工作
岗位要求：具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具
有大型项目经验者优先
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Jenkins", "Scikit-learn", "Spark"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n持续优化系统性能和用户体验\\n负责前端开发工程师相关的系统设计和开发工作"], "experienceRequirements": ["具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具\\n有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "成都", "salary": "19185-31176元/月", "salaryMin": 19185, "salaryMax": 31176, "salaryAvg": 25180, "platform": "招聘平台", "industry": "物流", "companySize": "500-999人", "collectedAt": "2026-02-14", "sourceJobId": "JOB524121", "education": "本科", "experience": "10年以上", "views": 1049, "applications": 159, "tags": ["Jenkins", "Scikit-learn", "Spark"]}',
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

-- JD 105: 前端开发工程师（美团·重庆）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    134, 1,
    '前端开发工程师',
    '美团',
    '岗位职责：持续优化系统性能和用户体验
参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
岗位要求：硕士及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具
有大型项目经验者优先',
    '{"requiredSkills": ["TensorFlow", "MySQL", "Redis", "PyTorch"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码"], "experienceRequirements": ["硕士及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具\\n有大型项目经验者优先"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "重庆", "salary": "8965-14568元/月", "salaryMin": 8965, "salaryMax": 14568, "salaryAvg": 11766, "platform": "招聘平台", "industry": "金融", "companySize": "500-999人", "collectedAt": "2026-02-10", "sourceJobId": "JOB520205", "education": "硕士", "experience": "不限", "views": 3679, "applications": 75, "tags": ["TensorFlow", "MySQL", "Redis", "PyTorch"]}',
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

-- JD 106: 运维工程师（比亚迪·广州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    135, 1,
    '运维工程师',
    '比亚迪',
    '岗位职责：参与技术方案评审，解决技术难题
负责运维工程师相关的系统设计和开发工作
编写高质量、可维护的代码
岗位要求：大专及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力
有大型项目经验者优先
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["Django", "TensorFlow", "数据结构", "Jenkins"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n负责运维工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码"], "experienceRequirements": ["大专及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力\\n有大型项目经验者优先\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "广州", "salary": "8646-14050元/月", "salaryMin": 8646, "salaryMax": 14050, "salaryAvg": 11348, "platform": "招聘平台", "industry": "互联网", "companySize": "100-499人", "collectedAt": "2026-02-19", "sourceJobId": "JOB390955", "education": "大专", "experience": "1-3年", "views": 1053, "applications": 118, "tags": ["Django", "TensorFlow", "数据结构", "Jenkins"]}',
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

-- JD 107: 后端开发工程师（大疆创新·苏州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    136, 1,
    '后端开发工程师',
    '大疆创新',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
负责后端开发工程师相关的系统设计和开发工作
参与技术方案评审，解决技术难题
岗位要求：有大型项目经验者优先
具备良好的沟通能力和团队协作精神
熟悉相关技术栈和开发工具
10年以上以上相关工作经验',
    '{"requiredSkills": ["Kubernetes", "PyTorch", "NumPy", "JavaScript"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n负责后端开发工程师相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神\\n熟悉相关技术栈和开发工具\\n10年以上以上相关工作经验"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "苏州", "salary": "28931-47014元/月", "salaryMin": 28931, "salaryMax": 47014, "salaryAvg": 37972, "platform": "招聘平台", "industry": "能源", "companySize": "100-499人", "collectedAt": "2026-03-02", "sourceJobId": "JOB116701", "education": "硕士", "experience": "10年以上", "views": 2886, "applications": 141, "tags": ["Kubernetes", "PyTorch", "NumPy", "JavaScript"]}',
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

-- JD 108: 技术总监（海尔·长沙）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    137, 1,
    '技术总监',
    '海尔',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
参与技术方案评审，解决技术难题
岗位要求：有大型项目经验者优先
具备良好的沟通能力和团队协作精神
不限以上相关工作经验
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["微服务", "分布式系统", "PyTorch"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神\\n不限以上相关工作经验\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "长沙", "salary": "5331-8663元/月", "salaryMin": 5331, "salaryMax": 8663, "salaryAvg": 6997, "platform": "招聘平台", "industry": "零售", "companySize": "1000-9999人", "collectedAt": "2026-02-26", "sourceJobId": "JOB235019", "education": "不限", "experience": "不限", "views": 4804, "applications": 18, "tags": ["微服务", "分布式系统", "PyTorch"]}',
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

-- JD 109: 技术总监（百度·苏州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    138, 1,
    '技术总监',
    '百度',
    '岗位职责：持续优化系统性能和用户体验
负责技术总监相关的系统设计和开发工作
参与技术方案评审，解决技术难题
岗位要求：具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具
有大型项目经验者优先
大专及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["Vue", "Java", "Spark"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n负责技术总监相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具\\n有大型项目经验者优先\\n大专及以上学历，计算机相关专业优先"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "苏州", "salary": "23474-38145元/月", "salaryMin": 23474, "salaryMax": 38145, "salaryAvg": 30809, "platform": "招聘平台", "industry": "零售", "companySize": "10000人以上", "collectedAt": "2026-02-09", "sourceJobId": "JOB370289", "education": "大专", "experience": "10年以上", "views": 4753, "applications": 149, "tags": ["Vue", "Java", "Spark"]}',
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

-- JD 110: 前端开发工程师（拼多多·广州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    139, 1,
    '前端开发工程师',
    '拼多多',
    '岗位职责：持续优化系统性能和用户体验
编写高质量、可维护的代码
参与技术方案评审，解决技术难题
岗位要求：5-10年以上相关工作经验
熟悉相关技术栈和开发工具
有大型项目经验者优先
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["TypeScript", "高并发", "Pandas", "Hadoop", "React"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["5-10年以上相关工作经验\\n熟悉相关技术栈和开发工具\\n有大型项目经验者优先\\n具备良好的编程基础和算法能力"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "广州", "salary": "22392-36387元/月", "salaryMin": 22392, "salaryMax": 36387, "salaryAvg": 29389, "platform": "招聘平台", "industry": "医疗", "companySize": "1000-9999人", "collectedAt": "2026-03-02", "sourceJobId": "JOB493044", "education": "不限", "experience": "5-10年", "views": 3746, "applications": 12, "tags": ["TypeScript", "高并发", "Pandas", "Hadoop", "React"]}',
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

-- JD 111: 技术总监（优酷·西安）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    140, 1,
    '技术总监',
    '优酷',
    '岗位职责：负责技术总监相关的系统设计和开发工作
参与技术方案评审，解决技术难题
持续优化系统性能和用户体验
岗位要求：具备良好的沟通能力和团队协作精神
3-5年以上相关工作经验
具备良好的编程基础和算法能力
有大型项目经验者优先',
    '{"requiredSkills": ["TensorFlow", "机器学习", "Vue", "大数据"], "preferredSkills": [], "responsibilities": ["负责技术总监相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题\\n持续优化系统性能和用户体验"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n3-5年以上相关工作经验\\n具备良好的编程基础和算法能力\\n有大型项目经验者优先"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "西安", "salary": "8138-13225元/月", "salaryMin": 8138, "salaryMax": 13225, "salaryAvg": 10681, "platform": "招聘平台", "industry": "零售", "companySize": "0-20人", "collectedAt": "2026-02-05", "sourceJobId": "JOB752300", "education": "大专", "experience": "3-5年", "views": 4589, "applications": 104, "tags": ["TensorFlow", "机器学习", "Vue", "大数据"]}',
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

-- JD 112: 后端开发工程师（中兴通讯·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    141, 1,
    '后端开发工程师',
    '中兴通讯',
    '岗位职责：持续优化系统性能和用户体验
编写高质量、可维护的代码
参与技术方案评审，解决技术难题
岗位要求：3-5年以上相关工作经验
有大型项目经验者优先
具备良好的编程基础和算法能力
本科及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["分布式系统", "Django", "PyTorch"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["3-5年以上相关工作经验\\n有大型项目经验者优先\\n具备良好的编程基础和算法能力\\n本科及以上学历，计算机相关专业优先"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "17075-27746元/月", "salaryMin": 17075, "salaryMax": 27746, "salaryAvg": 22410, "platform": "招聘平台", "industry": "物流", "companySize": "0-20人", "collectedAt": "2026-03-03", "sourceJobId": "JOB630154", "education": "本科", "experience": "3-5年", "views": 633, "applications": 109, "tags": ["分布式系统", "Django", "PyTorch"]}',
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

-- JD 113: Java开发工程师（依图科技·天津）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    142, 1,
    'Java开发工程师',
    '依图科技',
    '岗位职责：持续优化系统性能和用户体验
编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
岗位要求：有大型项目经验者优先
熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神
博士及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["Git", "Hadoop", "算法", "Django", "Pandas", "Jenkins"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["有大型项目经验者优先\\n熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神\\n博士及以上学历，计算机相关专业优先"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "天津", "salary": "35132-57089元/月", "salaryMin": 35132, "salaryMax": 57089, "salaryAvg": 46110, "platform": "招聘平台", "industry": "其他", "companySize": "100-499人", "collectedAt": "2026-02-20", "sourceJobId": "JOB672084", "education": "博士", "experience": "10年以上", "views": 4494, "applications": 97, "tags": ["Git", "Hadoop", "算法", "Django", "Pandas", "Jenkins"]}',
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

-- JD 114: 算法工程师（中兴通讯·苏州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    143, 1,
    '算法工程师',
    '中兴通讯',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
负责算法工程师相关的系统设计和开发工作
岗位要求：熟悉相关技术栈和开发工具
硕士及以上学历，计算机相关专业优先
有大型项目经验者优先
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Redis", "TypeScript", "微服务", "深度学习"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验\\n负责算法工程师相关的系统设计和开发工作"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n硕士及以上学历，计算机相关专业优先\\n有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "苏州", "salary": "33028-53672元/月", "salaryMin": 33028, "salaryMax": 53672, "salaryAvg": 43350, "platform": "招聘平台", "industry": "零售", "companySize": "500-999人", "collectedAt": "2026-02-17", "sourceJobId": "JOB981515", "education": "硕士", "experience": "10年以上", "views": 4624, "applications": 165, "tags": ["Redis", "TypeScript", "微服务", "深度学习"]}',
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

-- JD 115: 数据工程师（蚂蚁集团·天津）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    144, 1,
    '数据工程师',
    '蚂蚁集团',
    '岗位职责：编写高质量、可维护的代码
参与技术方案评审，解决技术难题
负责数据工程师相关的系统设计和开发工作
岗位要求：不限及以上学历，计算机相关专业优先
3-5年以上相关工作经验
具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Vue", "React", "PyTorch"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题\\n负责数据工程师相关的系统设计和开发工作"], "experienceRequirements": ["不限及以上学历，计算机相关专业优先\\n3-5年以上相关工作经验\\n具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "天津", "salary": "8840-14365元/月", "salaryMin": 8840, "salaryMax": 14365, "salaryAvg": 11602, "platform": "招聘平台", "industry": "零售", "companySize": "0-20人", "collectedAt": "2026-02-12", "sourceJobId": "JOB569063", "education": "不限", "experience": "3-5年", "views": 780, "applications": 95, "tags": ["Vue", "React", "PyTorch"]}',
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

-- JD 116: DevOps工程师（京东·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    145, 1,
    'DevOps工程师',
    '京东',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
负责DevOps工程师相关的系统设计和开发工作
参与技术方案评审，解决技术难题
岗位要求：大专及以上学历，计算机相关专业优先
有大型项目经验者优先
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["React", "TensorFlow", "微服务"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n负责DevOps工程师相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["大专及以上学历，计算机相关专业优先\\n有大型项目经验者优先\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "11481-18657元/月", "salaryMin": 11481, "salaryMax": 18657, "salaryAvg": 15069, "platform": "招聘平台", "industry": "零售", "companySize": "20-99人", "collectedAt": "2026-03-04", "sourceJobId": "JOB219039", "education": "大专", "experience": "1-3年", "views": 1379, "applications": 154, "tags": ["React", "TensorFlow", "微服务"]}',
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

-- JD 117: 机器学习工程师（旷视科技·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    146, 1,
    '机器学习工程师',
    '旷视科技',
    '岗位职责：负责机器学习工程师相关的系统设计和开发工作
参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
岗位要求：具备良好的沟通能力和团队协作精神
本科及以上学历，计算机相关专业优先
熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Linux", "MongoDB", "数据结构"], "preferredSkills": [], "responsibilities": ["负责机器学习工程师相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n本科及以上学历，计算机相关专业优先\\n熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "6979-11342元/月", "salaryMin": 6979, "salaryMax": 11342, "salaryAvg": 9160, "platform": "招聘平台", "industry": "零售", "companySize": "100-499人", "collectedAt": "2026-02-09", "sourceJobId": "JOB258932", "education": "本科", "experience": "应届生", "views": 3343, "applications": 182, "tags": ["Linux", "MongoDB", "数据结构"]}',
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

-- JD 118: 算法工程师（旷视科技·广州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    147, 1,
    '算法工程师',
    '旷视科技',
    '岗位职责：编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
岗位要求：熟悉相关技术栈和开发工具
5-10年以上相关工作经验
本科及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Scikit-learn", "Java", "Redis", "数据结构", "Python", "TypeScript"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n5-10年以上相关工作经验\\n本科及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "广州", "salary": "19444-31597元/月", "salaryMin": 19444, "salaryMax": 31597, "salaryAvg": 25520, "platform": "招聘平台", "industry": "制造业", "companySize": "0-20人", "collectedAt": "2026-02-11", "sourceJobId": "JOB201955", "education": "本科", "experience": "5-10年", "views": 348, "applications": 126, "tags": ["Scikit-learn", "Java", "Redis", "数据结构", "Python", "TypeScript"]}',
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

-- JD 119: Python开发工程师（中通·上海）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    148, 1,
    'Python开发工程师',
    '中通',
    '岗位职责：负责Python开发工程师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
岗位要求：本科及以上学历，计算机相关专业优先
熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神
有大型项目经验者优先',
    '{"requiredSkills": ["MySQL", "Java", "NumPy", "TensorFlow", "Jenkins"], "preferredSkills": [], "responsibilities": ["负责Python开发工程师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验"], "experienceRequirements": ["本科及以上学历，计算机相关专业优先\\n熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神\\n有大型项目经验者优先"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "上海", "salary": "21601-35103元/月", "salaryMin": 21601, "salaryMax": 35103, "salaryAvg": 28352, "platform": "招聘平台", "industry": "能源", "companySize": "10000人以上", "collectedAt": "2026-03-05", "sourceJobId": "JOB283061", "education": "本科", "experience": "5-10年", "views": 4174, "applications": 115, "tags": ["MySQL", "Java", "NumPy", "TensorFlow", "Jenkins"]}',
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

-- JD 120: 全栈开发工程师（商汤科技·南京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    149, 1,
    '全栈开发工程师',
    '商汤科技',
    '岗位职责：编写高质量、可维护的代码
参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
岗位要求：5-10年以上相关工作经验
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["NumPy", "PyTorch", "Redis"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["5-10年以上相关工作经验\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "南京", "salary": "14699-23886元/月", "salaryMin": 14699, "salaryMax": 23886, "salaryAvg": 19292, "platform": "招聘平台", "industry": "房地产", "companySize": "10000人以上", "collectedAt": "2026-02-16", "sourceJobId": "JOB174801", "education": "大专", "experience": "5-10年", "views": 4019, "applications": 167, "tags": ["NumPy", "PyTorch", "Redis"]}',
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

-- JD 121: 运维工程师（中兴通讯·西安）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    150, 1,
    '运维工程师',
    '中兴通讯',
    '岗位职责：参与技术方案评审，解决技术难题
负责运维工程师相关的系统设计和开发工作
编写高质量、可维护的代码
岗位要求：1-3年以上相关工作经验
博士及以上学历，计算机相关专业优先
有大型项目经验者优先
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["Redis", "PyTorch", "MongoDB"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n负责运维工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码"], "experienceRequirements": ["1-3年以上相关工作经验\\n博士及以上学历，计算机相关专业优先\\n有大型项目经验者优先\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "西安", "salary": "13617-22128元/月", "salaryMin": 13617, "salaryMax": 22128, "salaryAvg": 17872, "platform": "招聘平台", "industry": "教育", "companySize": "100-499人", "collectedAt": "2026-02-19", "sourceJobId": "JOB714110", "education": "博士", "experience": "1-3年", "views": 2140, "applications": 6, "tags": ["Redis", "PyTorch", "MongoDB"]}',
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

-- JD 122: 全栈开发工程师（云从科技·上海）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    151, 1,
    '全栈开发工程师',
    '云从科技',
    '岗位职责：参与技术方案评审，解决技术难题
编写高质量、可维护的代码
持续优化系统性能和用户体验
岗位要求：具备良好的沟通能力和团队协作精神
熟悉相关技术栈和开发工具
10年以上以上相关工作经验
有大型项目经验者优先',
    '{"requiredSkills": ["微服务", "Hadoop", "MySQL", "Jenkins"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n编写高质量、可维护的代码\\n持续优化系统性能和用户体验"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n熟悉相关技术栈和开发工具\\n10年以上以上相关工作经验\\n有大型项目经验者优先"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "上海", "salary": "70443-114470元/月", "salaryMin": 70443, "salaryMax": 114470, "salaryAvg": 92456, "platform": "招聘平台", "industry": "金融", "companySize": "0-20人", "collectedAt": "2026-03-05", "sourceJobId": "JOB735420", "education": "博士", "experience": "10年以上", "views": 3603, "applications": 165, "tags": ["微服务", "Hadoop", "MySQL", "Jenkins"]}',
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

-- JD 123: DevOps工程师（中兴通讯·西安）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    152, 1,
    'DevOps工程师',
    '中兴通讯',
    '岗位职责：持续优化系统性能和用户体验
负责DevOps工程师相关的系统设计和开发工作
参与技术方案评审，解决技术难题
岗位要求：熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力
有大型项目经验者优先
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Pandas", "数据结构", "深度学习"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n负责DevOps工程师相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力\\n有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "西安", "salary": "5302-8616元/月", "salaryMin": 5302, "salaryMax": 8616, "salaryAvg": 6959, "platform": "招聘平台", "industry": "房地产", "companySize": "500-999人", "collectedAt": "2026-02-11", "sourceJobId": "JOB485841", "education": "不限", "experience": "应届生", "views": 2288, "applications": 41, "tags": ["Pandas", "数据结构", "深度学习"]}',
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

-- JD 124: Python开发工程师（海尔·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    153, 1,
    'Python开发工程师',
    '海尔',
    '岗位职责：参与技术方案评审，解决技术难题
编写高质量、可维护的代码
持续优化系统性能和用户体验
岗位要求：有大型项目经验者优先
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具
3-5年以上相关工作经验',
    '{"requiredSkills": ["Hadoop", "Vue", "Pandas", "Jenkins", "TypeScript", "JavaScript"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n编写高质量、可维护的代码\\n持续优化系统性能和用户体验"], "experienceRequirements": ["有大型项目经验者优先\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具\\n3-5年以上相关工作经验"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "15889-25820元/月", "salaryMin": 15889, "salaryMax": 25820, "salaryAvg": 20854, "platform": "招聘平台", "industry": "医疗", "companySize": "0-20人", "collectedAt": "2026-02-16", "sourceJobId": "JOB320846", "education": "大专", "experience": "3-5年", "views": 4675, "applications": 145, "tags": ["Hadoop", "Vue", "Pandas", "Jenkins", "TypeScript", "JavaScript"]}',
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

-- JD 125: 数据工程师（京东·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    154, 1,
    '数据工程师',
    '京东',
    '岗位职责：参与技术方案评审，解决技术难题
负责数据工程师相关的系统设计和开发工作
编写高质量、可维护的代码
岗位要求：有大型项目经验者优先
具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["TensorFlow", "Django", "NumPy", "Jenkins", "Linux"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n负责数据工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码"], "experienceRequirements": ["有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "10453-16986元/月", "salaryMin": 10453, "salaryMax": 16986, "salaryAvg": 13719, "platform": "招聘平台", "industry": "零售", "companySize": "100-499人", "collectedAt": "2026-02-28", "sourceJobId": "JOB254853", "education": "本科", "experience": "不限", "views": 3976, "applications": 172, "tags": ["TensorFlow", "Django", "NumPy", "Jenkins", "Linux"]}',
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

-- JD 126: 数据工程师（商汤科技·广州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    155, 1,
    '数据工程师',
    '商汤科技',
    '岗位职责：参与技术方案评审，解决技术难题
负责数据工程师相关的系统设计和开发工作
持续优化系统性能和用户体验
岗位要求：大专及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神
有大型项目经验者优先
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Linux", "算法", "分布式系统", "Java", "MongoDB"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n负责数据工程师相关的系统设计和开发工作\\n持续优化系统性能和用户体验"], "experienceRequirements": ["大专及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神\\n有大型项目经验者优先\\n具备良好的编程基础和算法能力"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "广州", "salary": "23194-37690元/月", "salaryMin": 23194, "salaryMax": 37690, "salaryAvg": 30442, "platform": "招聘平台", "industry": "零售", "companySize": "0-20人", "collectedAt": "2026-03-02", "sourceJobId": "JOB144003", "education": "大专", "experience": "10年以上", "views": 3978, "applications": 131, "tags": ["Linux", "算法", "分布式系统", "Java", "MongoDB"]}',
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

-- JD 127: 前端开发工程师（阿里巴巴·上海）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    156, 1,
    '前端开发工程师',
    '阿里巴巴',
    '岗位职责：参与技术方案评审，解决技术难题
负责前端开发工程师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
岗位要求：10年以上以上相关工作经验
具备良好的沟通能力和团队协作精神
有大型项目经验者优先
大专及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["Jenkins", "微服务", "Docker"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n负责前端开发工程师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["10年以上以上相关工作经验\\n具备良好的沟通能力和团队协作精神\\n有大型项目经验者优先\\n大专及以上学历，计算机相关专业优先"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "上海", "salary": "26686-43365元/月", "salaryMin": 26686, "salaryMax": 43365, "salaryAvg": 35025, "platform": "招聘平台", "industry": "物流", "companySize": "0-20人", "collectedAt": "2026-02-24", "sourceJobId": "JOB877093", "education": "大专", "experience": "10年以上", "views": 1479, "applications": 52, "tags": ["Jenkins", "微服务", "Docker"]}',
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

-- JD 128: 运维工程师（携程·南京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    157, 1,
    '运维工程师',
    '携程',
    '岗位职责：负责运维工程师相关的系统设计和开发工作
参与技术方案评审，解决技术难题
编写高质量、可维护的代码
岗位要求：不限及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神
熟悉相关技术栈和开发工具
3-5年以上相关工作经验',
    '{"requiredSkills": ["React", "Redis", "MySQL", "Vue", "Linux"], "preferredSkills": [], "responsibilities": ["负责运维工程师相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题\\n编写高质量、可维护的代码"], "experienceRequirements": ["不限及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神\\n熟悉相关技术栈和开发工具\\n3-5年以上相关工作经验"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "南京", "salary": "13668-22211元/月", "salaryMin": 13668, "salaryMax": 22211, "salaryAvg": 17939, "platform": "招聘平台", "industry": "金融", "companySize": "500-999人", "collectedAt": "2026-02-25", "sourceJobId": "JOB164387", "education": "不限", "experience": "3-5年", "views": 1937, "applications": 120, "tags": ["React", "Redis", "MySQL", "Vue", "Linux"]}',
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

-- JD 129: 数据分析师（搜狐·南京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    158, 1,
    '数据分析师',
    '搜狐',
    '岗位职责：编写高质量、可维护的代码
参与技术方案评审，解决技术难题
负责数据分析师相关的系统设计和开发工作
岗位要求：熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神
不限及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Docker", "Jenkins", "数据结构", "Django", "深度学习"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题\\n负责数据分析师相关的系统设计和开发工作"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神\\n不限及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "南京", "salary": "8903-14468元/月", "salaryMin": 8903, "salaryMax": 14468, "salaryAvg": 11685, "platform": "招聘平台", "industry": "互联网", "companySize": "100-499人", "collectedAt": "2026-03-04", "sourceJobId": "JOB474223", "education": "不限", "experience": "1-3年", "views": 945, "applications": 149, "tags": ["Docker", "Jenkins", "数据结构", "Django", "深度学习"]}',
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

-- JD 130: 测试工程师（爱奇艺·苏州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    159, 1,
    '测试工程师',
    '爱奇艺',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
负责测试工程师相关的系统设计和开发工作
岗位要求：不限以上相关工作经验
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具
大专及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["Django", "算法", "Python", "Vue"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验\\n负责测试工程师相关的系统设计和开发工作"], "experienceRequirements": ["不限以上相关工作经验\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具\\n大专及以上学历，计算机相关专业优先"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "苏州", "salary": "7211-11718元/月", "salaryMin": 7211, "salaryMax": 11718, "salaryAvg": 9464, "platform": "招聘平台", "industry": "教育", "companySize": "20-99人", "collectedAt": "2026-02-13", "sourceJobId": "JOB874793", "education": "大专", "experience": "不限", "views": 3782, "applications": 62, "tags": ["Django", "算法", "Python", "Vue"]}',
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

-- JD 131: 数据工程师（旷视科技·长沙）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    160, 1,
    '数据工程师',
    '旷视科技',
    '岗位职责：编写高质量、可维护的代码
参与技术方案评审，解决技术难题
持续优化系统性能和用户体验
岗位要求：不限及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力
不限以上相关工作经验',
    '{"requiredSkills": ["算法", "React", "Docker"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题\\n持续优化系统性能和用户体验"], "experienceRequirements": ["不限及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力\\n不限以上相关工作经验"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "长沙", "salary": "6460-10498元/月", "salaryMin": 6460, "salaryMax": 10498, "salaryAvg": 8479, "platform": "招聘平台", "industry": "其他", "companySize": "0-20人", "collectedAt": "2026-02-26", "sourceJobId": "JOB944495", "education": "不限", "experience": "不限", "views": 1404, "applications": 26, "tags": ["算法", "React", "Docker"]}',
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

-- JD 132: 前端开发工程师（旷视科技·武汉）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    161, 1,
    '前端开发工程师',
    '旷视科技',
    '岗位职责：负责前端开发工程师相关的系统设计和开发工作
持续优化系统性能和用户体验
参与需求分析、系统设计、编码实现和测试
岗位要求：熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力
有大型项目经验者优先
不限以上相关工作经验',
    '{"requiredSkills": ["Redis", "Spring Boot", "Spark", "高并发"], "preferredSkills": [], "responsibilities": ["负责前端开发工程师相关的系统设计和开发工作\\n持续优化系统性能和用户体验\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力\\n有大型项目经验者优先\\n不限以上相关工作经验"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "武汉", "salary": "7318-11893元/月", "salaryMin": 7318, "salaryMax": 11893, "salaryAvg": 9605, "platform": "招聘平台", "industry": "能源", "companySize": "100-499人", "collectedAt": "2026-02-07", "sourceJobId": "JOB404728", "education": "本科", "experience": "不限", "views": 2480, "applications": 136, "tags": ["Redis", "Spring Boot", "Spark", "高并发"]}',
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

-- JD 133: 后端开发工程师（海尔·重庆）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    162, 1,
    '后端开发工程师',
    '海尔',
    '岗位职责：参与技术方案评审，解决技术难题
负责后端开发工程师相关的系统设计和开发工作
编写高质量、可维护的代码
岗位要求：有大型项目经验者优先
3-5年以上相关工作经验
熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["算法", "Redis", "分布式系统", "PyTorch", "TypeScript"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n负责后端开发工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码"], "experienceRequirements": ["有大型项目经验者优先\\n3-5年以上相关工作经验\\n熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "重庆", "salary": "23680-38480元/月", "salaryMin": 23680, "salaryMax": 38480, "salaryAvg": 31080, "platform": "招聘平台", "industry": "物流", "companySize": "10000人以上", "collectedAt": "2026-02-15", "sourceJobId": "JOB649317", "education": "博士", "experience": "3-5年", "views": 2932, "applications": 39, "tags": ["算法", "Redis", "分布式系统", "PyTorch", "TypeScript"]}',
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

-- JD 134: 全栈开发工程师（比亚迪·苏州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    163, 1,
    '全栈开发工程师',
    '比亚迪',
    '岗位职责：持续优化系统性能和用户体验
参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
岗位要求：大专及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神
应届生以上相关工作经验
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["TypeScript", "深度学习", "Redis", "Django", "Kubernetes"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["大专及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神\\n应届生以上相关工作经验\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "苏州", "salary": "4805-7808元/月", "salaryMin": 4805, "salaryMax": 7808, "salaryAvg": 6306, "platform": "招聘平台", "industry": "其他", "companySize": "10000人以上", "collectedAt": "2026-02-28", "sourceJobId": "JOB160759", "education": "大专", "experience": "应届生", "views": 1497, "applications": 190, "tags": ["TypeScript", "深度学习", "Redis", "Django", "Kubernetes"]}',
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

-- JD 135: 机器学习工程师（京东·成都）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    164, 1,
    '机器学习工程师',
    '京东',
    '岗位职责：编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
岗位要求：具备良好的沟通能力和团队协作精神
有大型项目经验者优先
熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Hadoop", "Java", "NumPy", "Git"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n有大型项目经验者优先\\n熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "成都", "salary": "17757-28855元/月", "salaryMin": 17757, "salaryMax": 28855, "salaryAvg": 23306, "platform": "招聘平台", "industry": "制造业", "companySize": "100-499人", "collectedAt": "2026-02-16", "sourceJobId": "JOB962198", "education": "不限", "experience": "10年以上", "views": 3179, "applications": 173, "tags": ["Hadoop", "Java", "NumPy", "Git"]}',
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

-- JD 136: 数据工程师（比亚迪·武汉）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    165, 1,
    '数据工程师',
    '比亚迪',
    '岗位职责：编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
岗位要求：具备良好的编程基础和算法能力
有大型项目经验者优先
本科及以上学历，计算机相关专业优先
不限以上相关工作经验',
    '{"requiredSkills": ["Scikit-learn", "Linux", "Spark", "React", "Python", "算法"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验"], "experienceRequirements": ["具备良好的编程基础和算法能力\\n有大型项目经验者优先\\n本科及以上学历，计算机相关专业优先\\n不限以上相关工作经验"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "武汉", "salary": "5256-8541元/月", "salaryMin": 5256, "salaryMax": 8541, "salaryAvg": 6898, "platform": "招聘平台", "industry": "其他", "companySize": "20-99人", "collectedAt": "2026-02-20", "sourceJobId": "JOB437004", "education": "本科", "experience": "不限", "views": 3726, "applications": 158, "tags": ["Scikit-learn", "Linux", "Spark", "React", "Python", "算法"]}',
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

-- JD 137: 运维工程师（商汤科技·天津）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    166, 1,
    '运维工程师',
    '商汤科技',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
负责运维工程师相关的系统设计和开发工作
编写高质量、可维护的代码
岗位要求：熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力
有大型项目经验者优先
博士及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["Git", "Kubernetes", "Jenkins", "Spring Boot", "深度学习", "数据结构"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n负责运维工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力\\n有大型项目经验者优先\\n博士及以上学历，计算机相关专业优先"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "天津", "salary": "10771-17502元/月", "salaryMin": 10771, "salaryMax": 17502, "salaryAvg": 14136, "platform": "招聘平台", "industry": "医疗", "companySize": "20-99人", "collectedAt": "2026-02-04", "sourceJobId": "JOB580127", "education": "博士", "experience": "应届生", "views": 620, "applications": 189, "tags": ["Git", "Kubernetes", "Jenkins", "Spring Boot", "深度学习", "数据结构"]}',
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

-- JD 138: 深度学习工程师（优酷·成都）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    167, 1,
    '深度学习工程师',
    '优酷',
    '岗位职责：持续优化系统性能和用户体验
负责深度学习工程师相关的系统设计和开发工作
编写高质量、可维护的代码
岗位要求：具备良好的沟通能力和团队协作精神
有大型项目经验者优先
博士及以上学历，计算机相关专业优先
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["Spring Boot", "Redis", "Scikit-learn"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n负责深度学习工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n有大型项目经验者优先\\n博士及以上学历，计算机相关专业优先\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "成都", "salary": "12302-19991元/月", "salaryMin": 12302, "salaryMax": 19991, "salaryAvg": 16146, "platform": "招聘平台", "industry": "医疗", "companySize": "100-499人", "collectedAt": "2026-02-05", "sourceJobId": "JOB440133", "education": "博士", "experience": "不限", "views": 4042, "applications": 158, "tags": ["Spring Boot", "Redis", "Scikit-learn"]}',
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

-- JD 139: Java开发工程师（联想·长沙）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    168, 1,
    'Java开发工程师',
    '联想',
    '岗位职责：参与技术方案评审，解决技术难题
负责Java开发工程师相关的系统设计和开发工作
编写高质量、可维护的代码
岗位要求：有大型项目经验者优先
1-3年以上相关工作经验
熟悉相关技术栈和开发工具
硕士及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["NumPy", "TensorFlow", "机器学习"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n负责Java开发工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码"], "experienceRequirements": ["有大型项目经验者优先\\n1-3年以上相关工作经验\\n熟悉相关技术栈和开发工具\\n硕士及以上学历，计算机相关专业优先"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "长沙", "salary": "11084-18012元/月", "salaryMin": 11084, "salaryMax": 18012, "salaryAvg": 14548, "platform": "招聘平台", "industry": "其他", "companySize": "500-999人", "collectedAt": "2026-02-26", "sourceJobId": "JOB907151", "education": "硕士", "experience": "1-3年", "views": 3057, "applications": 38, "tags": ["NumPy", "TensorFlow", "机器学习"]}',
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

-- JD 140: 技术总监（当当网·北京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    169, 1,
    '技术总监',
    '当当网',
    '岗位职责：负责技术总监相关的系统设计和开发工作
编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
岗位要求：大专及以上学历，计算机相关专业优先
1-3年以上相关工作经验
有大型项目经验者优先
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["JavaScript", "Spark", "Django", "React"], "preferredSkills": [], "responsibilities": ["负责技术总监相关的系统设计和开发工作\\n编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["大专及以上学历，计算机相关专业优先\\n1-3年以上相关工作经验\\n有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "北京", "salary": "9747-15839元/月", "salaryMin": 9747, "salaryMax": 15839, "salaryAvg": 12793, "platform": "招聘平台", "industry": "医疗", "companySize": "100-499人", "collectedAt": "2026-02-24", "sourceJobId": "JOB242737", "education": "大专", "experience": "1-3年", "views": 4036, "applications": 13, "tags": ["JavaScript", "Spark", "Django", "React"]}',
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

-- JD 141: 架构师（小米·苏州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    170, 1,
    '架构师',
    '小米',
    '岗位职责：持续优化系统性能和用户体验
负责架构师相关的系统设计和开发工作
参与技术方案评审，解决技术难题
岗位要求：3-5年以上相关工作经验
具备良好的沟通能力和团队协作精神
有大型项目经验者优先
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["分布式系统", "高并发", "Vue", "Spark", "Spring Boot"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n负责架构师相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["3-5年以上相关工作经验\\n具备良好的沟通能力和团队协作精神\\n有大型项目经验者优先\\n具备良好的编程基础和算法能力"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "苏州", "salary": "10266-16682元/月", "salaryMin": 10266, "salaryMax": 16682, "salaryAvg": 13474, "platform": "招聘平台", "industry": "金融", "companySize": "500-999人", "collectedAt": "2026-02-04", "sourceJobId": "JOB776758", "education": "不限", "experience": "3-5年", "views": 4907, "applications": 196, "tags": ["分布式系统", "高并发", "Vue", "Spark", "Spring Boot"]}',
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

-- JD 142: 测试工程师（携程·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    171, 1,
    '测试工程师',
    '携程',
    '岗位职责：负责测试工程师相关的系统设计和开发工作
持续优化系统性能和用户体验
参与技术方案评审，解决技术难题
岗位要求：本科及以上学历，计算机相关专业优先
熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神
有大型项目经验者优先',
    '{"requiredSkills": ["Linux", "Git", "数据结构", "Pandas", "React", "Python"], "preferredSkills": [], "responsibilities": ["负责测试工程师相关的系统设计和开发工作\\n持续优化系统性能和用户体验\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["本科及以上学历，计算机相关专业优先\\n熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神\\n有大型项目经验者优先"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "8691-14123元/月", "salaryMin": 8691, "salaryMax": 14123, "salaryAvg": 11407, "platform": "招聘平台", "industry": "金融", "companySize": "20-99人", "collectedAt": "2026-03-02", "sourceJobId": "JOB517143", "education": "本科", "experience": "应届生", "views": 1580, "applications": 15, "tags": ["Linux", "Git", "数据结构", "Pandas", "React", "Python"]}',
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

-- JD 143: 前端开发工程师（商汤科技·杭州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    172, 1,
    '前端开发工程师',
    '商汤科技',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
负责前端开发工程师相关的系统设计和开发工作
持续优化系统性能和用户体验
岗位要求：本科及以上学历，计算机相关专业优先
5-10年以上相关工作经验
有大型项目经验者优先
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["Kubernetes", "Redis", "数据结构", "Hadoop", "React"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n负责前端开发工程师相关的系统设计和开发工作\\n持续优化系统性能和用户体验"], "experienceRequirements": ["本科及以上学历，计算机相关专业优先\\n5-10年以上相关工作经验\\n有大型项目经验者优先\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "杭州", "salary": "20561-33412元/月", "salaryMin": 20561, "salaryMax": 33412, "salaryAvg": 26986, "platform": "招聘平台", "industry": "零售", "companySize": "1000-9999人", "collectedAt": "2026-02-19", "sourceJobId": "JOB575774", "education": "本科", "experience": "5-10年", "views": 540, "applications": 113, "tags": ["Kubernetes", "Redis", "数据结构", "Hadoop", "React"]}',
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

-- JD 144: DevOps工程师（字节跳动·上海）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    173, 1,
    'DevOps工程师',
    '字节跳动',
    '岗位职责：负责DevOps工程师相关的系统设计和开发工作
参与技术方案评审，解决技术难题
持续优化系统性能和用户体验
岗位要求：熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神
不限及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["深度学习", "Django", "TypeScript", "Git", "Kubernetes", "MySQL"], "preferredSkills": [], "responsibilities": ["负责DevOps工程师相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题\\n持续优化系统性能和用户体验"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神\\n不限及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "上海", "salary": "8386-13627元/月", "salaryMin": 8386, "salaryMax": 13627, "salaryAvg": 11006, "platform": "招聘平台", "industry": "互联网", "companySize": "20-99人", "collectedAt": "2026-03-06", "sourceJobId": "JOB681898", "education": "不限", "experience": "应届生", "views": 4334, "applications": 29, "tags": ["深度学习", "Django", "TypeScript", "Git", "Kubernetes", "MySQL"]}',
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

-- JD 145: Python开发工程师（大华股份·重庆）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    174, 1,
    'Python开发工程师',
    '大华股份',
    '岗位职责：持续优化系统性能和用户体验
参与技术方案评审，解决技术难题
编写高质量、可维护的代码
岗位要求：1-3年以上相关工作经验
硕士及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神
有大型项目经验者优先',
    '{"requiredSkills": ["Spring Boot", "Git", "Python"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n参与技术方案评审", "解决技术难题\\n编写高质量、可维护的代码"], "experienceRequirements": ["1-3年以上相关工作经验\\n硕士及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神\\n有大型项目经验者优先"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "重庆", "salary": "11302-18366元/月", "salaryMin": 11302, "salaryMax": 18366, "salaryAvg": 14834, "platform": "招聘平台", "industry": "教育", "companySize": "10000人以上", "collectedAt": "2026-03-05", "sourceJobId": "JOB567736", "education": "硕士", "experience": "1-3年", "views": 1397, "applications": 174, "tags": ["Spring Boot", "Git", "Python"]}',
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

-- JD 146: 架构师（依图科技·郑州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    175, 1,
    '架构师',
    '依图科技',
    '岗位职责：负责架构师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
岗位要求：熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力
本科及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["PyTorch", "Kubernetes", "JavaScript", "数据结构", "Scikit-learn"], "preferredSkills": [], "responsibilities": ["负责架构师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力\\n本科及以上学历，计算机相关专业优先"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "郑州", "salary": "6350-10319元/月", "salaryMin": 6350, "salaryMax": 10319, "salaryAvg": 8334, "platform": "招聘平台", "industry": "金融", "companySize": "1000-9999人", "collectedAt": "2026-02-15", "sourceJobId": "JOB798964", "education": "本科", "experience": "1-3年", "views": 3569, "applications": 148, "tags": ["PyTorch", "Kubernetes", "JavaScript", "数据结构", "Scikit-learn"]}',
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

-- JD 147: Java开发工程师（联想·上海）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    176, 1,
    'Java开发工程师',
    '联想',
    '岗位职责：持续优化系统性能和用户体验
参与技术方案评审，解决技术难题
编写高质量、可维护的代码
岗位要求：熟悉相关技术栈和开发工具
不限及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Python", "Hadoop", "Java"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n参与技术方案评审", "解决技术难题\\n编写高质量、可维护的代码"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n不限及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "上海", "salary": "26009-42265元/月", "salaryMin": 26009, "salaryMax": 42265, "salaryAvg": 34137, "platform": "招聘平台", "industry": "房地产", "companySize": "500-999人", "collectedAt": "2026-03-04", "sourceJobId": "JOB284265", "education": "不限", "experience": "5-10年", "views": 2520, "applications": 53, "tags": ["Python", "Hadoop", "Java"]}',
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

-- JD 148: 运维工程师（云从科技·北京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    177, 1,
    '运维工程师',
    '云从科技',
    '岗位职责：编写高质量、可维护的代码
参与技术方案评审，解决技术难题
负责运维工程师相关的系统设计和开发工作
岗位要求：具备良好的沟通能力和团队协作精神
不限以上相关工作经验
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["MySQL", "Python", "Git", "Vue", "Spring Boot", "React"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题\\n负责运维工程师相关的系统设计和开发工作"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n不限以上相关工作经验\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "北京", "salary": "9534-15494元/月", "salaryMin": 9534, "salaryMax": 15494, "salaryAvg": 12514, "platform": "招聘平台", "industry": "零售", "companySize": "20-99人", "collectedAt": "2026-02-06", "sourceJobId": "JOB702430", "education": "大专", "experience": "不限", "views": 2393, "applications": 6, "tags": ["MySQL", "Python", "Git", "Vue", "Spring Boot", "React"]}',
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

-- JD 149: 数据工程师（快手·西安）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    178, 1,
    '数据工程师',
    '快手',
    '岗位职责：持续优化系统性能和用户体验
编写高质量、可维护的代码
负责数据工程师相关的系统设计和开发工作
岗位要求：熟悉相关技术栈和开发工具
不限及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神
1-3年以上相关工作经验',
    '{"requiredSkills": ["Django", "JavaScript", "React", "Linux", "Hadoop", "Kubernetes"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n编写高质量、可维护的代码\\n负责数据工程师相关的系统设计和开发工作"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n不限及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神\\n1-3年以上相关工作经验"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "西安", "salary": "6983-11347元/月", "salaryMin": 6983, "salaryMax": 11347, "salaryAvg": 9165, "platform": "招聘平台", "industry": "制造业", "companySize": "10000人以上", "collectedAt": "2026-02-04", "sourceJobId": "JOB161248", "education": "不限", "experience": "1-3年", "views": 4707, "applications": 169, "tags": ["Django", "JavaScript", "React", "Linux", "Hadoop", "Kubernetes"]}',
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

-- JD 150: Python开发工程师（阿里巴巴·上海）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    179, 1,
    'Python开发工程师',
    '阿里巴巴',
    '岗位职责：编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
岗位要求：不限及以上学历，计算机相关专业优先
有大型项目经验者优先
具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["大数据", "微服务", "TypeScript", "分布式系统", "Pandas", "JavaScript"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["不限及以上学历，计算机相关专业优先\\n有大型项目经验者优先\\n具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "上海", "salary": "14310-23253元/月", "salaryMin": 14310, "salaryMax": 23253, "salaryAvg": 18781, "platform": "招聘平台", "industry": "教育", "companySize": "1000-9999人", "collectedAt": "2026-02-04", "sourceJobId": "JOB831921", "education": "不限", "experience": "3-5年", "views": 3709, "applications": 18, "tags": ["大数据", "微服务", "TypeScript", "分布式系统", "Pandas", "JavaScript"]}',
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

-- JD 151: 数据分析师（快手·南京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    180, 1,
    '数据分析师',
    '快手',
    '岗位职责：编写高质量、可维护的代码
负责数据分析师相关的系统设计和开发工作
参与技术方案评审，解决技术难题
岗位要求：熟悉相关技术栈和开发工具
5-10年以上相关工作经验
有大型项目经验者优先
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Spring Boot", "Vue", "Scikit-learn"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n负责数据分析师相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n5-10年以上相关工作经验\\n有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "南京", "salary": "39728-64559元/月", "salaryMin": 39728, "salaryMax": 64559, "salaryAvg": 52143, "platform": "招聘平台", "industry": "零售", "companySize": "100-499人", "collectedAt": "2026-02-26", "sourceJobId": "JOB121507", "education": "博士", "experience": "5-10年", "views": 1165, "applications": 105, "tags": ["Spring Boot", "Vue", "Scikit-learn"]}',
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

-- JD 152: 全栈开发工程师（京东·广州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    181, 1,
    '全栈开发工程师',
    '京东',
    '岗位职责：负责全栈开发工程师相关的系统设计和开发工作
参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
岗位要求：不限及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神
有大型项目经验者优先
3-5年以上相关工作经验',
    '{"requiredSkills": ["Vue", "React", "TensorFlow", "JavaScript", "微服务", "Spring Boot"], "preferredSkills": [], "responsibilities": ["负责全栈开发工程师相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["不限及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神\\n有大型项目经验者优先\\n3-5年以上相关工作经验"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "广州", "salary": "12140-19728元/月", "salaryMin": 12140, "salaryMax": 19728, "salaryAvg": 15934, "platform": "招聘平台", "industry": "房地产", "companySize": "1000-9999人", "collectedAt": "2026-02-22", "sourceJobId": "JOB225128", "education": "不限", "experience": "3-5年", "views": 4664, "applications": 99, "tags": ["Vue", "React", "TensorFlow", "JavaScript", "微服务", "Spring Boot"]}',
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

-- JD 153: 前端开发工程师（腾讯·成都）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    182, 1,
    '前端开发工程师',
    '腾讯',
    '岗位职责：持续优化系统性能和用户体验
负责前端开发工程师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
岗位要求：3-5年以上相关工作经验
熟悉相关技术栈和开发工具
有大型项目经验者优先
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Hadoop", "分布式系统", "PyTorch", "Spark", "Redis"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n负责前端开发工程师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["3-5年以上相关工作经验\\n熟悉相关技术栈和开发工具\\n有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "成都", "salary": "15136-24597元/月", "salaryMin": 15136, "salaryMax": 24597, "salaryAvg": 19866, "platform": "招聘平台", "industry": "互联网", "companySize": "1000-9999人", "collectedAt": "2026-02-09", "sourceJobId": "JOB232616", "education": "硕士", "experience": "3-5年", "views": 3140, "applications": 152, "tags": ["Hadoop", "分布式系统", "PyTorch", "Spark", "Redis"]}',
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

-- JD 154: 全栈开发工程师（字节跳动·青岛）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    183, 1,
    '全栈开发工程师',
    '字节跳动',
    '岗位职责：负责全栈开发工程师相关的系统设计和开发工作
持续优化系统性能和用户体验
参与需求分析、系统设计、编码实现和测试
岗位要求：具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神
大专及以上学历，计算机相关专业优先
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["分布式系统", "Git", "Hadoop", "深度学习"], "preferredSkills": [], "responsibilities": ["负责全栈开发工程师相关的系统设计和开发工作\\n持续优化系统性能和用户体验\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神\\n大专及以上学历，计算机相关专业优先\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "青岛", "salary": "5708-9275元/月", "salaryMin": 5708, "salaryMax": 9275, "salaryAvg": 7491, "platform": "招聘平台", "industry": "零售", "companySize": "10000人以上", "collectedAt": "2026-03-06", "sourceJobId": "JOB103570", "education": "大专", "experience": "不限", "views": 1345, "applications": 128, "tags": ["分布式系统", "Git", "Hadoop", "深度学习"]}',
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

-- JD 155: 算法工程师（大疆创新·天津）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    184, 1,
    '算法工程师',
    '大疆创新',
    '岗位职责：参与技术方案评审，解决技术难题
持续优化系统性能和用户体验
编写高质量、可维护的代码
岗位要求：具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神
有大型项目经验者优先
1-3年以上相关工作经验',
    '{"requiredSkills": ["NumPy", "JavaScript", "Spark", "高并发"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n持续优化系统性能和用户体验\\n编写高质量、可维护的代码"], "experienceRequirements": ["具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神\\n有大型项目经验者优先\\n1-3年以上相关工作经验"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "天津", "salary": "6026-9792元/月", "salaryMin": 6026, "salaryMax": 9792, "salaryAvg": 7909, "platform": "招聘平台", "industry": "能源", "companySize": "10000人以上", "collectedAt": "2026-02-19", "sourceJobId": "JOB515682", "education": "不限", "experience": "1-3年", "views": 1740, "applications": 88, "tags": ["NumPy", "JavaScript", "Spark", "高并发"]}',
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

-- JD 156: 深度学习工程师（华为·郑州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    185, 1,
    '深度学习工程师',
    '华为',
    '岗位职责：持续优化系统性能和用户体验
参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
岗位要求：熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神
有大型项目经验者优先
应届生以上相关工作经验',
    '{"requiredSkills": ["Redis", "JavaScript", "Spark", "Spring Boot"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神\\n有大型项目经验者优先\\n应届生以上相关工作经验"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "郑州", "salary": "7171-11653元/月", "salaryMin": 7171, "salaryMax": 11653, "salaryAvg": 9412, "platform": "招聘平台", "industry": "金融", "companySize": "10000人以上", "collectedAt": "2026-02-17", "sourceJobId": "JOB115633", "education": "博士", "experience": "应届生", "views": 4741, "applications": 170, "tags": ["Redis", "JavaScript", "Spark", "Spring Boot"]}',
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

-- JD 157: 架构师（美的·青岛）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    186, 1,
    '架构师',
    '美的',
    '岗位职责：编写高质量、可维护的代码
负责架构师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
岗位要求：具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神
不限以上相关工作经验
有大型项目经验者优先',
    '{"requiredSkills": ["TypeScript", "PyTorch", "微服务", "分布式系统", "Hadoop", "Vue"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n负责架构师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神\\n不限以上相关工作经验\\n有大型项目经验者优先"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "青岛", "salary": "12841-20868元/月", "salaryMin": 12841, "salaryMax": 20868, "salaryAvg": 16854, "platform": "招聘平台", "industry": "制造业", "companySize": "100-499人", "collectedAt": "2026-02-07", "sourceJobId": "JOB865090", "education": "博士", "experience": "不限", "views": 3710, "applications": 44, "tags": ["TypeScript", "PyTorch", "微服务", "分布式系统", "Hadoop", "Vue"]}',
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

-- JD 158: 测试工程师（百度·青岛）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    187, 1,
    '测试工程师',
    '百度',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
参与技术方案评审，解决技术难题
岗位要求：大专及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["微服务", "Scikit-learn", "Hadoop"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["大专及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "青岛", "salary": "14060-22847元/月", "salaryMin": 14060, "salaryMax": 22847, "salaryAvg": 18453, "platform": "招聘平台", "industry": "制造业", "companySize": "500-999人", "collectedAt": "2026-02-23", "sourceJobId": "JOB744025", "education": "大专", "experience": "5-10年", "views": 1559, "applications": 158, "tags": ["微服务", "Scikit-learn", "Hadoop"]}',
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

-- JD 159: DevOps工程师（格力·武汉）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    188, 1,
    'DevOps工程师',
    '格力',
    '岗位职责：参与技术方案评审，解决技术难题
持续优化系统性能和用户体验
参与需求分析、系统设计、编码实现和测试
岗位要求：熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力
本科及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Django", "分布式系统", "MongoDB", "机器学习", "大数据", "数据结构"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n持续优化系统性能和用户体验\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力\\n本科及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "武汉", "salary": "25461-41374元/月", "salaryMin": 25461, "salaryMax": 41374, "salaryAvg": 33417, "platform": "招聘平台", "industry": "物流", "companySize": "500-999人", "collectedAt": "2026-02-21", "sourceJobId": "JOB101006", "education": "本科", "experience": "10年以上", "views": 1159, "applications": 60, "tags": ["Django", "分布式系统", "MongoDB", "机器学习", "大数据", "数据结构"]}',
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

-- JD 160: 运维工程师（阿里巴巴·重庆）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    189, 1,
    '运维工程师',
    '阿里巴巴',
    '岗位职责：编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
岗位要求：熟悉相关技术栈和开发工具
不限以上相关工作经验
本科及以上学历，计算机相关专业优先
有大型项目经验者优先',
    '{"requiredSkills": ["Hadoop", "Spring Boot", "算法", "Vue", "深度学习"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n不限以上相关工作经验\\n本科及以上学历，计算机相关专业优先\\n有大型项目经验者优先"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "重庆", "salary": "7165-11643元/月", "salaryMin": 7165, "salaryMax": 11643, "salaryAvg": 9404, "platform": "招聘平台", "industry": "互联网", "companySize": "0-20人", "collectedAt": "2026-02-24", "sourceJobId": "JOB529728", "education": "本科", "experience": "不限", "views": 480, "applications": 88, "tags": ["Hadoop", "Spring Boot", "算法", "Vue", "深度学习"]}',
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

-- JD 161: 深度学习工程师（中兴通讯·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    190, 1,
    '深度学习工程师',
    '中兴通讯',
    '岗位职责：参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
岗位要求：有大型项目经验者优先
具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["Hadoop", "机器学习", "NumPy", "TypeScript", "Docker", "TensorFlow"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码"], "experienceRequirements": ["有大型项目经验者优先\\n具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "68453-111236元/月", "salaryMin": 68453, "salaryMax": 111236, "salaryAvg": 89844, "platform": "招聘平台", "industry": "金融", "companySize": "500-999人", "collectedAt": "2026-02-19", "sourceJobId": "JOB907743", "education": "博士", "experience": "10年以上", "views": 1559, "applications": 193, "tags": ["Hadoop", "机器学习", "NumPy", "TypeScript", "Docker", "TensorFlow"]}',
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

-- JD 162: 运维工程师（云从科技·郑州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    191, 1,
    '运维工程师',
    '云从科技',
    '岗位职责：持续优化系统性能和用户体验
负责运维工程师相关的系统设计和开发工作
参与技术方案评审，解决技术难题
岗位要求：具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具
不限以上相关工作经验
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["深度学习", "Kubernetes", "数据结构", "Vue", "PyTorch", "Docker"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n负责运维工程师相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具\\n不限以上相关工作经验\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "郑州", "salary": "6783-11023元/月", "salaryMin": 6783, "salaryMax": 11023, "salaryAvg": 8903, "platform": "招聘平台", "industry": "零售", "companySize": "500-999人", "collectedAt": "2026-02-15", "sourceJobId": "JOB808372", "education": "本科", "experience": "不限", "views": 4255, "applications": 156, "tags": ["深度学习", "Kubernetes", "数据结构", "Vue", "PyTorch", "Docker"]}',
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

-- JD 163: DevOps工程师（云从科技·南京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    192, 1,
    'DevOps工程师',
    '云从科技',
    '岗位职责：参与技术方案评审，解决技术难题
编写高质量、可维护的代码
持续优化系统性能和用户体验
岗位要求：有大型项目经验者优先
熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["深度学习", "微服务", "PyTorch"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n编写高质量、可维护的代码\\n持续优化系统性能和用户体验"], "experienceRequirements": ["有大型项目经验者优先\\n熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "南京", "salary": "26376-42861元/月", "salaryMin": 26376, "salaryMax": 42861, "salaryAvg": 34618, "platform": "招聘平台", "industry": "互联网", "companySize": "500-999人", "collectedAt": "2026-02-23", "sourceJobId": "JOB419304", "education": "不限", "experience": "10年以上", "views": 4777, "applications": 77, "tags": ["深度学习", "微服务", "PyTorch"]}',
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

-- JD 164: DevOps工程师（美的·杭州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    193, 1,
    'DevOps工程师',
    '美的',
    '岗位职责：负责DevOps工程师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
岗位要求：具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具
有大型项目经验者优先',
    '{"requiredSkills": ["机器学习", "TypeScript", "JavaScript", "Vue", "算法", "Spring Boot"], "preferredSkills": [], "responsibilities": ["负责DevOps工程师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具\\n有大型项目经验者优先"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "杭州", "salary": "18656-30317元/月", "salaryMin": 18656, "salaryMax": 30317, "salaryAvg": 24486, "platform": "招聘平台", "industry": "能源", "companySize": "20-99人", "collectedAt": "2026-03-03", "sourceJobId": "JOB444699", "education": "本科", "experience": "5-10年", "views": 771, "applications": 152, "tags": ["机器学习", "TypeScript", "JavaScript", "Vue", "算法", "Spring Boot"]}',
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

-- JD 165: Java开发工程师（华为·杭州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    194, 1,
    'Java开发工程师',
    '华为',
    '岗位职责：持续优化系统性能和用户体验
参与技术方案评审，解决技术难题
负责Java开发工程师相关的系统设计和开发工作
岗位要求：熟悉相关技术栈和开发工具
有大型项目经验者优先
应届生以上相关工作经验
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Spring Boot", "Kubernetes", "JavaScript", "MongoDB", "NumPy", "微服务"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n参与技术方案评审", "解决技术难题\\n负责Java开发工程师相关的系统设计和开发工作"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n有大型项目经验者优先\\n应届生以上相关工作经验\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "杭州", "salary": "10099-16411元/月", "salaryMin": 10099, "salaryMax": 16411, "salaryAvg": 13255, "platform": "招聘平台", "industry": "医疗", "companySize": "100-499人", "collectedAt": "2026-03-04", "sourceJobId": "JOB852016", "education": "硕士", "experience": "应届生", "views": 1877, "applications": 181, "tags": ["Spring Boot", "Kubernetes", "JavaScript", "MongoDB", "NumPy", "微服务"]}',
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

-- JD 166: 深度学习工程师（携程·重庆）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    195, 1,
    '深度学习工程师',
    '携程',
    '岗位职责：负责深度学习工程师相关的系统设计和开发工作
持续优化系统性能和用户体验
参与技术方案评审，解决技术难题
岗位要求：有大型项目经验者优先
具备良好的沟通能力和团队协作精神
博士及以上学历，计算机相关专业优先
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["Linux", "分布式系统", "MySQL", "Python", "Docker"], "preferredSkills": [], "responsibilities": ["负责深度学习工程师相关的系统设计和开发工作\\n持续优化系统性能和用户体验\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神\\n博士及以上学历，计算机相关专业优先\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "重庆", "salary": "19064-30979元/月", "salaryMin": 19064, "salaryMax": 30979, "salaryAvg": 25021, "platform": "招聘平台", "industry": "能源", "companySize": "100-499人", "collectedAt": "2026-02-19", "sourceJobId": "JOB136660", "education": "博士", "experience": "3-5年", "views": 1853, "applications": 7, "tags": ["Linux", "分布式系统", "MySQL", "Python", "Docker"]}',
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

-- JD 167: 数据工程师（大疆创新·上海）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    196, 1,
    '数据工程师',
    '大疆创新',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
负责数据工程师相关的系统设计和开发工作
岗位要求：1-3年以上相关工作经验
有大型项目经验者优先
具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Git", "MySQL", "Redis"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题\\n负责数据工程师相关的系统设计和开发工作"], "experienceRequirements": ["1-3年以上相关工作经验\\n有大型项目经验者优先\\n具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "上海", "salary": "9481-15408元/月", "salaryMin": 9481, "salaryMax": 15408, "salaryAvg": 12444, "platform": "招聘平台", "industry": "物流", "companySize": "0-20人", "collectedAt": "2026-02-07", "sourceJobId": "JOB527308", "education": "不限", "experience": "1-3年", "views": 1179, "applications": 52, "tags": ["Git", "MySQL", "Redis"]}',
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

-- JD 168: 数据分析师（滴滴·北京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    197, 1,
    '数据分析师',
    '滴滴',
    '岗位职责：负责数据分析师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
岗位要求：具备良好的沟通能力和团队协作精神
熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力
有大型项目经验者优先',
    '{"requiredSkills": ["Spark", "Hadoop", "深度学习", "TensorFlow", "分布式系统"], "preferredSkills": [], "responsibilities": ["负责数据分析师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力\\n有大型项目经验者优先"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "北京", "salary": "7642-12418元/月", "salaryMin": 7642, "salaryMax": 12418, "salaryAvg": 10030, "platform": "招聘平台", "industry": "金融", "companySize": "1000-9999人", "collectedAt": "2026-02-15", "sourceJobId": "JOB467793", "education": "本科", "experience": "应届生", "views": 4489, "applications": 110, "tags": ["Spark", "Hadoop", "深度学习", "TensorFlow", "分布式系统"]}',
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

-- JD 169: 运维工程师（华为·青岛）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    198, 1,
    '运维工程师',
    '华为',
    '岗位职责：负责运维工程师相关的系统设计和开发工作
编写高质量、可维护的代码
参与技术方案评审，解决技术难题
岗位要求：有大型项目经验者优先
具备良好的编程基础和算法能力
本科及以上学历，计算机相关专业优先
1-3年以上相关工作经验',
    '{"requiredSkills": ["Docker", "JavaScript", "React"], "preferredSkills": [], "responsibilities": ["负责运维工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["有大型项目经验者优先\\n具备良好的编程基础和算法能力\\n本科及以上学历，计算机相关专业优先\\n1-3年以上相关工作经验"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "青岛", "salary": "9397-15270元/月", "salaryMin": 9397, "salaryMax": 15270, "salaryAvg": 12333, "platform": "招聘平台", "industry": "房地产", "companySize": "1000-9999人", "collectedAt": "2026-02-11", "sourceJobId": "JOB131063", "education": "本科", "experience": "1-3年", "views": 3617, "applications": 116, "tags": ["Docker", "JavaScript", "React"]}',
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

-- JD 170: 测试工程师（携程·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    199, 1,
    '测试工程师',
    '携程',
    '岗位职责：持续优化系统性能和用户体验
编写高质量、可维护的代码
参与技术方案评审，解决技术难题
岗位要求：具备良好的沟通能力和团队协作精神
熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力
不限及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["Linux", "Hadoop", "TypeScript"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力\\n不限及以上学历，计算机相关专业优先"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "6241-10141元/月", "salaryMin": 6241, "salaryMax": 10141, "salaryAvg": 8191, "platform": "招聘平台", "industry": "互联网", "companySize": "100-499人", "collectedAt": "2026-03-02", "sourceJobId": "JOB970181", "education": "不限", "experience": "应届生", "views": 2027, "applications": 85, "tags": ["Linux", "Hadoop", "TypeScript"]}',
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

-- JD 171: 后端开发工程师（哔哩哔哩·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    200, 1,
    '后端开发工程师',
    '哔哩哔哩',
    '岗位职责：负责后端开发工程师相关的系统设计和开发工作
持续优化系统性能和用户体验
参与需求分析、系统设计、编码实现和测试
岗位要求：大专及以上学历，计算机相关专业优先
有大型项目经验者优先
不限以上相关工作经验
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["深度学习", "分布式系统", "TensorFlow", "PyTorch", "高并发", "Python"], "preferredSkills": [], "responsibilities": ["负责后端开发工程师相关的系统设计和开发工作\\n持续优化系统性能和用户体验\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["大专及以上学历，计算机相关专业优先\\n有大型项目经验者优先\\n不限以上相关工作经验\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "8010-13017元/月", "salaryMin": 8010, "salaryMax": 13017, "salaryAvg": 10513, "platform": "招聘平台", "industry": "其他", "companySize": "0-20人", "collectedAt": "2026-02-05", "sourceJobId": "JOB155439", "education": "大专", "experience": "不限", "views": 3989, "applications": 34, "tags": ["深度学习", "分布式系统", "TensorFlow", "PyTorch", "高并发", "Python"]}',
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

-- JD 172: DevOps工程师（滴滴·重庆）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    201, 1,
    'DevOps工程师',
    '滴滴',
    '岗位职责：参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
岗位要求：有大型项目经验者优先
具备良好的沟通能力和团队协作精神
博士及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Python", "数据结构", "算法", "TypeScript", "Scikit-learn", "Hadoop"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验"], "experienceRequirements": ["有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神\\n博士及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "重庆", "salary": "11115-18062元/月", "salaryMin": 11115, "salaryMax": 18062, "salaryAvg": 14588, "platform": "招聘平台", "industry": "物流", "companySize": "1000-9999人", "collectedAt": "2026-02-18", "sourceJobId": "JOB352394", "education": "博士", "experience": "不限", "views": 2216, "applications": 30, "tags": ["Python", "数据结构", "算法", "TypeScript", "Scikit-learn", "Hadoop"]}',
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

-- JD 173: DevOps工程师（依图科技·武汉）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    202, 1,
    'DevOps工程师',
    '依图科技',
    '岗位职责：参与技术方案评审，解决技术难题
编写高质量、可维护的代码
负责DevOps工程师相关的系统设计和开发工作
岗位要求：熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神
不限及以上学历，计算机相关专业优先
有大型项目经验者优先',
    '{"requiredSkills": ["MySQL", "Kubernetes", "Pandas", "Jenkins", "Java", "Hadoop"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n编写高质量、可维护的代码\\n负责DevOps工程师相关的系统设计和开发工作"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神\\n不限及以上学历，计算机相关专业优先\\n有大型项目经验者优先"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "武汉", "salary": "6532-10614元/月", "salaryMin": 6532, "salaryMax": 10614, "salaryAvg": 8573, "platform": "招聘平台", "industry": "零售", "companySize": "1000-9999人", "collectedAt": "2026-02-16", "sourceJobId": "JOB713501", "education": "不限", "experience": "不限", "views": 3087, "applications": 183, "tags": ["MySQL", "Kubernetes", "Pandas", "Jenkins", "Java", "Hadoop"]}',
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

-- JD 174: 数据分析师（大疆创新·南京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    203, 1,
    '数据分析师',
    '大疆创新',
    '岗位职责：编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
岗位要求：具备良好的沟通能力和团队协作精神
硕士及以上学历，计算机相关专业优先
有大型项目经验者优先
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["NumPy", "微服务", "Django", "大数据", "Pandas", "PyTorch"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n硕士及以上学历，计算机相关专业优先\\n有大型项目经验者优先\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "南京", "salary": "11857-19268元/月", "salaryMin": 11857, "salaryMax": 19268, "salaryAvg": 15562, "platform": "招聘平台", "industry": "制造业", "companySize": "500-999人", "collectedAt": "2026-02-24", "sourceJobId": "JOB486191", "education": "硕士", "experience": "不限", "views": 239, "applications": 35, "tags": ["NumPy", "微服务", "Django", "大数据", "Pandas", "PyTorch"]}',
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

-- JD 175: DevOps工程师（云从科技·杭州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    204, 1,
    'DevOps工程师',
    '云从科技',
    '岗位职责：编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
岗位要求：不限及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力
有大型项目经验者优先
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Python", "NumPy", "MongoDB", "数据结构"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["不限及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力\\n有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "杭州", "salary": "29815-48450元/月", "salaryMin": 29815, "salaryMax": 48450, "salaryAvg": 39132, "platform": "招聘平台", "industry": "医疗", "companySize": "1000-9999人", "collectedAt": "2026-02-18", "sourceJobId": "JOB827798", "education": "不限", "experience": "10年以上", "views": 1286, "applications": 126, "tags": ["Python", "NumPy", "MongoDB", "数据结构"]}',
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

-- JD 176: 架构师（百度·武汉）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    205, 1,
    '架构师',
    '百度',
    '岗位职责：参与技术方案评审，解决技术难题
持续优化系统性能和用户体验
编写高质量、可维护的代码
岗位要求：具备良好的编程基础和算法能力
3-5年以上相关工作经验
熟悉相关技术栈和开发工具
本科及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["Python", "React", "分布式系统", "Linux", "NumPy", "高并发"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n持续优化系统性能和用户体验\\n编写高质量、可维护的代码"], "experienceRequirements": ["具备良好的编程基础和算法能力\\n3-5年以上相关工作经验\\n熟悉相关技术栈和开发工具\\n本科及以上学历，计算机相关专业优先"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "武汉", "salary": "10012-16270元/月", "salaryMin": 10012, "salaryMax": 16270, "salaryAvg": 13141, "platform": "招聘平台", "industry": "其他", "companySize": "100-499人", "collectedAt": "2026-02-17", "sourceJobId": "JOB700406", "education": "本科", "experience": "3-5年", "views": 2588, "applications": 96, "tags": ["Python", "React", "分布式系统", "Linux", "NumPy", "高并发"]}',
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

-- JD 177: 深度学习工程师（字节跳动·郑州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    206, 1,
    '深度学习工程师',
    '字节跳动',
    '岗位职责：编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
岗位要求：本科及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["大数据", "Docker", "数据结构"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验"], "experienceRequirements": ["本科及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "郑州", "salary": "17399-28274元/月", "salaryMin": 17399, "salaryMax": 28274, "salaryAvg": 22836, "platform": "招聘平台", "industry": "教育", "companySize": "20-99人", "collectedAt": "2026-02-16", "sourceJobId": "JOB849593", "education": "本科", "experience": "10年以上", "views": 383, "applications": 154, "tags": ["大数据", "Docker", "数据结构"]}',
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

-- JD 178: 技术总监（中兴通讯·苏州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    207, 1,
    '技术总监',
    '中兴通讯',
    '岗位职责：持续优化系统性能和用户体验
编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
岗位要求：应届生以上相关工作经验
具备良好的编程基础和算法能力
博士及以上学历，计算机相关专业优先
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["TensorFlow", "Vue", "Pandas"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["应届生以上相关工作经验\\n具备良好的编程基础和算法能力\\n博士及以上学历，计算机相关专业优先\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "苏州", "salary": "12235-19882元/月", "salaryMin": 12235, "salaryMax": 19882, "salaryAvg": 16058, "platform": "招聘平台", "industry": "互联网", "companySize": "20-99人", "collectedAt": "2026-03-01", "sourceJobId": "JOB835281", "education": "博士", "experience": "应届生", "views": 967, "applications": 21, "tags": ["TensorFlow", "Vue", "Pandas"]}',
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

-- JD 179: 深度学习工程师（快手·重庆）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    208, 1,
    '深度学习工程师',
    '快手',
    '岗位职责：负责深度学习工程师相关的系统设计和开发工作
编写高质量、可维护的代码
参与需求分析、系统设计、编码实现和测试
岗位要求：博士及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力
5-10年以上相关工作经验',
    '{"requiredSkills": ["React", "Linux", "Kubernetes"], "preferredSkills": [], "responsibilities": ["负责深度学习工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["博士及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力\\n5-10年以上相关工作经验"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "重庆", "salary": "27602-44853元/月", "salaryMin": 27602, "salaryMax": 44853, "salaryAvg": 36227, "platform": "招聘平台", "industry": "物流", "companySize": "10000人以上", "collectedAt": "2026-02-28", "sourceJobId": "JOB112000", "education": "博士", "experience": "5-10年", "views": 1616, "applications": 22, "tags": ["React", "Linux", "Kubernetes"]}',
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

-- JD 180: 深度学习工程师（字节跳动·广州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    209, 1,
    '深度学习工程师',
    '字节跳动',
    '岗位职责：编写高质量、可维护的代码
参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
岗位要求：熟悉相关技术栈和开发工具
博士及以上学历，计算机相关专业优先
有大型项目经验者优先
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Git", "Jenkins", "Pandas", "Python"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n博士及以上学历，计算机相关专业优先\\n有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "广州", "salary": "11765-19118元/月", "salaryMin": 11765, "salaryMax": 19118, "salaryAvg": 15441, "platform": "招聘平台", "industry": "互联网", "companySize": "500-999人", "collectedAt": "2026-02-19", "sourceJobId": "JOB198195", "education": "博士", "experience": "应届生", "views": 3671, "applications": 38, "tags": ["Git", "Jenkins", "Pandas", "Python"]}',
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

-- JD 181: 深度学习工程师（宁德时代·长沙）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    210, 1,
    '深度学习工程师',
    '宁德时代',
    '岗位职责：参与技术方案评审，解决技术难题
负责深度学习工程师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
岗位要求：5-10年以上相关工作经验
熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力
有大型项目经验者优先',
    '{"requiredSkills": ["Python", "Redis", "Jenkins", "高并发", "Scikit-learn", "分布式系统"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n负责深度学习工程师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["5-10年以上相关工作经验\\n熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力\\n有大型项目经验者优先"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "长沙", "salary": "17354-28201元/月", "salaryMin": 17354, "salaryMax": 28201, "salaryAvg": 22777, "platform": "招聘平台", "industry": "制造业", "companySize": "0-20人", "collectedAt": "2026-02-28", "sourceJobId": "JOB116780", "education": "本科", "experience": "5-10年", "views": 3844, "applications": 18, "tags": ["Python", "Redis", "Jenkins", "高并发", "Scikit-learn", "分布式系统"]}',
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

-- JD 182: 深度学习工程师（哔哩哔哩·成都）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    211, 1,
    '深度学习工程师',
    '哔哩哔哩',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
编写高质量、可维护的代码
参与技术方案评审，解决技术难题
岗位要求：5-10年以上相关工作经验
不限及以上学历，计算机相关专业优先
有大型项目经验者优先
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["PyTorch", "机器学习", "Vue", "Spark", "MySQL"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n编写高质量、可维护的代码\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["5-10年以上相关工作经验\\n不限及以上学历，计算机相关专业优先\\n有大型项目经验者优先\\n具备良好的编程基础和算法能力"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "成都", "salary": "17836-28984元/月", "salaryMin": 17836, "salaryMax": 28984, "salaryAvg": 23410, "platform": "招聘平台", "industry": "零售", "companySize": "0-20人", "collectedAt": "2026-02-13", "sourceJobId": "JOB288358", "education": "不限", "experience": "5-10年", "views": 717, "applications": 124, "tags": ["PyTorch", "机器学习", "Vue", "Spark", "MySQL"]}',
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

-- JD 183: Python开发工程师（大华股份·武汉）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    212, 1,
    'Python开发工程师',
    '大华股份',
    '岗位职责：参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
负责Python开发工程师相关的系统设计和开发工作
岗位要求：具备良好的沟通能力和团队协作精神
不限及以上学历，计算机相关专业优先
应届生以上相关工作经验
有大型项目经验者优先',
    '{"requiredSkills": ["高并发", "TypeScript", "Vue", "微服务", "Jenkins", "Kubernetes"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试\\n负责Python开发工程师相关的系统设计和开发工作"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n不限及以上学历，计算机相关专业优先\\n应届生以上相关工作经验\\n有大型项目经验者优先"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "武汉", "salary": "5248-8529元/月", "salaryMin": 5248, "salaryMax": 8529, "salaryAvg": 6888, "platform": "招聘平台", "industry": "金融", "companySize": "1000-9999人", "collectedAt": "2026-02-27", "sourceJobId": "JOB388998", "education": "不限", "experience": "应届生", "views": 1083, "applications": 121, "tags": ["高并发", "TypeScript", "Vue", "微服务", "Jenkins", "Kubernetes"]}',
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

-- JD 184: 数据工程师（滴滴·苏州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    213, 1,
    '数据工程师',
    '滴滴',
    '岗位职责：编写高质量、可维护的代码
负责数据工程师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
岗位要求：5-10年以上相关工作经验
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具
有大型项目经验者优先',
    '{"requiredSkills": ["Jenkins", "React", "数据结构", "Scikit-learn", "Hadoop"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n负责数据工程师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["5-10年以上相关工作经验\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具\\n有大型项目经验者优先"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "苏州", "salary": "38955-63302元/月", "salaryMin": 38955, "salaryMax": 63302, "salaryAvg": 51128, "platform": "招聘平台", "industry": "房地产", "companySize": "1000-9999人", "collectedAt": "2026-03-03", "sourceJobId": "JOB758450", "education": "博士", "experience": "5-10年", "views": 1438, "applications": 86, "tags": ["Jenkins", "React", "数据结构", "Scikit-learn", "Hadoop"]}',
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

-- JD 185: 测试工程师（平安科技·广州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    214, 1,
    '测试工程师',
    '平安科技',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
负责测试工程师相关的系统设计和开发工作
持续优化系统性能和用户体验
岗位要求：具备良好的编程基础和算法能力
博士及以上学历，计算机相关专业优先
不限以上相关工作经验
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["Python", "Jenkins", "Spring Boot"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n负责测试工程师相关的系统设计和开发工作\\n持续优化系统性能和用户体验"], "experienceRequirements": ["具备良好的编程基础和算法能力\\n博士及以上学历，计算机相关专业优先\\n不限以上相关工作经验\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "广州", "salary": "18009-29265元/月", "salaryMin": 18009, "salaryMax": 29265, "salaryAvg": 23637, "platform": "招聘平台", "industry": "金融", "companySize": "500-999人", "collectedAt": "2026-03-04", "sourceJobId": "JOB336494", "education": "博士", "experience": "不限", "views": 4815, "applications": 200, "tags": ["Python", "Jenkins", "Spring Boot"]}',
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

-- JD 186: 算法工程师（京东·西安）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    215, 1,
    '算法工程师',
    '京东',
    '岗位职责：负责算法工程师相关的系统设计和开发工作
参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
岗位要求：硕士及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具
有大型项目经验者优先',
    '{"requiredSkills": ["JavaScript", "Git", "Django"], "preferredSkills": [], "responsibilities": ["负责算法工程师相关的系统设计和开发工作\\n参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["硕士及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具\\n有大型项目经验者优先"], "educationRequirements": ["硕士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "西安", "salary": "5597-9096元/月", "salaryMin": 5597, "salaryMax": 9096, "salaryAvg": 7346, "platform": "招聘平台", "industry": "医疗", "companySize": "100-499人", "collectedAt": "2026-02-23", "sourceJobId": "JOB470482", "education": "硕士", "experience": "应届生", "views": 4413, "applications": 117, "tags": ["JavaScript", "Git", "Django"]}',
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

-- JD 187: 算法工程师（当当网·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    216, 1,
    '算法工程师',
    '当当网',
    '岗位职责：持续优化系统性能和用户体验
编写高质量、可维护的代码
负责算法工程师相关的系统设计和开发工作
岗位要求：有大型项目经验者优先
熟悉相关技术栈和开发工具
具备良好的编程基础和算法能力
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["TensorFlow", "MongoDB", "Redis", "Git"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n编写高质量、可维护的代码\\n负责算法工程师相关的系统设计和开发工作"], "experienceRequirements": ["有大型项目经验者优先\\n熟悉相关技术栈和开发工具\\n具备良好的编程基础和算法能力\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "20678-33601元/月", "salaryMin": 20678, "salaryMax": 33601, "salaryAvg": 27139, "platform": "招聘平台", "industry": "医疗", "companySize": "0-20人", "collectedAt": "2026-02-11", "sourceJobId": "JOB699871", "education": "本科", "experience": "3-5年", "views": 3093, "applications": 59, "tags": ["TensorFlow", "MongoDB", "Redis", "Git"]}',
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

-- JD 188: 机器学习工程师（美的·广州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    217, 1,
    '机器学习工程师',
    '美的',
    '岗位职责：负责机器学习工程师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
持续优化系统性能和用户体验
岗位要求：有大型项目经验者优先
熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Kubernetes", "深度学习", "Java", "Docker", "Pandas", "Git"], "preferredSkills": [], "responsibilities": ["负责机器学习工程师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试\\n持续优化系统性能和用户体验"], "experienceRequirements": ["有大型项目经验者优先\\n熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "广州", "salary": "13392-21763元/月", "salaryMin": 13392, "salaryMax": 21763, "salaryAvg": 17577, "platform": "招聘平台", "industry": "能源", "companySize": "100-499人", "collectedAt": "2026-03-04", "sourceJobId": "JOB507207", "education": "本科", "experience": "3-5年", "views": 4540, "applications": 72, "tags": ["Kubernetes", "深度学习", "Java", "Docker", "Pandas", "Git"]}',
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

-- JD 189: DevOps工程师（拼多多·天津）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    218, 1,
    'DevOps工程师',
    '拼多多',
    '岗位职责：参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
负责DevOps工程师相关的系统设计和开发工作
岗位要求：有大型项目经验者优先
10年以上以上相关工作经验
具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Linux", "Jenkins", "Django", "Spring Boot", "高并发"], "preferredSkills": [], "responsibilities": ["参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题\\n负责DevOps工程师相关的系统设计和开发工作"], "experienceRequirements": ["有大型项目经验者优先\\n10年以上以上相关工作经验\\n具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "天津", "salary": "16022-26035元/月", "salaryMin": 16022, "salaryMax": 26035, "salaryAvg": 21028, "platform": "招聘平台", "industry": "互联网", "companySize": "20-99人", "collectedAt": "2026-02-07", "sourceJobId": "JOB356230", "education": "大专", "experience": "10年以上", "views": 3416, "applications": 79, "tags": ["Linux", "Jenkins", "Django", "Spring Boot", "高并发"]}',
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

-- JD 190: 前端开发工程师（平安科技·郑州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    219, 1,
    '前端开发工程师',
    '平安科技',
    '岗位职责：持续优化系统性能和用户体验
参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
岗位要求：1-3年以上相关工作经验
熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神
不限及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["Hadoop", "微服务", "大数据", "Git", "Kubernetes", "PyTorch"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["1-3年以上相关工作经验\\n熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神\\n不限及以上学历，计算机相关专业优先"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "郑州", "salary": "7019-11406元/月", "salaryMin": 7019, "salaryMax": 11406, "salaryAvg": 9212, "platform": "招聘平台", "industry": "教育", "companySize": "20-99人", "collectedAt": "2026-02-15", "sourceJobId": "JOB371661", "education": "不限", "experience": "1-3年", "views": 1406, "applications": 62, "tags": ["Hadoop", "微服务", "大数据", "Git", "Kubernetes", "PyTorch"]}',
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

-- JD 191: 算法工程师（商汤科技·上海）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    220, 1,
    '算法工程师',
    '商汤科技',
    '岗位职责：编写高质量、可维护的代码
持续优化系统性能和用户体验
参与需求分析、系统设计、编码实现和测试
岗位要求：博士及以上学历，计算机相关专业优先
具备良好的沟通能力和团队协作精神
熟悉相关技术栈和开发工具
5-10年以上相关工作经验',
    '{"requiredSkills": ["React", "Vue", "大数据"], "preferredSkills": [], "responsibilities": ["编写高质量、可维护的代码\\n持续优化系统性能和用户体验\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["博士及以上学历，计算机相关专业优先\\n具备良好的沟通能力和团队协作精神\\n熟悉相关技术栈和开发工具\\n5-10年以上相关工作经验"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "上海", "salary": "44723-72676元/月", "salaryMin": 44723, "salaryMax": 72676, "salaryAvg": 58699, "platform": "招聘平台", "industry": "零售", "companySize": "100-499人", "collectedAt": "2026-03-06", "sourceJobId": "JOB816485", "education": "博士", "experience": "5-10年", "views": 1277, "applications": 56, "tags": ["React", "Vue", "大数据"]}',
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

-- JD 192: 测试工程师（新浪·青岛）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    221, 1,
    '测试工程师',
    '新浪',
    '岗位职责：参与技术方案评审，解决技术难题
负责测试工程师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
岗位要求：有大型项目经验者优先
熟悉相关技术栈和开发工具
大专及以上学历，计算机相关专业优先
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["Spring Boot", "Kubernetes", "深度学习"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n负责测试工程师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["有大型项目经验者优先\\n熟悉相关技术栈和开发工具\\n大专及以上学历，计算机相关专业优先\\n具备良好的编程基础和算法能力"], "educationRequirements": ["大专及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "青岛", "salary": "6351-10320元/月", "salaryMin": 6351, "salaryMax": 10320, "salaryAvg": 8335, "platform": "招聘平台", "industry": "零售", "companySize": "10000人以上", "collectedAt": "2026-03-05", "sourceJobId": "JOB606878", "education": "大专", "experience": "1-3年", "views": 4874, "applications": 168, "tags": ["Spring Boot", "Kubernetes", "深度学习"]}',
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

-- JD 193: Python开发工程师（平安科技·武汉）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    222, 1,
    'Python开发工程师',
    '平安科技',
    '岗位职责：持续优化系统性能和用户体验
参与技术方案评审，解决技术难题
参与需求分析、系统设计、编码实现和测试
岗位要求：熟悉相关技术栈和开发工具
1-3年以上相关工作经验
具备良好的沟通能力和团队协作精神
具备良好的编程基础和算法能力',
    '{"requiredSkills": ["机器学习", "Pandas", "Spring Boot", "高并发", "TypeScript", "Docker"], "preferredSkills": [], "responsibilities": ["持续优化系统性能和用户体验\\n参与技术方案评审", "解决技术难题\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n1-3年以上相关工作经验\\n具备良好的沟通能力和团队协作精神\\n具备良好的编程基础和算法能力"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "武汉", "salary": "7897-12834元/月", "salaryMin": 7897, "salaryMax": 12834, "salaryAvg": 10365, "platform": "招聘平台", "industry": "医疗", "companySize": "500-999人", "collectedAt": "2026-02-20", "sourceJobId": "JOB953963", "education": "本科", "experience": "1-3年", "views": 2013, "applications": 31, "tags": ["机器学习", "Pandas", "Spring Boot", "高并发", "TypeScript", "Docker"]}',
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

-- JD 194: DevOps工程师（商汤科技·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    223, 1,
    'DevOps工程师',
    '商汤科技',
    '岗位职责：参与技术方案评审，解决技术难题
持续优化系统性能和用户体验
参与需求分析、系统设计、编码实现和测试
岗位要求：熟悉相关技术栈和开发工具
博士及以上学历，计算机相关专业优先
有大型项目经验者优先
不限以上相关工作经验',
    '{"requiredSkills": ["NumPy", "分布式系统", "Vue", "TypeScript", "深度学习", "Spring Boot"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n持续优化系统性能和用户体验\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["熟悉相关技术栈和开发工具\\n博士及以上学历，计算机相关专业优先\\n有大型项目经验者优先\\n不限以上相关工作经验"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "16653-27061元/月", "salaryMin": 16653, "salaryMax": 27061, "salaryAvg": 21857, "platform": "招聘平台", "industry": "互联网", "companySize": "10000人以上", "collectedAt": "2026-02-21", "sourceJobId": "JOB908085", "education": "博士", "experience": "不限", "views": 3667, "applications": 24, "tags": ["NumPy", "分布式系统", "Vue", "TypeScript", "深度学习", "Spring Boot"]}',
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

-- JD 195: 全栈开发工程师（平安科技·广州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    224, 1,
    '全栈开发工程师',
    '平安科技',
    '岗位职责：负责全栈开发工程师相关的系统设计和开发工作
持续优化系统性能和用户体验
参与需求分析、系统设计、编码实现和测试
岗位要求：具备良好的沟通能力和团队协作精神
不限及以上学历，计算机相关专业优先
熟悉相关技术栈和开发工具
有大型项目经验者优先',
    '{"requiredSkills": ["Django", "Linux", "Jenkins", "深度学习", "Vue"], "preferredSkills": [], "responsibilities": ["负责全栈开发工程师相关的系统设计和开发工作\\n持续优化系统性能和用户体验\\n参与需求分析、系统设计、编码实现和测试"], "experienceRequirements": ["具备良好的沟通能力和团队协作精神\\n不限及以上学历，计算机相关专业优先\\n熟悉相关技术栈和开发工具\\n有大型项目经验者优先"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "广州", "salary": "6553-10648元/月", "salaryMin": 6553, "salaryMax": 10648, "salaryAvg": 8600, "platform": "招聘平台", "industry": "金融", "companySize": "0-20人", "collectedAt": "2026-03-05", "sourceJobId": "JOB355022", "education": "不限", "experience": "应届生", "views": 3420, "applications": 166, "tags": ["Django", "Linux", "Jenkins", "深度学习", "Vue"]}',
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

-- JD 196: Java开发工程师（拼多多·深圳）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    225, 1,
    'Java开发工程师',
    '拼多多',
    '岗位职责：负责Java开发工程师相关的系统设计和开发工作
编写高质量、可维护的代码
持续优化系统性能和用户体验
岗位要求：应届生以上相关工作经验
具备良好的编程基础和算法能力
本科及以上学历，计算机相关专业优先
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["Scikit-learn", "NumPy", "Spark", "MongoDB", "Hadoop"], "preferredSkills": [], "responsibilities": ["负责Java开发工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码\\n持续优化系统性能和用户体验"], "experienceRequirements": ["应届生以上相关工作经验\\n具备良好的编程基础和算法能力\\n本科及以上学历，计算机相关专业优先\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "深圳", "salary": "6993-11364元/月", "salaryMin": 6993, "salaryMax": 11364, "salaryAvg": 9178, "platform": "招聘平台", "industry": "房地产", "companySize": "1000-9999人", "collectedAt": "2026-02-25", "sourceJobId": "JOB523880", "education": "本科", "experience": "应届生", "views": 1356, "applications": 11, "tags": ["Scikit-learn", "NumPy", "Spark", "MongoDB", "Hadoop"]}',
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

-- JD 197: 算法工程师（拼多多·广州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    226, 1,
    '算法工程师',
    '拼多多',
    '岗位职责：负责算法工程师相关的系统设计和开发工作
持续优化系统性能和用户体验
编写高质量、可维护的代码
岗位要求：有大型项目经验者优先
具备良好的沟通能力和团队协作精神
不限及以上学历，计算机相关专业优先
熟悉相关技术栈和开发工具',
    '{"requiredSkills": ["Docker", "Kubernetes", "React"], "preferredSkills": [], "responsibilities": ["负责算法工程师相关的系统设计和开发工作\\n持续优化系统性能和用户体验\\n编写高质量、可维护的代码"], "experienceRequirements": ["有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神\\n不限及以上学历，计算机相关专业优先\\n熟悉相关技术栈和开发工具"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "广州", "salary": "11035-17932元/月", "salaryMin": 11035, "salaryMax": 17932, "salaryAvg": 14483, "platform": "招聘平台", "industry": "教育", "companySize": "10000人以上", "collectedAt": "2026-03-03", "sourceJobId": "JOB139172", "education": "不限", "experience": "1-3年", "views": 1801, "applications": 70, "tags": ["Docker", "Kubernetes", "React"]}',
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

-- JD 198: 全栈开发工程师（旷视科技·南京）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    227, 1,
    '全栈开发工程师',
    '旷视科技',
    '岗位职责：参与技术方案评审，解决技术难题
编写高质量、可维护的代码
负责全栈开发工程师相关的系统设计和开发工作
岗位要求：具备良好的编程基础和算法能力
熟悉相关技术栈和开发工具
有大型项目经验者优先
博士及以上学历，计算机相关专业优先',
    '{"requiredSkills": ["PyTorch", "Hadoop", "Linux"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n编写高质量、可维护的代码\\n负责全栈开发工程师相关的系统设计和开发工作"], "experienceRequirements": ["具备良好的编程基础和算法能力\\n熟悉相关技术栈和开发工具\\n有大型项目经验者优先\\n博士及以上学历，计算机相关专业优先"], "educationRequirements": ["博士及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "南京", "salary": "18152-29497元/月", "salaryMin": 18152, "salaryMax": 29497, "salaryAvg": 23824, "platform": "招聘平台", "industry": "能源", "companySize": "20-99人", "collectedAt": "2026-02-27", "sourceJobId": "JOB988209", "education": "博士", "experience": "1-3年", "views": 3412, "applications": 153, "tags": ["PyTorch", "Hadoop", "Linux"]}',
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

-- JD 199: 后端开发工程师（哔哩哔哩·杭州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    228, 1,
    '后端开发工程师',
    '哔哩哔哩',
    '岗位职责：负责后端开发工程师相关的系统设计和开发工作
参与需求分析、系统设计、编码实现和测试
参与技术方案评审，解决技术难题
岗位要求：应届生以上相关工作经验
本科及以上学历，计算机相关专业优先
熟悉相关技术栈和开发工具
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["Redis", "深度学习", "MySQL", "大数据", "React"], "preferredSkills": [], "responsibilities": ["负责后端开发工程师相关的系统设计和开发工作\\n参与需求分析、系统设计、编码实现和测试\\n参与技术方案评审", "解决技术难题"], "experienceRequirements": ["应届生以上相关工作经验\\n本科及以上学历，计算机相关专业优先\\n熟悉相关技术栈和开发工具\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["本科及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "杭州", "salary": "6760-10985元/月", "salaryMin": 6760, "salaryMax": 10985, "salaryAvg": 8872, "platform": "招聘平台", "industry": "能源", "companySize": "100-499人", "collectedAt": "2026-02-12", "sourceJobId": "JOB731475", "education": "本科", "experience": "应届生", "views": 4016, "applications": 149, "tags": ["Redis", "深度学习", "MySQL", "大数据", "React"]}',
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

-- JD 200: 数据工程师（携程·郑州）
INSERT INTO job_descriptions (
    id, user_id, job_title, company_name, raw_text, parsed_json, parse_status,
    prompt_version, parse_ai_invocation_id, source_meta_json, job_type
) VALUES (
    229, 1,
    '数据工程师',
    '携程',
    '岗位职责：参与技术方案评审，解决技术难题
负责数据工程师相关的系统设计和开发工作
编写高质量、可维护的代码
岗位要求：不限及以上学历，计算机相关专业优先
熟悉相关技术栈和开发工具
有大型项目经验者优先
具备良好的沟通能力和团队协作精神',
    '{"requiredSkills": ["JavaScript", "MongoDB", "微服务"], "preferredSkills": [], "responsibilities": ["参与技术方案评审", "解决技术难题\\n负责数据工程师相关的系统设计和开发工作\\n编写高质量、可维护的代码"], "experienceRequirements": ["不限及以上学历，计算机相关专业优先\\n熟悉相关技术栈和开发工具\\n有大型项目经验者优先\\n具备良好的沟通能力和团队协作精神"], "educationRequirements": ["不限及以上学历"]}',
    'succeeded',
    'csv-import-v1',
    NULL,
    '{"base": "郑州", "salary": "4305-6996元/月", "salaryMin": 4305, "salaryMax": 6996, "salaryAvg": 5650, "platform": "招聘平台", "industry": "房地产", "companySize": "1000-9999人", "collectedAt": "2026-02-06", "sourceJobId": "JOB803908", "education": "不限", "experience": "应届生", "views": 1207, "applications": 185, "tags": ["JavaScript", "MongoDB", "微服务"]}',
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
