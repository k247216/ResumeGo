# Interview Engine V2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有单一岗位模拟重构为来源清晰、可恢复、可回放的岗位模拟、知识训练和面经模拟三模式引擎。

**Architecture:** 保留 `InterviewPlan` + 多 `InterviewSession` 和确定性状态机，在 Plan 上增加不可变 mode 与开始上下文快照；三种模式使用独立请求校验器和问题来源适配器，共享会话、回答、评价和总结基础设施。前端将大厅、配置、房间、报告和历史拆开，避免继续扩张 `InterviewView.vue`。

**Tech Stack:** Vue 3、TypeScript、Vitest、Spring Boot、Flyway、H2/MySQL、现有 AI invocation/provider 基础设施

**Spec:** `docs/superpowers/specs/2026-08-25-resume-interview-workspace-contract.md`

## Global Constraints

- 基线为 Resume System V2 集成提交或 Core Controller 明确提供的后继提交。
- 只有 `ROLE_BASED`、`KNOWLEDGE_TRAINING`、`EXPERIENCE_SIMULATION` 三种模式。
- 创建后 mode 和 startContextSnapshot 不可修改。
- AI 不得控制状态机、权限、题目来源类型或 Pipeline 阶段。
- 历史回放不得用当前简历、JD 或知识标题覆盖开始快照。
- 不修改 Workspace；不实现语音、实时音视频、自动投递和游戏化总分。
- 测试使用虚构数据与 stub AI，不调用真实模型。

---

### Task 1: 三模式数据库契约与题集资产

**Files:**
- Create: `backend/src/main/resources/db/migration/V40__add_interview_modes_and_question_sets.sql`
- Create: `backend/src/main/resources/db/migration-h2/V20__add_interview_modes_and_question_sets.sql`
- Create: `backend/src/main/java/com/resumego/interview/InterviewMode.java`
- Create: `backend/src/main/java/com/resumego/interview/QuestionSourceType.java`
- Create: `backend/src/test/java/com/resumego/interview/repository/InterviewModeRepositoryTest.java`

**Interfaces:**
- Adds `interview_plans.mode`, `context_contract_version`, `start_context_snapshot_json`.
- Adds user-owned `interview_question_sets` and ordered `interview_question_set_items`.
- Question source types: `AI_GENERATED`、`SYSTEM_DEFINED`、`USER_MANUAL`、`IMPORTED_EXPERIENCE`、`GENERATED_PRACTICE`、`AI_FOLLOW_UP`.

- [ ] **Step 1: 写迁移 RED 测试**

覆盖旧计划回填 `ROLE_BASED`、mode/snapshot 可读、题集顺序稳定、跨用户不可读、非法 mode/source 被拒绝。

```bash
cd backend
mvn -q -Dtest=InterviewModeRepositoryTest test
```

预期因字段和 Repository 尚不存在失败。

- [ ] **Step 2: 编写双数据库迁移**

MySQL/H2 语义一致。旧计划只根据现有外键生成兼容快照，不复制简历/JD 正文，不伪造 Pipeline ID。

- [ ] **Step 3: 运行测试并提交**

```bash
cd backend
mvn -q -Dtest=InterviewModeRepositoryTest test
git add backend/src/main/resources/db/migration backend/src/main/resources/db/migration-h2 backend/src/main/java/com/resumego/interview/InterviewMode.java backend/src/main/java/com/resumego/interview/QuestionSourceType.java backend/src/test/java/com/resumego/interview/repository/InterviewModeRepositoryTest.java
git commit -m "feat(interview): add mode and source contracts"
```

### Task 2: 不可变开始上下文和模式校验器

