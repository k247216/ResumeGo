# Workbench Shell Phase A Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the feature-menu homepage with a desktop-style workbench that supports first-run, local-library, and active-target states, plus a focused resume editor that excludes retired product features.

**Architecture:** A small Pinia target store owns target loading, selection, creation, and errors. A stable `DesktopShell` renders navigation and local-storage status around route views. `WorkbenchView` composes state-specific components and existing typed APIs. A new `ResumeEditorView` incrementally migrates only core editing capabilities from the legacy `HomeView`; the old mixed page remains as unreachable source until replacement is verified.

**Tech Stack:** Vue 3.5, TypeScript 6, Vue Router 4, Pinia 3, Element Plus, Vitest 4, Vue Test Utils, Spring Boot APIs already implemented by RG-002.

**Spec:** `docs/superpowers/specs/2026-08-19-desktop-workbench-foundation-design.md`

## Global Constraints

- Product copy says “求职目标”; “项目经历” is reserved for resume evidence.
- Stage A does not implement Markdown persistence, H2, Electron, AI extraction, or visual-system redesign.
- AI must not create facts, choose target state, or accept resume changes.
- Loading failures render a retry state and never masquerade as an empty workspace.
- Existing resume editing, versioning, templates, layout, preview, and export remain in scope.
- Job library, job exploration, job recommendations, resume total score, assessment, and matching have no product routes or controls.
- Existing scoring and matching source stays in the repository but is frozen and must not be called by the new workbench.
- The current `main` branch remains buildable and tested after every task.

---

### Task 1: Workspace launch state model

**Files:**
- Modify: `frontend/src/utils/workspaceLaunchState.ts`
- Modify: `frontend/src/utils/workspaceLaunchState.test.ts`

**Interfaces:**
- Produces: `WorkspaceLaunchState = 'loading' | 'error' | 'first-run' | 'library' | 'target'`.
- Produces: `resolveWorkspaceLaunchState({ loading, hasError, resumeCount, targetCount })`.

- [ ] **Step 1: Extend the failing state table**

Add test cases that assert these exact mappings:

```ts
expect(resolveWorkspaceLaunchState({ loading: true, hasError: false, resumeCount: 0, targetCount: 0 })).toBe('loading')
expect(resolveWorkspaceLaunchState({ loading: false, hasError: true, resumeCount: 0, targetCount: 0 })).toBe('error')
expect(resolveWorkspaceLaunchState({ loading: false, hasError: false, resumeCount: 0, targetCount: 0 })).toBe('first-run')
expect(resolveWorkspaceLaunchState({ loading: false, hasError: false, resumeCount: 2, targetCount: 0 })).toBe('library')
expect(resolveWorkspaceLaunchState({ loading: false, hasError: false, resumeCount: 0, targetCount: 1 })).toBe('target')
```

- [ ] **Step 2: Verify RED**

Run: `cd frontend && npm test -- workspaceLaunchState.test.ts`

Expected: FAIL because the existing resolver accepts `jobCount` and only returns `first-run | workspace`.

- [ ] **Step 3: Implement the state resolver**

Return `loading` first, then `error`, then `target` when `targetCount > 0`, then `library` when `resumeCount > 0`, otherwise `first-run`.

- [ ] **Step 4: Verify GREEN**

Run: `cd frontend && npm test -- workspaceLaunchState.test.ts`

