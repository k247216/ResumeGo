# Resume Asset Version Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将基础简历、岗位简历、求职目标绑定和版本说明落实到简历库，同时把预览调整为可读工作区比例并限制滚动范围。

**Architecture:** 复用现有 `GENERAL` / `JOB_EXPRESSION`、`forkedFromVersionId`、`targetJobDescriptionId` 和 `changeSummary` 契约。前端将绑定从原始 select 改为状态卡片 + modal，保存说明通过现有版本接口传递；预览容器独立滚动，变更栏和检查器不随正文滚动。

**Tech Stack:** Vue 3、TypeScript、Element Plus、Vitest、现有 Resume API 与 Pinia targets store。

**Spec:** `docs/superpowers/specs/2026-08-26-resume-asset-version-contract.md`

## Global Constraints

- 基础简历与岗位简历必须使用独立版本时间轴。
- 求职目标创建不得静默创建岗位简历。
- 不得编造用户经历、岗位事实或版本说明。
- 简历正文、版本说明和绑定关系优先保存在本地现有数据层。
- 不改变全局工具栏和日程、知识库等无关模块。

---

### Task 1: 调整可读简历预览并隔离滚动

**Files:**
- Modify: `frontend/src/components/resume-library/ResumeDocumentPreview.vue`
- Modify: `frontend/src/components/resume-library/ResumeCompareToolbar.vue`
- Modify: `frontend/src/views/resumes/ResumeLibraryView.vue`
- Test: `frontend/src/components/resume-library/ResumeDocumentPreview.test.ts`
- Test: `frontend/src/views/resumes/ResumeLibraryView.test.ts`

**Interfaces:**
- Consumes: existing `scale` prop and `studio-preview` test selector.
- Produces: a readable `0.7` paper preview with `overflow:auto` limited to `.studio-preview`; no misleading zoom label is rendered.

- [x] **Step 1: Write failing tests**

```ts
it('keeps the preview readable instead of fitting to a tiny viewport', () => {
  const wrapper = mount(ResumeDocumentPreview, { props: { content: nonEmptyContent, scale: 0.7 } })
  expect(wrapper.find('.paper-holder').attributes('style')).toContain('555.8px')
  expect(wrapper.find('.studio-preview').classes()).toContain('scrollable')
})

it('does not render a misleading zoom control', () => {
  const wrapper = mount(ResumeCompareToolbar, { props: baseToolbarProps })
  expect(wrapper.find('[data-test="zoom-controls"]').exists()).toBe(false)
})
```

- [x] **Step 2: Run tests to verify they fail**

Run: `cd frontend && npm run test -- --run src/components/resume-library/ResumeDocumentPreview.test.ts src/views/resumes/ResumeLibraryView.test.ts`

Expected: FAIL because the preview currently multiplies a fit scale and the toolbar still emits zoom actions.

- [x] **Step 3: Implement the minimal behavior**

Change `ResumeDocumentPreview.vue` so `effectiveScale` defaults to `0.7`, set `.studio-preview` to `overflow:auto`, and keep the 794×1123 paper dimensions scaled by that value. Remove the zoom label and zoom events from `ResumeCompareToolbar.vue`; retain only compare toggle behavior. Keep the page passing the readable scale without making it a user-facing control.

- [x] **Step 4: Run tests and build**

Run: `cd frontend && npm run test -- --run src/components/resume-library/ResumeDocumentPreview.test.ts src/views/resumes/ResumeLibraryView.test.ts && npm run build`

Expected: PASS; only the existing Rolldown and chunk-size warnings may remain.

- [x] **Step 5: Verify manually**

Open `/resumes`, select a non-empty version, confirm the paper is readable and centered, scroll inside the paper viewport, and confirm the page and change column do not move.

### Task 2: Replace binding select with target binding cards and modal

**Files:**
- Modify: `frontend/src/components/resume-library/ResumeVersionInspector.vue`
- Modify: `frontend/src/views/resumes/ResumeLibraryView.vue`
- Create: `frontend/src/components/resume-library/ResumeTargetBindingDialog.vue`
- Test: `frontend/src/components/resume-library/ResumeVersionInspector.test.ts`
- Test: `frontend/src/components/resume-library/ResumeTargetBindingDialog.test.ts`

