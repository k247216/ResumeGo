# 岗位匹配测试用例

状态：Draft  
适用范围：Sprint 1  
主责：成员 2  
测试主责：成员 3  
算法版本：`v1.1.0`  
是否涉及 AI 禁飞区：是

## 1. 测试目标

验证岗位匹配算法是否满足：

- 相同输入结果稳定（幂等）；
- `matchScore` 范围为 0—100，整数；
- 各维度覆盖率可解释；
- `matchedItems` / `missingItems` / `unknownItems` 能定位问题；
- 必备技能覆盖率 < 60% 触发一票否决（`matchScore ≤ 40`）；
- JD 无某维度要求时，该维度覆盖率默认 100 且权重重分配；
- 空缺、空值、未解析和异常输入可处理；
- 技能别名匹配（精确 + 别名模糊匹配）；
- 不依赖 AI 主观评分。

## 2. 测试前提

匹配接口：

```text
POST /api/resume-versions/{versionId}/job-matches
```

请求体：

```json
{
  "jobDescriptionId": 1
}
```

匹配结果保存到：

```text
job_matches
```

核心断言：

```text
matchScore 为整数
0 <= matchScore <= 100
details.requiredCoverage ∈ [0, 100]
details.preferredCoverage ∈ [0, 100]
details.experienceCoverage ∈ [0, 100]
details.educationMatch 为 boolean
details.responsibilityCoverage ∈ [0, 100]
details.matchedItems 为字符串数组
details.missingItems 为字符串数组
details.unknownItems 为字符串数组
details.dimensionScores 中各维度得分 = 覆盖率 × 该维度最终权重
必备技能覆盖率 < 60 时，matchScore ≤ 40（一票否决）
JD 无某维度要求时，该维度覆盖率 = 100（默认满分）
algorithmVersion == "v1.1.0"
inputFingerprint 非空且为 64 字符 SHA-256
相同输入重复请求返回完全一致的结果（幂等）
```

## 3. 测试用例表

| 编号 | 用例名称 | 输入特征 | 预期维度影响 | 预期关键断言 | 边界 |
|---|---|---|---|---|---|
| MATCH-001 | 高匹配-全栈工程师 | 简历有 Go/TypeScript/React/MySQL/Redis/Docker/Linux/Git，3年后端+1年前端经历，本科学历；JD 必备 Go/MySQL/Linux，加分 Docker/K8s/React，经验要求 3年后端+微服务，学历要求本科及以上 | `requiredCoverage` 高；`preferredCoverage` 中等（缺 K8s）；`experienceCoverage` 高；`educationMatch = true` | `requiredCoverage = 100`；`matchScore >= 80`；`missingItems` 包含 `Kubernetes`；`matchedItems` 包含 `Go, MySQL, Linux, Docker, React` | 否 |
| MATCH-002 | 必备技能覆盖率刚好60% | 简历有 Java/Spring Boot/MySQL（3个技能）；JD 必备 Java/Spring Boot/MySQL/Redis/Docker（5个技能） | `requiredCoverage` 为 60%；`preferredCoverage` 受影响；`experienceCoverage` 受影响 | `requiredCoverage = 60`；不触发一票否决（≥60% 不封顶）；`missingItems` 包含 `Redis, Docker` | 是 |
| MATCH-003 | 必备技能低于60%触发一票否决 | 简历有 Python/TensorFlow/PyTorch（AI方向）；JD 必备 Go/gRPC/Kubernetes/Docker/Linux（后端基础设施方向） | `requiredCoverage` 极低；触发一票否决；`preferredCoverage` 为 0；`experienceCoverage` 极低 | `requiredCoverage < 60`；`matchScore ≤ 40`（一票否决封顶）；`matchLevel = "低匹配"` 或 `"不匹配"`；`missingItems` 包含大部分 JD 必备技能 | 是 |
| MATCH-004 | JD无加分项和学历要求 | 简历有 JavaScript/Vue/CSS/HTML/Git，1年前端经历，大专学历；JD 必备 JavaScript/Vue/CSS/HTML，加分项为空，经验要求 1年前端，学历要求为空 | `preferredCoverage` 默认 100；`educationMatch = true`（无要求即满足）；权重从加分/学历重新分配到其他维度 | `preferredCoverage = 100`；`educationMatch = true`；`matchScore` 接近 100；`dynamicWeightMap` 中加分和学历权重为 0 | 是 |
| MATCH-005 | 技能别名匹配 | 简历技能写 K8s/Node.js/React.js/PostgreSQL；JD 必备 Kubernetes/Node/React/Postgres | `requiredCoverage` 全部通过别名命中；`aliasMatches` 记录别名映射 | `requiredCoverage = 100`；`aliasMatches` 包含 `{"K8s": "Kubernetes", "Node.js": "Node", "React.js": "React", "PostgreSQL": "Postgres"}`；`missingItems` 为空 | 是 |
| MATCH-006 | JD所有维度无要求 | 简历有任意内容；JD `parsed_json` 中 `requiredSkills/preferredSkills/experienceRequirements/educationRequirements/responsibilities` 全为空数组 | 所有维度默认 100；权重有效和为 0 | `matchScore = 100`；`requiredCoverage = 100`；`preferredCoverage = 100`；`experienceCoverage = 100`；`educationMatch = true`；`responsibilityCoverage = 100` | 是 |
| MATCH-007 | 简历技能为空 | 简历 `skills` 为空数组；JD 有必备技能要求 | `requiredCoverage` 为 0；所有 JD 必备技能归入 `missingItems` | `requiredCoverage = 0`；`matchScore ≤ 40`（一票否决）；`missingItems` 包含 JD 全部必备技能 | 是 |
| MATCH-008 | 简历项目经历为空 | 简历 `projects` 为空数组；JD 有经验要求 | `experienceCoverage` 为 0；经验要求全部归入 `missingItems` | `experienceCoverage = 0`；`missingItems` 包含 JD 经验要求关键词 | 是 |
| MATCH-009 | 简历学历为空 | 简历 `education` 为空数组；JD 有学历要求 | `educationMatch = false` | `educationMatch = false`；`missingItems` 包含 JD 学历要求 | 是 |
| MATCH-010 | 经验年限归入unknown | 简历项目经历未体现具体年限；JD 经验要求含"3年以上"关键词 | 含数字或"年"字的关键词归入 `unknownItems` | `unknownItems` 包含年限相关关键词（如"3年以上"）；年限关键词不出现在 `missingItems` 中 | 是 |
| MATCH-011 | JD未解析 | JD 的 `parse_status != "succeeded"` | 不执行匹配 | 返回 HTTP 422；error 为 `JD_NOT_PARSED`；`job_matches` 不新增记录 | 是 |
| MATCH-012 | 简历版本不存在 | 请求不存在的 `versionId` | 不执行匹配 | 返回 HTTP 404；error 为 `RESUME_VERSION_NOT_FOUND` | 是 |
| MATCH-013 | JD不存在 | 请求不存在的 `jobDescriptionId` | 不执行匹配 | 返回 HTTP 404；error 为 `JOB_DESCRIPTION_NOT_FOUND` | 是 |
| MATCH-014 | 相同输入重复请求-幂等 | 同一 `resume_version_id` + 同一 `job_description_id` 连续 POST 两次 | 第二次直接返回已有结果 | 两次返回的 `matchScore`、各维度覆盖率、`matchedItems`、`missingItems` 完全一致；`inputFingerprint` 一致；`job_matches` 表记录数不增加 | 是 |
| MATCH-015 | 简历学历高于JD要求 | 简历 `education[0].degree = "硕士"`；JD 学历要求 `"本科及以上"` | `educationMatch = true` | `educationMatch = true`；学历维度 `coverage = 100` | 否 |
| MATCH-016 | 简历学历低于JD要求 | 简历 `education[0].degree = "本科"`；JD 学历要求 `"硕士及以上"` | `educationMatch = false` | `educationMatch = false`；学历维度 `coverage = 0` | 否 |
| MATCH-017 | 重复技能去重 | JD `requiredSkills` 包含重复项 `["Go", "Go"]` | 去重后正确计数 | `requiredCoverage` 计算时分母按去重后数量计算；JD 中 `Go` 出现多次不影响覆盖率 | 是 |
| MATCH-018 | 请求体缺失jobDescriptionId | 请求体为 `{}` 或不含 `jobDescriptionId` 字段 | 不执行匹配 | 返回 HTTP 400 `VALIDATION_ERROR` | 是 |
| MATCH-019 | n-gram经验匹配正向命中 | 简历项目描述"4 年全栈开发经验"；JD 经验要求"3 年以上全栈开发" | bigram 重叠 {全栈,栈开,开发} = 3/7 = 42.9% ≥ 40% | `experienceCoverage = 100`；`matchedItems` 包含 "3 年以上全栈开发" | 否 |

