# Career OS V2 简历、面试与工作台产品契约

Status: Frozen for implementation

Date: 2026-08-25

Owner: Core Controller

## 1. 产品目标

本契约把用户已经确认的 Career OS 模型落实为三个连续但边界独立的系统：

```text
简历资产与版本
      ↓（用户显式选择）
面试训练上下文
      ↓（结构化反馈事件）
Workspace 下一步行动
```

产品不是围绕单个岗位自动切换全部上下文的“求职项目容器”。岗位、简历、知识和训练记录分别拥有自己的生命周期，只在用户明确发起某个动作时建立关联。

## 2. 已确认的用户心智

### 2.1 简历不是岗位的附属品

- 用户通常长期维护少量相近方向的简历，不会为每个岗位被迫重新建立一份简历。
- 切换 Career Pipeline 不得自动切换、复制或修改简历。
- Pipeline 只能显式引用用户选择的一个具体简历版本。
- 用户确实需要岗位化表达时，可以从任意版本手动创建“岗位表达副本”。
- 副本创建后独立演进；源简历后续修改不会自动传播，副本修改也不会污染源简历。

### 2.2 模拟面试在开始时绑定上下文

- 用户进入模拟面试页面后选择训练模式，再补齐该模式所需上下文。
- 岗位目标和简历版本只在岗位模拟模式中必须绑定。
- 知识训练不要求虚构一个岗位或简历。
- 面经模拟必须标明真实题目来源；AI 只能主持、追问和评价，不能把生成题目伪装成真实面经。

### 2.3 Workspace 只给出今天的行动

- Workspace 不负责完整简历编辑、完整面试配置、日历管理或趋势分析。
- Workspace 展示一个可解释的主行动、少量最近活动和轻量成长反馈。
- 首页不通过切换 Pipeline 改写简历或面试上下文。

## 3. 领域边界

| 模块 | 拥有的数据 | 可以引用 | 不得拥有或推断 |
| --- | --- | --- | --- |
| Resume System | 简历资产、不可变版本、版本谱系、AI 建议及用户决定 | 用户确认的知识/能力事实 | Pipeline 阶段、面试状态、自动岗位归属 |
| Career Pipeline | 公司、岗位、JD、阶段、日程、显式关联的简历版本 | Resume Version、Interview Plan、Knowledge Document | 简历正文、知识正文、自动阶段推进 |
| Interview Engine | 训练计划、模式、开始上下文快照、问题、回答、评价、总结 | Pipeline、Resume Version、Knowledge、面经题集 | 修改源资产、决定 Pipeline 阶段、伪造题目来源 |
| Growth Feedback | 规范化反馈事件、薄弱点、建议动作 | Interview、Resume、Knowledge 活动 | 排行、惩罚性连续天数、最终能力真值 |
| Workspace Action | 确定性行动投影及用户处理状态 | 各模块公开的事实和事件 | 复制源数据、AI 排序、隐藏改变领域状态 |

## 4. Resume System 冻结契约

### 4.1 核心对象

#### ResumeAsset

一份用户能够独立打开、命名、归档和维护的简历。

必需语义：

- `id`：本地稳定标识；
- `title`：用户可修改的名称；
- `kind`：`GENERAL` 或 `JOB_EXPRESSION`；
- `forkedFromVersionId`：仅岗位表达副本需要，指向创建副本时复制的源版本；
- `currentVersionId`：当前默认打开的版本；
- `archivedAt`：归档时间，归档不删除历史和关联。

现有 `resumes.target_job_description_id` 仅作为历史兼容字段，不再代表 V2 关系真值，不得由新界面写入。V2 中岗位与简历的关系只存在于 Pipeline 对具体版本的显式引用，以及面试开始上下文快照中。

#### ResumeVersion

一份简历在某次保存后的不可变快照。

- `resumeId` 必须属于当前用户；
- `versionNo` 在同一 ResumeAsset 内从 1 单调递增；
- `parentVersionId` 只允许指向同一 ResumeAsset 的直接前一版本；
- `content` 保存完整结构化正文快照；
- `changeSummary` 说明本次修改；
- `createdByType` 只允许现有 `user`、`ai_suggestion`、`import` 以及新增 `fork`；
- 已创建版本不得原地覆盖。

