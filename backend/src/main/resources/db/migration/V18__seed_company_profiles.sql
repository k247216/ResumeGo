-- V18: 录入首批10家公司偏好Profile完整资料，覆盖原有演示数据。
-- 说明：source_type 改为 JSON 数组以支持多来源标注；新增 confidence_level 和 last_verified_at 字段。

-- 1. 清空原有演示数据
DELETE FROM company_profiles;

-- 2. 删除旧约束并调整列类型
ALTER TABLE company_profiles DROP CONSTRAINT chk_company_profiles_source_type;
ALTER TABLE company_profiles MODIFY COLUMN source_type JSON NOT NULL COMMENT '资料来源类型数组，如["official","public_interview_experience"]';
ALTER TABLE company_profiles ADD COLUMN confidence_level VARCHAR(20) NULL COMMENT '资料置信度：high/medium_high/medium/low' AFTER resume_advice_rules;
ALTER TABLE company_profiles ADD COLUMN last_verified_at DATE NULL COMMENT '资料最后验证日期' AFTER confidence_level;
ALTER TABLE company_profiles ADD CONSTRAINT chk_company_profiles_source_type_array CHECK (JSON_TYPE(source_type) = 'ARRAY');

-- 3. 插入10家公司完整Profile

-- 字节跳动
INSERT INTO company_profiles (company_name, normalized_name, source_type, source_note, preference_tags, writing_style, interview_focus, resume_advice_rules, confidence_level, last_verified_at) VALUES (
    '字节跳动', 'bytedance',
    JSON_ARRAY('official', 'interviewer_public_post', 'public_interview_experience'),
    '字节招聘官网公开展示产品与技术岗位及公司文化；公开的字节技术面试官分享提到，会重点考察候选人对项目的思考、项目中遇到的问题、如何改进以及需求变化后的实现方式。因此，本条将"项目深挖、问题解决、迭代改进"作为主要上下文。"业务结果、数据指标"属于对结果表达方式的保守归纳，不代表字节官方统一简历评分标准。',
    JSON_ARRAY('项目深挖', '问题解决', '业务结果', '数据指标', '快速迭代', '技术取舍'),
    '优先使用"问题—行动—结果"的结构描述经历。减少单纯罗列功能，突出候选人在项目中发现了什么问题、负责了什么关键部分、采用了什么方案、方案产生了什么可验证结果。存在数据时优先呈现用户量、性能、效率、转化率、稳定性或成本变化。',
    JSON_ARRAY('项目中遇到的具体问题', '候选人的实际职责', '技术方案和选择原因', '需求变化后的处理方式', '失败、改进与复盘', '算法和基础能力'),
    JSON_ARRAY('项目描述不能只写完成了哪些功能', '明确区分个人贡献与团队成果', '补充关键技术方案的选择原因', '有可靠数据时补充性能或业务结果', '补充项目中出现的问题及改进过程', '不要虚构用户量、并发量或增长指标'),
    'high', '2026-07-16'
);

-- 腾讯
INSERT INTO company_profiles (company_name, normalized_name, source_type, source_note, preference_tags, writing_style, interview_focus, resume_advice_rules, confidence_level, last_verified_at) VALUES (
    '腾讯', 'tencent',
    JSON_ARRAY('official', 'public_interview_experience'),
    '腾讯官方使命是"用户为本，科技向善"，并强调通过技术丰富用户生活、帮助企业数字化升级。公开面经中常见围绕简历、实习和项目进行追问，同时穿插基础题与编码题。因此，本条重点放在用户价值、技术落地、项目真实性和基础能力上，但不代表腾讯各事业群采用完全一致的标准。',
    JSON_ARRAY('用户价值', '产品体验', '技术落地', '项目真实性', '基础能力', '协作意识'),
    '围绕用户场景和实际价值表达技术经历。除技术实现外，说明项目服务了什么用户、解决了什么真实问题、候选人在其中承担了什么职责。避免只堆技术名词，应体现技术方案和产品、业务或用户体验之间的联系。',
    JSON_ARRAY('简历和项目经历深挖', '候选人的真实贡献', '技术基础与编码能力', '最困难的技术问题', '方案如何服务用户需求', '团队协作和沟通'),
    JSON_ARRAY('项目开头说明目标用户或使用场景', '技术方案后说明其带来的实际价值', '突出本人负责的模块和关键决策', '对核心技术点补充实现细节', '不要用团队整体成果代替个人贡献', '不要为了贴合公司文化生硬加入口号'),
    'medium_high', '2026-07-16'
);

