# Resume System V2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有简历库升级为可独立维护通用简历、线性版本和岗位表达副本的本地简历资产系统。

**Architecture:** 复用现有 `resumes` 与不可变 `resume_versions`，在简历资产上增加种类、fork 来源和归档状态；同资产版本继续使用 `parent_version_id`，跨资产来源只使用 `forked_from_version_id`。前端保持 Library → Workspace → Inspector，不让 Pipeline 切换自动改变简历。

**Tech Stack:** Vue 3、TypeScript、Vitest、Spring Boot、JdbcTemplate/MyBatis-Plus、Flyway、H2/MySQL

**Spec:** `docs/superpowers/specs/2026-08-25-resume-interview-workspace-contract.md`

## Global Constraints

- 基线提交为 `3fef48029827971b2eb97a56732ed6d58e32fd28` 或 Core Controller 明确提供的后继提交。
- 不写入或重新解释 `resumes.target_job_description_id`；它仅为历史兼容字段。
- Pipeline 只显式绑定 `resumeVersionId`；本计划不得自动改绑任何 Pipeline。
- 简历版本不可原地覆盖；fork 在服务端复制源版本内容。
- 所有 ID 操作校验当前用户，跨用户按不存在处理。
- 不记录简历正文、联系方式、AI Key 或真实用户数据到日志和测试夹具。
- 不修改 Workspace、Interview、Knowledge、Schedule 页面。

---

### Task 1: 冻结 ResumeAsset 持久化字段和双数据库迁移

**Files:**
- Create: `backend/src/main/resources/db/migration/V39__add_resume_asset_lineage.sql`
- Create: `backend/src/main/resources/db/migration-h2/V19__add_resume_asset_lineage.sql`
- Modify: `backend/src/test/java/com/resumego/resume/repository/ResumeRepositoryTest.java`

**Interfaces:**
- Produces: `resumes.kind VARCHAR(24) NOT NULL DEFAULT 'GENERAL'`、`forked_from_version_id BIGINT NULL`、`archived_at DATETIME/TIMESTAMP NULL`。
- Constraint: `kind IN ('GENERAL','JOB_EXPRESSION')`; `forked_from_version_id` references `resume_versions(id)` with `ON DELETE RESTRICT`.
- Extends the existing `resume_versions.created_by_type` constraint to allow `fork` without weakening the existing values.

- [ ] **Step 1: 写迁移失败测试**

在 `ResumeRepositoryTest` 增加断言：默认资产为 `GENERAL`；岗位表达资产保存来源版本；归档时间可读回。先运行：

```bash
cd backend
mvn -q -Dtest=ResumeRepositoryTest test
```

预期：因字段或 repository 方法不存在失败。

- [ ] **Step 2: 编写 MySQL 与 H2 等价迁移**

两套迁移必须创建相同字段、索引和约束；不得修改或删除历史字段，不得回填虚构来源。现存行统一为 `GENERAL`、来源为空、未归档。

- [ ] **Step 3: 运行 Repository 测试**

```bash
cd backend
mvn -q -Dtest=ResumeRepositoryTest test
```

预期：退出 0。

- [ ] **Step 4: 提交**

```bash
git add backend/src/main/resources/db/migration/V39__add_resume_asset_lineage.sql backend/src/main/resources/db/migration-h2/V19__add_resume_asset_lineage.sql backend/src/test/java/com/resumego/resume/repository/ResumeRepositoryTest.java
git commit -m "feat(resume): add asset lineage fields"
```

### Task 2: 实现资产查询、fork、归档和恢复领域行为

**Files:**
- Create: `backend/src/main/java/com/resumego/resume/dto/ForkResumeVersionRequest.java`
- Create: `backend/src/main/java/com/resumego/resume/dto/UpdateResumeAssetRequest.java`
- Modify: `backend/src/main/java/com/resumego/resume/dto/ResumeDTO.java`
- Modify: `backend/src/main/java/com/resumego/resume/repository/ResumeRepository.java`
- Modify: `backend/src/main/java/com/resumego/resume/service/ResumeService.java`
- Modify: `backend/src/main/java/com/resumego/resume/controller/ResumeController.java`
- Modify: `backend/src/test/java/com/resumego/resume/ResumeServiceTest.java`
- Modify: `backend/src/test/java/com/resumego/resume/controller/ResumeControllerTest.java`