跨 ResumeAsset 的来源关系不得复用 `parentVersionId`，统一使用目标 ResumeAsset 的 `forkedFromVersionId`。

### 4.2 创建与演进规则

1. 新建空白简历：创建 `GENERAL` 资产和 V1。
2. 导入简历：解析结果必须先由用户检查；确认后创建 `GENERAL` 资产和 V1。
3. 普通保存：只允许从该资产的 `currentVersionId` 创建下一版本，父版本必须是保存前的当前版本。历史版本只读；用户要继续使用历史内容时，必须显式创建新的岗位表达副本，不能在同一资产内形成分支。
4. 创建岗位表达副本：复制选定版本正文，创建新的 `JOB_EXPRESSION` 资产和 V1，并记录 `forkedFromVersionId`。
5. 副本与源资产此后完全独立，不自动同步。
6. Pipeline 绑定：用户明确选择一个 `resumeVersionId`；切换目标或更新简历都不得自动改绑。
7. 删除：当前阶段只支持归档 ResumeAsset。存在 Pipeline、Interview 或 AI 建议引用时禁止物理删除版本。

### 4.3 AI 建议规则

- 建议必须绑定一个源 `resumeVersionId`。
- 持久化原文片段、依据、建议稿、差异和用户决定。
- `accept` 以现有 `ai_suggestion` 类型创建同一 ResumeAsset 的新版本并记录 `acceptedVersionId`。
- 只有基于当前版本的建议可以直接接受；基于历史版本的过期建议必须重新生成或由用户从该历史版本创建副本，不能套用到当前正文。
- `reject` 和 `defer` 不修改简历正文。
- AI 不得新增用户未确认的经历、技能、数字、荣誉或项目结果。
- 从简历文字推导的“能力证据”只能是待确认提案，不能静默创建为已确认事实。

### 4.4 简历库界面

采用桌面 Library → Workspace → Inspector 结构：

- Library：搜索、全部/通用/岗位表达/归档过滤、简历列表；
- Workspace：选中简历的身份、当前版本摘要、预览入口、编辑入口、创建副本；
- Inspector：版本历史、来源版本、被哪些 Pipeline/面试引用；可关闭或窄化；
- 空状态只提供“新建空白简历”和“导入简历”，不制造示例内容；
- 列表清楚区分简历资产和版本，不把 V3 当成第三份简历；
- 当前版本、历史版本和岗位表达副本均使用真实数据。

### 4.5 Resume API 目标契约

保持现有读取和保存能力，新增或收敛以下行为：

```text
GET  /api/v1/resumes?kind=&archived=
GET  /api/v1/resumes/{resumeId}/versions
POST /api/v1/resumes
POST /api/v1/resumes/{resumeId}/versions
POST /api/v1/resume-versions/{versionId}/fork
PATCH /api/v1/resumes/{resumeId}
POST /api/v1/resumes/{resumeId}/archive
POST /api/v1/resumes/{resumeId}/restore
```

`fork` 请求只接受新标题，不接受 renderer 提交源正文；服务端读取并复制当前用户拥有的源版本。

所有按 ID 读取、版本创建、归档、恢复、fork 和引用查询都必须同时校验 `user_id`，跨用户统一按不存在处理且不得产生副作用。

## 5. Interview Engine 冻结契约

### 5.1 三种且仅三种模式

| 模式 | 枚举 | 必需输入 | 可选输入 | 禁止行为 |
| --- | --- | --- | --- | --- |
| 岗位模拟 | `ROLE_BASED` | `jobProjectId`、`resumeVersionId`、至少一个 persona | 关注点、补充说明 | 自动使用当前 Pipeline 或列表第一份简历 |
| 知识训练 | `KNOWLEDGE_TRAINING` | 至少一个当前用户 Knowledge Document | 难度、关注主题、题量 | 强迫选择岗位/简历；无来源却输出有依据结论 |
| 面经模拟 | `EXPERIENCE_SIMULATION` | 当前用户创建或导入的题集 | persona、追问强度、题量 | 将 AI 生成问题标成真实面经 |