-- 阿里巴巴
INSERT INTO company_profiles (company_name, normalized_name, source_type, source_note, preference_tags, writing_style, interview_focus, resume_advice_rules, confidence_level, last_verified_at) VALUES (
    '阿里巴巴', 'alibaba',
    JSON_ARRAY('official', 'public_recruitment_analysis'),
    '阿里招聘官网强调伙伴关系、共同价值目标、开放协作和敢于挑战。公开的人才招聘资料长期提到通过行为和经历细节考察候选人，而非只听抽象表态。因此，本条建议通过具体行为证明业务理解、协作和担当，不直接把"阿里味"作为确定事实或评分标签。',
    JSON_ARRAY('业务理解', '客户价值', '复杂问题', '主人翁意识', '协作', '复盘成长'),
    '强调项目所处的业务背景、服务对象、候选人的判断和最终结果。推荐采用 STAR 或"背景—挑战—行动—结果—复盘"结构。除技术实现外，说明为什么做、方案如何支撑业务、候选人如何推进问题解决。',
    JSON_ARRAY('项目和业务背景', '候选人的具体行为', '复杂问题处理过程', '方案取舍与推动能力', '协作中的角色', '价值观与经历的一致性'),
    JSON_ARRAY('每段核心经历补充业务背景', '明确说明本人推动了什么事情', '说明方案为什么适合当时的业务约束', '有事实依据时体现客户或业务价值', '补充困难、冲突或失败后的复盘', '禁止直接声称符合阿里价值观而不给事实'),
    'medium_high', '2026-07-16'
);

-- 拼多多
INSERT INTO company_profiles (company_name, normalized_name, source_type, source_note, preference_tags, writing_style, interview_focus, resume_advice_rules, confidence_level, last_verified_at) VALUES (
    '拼多多', 'pinduoduo',
    JSON_ARRAY('official_job_posting', 'public_interviewer_post', 'public_recruitment_material'),
    '拼多多公开招聘材料明确提到扎实的数据结构和算法能力、数据库技术及编程语言能力；公开招聘信息还描述了用户体量大、业务场景复杂、需求迭代快。另有公开自称拼多多服务端工程师及面试官的技术面试准备文章。由于面试官身份无法像官方文件一样完全核验，技术关注点应作为参考趋势，而不是确定标准。',
    JSON_ARRAY('算法基础', '数据结构', '高强度业务场景', '快速迭代', '问题解决', '结果效率'),
    '表达应直接、紧凑，优先突出问题规模、核心技术难点、解决方案和结果。技术项目应写清数据结构、算法、数据库、性能或稳定性相关内容。避免长篇铺垫和空泛描述。',
    JSON_ARRAY('数据结构与算法', '编程和基础知识', '项目技术细节', '方案复杂度', '高并发或大规模场景', '候选人的独立解决能力'),
    JSON_ARRAY('技术项目应写清核心难点而非只写框架名称', '能够量化时补充延迟、吞吐量或效率结果', '说明算法或数据结构为什么适合该场景', '补充数据库和系统设计相关细节', '项目成果应简洁直接', '没有真实压测时不得包装为高并发项目'),
    'medium_high', '2026-07-16'
);