**Interfaces:**
- Produces: `POST /api/v1/resume-versions/{versionId}/fork` with `{ "title": string }`.
- Produces: `PATCH /api/v1/resumes/{resumeId}` with `{ "title": string }`.
- Produces: `POST /api/v1/resumes/{resumeId}/archive` and `/restore`.
- Extends: `ResumeDTO` with `kind`, `forkedFromVersionId`, `archivedAt`.
- Query: `GET /api/v1/resumes?kind=GENERAL|JOB_EXPRESSION&archived=true|false`; omitted `archived` defaults to false.

- [ ] **Step 1: 写 Service RED 测试**

覆盖：fork 从服务端读取源正文；新资产为 `JOB_EXPRESSION` 和 V1；源与副本后续保存互不影响；跨用户、空白标题、归档资产再次归档均无副作用；`parentVersionId` 不跨资产。

```bash
cd backend
mvn -q -Dtest=ResumeServiceTest test
```

预期：新方法或字段不存在而失败。

- [ ] **Step 2: 实现 Repository 原子创建**

提供一个事务内操作：读取当前用户源版本 → 创建 ResumeAsset → 创建 V1 → 更新 `current_version_id`。renderer 只能提交标题，不能提交源正文或来源 ID 之外的谱系字段。

- [ ] **Step 3: 实现线性版本父子校验**

创建新版本时只接受该资产当前版本作为父版本，并确认它属于同一 `resume_id`。历史版本只读；尝试从历史版本普通保存时返回稳定冲突错误并提示 fork，不得在同一资产形成分支。版本号取资产最大值加一。

- [ ] **Step 4: 实现 Controller 与校验**

标题 trim 后长度 1–120；未知 kind 返回 400；跨用户和不存在返回现有 404 契约；归档资产默认列表不可见，恢复后重新出现。

- [ ] **Step 5: 写并运行 Controller 测试**

```bash
cd backend
mvn -q -Dtest=ResumeServiceTest,ResumeControllerTest,ResumeRepositoryTest test
```