**Files:**
- Create: `backend/src/main/java/com/resumego/interview/context/InterviewStartContext.java`
- Create: `backend/src/main/java/com/resumego/interview/context/InterviewContextSnapshot.java`
- Create: `backend/src/main/java/com/resumego/interview/context/InterviewContextValidator.java`
- Create: `backend/src/main/java/com/resumego/interview/context/RoleBasedContextValidator.java`
- Create: `backend/src/main/java/com/resumego/interview/context/KnowledgeTrainingContextValidator.java`
- Create: `backend/src/main/java/com/resumego/interview/context/ExperienceSimulationContextValidator.java`
- Create: `backend/src/test/java/com/resumego/interview/context/InterviewContextValidatorTest.java`
- Modify: `backend/src/main/java/com/resumego/interview/dto/CreateInterviewPlanRequest.java`
- Modify: `backend/src/main/java/com/resumego/interview/dto/InterviewPlanResponse.java`
- Modify: `backend/src/main/java/com/resumego/interview/entity/InterviewPlan.java`
- Modify: `backend/src/main/java/com/resumego/interview/service/InterviewPlanService.java`

**Interfaces:**
- Common request: `mode`, `questionCount`, `personaIds`, `focusTags`, `supplement`.
- Role: `jobProjectId` + `resumeVersionId` required.
- Knowledge: non-empty `knowledgeDocumentIds`, optional `difficulty`.
- Experience: `questionSetId` required.
- Response adds `mode`, `contextContractVersion`, `startContextSnapshot`.

- [ ] **Step 1: 写三模式 RED 测试**

每种模式覆盖正常、缺字段、混入其他模式字段、跨用户引用和不可用来源；验证源对象改名不改变历史快照。

- [ ] **Step 2: 实现明确模式类型**

使用 sealed interface 或等价显式类型，不允许一个全可选大 DTO 直接进入 Service。Controller 请求必须先转换为一种明确上下文。

- [ ] **Step 3: 构造最小快照**

快照保存 ID、名称、版本号、来源类型、persona 顺序、题量和 prompt/schema 版本，不保存正文、回答、Key 或绝对路径。

- [ ] **Step 4: 验证并提交**

```bash
cd backend
mvn -q -Dtest=InterviewContextValidatorTest,InterviewPlanServiceTest,InterviewPlanControllerTest test
git add backend/src/main/java/com/resumego/interview backend/src/test/java/com/resumego/interview
git commit -m "feat(interview): validate immutable start contexts"
```

### Task 3: 三种问题来源适配器

**Files:**
- Create: `backend/src/main/java/com/resumego/interview/source/InterviewQuestionSource.java`
- Create: `backend/src/main/java/com/resumego/interview/source/RoleBasedQuestionSource.java`
- Create: `backend/src/main/java/com/resumego/interview/source/KnowledgeTrainingQuestionSource.java`
- Create: `backend/src/main/java/com/resumego/interview/source/ExperienceQuestionSource.java`
- Create: `backend/src/main/java/com/resumego/interview/source/QuestionDraft.java`
- Create: `backend/src/test/java/com/resumego/interview/source/InterviewQuestionSourceTest.java`
- Modify: `backend/src/main/java/com/resumego/interview/service/InterviewPromptBuilder.java`
- Modify: `backend/src/main/java/com/resumego/interview/service/InterviewService.java`

**Interfaces:**
- `supports(InterviewMode mode): boolean`.
- `prepare(InterviewContextSnapshot snapshot, int count): List<QuestionDraft>`.
- `QuestionDraft` contains text, questionType, sourceType, optional sourceReference and provenanceLabel.

- [ ] **Step 1: 写来源 RED 测试**

岗位读取明确 Pipeline/JD/版本；知识只读取已选文档并产生片段引用；面经按题集顺序返回；AI 追问标为 `AI_FOLLOW_UP`。

- [ ] **Step 2: 抽取现有岗位问题生成**

保持当前岗位模式行为和状态机，只移动来源准备职责。

- [ ] **Step 3: 实现知识训练来源**

评价 schema 必须给出 source reference 或 `evidenceMissing=true`；无可用片段时不得生成虚假引用。

- [ ] **Step 4: 实现面经来源**