-- 哔哩哔哩
INSERT INTO company_profiles (company_name, normalized_name, source_type, source_note, preference_tags, writing_style, interview_focus, resume_advice_rules, confidence_level, last_verified_at) VALUES (
    '哔哩哔哩', 'bilibili',
    JSON_ARRAY('official_job_listing', 'public_interview_experience'),
    'B站官方招聘页面公开包含后端、大数据、平台工程及AI等岗位；公开的音视频技术岗面经显示，面试会追问设计、技术和实现细节，以及面对新项目、新技术时的学习方式。本条关于"内容社区理解"的建议结合了其公开业务和岗位性质，属于场景化建议，不是B站官方面试规则。',
    JSON_ARRAY('内容社区理解', '用户体验', '技术细节', '真实业务场景', '学习能力', '兴趣与岗位结合'),
    '除描述技术实现外，应说明项目面向的用户群体、内容场景或社区场景。涉及音视频、推荐、搜索、内容理解、创作工具等项目时，重点说明体验问题、技术难点和落地效果。',
    JSON_ARRAY('项目设计和实现细节', '新技术学习方式', '业务或内容场景理解', '岗位相关技术能力', '用户体验问题', '项目与个人兴趣的联系'),
    JSON_ARRAY('内容类项目需要写清目标用户和使用场景', '音视频项目补充延迟、清晰度、兼容性或稳定性', '推荐搜索项目补充效果指标和实验方法', '说明学习新技术并落地的过程', '避免只写热爱二次元或经常使用B站', '没有实际数据时不得虚构播放量或用户增长'),
    'medium', '2026-07-16'
);

-- 美团
INSERT INTO company_profiles (company_name, normalized_name, source_type, source_note, preference_tags, writing_style, interview_focus, resume_advice_rules, confidence_level, last_verified_at) VALUES (
    '美团', 'meituan',
    JSON_ARRAY('official', 'official_recruitment_material', 'public_technical_report'),
    '美团公开资料将公司定位为科技零售公司，并强调业务与消费者日常生活场景密切相关。美团公开技术报告使用用户满意度和系统效果指标验证技术落地。因此，本条将用户需求、业务落地、指标验证和持续优化作为表达重点，但不是美团招聘团队公布的统一简历规则。',
    JSON_ARRAY('业务落地', '用户需求', '本地生活场景', '数据效果', '工程稳定性', '持续优化'),
    '推荐把技术项目写成完整的业务解决方案：什么用户在什么场景下遇到什么问题，候选人采用什么技术方案，最终改善了哪些体验、效率、成本或稳定性指标。',
    JSON_ARRAY('项目和业务场景', '技术方案的落地效果', '用户需求理解', '工程问题和稳定性', '数据指标与评估方法', '持续优化过程'),
    JSON_ARRAY('项目描述需要同时包含业务问题和技术问题', '说明技术改动对应的用户体验或业务效果', '服务端项目补充稳定性、延迟或容量指标', '算法项目补充评估指标和线上验证方式', '说明项目上线后的继续优化过程', '不得用无法验证的数据包装业务价值'),
    'high', '2026-07-16'
);

-- 京东
INSERT INTO company_profiles (company_name, normalized_name, source_type, source_note, preference_tags, writing_style, interview_focus, resume_advice_rules, confidence_level, last_verified_at) VALUES (
    '京东', 'jd',
    JSON_ARRAY('official', 'official_recruitment_material'),
    '京东招聘官网将京东定位为以供应链为基础的技术与服务企业，并公开列出客户为先、创新、拼搏、担当、感恩、诚信等价值观。公开的 JD YOUNG 招聘材料也提到价值观匹配和对核心业务的理解。因此，本条重点体现客户、供应链、技术落地和责任，但不用于判断候选人的真实价值观。',
    JSON_ARRAY('客户价值', '供应链场景', '可靠性', '效率提升', '担当', '技术与业务结合'),
    '突出项目如何提升客户体验、供应链效率、履约效率、系统可靠性或运营效率。表达应体现候选人承担的责任、推进过程和最终结果，而不只是描述使用了哪些技术。',
    JSON_ARRAY('客户和业务价值', '供应链或零售场景理解', '候选人的责任范围', '系统可靠性与效率', '技术方案落地', '价值观与实际行为'),
    JSON_ARRAY('项目中明确服务对象和业务链路', '体现本人承担的责任和推进动作', '有依据时补充效率、成本或稳定性结果', '供应链项目应写清上下游关系', '说明技术如何支持实际业务', '不要直接写有担当，应通过事实体现'),
    'high', '2026-07-16'
);