**Interfaces:**
- Consumes: `availableTargets`, `usedByTargets`, `targetsStore.updateLinks`.
- Produces: bound state summary, explicit “更换绑定” / “解除绑定” actions, and a card-based target picker that emits `confirm(targetId)`.

- [x] **Step 1: Write failing tests**

```ts
it('shows a binding summary instead of a raw select when a target is bound', () => {
  const wrapper = mount(ResumeVersionInspector, { props: { ...props, availableTargets: [{ targetId: 7, label: '腾讯 Java 后端', resumeVersionId: 31 }] } })
  expect(wrapper.find('[data-test="bind-target-select"]').exists()).toBe(false)
  expect(wrapper.get('[data-test="binding-summary"]').text()).toContain('腾讯 Java 后端')
  expect(wrapper.get('[data-test="change-binding"]').exists()).toBe(true)
})

it('emits a selected target from the card dialog', async () => {
  const wrapper = mount(ResumeTargetBindingDialog, { props: { open: true, targets } })
  await wrapper.get('[data-test="target-option-7"]').trigger('click')
  await wrapper.get('[data-test="confirm-binding"]').trigger('click')
  expect(wrapper.emitted('confirm')?.[0]).toEqual([7])
})
```

- [x] **Step 2: Run tests to verify they fail**

Run: `cd frontend && npm run test -- --run src/components/resume-library/ResumeVersionInspector.test.ts src/components/resume-library/ResumeTargetBindingDialog.test.ts`

Expected: FAIL because the inspector currently renders a native select and no dialog component exists.

- [x] **Step 3: Implement the dialog and state card**

Add a compact modal with one selectable card per target, showing company/role label, current stage when available, and the currently bound version. In the inspector, render the summary card when any `availableTargets` row points to the selected version; render a single “绑定到求职目标” action otherwise. Emit `bind-target` only after dialog confirmation. Keep the existing `usedByTargets` list for navigation and make “解除绑定” call the existing `targetsStore.updateLinks(targetId, { resumeVersionId: null })` path.

- [x] **Step 4: Run target tests and build**

Run: `cd frontend && npm run test -- --run src/components/resume-library/ResumeVersionInspector.test.ts src/components/resume-library/ResumeTargetBindingDialog.test.ts src/views/resumes/ResumeLibraryView.test.ts && npm run build`

Expected: PASS.

### Task 3: Capture user-authored version change summaries

**Files:**
- Modify: `frontend/src/composables/useResumeEditor.ts`
- Modify: `frontend/src/views/resumes/ResumeEditorView.vue`
- Modify: `frontend/src/components/resume-library/ResumeVersionInspector.vue`
- Modify: `frontend/src/api/resume.ts`
- Modify: `backend/src/main/java/com/resumego/resume/controller/ResumeController.java`
- Modify: `backend/src/main/java/com/resumego/resume/service/ResumeService.java`
- Modify: `backend/src/main/java/com/resumego/resume/repository/ResumeRepository.java`
- Create: `backend/src/main/java/com/resumego/resume/dto/UpdateResumeVersionSummaryRequest.java`
- Test: `frontend/src/composables/useResumeEditor.test.ts`
- Test: `frontend/src/components/resume-library/ResumeVersionInspector.test.ts`
- Test: `backend/src/test/java/com/resumego/resume/controller/ResumeControllerTest.java`

**Interfaces:**
- Consumes: existing `CreateResumeVersionRequest.changeSummary`.
- Produces: a save-version prompt with optional manual summary; inspector displays and edits the persisted summary without emitting `fork`.

- [x] **Step 1: Write failing tests**