原题不经 AI 改写；题量不足不生成题冒充面经；AI 追问单独标源。

- [ ] **Step 5: 验证并提交**

```bash
cd backend
mvn -q -Dtest=InterviewQuestionSourceTest,InterviewPromptBuilderTest,InterviewServiceTest,InterviewStateMachineTest test
git add backend/src/main/java/com/resumego/interview backend/src/test/java/com/resumego/interview
git commit -m "feat(interview): add mode-specific question sources"
```

### Task 4: 面经题集 API

**Files:**
- Create: `backend/src/main/java/com/resumego/interview/controller/InterviewQuestionSetController.java`
- Create: `backend/src/main/java/com/resumego/interview/service/InterviewQuestionSetService.java`
- Create: `backend/src/main/java/com/resumego/interview/repository/InterviewQuestionSetRepository.java`
- Create: `backend/src/main/java/com/resumego/interview/dto/InterviewQuestionSetRequest.java`
- Create: `backend/src/main/java/com/resumego/interview/dto/InterviewQuestionSetResponse.java`
- Create: `backend/src/test/java/com/resumego/interview/controller/InterviewQuestionSetControllerTest.java`
- Create: `backend/src/test/java/com/resumego/interview/service/InterviewQuestionSetServiceTest.java`

**Interfaces:**
- `POST /api/v1/interview-question-sets` creates metadata and ordered questions.
- `GET /api/v1/interview-question-sets` lists current-user sets without full bodies.
- `GET /api/v1/interview-question-sets/{id}` returns ordered items.
- `PATCH /api/v1/interview-question-sets/{id}` atomically replaces metadata/items.
- `POST /api/v1/interview-question-sets/{id}/archive` prevents new use but preserves history.

- [ ] **Step 1: 写 CRUD 与越权 RED 测试**

覆盖空题集、重复顺序、超长题目、非法来源、跨用户、归档后不可开始、更新失败完整回滚。

- [ ] **Step 2: 实现事务服务和 API**

来源说明只是用户声明文本，不进行网络抓取；日志不得记录题目全文。

- [ ] **Step 3: 验证并提交**

```bash
cd backend
mvn -q -Dtest=InterviewQuestionSetServiceTest,InterviewQuestionSetControllerTest test
git add backend/src/main/java/com/resumego/interview backend/src/test/java/com/resumego/interview
git commit -m "feat(interview): add local experience question sets"
```

### Task 5: 前端三模式类型、API 与准备状态

**Files:**
- Modify: `frontend/src/types/interview.ts`
- Modify: `frontend/src/api/interview.ts`
- Create: `frontend/src/composables/useInterviewComposer.ts`
- Create: `frontend/src/composables/useInterviewComposer.test.ts`

**Interfaces:**
- Produces discriminated union `CreateInterviewPlanRequest` keyed by `mode`.
- Produces question-set types/client.
- Composer owns mode and mode-specific draft；切换模式不复制不兼容 ID。

- [ ] **Step 1: 写状态 RED 测试**

覆盖三模式必填项、切换模式、加载失败、保留各模式草稿、开始成功使用真实返回 plan。

- [ ] **Step 2: 实现 API 与 composable**

禁止 `any` 和全可选 request；不自动选择列表第一条 Pipeline、简历、资料或题集。

- [ ] **Step 3: 验证并提交**

```bash
cd frontend
npx vitest run src/composables/useInterviewComposer.test.ts
git add frontend/src/types/interview.ts frontend/src/api/interview.ts frontend/src/composables/useInterviewComposer.ts frontend/src/composables/useInterviewComposer.test.ts
git commit -m "feat(interview): add three-mode composer state"
```

### Task 6: 拆分大厅、配置、房间、报告和历史