-- 百度
INSERT INTO company_profiles (company_name, normalized_name, source_type, source_note, preference_tags, writing_style, interview_focus, resume_advice_rules, confidence_level, last_verified_at) VALUES (
    '百度', 'baidu',
    JSON_ARRAY('public_interviewer_post', 'public_case_study', 'public_technical_paper'),
    '公开转载的百度技术面试官文章提到，会评价候选人的编程能力、逻辑和思考能力，并根据候选人自身经历提问，而非完全依赖固定题库。百度人才测评案例还提到文化认同、学习敏锐和工作胜任三个维度。百度公开技术论文常使用线上指标验证方案效果。本条据此强调基础、逻辑、学习能力和实验验证，但不代表百度当前所有团队的统一标准。',
    JSON_ARRAY('技术基础', '逻辑思考', '编程能力', '学习敏锐度', '项目真实性', '数据与实验'),
    '技术项目应突出问题定义、技术路线、实验方法和结果。算法或AI项目需要说明数据来源、模型选择、评估指标、对比实验及实际贡献。工程项目需要写清设计思路和技术难点。',
    JSON_ARRAY('编程能力', '逻辑和思考能力', '项目与候选人特点', '学习能力', '技术基础', '实验和评估方法'),
    JSON_ARRAY('AI项目补充数据、模型、指标和实验结果', '明确说明候选人在项目中的技术贡献', '解释技术路线和备选方案', '避免只罗列模型或框架名称', '工程项目补充设计思路和性能验证', '实验结果必须来源真实可复现'),
    'medium_high', '2026-07-16'
);

-- 快手
INSERT INTO company_profiles (company_name, normalized_name, source_type, source_note, preference_tags, writing_style, interview_focus, resume_advice_rules, confidence_level, last_verified_at) VALUES (
    '快手', 'kuaishou',
    JSON_ARRAY('official', 'official_recruitment_guide', 'public_recruitment_material'),
    '快手招聘官网强调吸引有才华、有创造力的人才。快手公开发布的技术岗投递指南列出了招聘流程，并提供公开讲座、论文和视频等学习材料；公开校招资料显示其持续招聘算法和工程类岗位。本条结合快手公开业务属性，提炼内容场景、工程能力和创造性解决问题等建议，不视为内部评价标准。',
    JSON_ARRAY('用户与内容场景', '创造力', '工程能力', '推荐与多媒体', '技术深度', '业务落地'),
    '围绕真实用户和内容消费、内容创作或互动场景描述项目。推荐、搜索、直播、音视频和大数据项目应说明规模、实时性、效果指标和工程挑战。',
    JSON_ARRAY('技术项目深度', '基础和工程能力', '内容平台业务理解', '推荐搜索或音视频能力', '方案落地和效果', '创造性解决问题'),
    JSON_ARRAY('说明项目对应的用户或内容场景', '推荐项目补充离线指标和线上验证', '音视频项目补充实时性、稳定性或质量指标', '后端项目说明规模和工程约束', '突出解决问题的方法而非堆砌组件', '不要虚构DAU、播放量或线上提升比例'),
    'medium_high', '2026-07-16'
);

-- 华为
INSERT INTO company_profiles (company_name, normalized_name, source_type, source_note, preference_tags, writing_style, interview_focus, resume_advice_rules, confidence_level, last_verified_at) VALUES (
    '华为', 'huawei',
    JSON_ARRAY('official', 'public_interview_experience'),
    '华为官方资料明确强调以客户为中心、为客户创造价值，并强调技术创新、质量、安全可信和长期研发投入。公开面经中常见项目深挖、技术选型、难点解决、基础知识及系统设计等内容。因此，本条把客户价值、技术深度和工程质量作为重点，但不代表华为所有业务部门使用完全相同的招聘规则。',
    JSON_ARRAY('客户价值', '技术深度', '质量可靠', '长期投入', '工程落地', '责任与协作'),
    '强调项目如何解决客户或实际场景问题，技术方案如何保障质量、安全、可靠性和可维护性。需要写清技术选型依据、关键难点、个人职责和验证结果。',
    JSON_ARRAY('项目细节和个人贡献', '技术选型原因', '难点与解决方案', '基础知识和编码', '系统设计思路', '客户场景与工程质量'),
    JSON_ARRAY('项目开头说明客户需求或实际使用场景', '明确技术方案的质量和可靠性考虑', '解释技术选型及其约束条件', '补充测试、验证和故障处理过程', '说明本人负责的关键模块', '不得把普通课程项目包装成商用系统'),
    'high', '2026-07-16'
);