Expected: all resolver tests pass.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/utils/workspaceLaunchState.ts frontend/src/utils/workspaceLaunchState.test.ts
git commit -m "test(workbench): define launch states"
```

### Task 2: Target store and persistent selection

**Files:**
- Create: `frontend/src/stores/targets.ts`
- Create: `frontend/src/stores/targets.test.ts`
- Modify: `frontend/src/types/project.ts`

**Interfaces:**
- Consumes: `listProjects()` and `createProject()` from `frontend/src/api/project.ts`.
- Produces: Pinia store `useTargetsStore()` with `targets`, `activeTargetId`, `activeTarget`, `loading`, `errorMessage`, `load()`, `select(id)`, `create(payload)`, and `retry()`.
- Persists: selected ID under `resumego:activeTargetId`; invalid, deleted, or archived selections fall back to the first active target, then the first non-deleted target.

- [ ] **Step 1: Write failing store tests**

Mock `../api/project` with Vitest and assert:

```ts
await store.load()
expect(store.activeTarget?.id).toBe(2)
store.select(3)
expect(localStorage.getItem('resumego:activeTargetId')).toBe('3')
await store.create({ name: '腾讯 · Java 后端实习' })
expect(store.activeTarget?.name).toBe('腾讯 · Java 后端实习')
```

Also reject `select(999)` without changing selection, and assert a failed `listProjects` call sets `errorMessage` while preserving the previous `targets` array.

- [ ] **Step 2: Verify RED**

Run: `cd frontend && npm test -- targets.test.ts`

Expected: FAIL because `useTargetsStore` does not exist.

- [ ] **Step 3: Implement the store**

Use `defineStore('targets', ...)`, computed `activeTarget`, and a single `chooseValidTarget()` helper. Do not write resumes, jobs, or target status in this store.

- [ ] **Step 4: Verify GREEN**

Run: `cd frontend && npm test -- targets.test.ts`

Expected: all target store tests pass without a real backend.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/stores/targets.ts frontend/src/stores/targets.test.ts frontend/src/types/project.ts
git commit -m "feat(workbench): add target context store"
```

### Task 3: Stable desktop-style application shell

**Files:**
- Create: `frontend/src/layouts/DesktopShell.vue`
- Create: `frontend/src/layouts/DesktopShell.test.ts`
- Create: `frontend/src/views/settings/SettingsView.vue`
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/style.css`

**Interfaces:**
- Consumes: route meta `immersive: true` for editor and active interview screens.
- Produces: named routes `workbench`, `targets`, `resumes`, `evidences`, `interview`, `settings`, and `resume-editor`.
- Produces: stable navigation labels `工作台`, `求职目标`, `简历`, `能力证据`, `设置` and local status copy `数据保存在此设备`.

- [ ] **Step 1: Write the failing shell test**

Mount `DesktopShell` with a memory router and assert all five navigation labels, the ResumeGo brand, local-data status, and a default slot are rendered. Assert that no public job market, notification, gift, or user-avatar control is present.

- [ ] **Step 2: Verify RED**

Run: `cd frontend && npm test -- DesktopShell.test.ts`

Expected: FAIL because `DesktopShell.vue` does not exist.

- [ ] **Step 3: Implement the shell and route metadata**

`App.vue` must reduce to:

```vue
<router-view v-if="$route.meta.immersive" />
<DesktopShell v-else><router-view /></DesktopShell>
```

Map `/` to the new workbench, `/targets` to the target list, `/settings` to a local-mode settings summary, and `/editor` to the new `ResumeEditorView.vue` with `meta: { immersive: true }`. The settings summary displays local storage and model-configuration status without adding Electron-only controls. Remove product routes for job library, job creation, job detail, assessment, score, recommendations, and matching; retain their source files for possible future reconsideration.

- [ ] **Step 4: Verify GREEN and build**

Run: `cd frontend && npm test -- DesktopShell.test.ts && npm run build`

Expected: shell test and TypeScript production build pass.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/layouts frontend/src/App.vue frontend/src/router/index.ts frontend/src/style.css
git commit -m "feat(workbench): add desktop application shell"
```

### Task 4: Three-state workbench and target creation