预期：退出 0。

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/resumego/resume backend/src/test/java/com/resumego/resume
git commit -m "feat(resume): support explicit expression copies"
```

### Task 3: 收敛前端类型、API 和可测试状态

**Files:**
- Modify: `frontend/src/types/resume.ts`
- Modify: `frontend/src/api/resume.ts`
- Create: `frontend/src/composables/useResumeLibrary.ts`
- Create: `frontend/src/composables/useResumeLibrary.test.ts`

**Interfaces:**
- Produces: `ResumeKind = 'GENERAL' | 'JOB_EXPRESSION'`.
- Produces: `forkResumeVersion(versionId: number, title: string): Promise<ApiResponse<Resume>>`.
- Produces: `archiveResume(resumeId: number)`、`restoreResume(resumeId: number)`、`renameResume(resumeId: number, title: string)`.
- Produces composable state: `items`, `selectedResumeId`, `filter`, `loading`, `error`, `load`, `select`, `fork`, `archive`, `restore`.

- [ ] **Step 1: 写 composable RED 测试**

覆盖加载、选择保持、过滤、fork 后选中新资产、归档后移除、失败保留原状态和重试。

```bash
cd frontend
npx vitest run src/composables/useResumeLibrary.test.ts
```

- [ ] **Step 2: 扩展类型和 API client**

禁止在组件中重复声明 DTO；错误沿用 `apiFetch` 和现有 `ApiResponse` 处理方式。

- [ ] **Step 3: 实现 composable 并运行测试**

```bash
cd frontend
npx vitest run src/composables/useResumeLibrary.test.ts
```

- [ ] **Step 4: 提交**

```bash
git add frontend/src/types/resume.ts frontend/src/api/resume.ts frontend/src/composables/useResumeLibrary.ts frontend/src/composables/useResumeLibrary.test.ts
git commit -m "feat(resume): add library asset state"
```

### Task 4: 重构简历库为真实资产工作区

**Files:**
- Create: `frontend/src/components/resume-library/ResumeAssetList.vue`
- Create: `frontend/src/components/resume-library/ResumeAssetWorkspace.vue`
- Create: `frontend/src/components/resume-library/ResumeVersionInspector.vue`
- Create: `frontend/src/components/resume-library/ResumeForkDialog.vue`
- Create: `frontend/src/components/resume-library/ResumeArchiveDialog.vue`
- Create: `frontend/src/components/resume-library/ResumeAssetList.test.ts`
- Create: `frontend/src/components/resume-library/ResumeVersionInspector.test.ts`
- Modify: `frontend/src/views/resumes/ResumeLibraryView.vue`
- Modify: `frontend/src/views/resumes/ResumeLibraryView.test.ts`

**Interfaces:**
- Consumes: Task 3 composable and API.
- Produces: Library → Workspace → Inspector 页面；页面只组合状态和路由。
- Routes: 编辑使用现有 `resume-editor` 路由和明确 `versionId`; 预览使用现有预览能力，不在库页复制编辑器。

- [ ] **Step 1: 写页面行为 RED 测试**

测试真实用户结果：通用/岗位表达标签；V1/V2 是版本而非独立简历；历史版本只读；创建副本弹窗显示源标题和 Vn；fork 后源资产不变；归档确认；空/失败/重试；窄 Inspector 可关闭。

```bash
cd frontend
npx vitest run src/views/resumes/ResumeLibraryView.test.ts src/components/resume-library
```

- [ ] **Step 2: 实现 Asset List**

列表行只展示标题、种类、当前 Vn 和更新时间；不显示完整简历缩略图，不把绿色用于装饰。搜索和过滤不制造客户端假关联。

- [ ] **Step 3: 实现 Workspace 与 Inspector**

Workspace 主操作为继续编辑；次操作为预览和创建岗位表达副本。Inspector 展示完整版本历史、来源版本与真实引用摘要；数据缺失显示诚实空态。

- [ ] **Step 4: 实现 fork/归档流程**

操作包含 loading、失败和重试；成功后更新列表并选中真实返回对象，不使用乐观伪 ID。

- [ ] **Step 5: 运行前端验证**

```bash
cd frontend
npx vitest run src/composables/useResumeLibrary.test.ts src/views/resumes/ResumeLibraryView.test.ts src/components/resume-library
npm run build
```

预期：全部退出 0。

- [ ] **Step 6: 提交**

```bash
git add frontend/src/components/resume-library frontend/src/views/resumes/ResumeLibraryView.vue frontend/src/views/resumes/ResumeLibraryView.test.ts
git commit -m "feat(resume): redesign local asset library"
```

### Task 5: 模块纵向验收与交付

**Files:**
- Modify: `docs/testing/v2-final-test-matrix.md`
- Create: `docs/agent-messages/from-dsh/20260825-delivery-resume-system-v2.md`

**Interfaces:**
- Consumes: Tasks 1–4.
- Produces: Core Controller 可复跑的交付报告和测试证据。

- [ ] **Step 1: 执行后端模块测试**

```bash
cd backend
mvn -q -Dtest=ResumeRepositoryTest,ResumeServiceTest,ResumeControllerTest test
```

- [ ] **Step 2: 执行前端模块测试和构建**

```bash
cd frontend
npx vitest run src/composables/useResumeLibrary.test.ts src/views/resumes/ResumeLibraryView.test.ts src/views/resumes/ResumeEditorView.test.ts src/components/resume-library
npm run build
```

- [ ] **Step 3: 手工验证四条真实流程**

使用虚构资料完成：新建并保存 V2；从 V2 fork 岗位表达副本；分别修改源和副本确认隔离；绑定 Pipeline 后产生新版本确认不自动改绑。记录实际 ID/Vn 关系但不得记录正文或联系方式。

- [ ] **Step 4: 交付而不自行集成**

报告最终提交、迁移版本、接口、测试命令/退出码/数量、未执行项和风险。Core Controller 负责审查、集成和全量回归。
