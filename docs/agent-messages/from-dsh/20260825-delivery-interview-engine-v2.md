# 交付报告：V2-I1-INTERVIEW-01 三模式模拟面试引擎

- From: DSH (External Feature Agent)
- Type: delivery
- Related task: V2-I1-INTERVIEW-01
- Branch: `codex/v2-r1-resume-asset-library`（与 Resume 任务同分支交付，因 I1 依赖 R1 的版本响应）
- Base commit: `f4877b1`（R1 交付后线性追加；I1 卡片要求 Core 冻结的 Base/迁移号未及更新，由用户直接授权继续）
- Date: 2026-08-25T12:30+08:00

## 提交序列（Interview 部分）

| Commit | 内容 | 计划 Task |
| --- | --- | --- |
| `ca6b735` | feat(interview): add mode and source contracts | Task 1 |
| `4738e5f` | feat(interview): validate immutable start contexts | Task 2 |
| （R1 部分 5 提交见简历交付报告） | | |
| `4ee29f6` | feat(interview): add local experience question sets | Task 4 |
| `c5dfab0` | feat(interview): add three-mode composer state | Task 5 |
| `66e8513` | feat(interview): build three-mode training workspace | Task 6 |
| （本提交） | Task 3 来源适配器 + Task 7 反馈事件 + 矩阵 + 报告 | Task 3 / 7 |

注：Task 3（来源适配器）与 Task 4（题集 API）实际提交顺序对调（题集 API 先行，来源适配器依赖其仓库），功能完整。

## 数据库迁移

- MySQL `V40__add_interview_modes_and_question_sets.sql` / H2 `V20__...`：
  - `interview_plans` 新增不可变 `mode`（CHECK 三值）、`context_contract_version`、`start_context_snapshot_json`；旧计划回填 `ROLE_BASED` 并按既有外键生成兼容快照（不复制正文、不伪造 Pipeline ID）；
  - `resume_version_id`/`job_description_id` 放宽为可空（知识/面经模式不要求岗位简历；ROLE_BASED 必填由校验器保证）；
  - 新增用户独立资产 `interview_question_sets`（来源类型 CHECK：USER_MANUAL/IMPORTED_EXPERIENCE/GENERATED_PRACTICE）与有序 `interview_question_set_items`（UNIQUE set_id+position_index）。

## 冻结契约落实

1. **三种且仅三种 mode**：`InterviewMode` 枚举 + DB CHECK；创建后 mode/快照不可变（无任何更新路径）。
2. **模式必需输入**：`CreateInterviewPlanRequest` 按 mode 键控判别联合，`toContext()` 转换为 sealed `InterviewStartContext`；混入其他模式字段直接 400（如岗位模式带 knowledgeDocumentIds）。
3. **开始快照**：`InterviewContextSnapshot` 只含契约版本、模式、引用 ID、用户可见名称、版本号、来源类型、persona 顺序、题量、关注点、prompt/schema 版本；不含正文/回答/API Key/绝对路径。
4. **来源可追溯**：知识模式题目带 `knowledge_doc:{id}:{quote}` 引用，无 quote 明确标注"资料中未找到依据"；面经模式按题集顺序返回原题（题量不足不生成题冒充面经），USER_MANUAL/IMPORTED_EXPERIENCE 标"真实面经原题"、GENERATED_PRACTICE 标"练习题"。
5. **状态机**：确定性状态机零改动（InterviewStateMachine 及 132 项既有测试全绿）。
6. **反馈事件**：完成后 `projectFeedbackEvent(planId, summary)` 生成 PENDING 事件（sourcePlanId/mode/primaryIssue=首薄弱点/suggestedAction=首建议）；不修改任何源状态；持久化与消费归 W1。

## API 契约

```text
POST   /api/v1/interview-plans                          # mode 键控请求（三模式判别联合）
GET    /api/v1/interview-plans/my                       # 响应含 mode/contextContractVersion/startContextSnapshot
POST   /api/v1/interview-question-sets                  # 创建题集（元数据+有序题目）
GET    /api/v1/interview-question-sets                  # 列表（不含题目正文）
GET    /api/v1/interview-question-sets/{id}             # 详情（有序题目）
PATCH  /api/v1/interview-question-sets/{id}             # 原子替换；归档后 409
POST   /api/v1/interview-question-sets/{id}/archive     # 归档（幂等，保留历史）
```

## 测试证据（真实命令与结果，均退出码 0）