**Files:**
- Create: `frontend/src/views/workbench/WorkbenchView.vue`
- Create: `frontend/src/views/workbench/WorkbenchView.test.ts`
- Create: `frontend/src/components/workbench/WorkspaceLoadingState.vue`
- Create: `frontend/src/components/workbench/WorkspaceErrorState.vue`
- Create: `frontend/src/components/workbench/LocalLibraryState.vue`
- Create: `frontend/src/components/workbench/TargetDashboard.vue`
- Create: `frontend/src/components/targets/TargetCreateDialog.vue`
- Create: `frontend/src/components/targets/TargetCreateDialog.test.ts`
- Create: `frontend/src/views/targets/TargetListView.vue`
- Modify: `frontend/src/components/onboarding/FirstRunEmptyState.vue`
- Modify: `frontend/src/components/onboarding/FirstRunEmptyState.test.ts`
- Modify: `frontend/src/router/index.ts`

**Interfaces:**
- Consumes: target store, `listResumes()`, `resolveWorkspaceLaunchState()`.
- Produces: first-run actions `创建空白简历` and `添加目标岗位`; Markdown import remains absent until Phase B rather than presenting a dead control.
- Produces: library state with resume cards and `针对岗位开始优化`.
- Produces: target state with current target, linked job, linked resume version, deterministic next action, and `打开当前简历`.
- Produces: target creation payload `{ name, resumeVersionId }`; target-specific job content is entered inside the target context rather than selected from a public job library.

- [ ] **Step 1: Write failing workbench state tests**

Mock the target store and APIs, then assert:

```ts
expect(wrapper.get('[data-test="workspace-loading"]').exists()).toBe(true)
expect(wrapper.get('[data-test="workspace-error"]').text()).toContain('重新加载')
expect(wrapper.get('[data-test="first-run-workspace"]').text()).toContain('建立本地资料')
expect(wrapper.get('[data-test="local-library"]').text()).toContain('针对岗位开始优化')
expect(wrapper.get('[data-test="target-dashboard"]').text()).toContain('下一步')
```

Use one test per state; do not connect to Spring Boot.

- [ ] **Step 2: Write failing target dialog tests**

Assert blank names cannot submit; an optional resume selection produces exactly one `create` event with a numeric version ID; API failure text remains visible and the form contents are preserved.

- [ ] **Step 3: Verify RED**

Run: `cd frontend && npm test -- WorkbenchView.test.ts TargetCreateDialog.test.ts`

Expected: FAIL because the workbench components do not exist.

- [ ] **Step 4: Implement the state components and orchestration**

Load targets and resumes in one orchestrator. Derive state only after both settle. Keep each state component presentational; all retry and navigation events return to `WorkbenchView`.

The target dashboard chooses its next action deterministically:

```ts
if (!target.jobDescriptionId) return '添加目标岗位'
if (!target.resumeVersionId) return '选择当前简历'
return '继续定向优化'
```

- [ ] **Step 5: Verify GREEN and build**

Run: `cd frontend && npm test -- WorkbenchView.test.ts TargetCreateDialog.test.ts && npm run build`