**Files:**
- Create: `frontend/src/components/interview/InterviewModePicker.vue`
- Create: `frontend/src/components/interview/RoleBasedSetup.vue`
- Create: `frontend/src/components/interview/KnowledgeTrainingSetup.vue`
- Create: `frontend/src/components/interview/ExperienceSimulationSetup.vue`
- Create: `frontend/src/components/interview/InterviewComposer.vue`
- Create: `frontend/src/components/interview/InterviewResultWorkspace.vue`
- Create: `frontend/src/components/interview/InterviewModePicker.test.ts`
- Create: `frontend/src/components/interview/InterviewComposer.test.ts`
- Modify: `frontend/src/components/interview/InterviewHistoryPanel.vue`
- Modify: `frontend/src/components/interview/InterviewHistoryPanel.test.ts`
- Modify: `frontend/src/views/InterviewView.vue`
- Create: `frontend/src/views/InterviewView.mode.test.ts`

**Interfaces:**
- Existing `InterviewChatThread` and `InterviewRoomSidebar` remain shared room components.
- History rows consume immutable snapshot labels and filter by mode.

- [ ] **Step 1: 写页面 RED 测试**

覆盖三个入口、模式专属字段、缺失项、历史快照、面经原题/AI 追问标签和中断恢复。

- [ ] **Step 2: 实现模式入口和配置**

三个入口等权且描述用户目的；不写“规划中”或 AI 营销提示。岗位配置 Pipeline/版本/persona，知识配置资料/难度/题量，面经配置题集/persona/追问强度。

- [ ] **Step 3: 收敛 InterviewView**

页面只组合 Composer、Room、Result、History；请求、校验和草稿进入 composable；不得重写确定性状态机。

- [ ] **Step 4: 验证并提交**

```bash
cd frontend
npx vitest run src/composables/useInterviewComposer.test.ts src/components/interview src/views/InterviewView.mode.test.ts
npm run build
git add frontend/src/components/interview frontend/src/views/InterviewView.vue frontend/src/views/InterviewView.mode.test.ts
git commit -m "feat(interview): build three-mode training workspace"
```

### Task 7: 中断恢复、反馈事件与纵向验收

**Files:**
- Create: `backend/src/main/java/com/resumego/interview/feedback/InterviewFeedbackEvent.java`
- Create: `backend/src/main/java/com/resumego/interview/feedback/InterviewFeedbackProjector.java`
- Create: `backend/src/test/java/com/resumego/interview/feedback/InterviewFeedbackProjectorTest.java`
- Modify: `backend/src/main/java/com/resumego/interview/service/InterviewService.java`
- Modify: `backend/src/test/java/com/resumego/interview/service/InterviewStateMachineConcurrencyTest.java`
- Modify: `docs/testing/v2-final-test-matrix.md`

**Interfaces:**
- Produces `sourcePlanId`, `mode`, `primaryIssue`, `suggestedAction`, `status=PENDING`.
- Does not modify Resume、Pipeline、Knowledge 或 Workspace 状态。

- [ ] **Step 1: 写恢复与幂等 RED 测试**

覆盖重启后继续、重复提交无双答案、完成只生成一条反馈、失败可重试、源资料归档后历史可回放。

- [ ] **Step 2: 实现最小反馈投影**

只映射持久化总结中的最核心问题与建议；不计算全局排名，不自动接受行动。

- [ ] **Step 3: 运行模块回归**

```bash
cd backend
mvn -q -Dtest='Interview*Test' test
cd ../frontend
npx vitest run src/components/interview src/views/InterviewView.mode.test.ts src/composables/useInterviewComposer.test.ts
npm run build
```

- [ ] **Step 4: 手工验证三条纵向流程**

分别完成岗位、知识和面经模拟，记录开始快照、题目来源、结果、历史回放和失败恢复；只使用虚构材料和 stub AI。

- [ ] **Step 5: 交付而不自行集成**

交付提交哈希、迁移/API 兼容说明、RED/GREEN 证据、未执行项与风险。Core Controller 负责跨模块集成、全量回归和 Workspace 消费契约。