### 5.2 计划、轮次与开始快照

- `InterviewPlan` 表示一次完整训练；创建后 `mode` 不可改变。
- `InterviewSession` 表示计划内的一轮面试官对话，沿用确定性状态机。
- 创建计划时校验所有引用均属于当前用户，然后保存不可变 `startContextSnapshot`。
- 快照至少包含：契约版本、模式、各引用 ID、用户可见名称、版本号、来源类型、persona 顺序、题量、关注点、prompt 版本和输出 schema 版本。
- 源资产后续改名、更新或归档，不改变历史计划展示的开始上下文。
- 快照不复制完整简历正文、知识正文或 API Key；问题生成所需内容只在调用时最小化读取。

### 5.3 模式来源与可追溯性

#### ROLE_BASED

- Pipeline 与 Resume Version 分别由用户选择；Pipeline 当前绑定版本只用于默认建议，不得锁定或自动覆盖选择。
- 开始前显示公司/岗位、JD 状态、简历标题/Vn 和 persona。
- JD 缺失时必须阻止开始并指引补充，不能使用职位名称猜测完整 JD。

#### KNOWLEDGE_TRAINING

- 只允许选择状态可用且属于当前用户的 Knowledge Document。
- 问题和评价保存来源引用；知识性结论引用文档片段，找不到依据时明确标注“资料中未找到依据”。
- 删除或归档源文件后历史问题、回答和评价仍可回放，但打开来源时显示源已不可用。

#### EXPERIENCE_SIMULATION

- 题集是独立本地资产，至少保存标题、来源类型、可选来源说明和有序题目。
- 来源类型：`USER_MANUAL`、`IMPORTED_EXPERIENCE`、`GENERATED_PRACTICE`。
- 只有前两类可显示“真实题目/面经来源”；`GENERATED_PRACTICE` 必须显著标为练习题。
- AI 可以追问，但追问必须标记为 `AI_FOLLOW_UP`，不得混入原始题集。

### 5.4 共享状态机与反馈

沿用确定性状态机：

```text
READY → ASKING → WAITING_ANSWER → EVALUATING
       → ASKING（下一题）或 SUMMARIZING → COMPLETED
```

- AI 只能返回结构化问题、追问、评价和总结，不得写 `status`。
- 中断后依据持久化状态恢复；重复提交同一回答不得生成两条答案。
- 单题评价与整场总结都保存来源、prompt/schema 版本和 AI invocation 引用。
- 完成计划后生成规范化 Feedback Event，包含模式、来源计划、最核心问题、下一步建议和用户可处理状态。

### 5.5 面试界面

模拟面试一级页面分为：

1. 模式入口：三个等权但语义清楚的模式，不使用“规划中”占位；
2. 准备配置：只显示当前模式需要的字段；
3. 面试房间：对话、进度、当前来源提示、中断/继续；
4. 结果报告：核心总结、最需要改善的一点、下一步动作、逐题详情；
5. 历史记录：按模式筛选，显示当时的上下文快照。

当前 3000 行级 `InterviewView.vue` 必须按用户任务拆分，页面只负责路由和工作区组合；不得为了拆文件制造重复状态。

## 6. Workspace Action 冻结契约

### 6.1 页面职责

比例是产品权重而非机械网格面积：

- 70% 主行动；
- 20% 最近活动；
- 10% 轻量成长反馈。

首页不得出现完整简历预览、完整面试设置、完整日历、Pipeline 详情表单、AI Provider 大卡片或多个互相竞争的主要按钮。

### 6.2 主行动排序

排序由确定性规则产生，AI 只可润色解释：

1. 已开始或可恢复的面试训练；
2. 24 小时内现实面试/笔试的准备动作；
3. 用户明确设置且到期的行动；
4. 最近反馈产生的未处理改进动作；
5. Pipeline 缺失的必要准备项；
6. 知识训练建议；
7. 没有行动时的诚实空状态。