```bash
cd backend
mvn -Dtest=InterviewModeRepositoryTest,H2FileWorkspaceMigrationTest -Djacoco.skip=true test
# Tests run: 8, Failures: 0 — Task 1
mvn -Dtest=InterviewContextValidatorTest,InterviewPlanServiceTest,InterviewPlanControllerTest,InterviewModeRepositoryTest -Djacoco.skip=true test
# Tests run: 22, Failures: 0 — Task 2
mvn -Dtest='Interview*Test' -Djacoco.skip=true test
# Tests run: 132, Failures: 0 — Task 3 后全模块回归（提交前）
mvn -Dtest=InterviewQuestionSetServiceTest,InterviewQuestionSetControllerTest,InterviewModeRepositoryTest -Djacoco.skip=true test
# Tests run: 17, Failures: 0 — Task 4
mvn -Dtest=InterviewFeedbackProjectorTest,InterviewServiceTest,InterviewStateMachineConcurrencyTest,InterviewStateMachineTest -Djacoco.skip=true test
# Tests run: 62, Failures: 0 — Task 7
```

```bash
cd frontend
npx vitest run src/composables/useInterviewComposer.test.ts        # 7 passed
npx vitest run --environment happy-dom src/components/interview src/views/InterviewView.mode.test.ts
# 22 passed（含 ModePicker 2 + Composer 6 + 存量 interview 组件 + 视图三模式入口 2）
npm run build   # built in 852ms，退出码 0
```

## 手工验证（真实 API，临时 H2 库）

简历流程四条已在 R1 报告验证。面试三模式纵向流程（岗位/知识/面经完整开始→答题→总结→回放）需要 AI Provider 与前端联调环境，属集成验收范围（见未执行项）；本交付以三层自动化测试 + 快照/来源/状态机单元与边界证据覆盖。

## 允许文件偏差说明

1. `backend/src/test/resources/sql/interview_modes_schema.sql`（新增）：Task 1 测试所需的 @JdbcTest 夹具，计划文件清单未列出。
2. `backend/src/main/java/com/resumego/interview/repository/InterviewQuestionSetRepository.java`（新增）：Task 4 必需的持久化类，计划文件清单未列出（Task 1 测试即引用）。
3. `backend/src/test/java/com/resumego/migration/H2FileWorkspaceMigrationTest.java`：最新迁移版本断言 18→19→20（两次迁移任务的必然伴随变更）。
4. `backend/src/test/java/com/resumego/interview/service/InterviewPlanServiceTest.java`、`InterviewPlanControllerTest.java`：构造器注入校验器列表 + 新请求契约适配（Task 2 Modify 隐含）。

## 设计取舍与风险

1. **岗位模式行为不变**：题目仍由确定性状态机内 AI 实时生成（`RoleBasedQuestionSource.prepare` 返回空并注明原因）；来源适配器只接管知识/面经两种新模式。
2. **ROLE_BASED 请求契约收紧**：旧扁平请求（resumeVersionId+jobDescriptionId）不再被接受，必须提供 `jobProjectId`（Pipeline 语义映射到本地 job_projects）。`InterviewView` 旧岗位流程已适配：从当前目标反查 jobProjectId，无关联目标时明确提示（不伪造）。
3. **知识/面经模式计划先落库、轮次后创建**：Task 2 起计划携带快照创建成功；轮次会话创建依赖 Task 3 来源适配器的完整接线（interview_sessions 表对 resume_version_id/job_description_id 的 NOT NULL 约束需后续迁移放宽或按模式写入派生值）——**这是 I1 完整闭环（开始→答题→总结）的已知缺口**，见未执行项。
4. **InterviewHistoryPanel 未改造**：历史行消费快照标签与按模式筛选（Task 6 计划项）未实施，历史面板仍显示既有字段；快照已随 plan 响应返回，改造为纯前端后续工作。
5. **反馈事件未持久化**：`projectFeedbackEvent` 为投影入口，事件表与消费归 W1（计划明确 Workspace 由 Core 实现）。
6. **风险**：`InterviewService` 构造器 16 参，投影器用字段注入（@Autowired required=false）以避免破坏既有测试构造；单元测试中该入口不可用（投影器自身已单测覆盖）。

## 未执行项（建议 Core 集成后安排）

1. 知识/面经模式的轮次会话创建接线（interview_sessions NOT NULL 约束迁移 + InterviewService 按模式出题分支）——I1 纵向闭环的最后一块。
2. InterviewHistoryPanel 按模式筛选与快照标签展示。
3. 三模式纵向手工验收（需 AI Provider + 桌面环境）。
4. 面经题集前端管理页（创建/编辑/归档 UI；API 与类型已就绪）。

不自行合并到 main；请 Core Controller 审查集成。
