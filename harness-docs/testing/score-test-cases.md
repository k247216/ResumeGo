# 简历评分测试用例

状态：Draft  
适用范围：Sprint 1  
主责：成员 1  
测试主责：成员 3  
规则版本：`resume-score-v1`  
是否涉及 AI 禁飞区：是

## 1. 测试目标

验证简历评分规则是否满足：

- 相同输入结果稳定；
- 总分范围为 0—100；
- 各维度分数可解释；
- 扣分项能定位问题；
- 缺失、空值、极端长度和异常输入可处理；
- 不依赖 AI 主观评分。

## 2. 测试前提

评分接口建议：

```text
POST /api/v1/resume-versions/{versionId}/assessments
```

评分结果保存到：

```text
resume_assessments
```

核心断言：

```text
totalScore == sum(dimensionScores)
0 <= totalScore <= 100
每个维度最终得分 >= 0
每个维度最终得分 <= 该维度分值上限
ruleVersion == resume-score-v1
deductions 不为空时必须包含 code、dimension、reason、points
deductions.code 必须来自评分规则人工设计表
```

## 3. 测试用例表

| 编号 | 用例名称 | 输入特征 | 预期维度影响 | 预期关键断言 | 边界 |
|---|---|---|---|---|---|
| SCORE-001 | 完整简历 | 有 basicInfo、education、projects、skills、证据引用 | 各维度正常 | `totalScore >= 80`；`deductions` 为空或仅包含轻微建议 | 否 |
| SCORE-002 | 缺少项目经历 | `projects` 为空数组 | 完整度扣分；经历质量显著扣分；量化表达显著扣分 | `experienceQuality` 低于完整简历；`deductions.code` 包含 `missing_projects` | 是 |
| SCORE-003 | 缺少技能 | `skills` 为空数组或缺失 | 完整度扣分 | `completeness` 低于完整简历；`deductions.code` 包含 `missing_skills` | 是 |
| SCORE-004 | 无量化结果 | 项目描述无数字、规模、时间或结果指标 | 量化表达扣分 | `quantitativeExpression` 显著降低；`deductions.code` 包含 `missing_quantitative_result` | 否 |
| SCORE-005 | 无证据引用 | `resume_evidence_refs` 为空 | 证据支撑扣完 | `evidenceSupport == 0`；`deductions.code` 包含 `no_evidence_ref` | 是 |
| SCORE-006 | 极短项目描述 | 单个项目/实习经历描述少于人工设定阈值 | 经历质量和可读性扣分 | `readability` 降低；`deductions.code` 包含 `description_too_short` | 是 |
| SCORE-007 | 描述过长 | 单个项目/实习经历描述超过人工设定阈值 | 可读性扣分 | `readability` 降低；`deductions.code` 包含 `description_too_long` | 是 |
| SCORE-008 | 空简历版本 | `content_json` 为空对象或缺关键节点 | 多维度扣分 | `totalScore < 30`；`deductions.code` 包含 `empty_resume_content`；各维度不得为负分 | 是 |
| SCORE-009 | 只有技能堆砌 | `skills` 很多但 `projects` 为空且无证据引用 | 经历质量为低值；证据支撑为低值 | 不得得到高分；`deductions.code` 同时包含 `missing_projects` 和 `no_evidence_ref` | 否 |
| SCORE-010 | 相同输入重复评分 | 同一 `resume_version_id` 连续评分两次 | 结果完全一致 | 两次返回的 `totalScore`、`dimensionScores`、`inputFingerprint` 一致 | 是 |
| SCORE-011 | 字段格式异常 | `content_json.projects` 不是数组 | 不写入评分 | 返回业务错误；`resume_assessments` 不新增记录 | 是 |
| SCORE-012 | 证据引用存在但证据被软删除 | `resume_evidence_refs` 指向 `deleted_at` 非空证据 | 证据支撑扣分 | `evidenceSupport` 降低；`deductions.code` 包含 `invalid_evidence_ref` | 是 |
| SCORE-013 | 缺少项目结果描述 | 项目/实习经历有行动描述但没有产出、效果、影响或交付物 | 经历质量扣分 | `experienceQuality` 降低；`deductions.code` 包含 `missing_project_result` | 否 |

## 4. 样例数据建议

建议准备以下脱敏样例，不使用真实简历：

| 文件 | 覆盖用例 | 用途 |
|---|---|---|
| `complete-resume.json` | SCORE-001 | 正常基准 |
| `missing-projects.json` | SCORE-002、SCORE-009 | 项目缺失边界 |
| `missing-skills.json` | SCORE-003 | 技能缺失边界 |
| `no-evidence-ref.json` | SCORE-005 | 证据支撑边界 |
| `short-description.json` | SCORE-006 | 极短描述边界 |
| `missing-project-result.json` | SCORE-013 | 缺少结果描述 |
| `invalid-projects-format.json` | SCORE-011 | 异常字段格式 |

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

- [ ] 测试用例由成员人工确认；
- [ ] AI 未生成评分算法实现；
- [ ] AI 未决定评分权重；
- [ ] AI 未给出最终分数；
- [ ] 所有 `deductions.code` 都来自人工设计表；
- [ ] “待成员 1 人工确认”的规则已补齐后再开始编码；
- [ ] 同一维度多项扣分时，最终维度分数不会低于 0；
- [ ] 主责成员能解释每个维度和扣分条件；
- [ ] QA 能根据本表独立验证边界用例。