每个行动必须包含 `sourceType`、`sourceId`、`reasonCode`、`dueAt`、`route` 和用户可见解释。首页不得根据列表顺序或写死话术选择行动。

### 6.3 最近活动与成长

- 最近活动来自领域事件投影：简历版本创建、Pipeline 阶段更新、知识导入/编辑、面试完成、日程变更；
- 最多展示近期少量记录，不再增加独立通知中心；
- 成长只显示真实趋势或学习记录，不展示排行榜、总分、惩罚性打卡；
- 没有数据时保持留白，不生成假趋势。

## 7. 数据、隐私和删除边界

- 所有职业资产默认只存本地 V2 数据目录。
- API Key 继续由安全配置能力管理，不进入简历、面试快照、日志或导出。
- 普通日志不得记录简历正文、联系方式、面试回答、知识正文、面经题目全文或模型密钥。
- AI 调用只发送当前动作所需的最小内容，并保留用户可理解的用途说明。
- 归档不等于物理删除；被历史引用的版本和快照必须可回放。
- 物理删除需要先计算引用影响，当前阶段不开放跨模块级联物理删除。
- 所有测试使用虚构数据和伪 AI Provider。

## 8. 跨模块集成规则

1. Pipeline 通过 `resumeVersionId` 引用版本，不引用 `currentVersionId`。
2. Interview Plan 使用开始快照，不在历史回放时重新读取“当前版本”。
3. Feedback Event 只能引用来源对象，不反向修改来源。
4. Workspace 只消费公开投影，不直接查询或拼装各模块私有表。
5. 任意外部 Agent 不得同时修改 Resume、Interview 与 Workspace 三个页面。
6. 数据库迁移、公共 DTO、状态机和行动排序由 Core Controller 冻结后才允许实施。

## 9. 分阶段交付

### R1 — Resume Library 可用化

先完成真实资产/版本展示、fork、归档、引用信息和编辑入口。不得把 AI 优化、PDF 高保真解析和模板市场塞入本阶段。

### I1 — Interview Engine 共享契约

引入 mode、开始快照、模式校验、来源标识和可恢复状态，不改变 AI 对话的基本状态机。

### I2 — 三模式纵向切片

先让现有岗位模拟迁入新契约，再完成知识训练，最后完成面经题集与面经模拟。每种模式必须分别通过真实用户流程验收。

### W1 — Workspace Action

在 Resume、Pipeline、Knowledge、Interview、Schedule 的事件和引用稳定后，由 Core Controller 实现确定性行动投影和新首页。

## 10. 最终验收场景

1. 用户维护一份通用简历，保存多个线性版本；切换 Pipeline 不改变当前简历。
2. 用户从 V2 创建腾讯岗位表达副本，继续修改后不影响源简历 V2。
3. 两个 Pipeline 可以分别绑定不同版本；简历后来产生 V4 时，两条 Pipeline 均不会静默改绑。
4. 岗位模拟明确显示所选 Pipeline 和简历版本，完成后历史仍显示开始时的 Vn。
5. 知识训练只选择资料即可开始，反馈能够定位来源或明确证据不足。
6. 面经模拟区分原题和 AI 追问，历史回放不会把追问标为真实题。
7. 中途中断面试并重启桌面应用后可以继续，重复提交不产生重复答案。
8. 完成训练后生成一条反馈和一个候选下一步行动，但不会自动修改简历或 Pipeline 阶段。
9. Workspace 只展示一个最应该执行的动作，点击进入正确模块和上下文。
10. AI 未配置或调用失败时，简历手工编辑、版本管理、题集管理、历史回放和日程仍可使用。

## 11. 明确非目标

- 自动投递、岗位爬虫和招聘平台；
- 自动为每个 Pipeline 创建简历；
- 源简历和岗位表达副本自动同步；
- AI 自动接受简历修改或推进阶段；
- 语音面试、本阶段的实时音视频能力；
- 全局能力分数、排行榜和游戏化成长系统；
- 在 Workspace 重复实现各模块完整页面。