```ts
it('passes the user-entered summary when creating a new version', async () => {
  await saveCurrentVersion({ changeSummary: '补充 Redis 项目量化结果' })
  expect(createResumeVersionMock).toHaveBeenCalledWith(expect.any(Number), expect.objectContaining({ changeSummary: '补充 Redis 项目量化结果' }))
})

it('opens the edit-summary state instead of forking when 编辑说明 is clicked', async () => {
  const wrapper = mount(ResumeVersionInspector, { props: { ...props, selectedVersion: { ...version, changeSummary: '旧说明' } } })
  await wrapper.get('.inline-action').trigger('click')
  expect(wrapper.find('[data-test="change-summary-input"]').exists()).toBe(true)
  expect(wrapper.emitted('fork')).toBeUndefined()
})
```

- [x] **Step 2: Run tests to verify they fail**

Run: `cd frontend && npm run test -- --run src/composables/useResumeEditor.test.ts src/components/resume-library/ResumeVersionInspector.test.ts`

Expected: FAIL because the editor uses hardcoded summaries and the inspector maps “编辑说明” to `fork`.

- [x] **Step 3: Implement summary input and save flow**

Add an optional field to the save-new-version dialog in `ResumeEditorView.vue`. Pass the trimmed value to `createResumeVersion`; if blank, send no summary and let the backend preserve its existing non-fictional fallback. Add `PATCH /api/v1/resume-versions/{versionId}/summary` with `UpdateResumeVersionSummaryRequest(String changeSummary)` and user ownership validation in the existing service/repository. In the inspector, replace the fork emit with an inline edit form containing `change-summary-input`, cancel, and save actions that emits `update-summary`; `ResumeLibraryView.vue` calls the new API and refreshes the selected version. Do not mutate content while editing the note.

- [x] **Step 4: Run tests and build**

Run: `cd frontend && npm run test -- --run src/composables/useResumeEditor.test.ts src/components/resume-library/ResumeVersionInspector.test.ts && npm run build`

Expected: PASS.

### Task 4: Persist asset-level favorite and replace dead “more” action

**Files:**
- Modify: `frontend/src/components/resume-library/ResumeAssetHeader.vue`
- Modify: `frontend/src/components/resume-library/ResumeVersionInspector.vue`
- Test: `frontend/src/components/resume-library/ResumeAssetHeader.test.ts`

**Interfaces:**
- Consumes: stable `resume.id` and existing archive/fork/rename events.
- Produces: local persistent favorite state keyed by asset ID and an actionable compact menu; no JSON copy or placeholder toast.

- [x] **Step 1: Write failing tests**

```ts
it('persists favorite by resume asset id', async () => {
  const wrapper = mount(ResumeAssetHeader, { props: { resume } })
  await wrapper.get('[data-test="favorite-resume"]').trigger('click')
  expect(localStorage.getItem(`resumego:resume-favorite:${resume.id}`)).toBe('true')
})
```

- [x] **Step 2: Run test to verify it fails**

Run: `cd frontend && npm run test -- --run src/components/resume-library/ResumeAssetHeader.test.ts`

Expected: FAIL because favorite is currently transient or not exposed with a stable action.

- [x] **Step 3: Implement minimal persistence**

Use `localStorage` key `resumego:resume-favorite:<resumeId>`, initialize from storage on resume change, and make the inspector’s more action menu expose rename, create job version, archive, and origin actions through existing emits. Keep favorite asset-level; do not copy or modify version content.

- [x] **Step 4: Run target tests and build**

Run: `cd frontend && npm run test -- --run src/components/resume-library/ResumeAssetHeader.test.ts src/components/resume-library/ResumeVersionInspector.test.ts && npm run build`

Expected: PASS.

### Task 5: Documentation and acceptance review

**Files:**
- Modify: `docs/decisions.md`
- Modify: `docs/design-qa.md`

- [x] **Step 1: Record the frozen asset/version decision**

Append a dated decision referencing `docs/superpowers/specs/2026-08-26-resume-asset-version-contract.md` and state that target creation does not auto-fork a resume.

- [x] **Step 2: Add UI acceptance evidence**

Record readable preview, internal scrolling, card binding state, manual change summary, delete-to-trash semantics, and favorite persistence results in `docs/design-qa.md`.

- [ ] **Step 3: Run final verification**

Run: `cd frontend && npm run test -- --run && npm run build && git diff --check`

Expected: all tests pass; build completes with only previously documented warnings; diff check is clean.