## 4. 样例数据建议

建议准备以下脱敏样例，使用种子数据或自行构建：

| 文件 | 覆盖用例 | 用途 |
|---|---|---|
| `fullstack-resume.json` + `fullstack-jd.json` | MATCH-001 | 高匹配基准 |
| `java-junior-resume.json` + `java-senior-jd.json` | MATCH-002 | 必备技能 60% 边界 |
| `ai-resume.json` + `infra-jd.json` | MATCH-003 | 跨领域一票否决 |
| `frontend-resume.json` + `frontend-jd-no-bonus.json` | MATCH-004 | JD 无加分/学历维度 |
| `alias-resume.json` + `alias-jd.json` | MATCH-005 | 技能别名全覆盖 |
| `empty-jd.json` | MATCH-006 | JD 全空维度 |
| `empty-skills-resume.json` + `fullstack-jd.json` | MATCH-007 | 简历技能空边界 |
| `empty-projects-resume.json` + `fullstack-jd.json` | MATCH-008 | 简历经历空边界 |
| `empty-edu-resume.json` + `fullstack-jd.json` | MATCH-009 | 简历学历空边界 |
| `unparsed-jd.json` | MATCH-011 | JD 未解析异常 |

## 5. 人工测试记录模板

```text
用例编号：
执行人：
执行时间：
输入版本：
接口：
预期结果：
实际结果：
是否通过：
备注：
```

## 6. 禁飞区检查清单

- [ ] 测试用例由成员 2 人工确认；
- [ ] AI 未生成匹配算法核心逻辑；
- [ ] AI 未决定维度权重数值；
- [ ] AI 未决定一票否决阈值；
- [ ] AI 未生成技能别名映射表；
- [ ] 所有覆盖率公式由成员 2 手写实现；
- [ ] 学历等级映射表写死在代码中，不由 AI 推断；
- [ ] 必备技能 < 60% 一票否决规则由成员 2 人工确定；
- [ ] 主责成员能逐行解释匹配算法实现；
- [ ] QA 能根据本表独立验证每条用例。
