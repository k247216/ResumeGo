# Prompt 记录

本文档记录对项目有复用价值的重要 Prompt。普通一次性 AI 使用记录写入 `ai-usage-log.md`。

## 记录规则

1. 不记录 API Key、访问令牌、手机号、邮箱、完整简历原文等敏感信息。
2. 不记录包含禁飞区实现要求的 Prompt。
3. 如果 Prompt 涉及用户经历，只保留脱敏摘要。
4. 每条 Prompt 说明用途、适用范围和注意事项。

## 模板

```text
### PR-YYYYMMDD-序号｜标题

用途：

适用场景：

模型：

Prompt 摘要：

输入变量：

输出格式要求：

注意事项：

是否涉及禁飞区：

```

## 示例

```text
### PR-20260707-01｜生成简历优化建议 JSON

用途：
让模型基于用户已提供的能力证据，生成结构化简历优化建议。

适用场景：
用户已经录入简历版本、能力证据和目标岗位 JD。

模型：
通义千问 Max

Prompt 摘要：
要求模型只基于给定证据提出建议，不得编造经历；返回 JSON 数组，每条建议包含建议类型、引用证据、原文片段、修改建议和风险提示。

输入变量：
- resumeVersionSummary
- evidenceSummaries
- jobRequirementSummary

输出格式要求：
必须符合后端定义的 Suggestion JSON Schema。

注意事项：
模型不得返回评分、排序或状态流转结果。

是否涉及禁飞区：
否
```

## 已采用 Prompt 资产

### PR-20260715-01｜AI 能力教练：薄弱点与训练计划建议

用途：
让模型基于当前简历、目标岗位、匹配缺口、能力证据和可选公司 Profile，生成项目技术表达建议与训练计划。

适用场景：
用户已经选择一个目标岗位，并在工作台右侧打开 AI 建议面板；建议用于人工理解和修改，不直接覆盖简历。

模型：
通义千问 Max；本地开发可使用 Mock 兜底。

Prompt 摘要：
要求模型扮演“项目技术表达教练”，围绕项目经历、工作/实习经历、技能表达、工程难点和岗位要求生成建议。每条建议必须体现“薄弱点 → 行动建议 → 训练计划”的结构。若证据不足，应提出追问，不得编造经历、数字、技术或荣誉。

输入变量：
- resumeContent：脱敏后的简历结构化内容；
- parsedJd：结构化岗位要求；
- matchGaps 或 matchResponse：岗位匹配缺口；
- assessmentResponse：可选简历评分摘要；
- capabilityEvidences：用户已提供的能力证据；
- companyProfile：可选公司偏好 Profile。

输出格式要求：
严格返回 JSON，根节点包含 `suggestions` 数组。每条建议包含 `sectionKey`、`originalText`、`suggestedText`、`reason`、`targetRequirement`、`evidenceId`、`confidence`。

注意事项：
- `suggestedText` 是行动建议和训练计划，不是可一键替换文本；
- 无证据时 `evidenceId=null` 且 `suggestedText=null`，在 `reason` 中追问用户补充事实；
- companyProfile 只能作为表达风格参考，不得作为评分、排序、录用概率或事实断言依据；
- 模型输出不能创建简历版本，也不能改变建议状态。

是否涉及禁飞区：
否。该 Prompt 不生成简历评分权重、岗位排序规则或面试状态机实现。

### PR-20260715-02｜模拟面试评价：避免固定分数锚定

用途：
让模型对单题面试回答生成结构化评价，展示清晰度、相关性、深度和准确性四个维度。

适用场景：
用户提交模拟面试回答后，系统需要展示可行动的回答评价。

模型：
通义千问 Max；本地开发可使用 Mock 兜底。

Prompt 摘要：
要求模型根据问题、目标岗位、简历上下文和用户回答，对回答质量进行评价。Prompt 避免给出固定数字示例，要求空泛回答不得高分，只有具体场景、个人动作、技术细节和可核实结果充分时才能给高分。

输入变量：
- questionText；
- answerText；
- resumeVersionSummary；
- jobDescriptionSummary；
- personaStyle。

输出格式要求：
返回 JSON，包含 `score`、`strengths`、`weaknesses`、`suggestions`、`referenceAnswer`。其中 `score` 包含 `clarity`、`relevance`、`depth`、`accuracy`。

注意事项：
- 评价分数只是 AI 对回答表现的反馈，不控制状态机；
- 不得输出录用概率；
- 结构化校验失败时不得写入业务结果。

是否涉及禁飞区：
否。状态推进、题号和结束条件仍由人工状态机控制。