Expected: component tests and production build pass.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/views/workbench frontend/src/views/targets frontend/src/components/workbench frontend/src/components/targets frontend/src/components/onboarding/FirstRunEmptyState.vue frontend/src/router/index.ts
git commit -m "feat(workbench): add target-centered launch experience"
```

### Task 5: Focused editor route and context return

**Files:**
- Create: `frontend/src/views/resumes/ResumeEditorView.vue`
- Create: `frontend/src/views/resumes/ResumeEditorView.test.ts`
- Create: `frontend/src/composables/useResumeEditor.ts`
- Create: `frontend/src/composables/useResumeEditor.test.ts`
- Modify: `frontend/src/views/workbench/WorkbenchView.vue`
- Modify: `frontend/src/router/index.ts`
- Create: `frontend/src/utils/editorRoute.ts`
- Create: `frontend/src/utils/editorRoute.test.ts`

**Interfaces:**
- Produces: `buildResumeEditorLocation({ resumeId?, versionId?, targetId?, mode? })` returning route name `resume-editor` and validated positive query values; `mode` only accepts `blank`.
- Consumes: `/editor?resumeId=<id>&versionId=<id>&targetId=<id>`.
- Editor back action returns to `{ name: 'workbench', query: { targetId } }` after the existing unsaved-change guard succeeds.

- [ ] **Step 1: Write failing route helper tests**

Assert positive IDs are serialized, null or non-positive IDs are omitted, `mode: 'blank'` is serialized, and unsupported modes are omitted.

- [ ] **Step 2: Verify RED**

Run: `cd frontend && npm test -- editorRoute.test.ts`

Expected: FAIL because `editorRoute.ts` does not exist.

- [ ] **Step 3: Implement route helper and replace workbench editor links**

Use the helper for first-run blank creation, library resume cards, and the target dashboard current-resume action. `ResumeEditorView` may reuse focused editor components, but must not import or render `HomeView`.

Migrate content editing, version save/history, undo/redo, template and layout controls, real-time preview, and PDF export behind a small editor composable. Do not migrate job library, exploration, recommendations, total score, assessment, matching, AI suggestions, or interview controls into this view.

When `mode=blank`, `ResumeEditorView` enters blank resume mode without requiring a target job. Saving continues to use the existing nullable `targetJobDescriptionId` contract.

- [ ] **Step 4: Update editor exit behavior**

After the dirty-state guard, call `router.push({ name: 'workbench', query: targetId ? { targetId } : {} })`. Remove legacy product routes from the router while retaining their implementation files as unreachable source.

- [ ] **Step 5: Verify targeted and full frontend checks**

Run: `cd frontend && npm test && npm run build`

Expected: all frontend tests pass and the production bundle builds.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/views/resumes frontend/src/composables/useResumeEditor.ts frontend/src/composables/useResumeEditor.test.ts frontend/src/views/workbench/WorkbenchView.vue frontend/src/router/index.ts frontend/src/utils/editorRoute.ts frontend/src/utils/editorRoute.test.ts
git commit -m "refactor(editor): add focused resume workspace"
```

### Task 6: Phase A regression and roadmap closure

**Files:**
- Modify: `docs/roadmap.md`
- Modify: `docs/superpowers/plans/2026-08-19-workbench-shell-phase-a.md`

**Interfaces:**
- Records: RG-003 test infrastructure complete for launch, target creation, and async failure.
- Records: RG-004 workbench shell and focused editor routing complete; legacy mixed-page source remains frozen and unreachable pending safe deletion.

- [ ] **Step 1: Verify all acceptance paths**

Run:

```bash
cd frontend && npm test
cd frontend && npm run build
cd backend && mvn test
git diff --check
```

Expected: zero test failures, successful frontend build, successful backend build, and no whitespace errors.

- [ ] **Step 2: Update roadmap status**

Mark RG-003 complete when first-run, target creation, and async failure tests exist. Mark RG-004 foundation complete while explicitly retaining editor/interview decomposition as follow-up work.

- [ ] **Step 3: Commit**

```bash
git add docs/roadmap.md docs/superpowers/plans/2026-08-19-workbench-shell-phase-a.md
git commit -m "docs(roadmap): close workbench phase a"
```

### Completion note: target-scoped interview decomposition

RG-004 后续收束已于 2026-08-19 完成：

- 面试记录分组、状态与进度计算迁移到 `frontend/src/utils/interviewRecords.ts`；
- 面试评分与训练提示迁移到 `frontend/src/utils/interviewReview.ts`；
- 多会话编辑状态迁移到 `frontend/src/composables/useInterviewSessions.ts`；
- 大厅头部、历史面板、进行中侧栏和整次复盘弹窗迁移到 `frontend/src/components/interview/`；
- 前端完整回归为 28 个测试文件、71 条测试通过；
- 后端完整回归为 421 条测试通过、1 条跳过。

视觉细化与进一步拆分不阻塞 RG-005，后续只在实际维护需求出现时继续，避免为了文件行数进行无目标重构。
