# Career OS V2 Agent Governance Rollout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the approved V2 multi-agent governance design into enforceable repository rules, reusable task and delivery templates, a controlled workstream registry, two dispatch-ready feature tasks and a final integration test matrix.

**Architecture:** Governance assets live under `docs/architecture`, `docs/templates`, `docs/product`, `docs/tasks/v2` and `docs/testing`. The product-control documents define authority and state; individual task cards freeze file ownership and test contracts without changing product behavior. Only tasks whose public contracts are already stable may enter `READY`.

**Tech Stack:** Markdown, Git short-lived branches/worktrees, Java 21/Spring Boot test commands, Vue 3/TypeScript/Vitest build commands, Electron packaging commands

**Spec:** `docs/superpowers/specs/2026-08-22-v2-multi-agent-product-governance-design.md`

## Global Constraints

- `codex/v2-career-os` is the V2 integration branch; feature agents never commit directly to `main`, the V1 maintenance line or the V2 integration worktree.
- Every feature task names one exact base commit, one short-lived branch, owned files, frozen contracts and real verification commands.
- Database migrations, shared DTOs, domain state machines, cross-module ports, Electron identity and product/architecture decisions remain core-owned unless a task explicitly grants access.
- Agents must demonstrate RED before production code and report exact GREEN commands and results.
- V1 data is read-only migration input; local personal data, secrets and real model calls are forbidden in tests.
- The core controller owns integration, full regression, desktop build and release evidence.

---

### Task 1: Publish collaboration authority and the durable decision

**Files:**
- Create: `docs/architecture/agent-collaboration.md`
- Modify: `docs/decisions.md`

**Interfaces:**
- Consumes: the approved authority, branch, scope, privacy and rejection rules from the governance spec.
- Produces: the normative policy referenced by every V2 task card and a durable decision explaining central control.

- [ ] **Step 1: Write the collaboration policy**

Create sections with these literal responsibilities: Core Controller, Feature Agent, branch/worktree isolation, protected artifacts, scope escalation, TDD evidence, delivery evidence and direct rejection conditions. State that any conflict between a task card and this policy must be resolved by the Core Controller before coding.

- [ ] **Step 2: Record the decision**

Append a dated decision named `V2 采用中央总控、多分支隔离与统一集成` to `docs/decisions.md`. Record why working code alone is insufficient and the impact on branch ownership, public contracts and final testing.

- [ ] **Step 3: Validate the documents**

Run:

```bash
rg -n "Core Controller|Feature Agent|protected|RED|GREEN|退回" docs/architecture/agent-collaboration.md
rg -n "中央总控、多分支隔离与统一集成" docs/decisions.md
git diff --check
```

Expected: every required policy concept appears and `git diff --check` exits `0`.

- [ ] **Step 4: Commit**

```bash
git add docs/architecture/agent-collaboration.md docs/decisions.md
git commit -m "docs(governance): establish V2 collaboration authority"
```

### Task 2: Create executable task and delivery templates

**Files:**
- Create: `docs/templates/v2-feature-task-card.md`
- Create: `docs/templates/v2-agent-delivery-report.md`

**Interfaces:**
- Consumes: `docs/architecture/agent-collaboration.md`.
- Produces: one complete task-card schema and one evidence-based handoff schema used by all feature agents.

- [ ] **Step 1: Write the task-card template**

Include exact fields for task ID, owner, milestone, status, base commit, branch, user result, dependencies, scope in/out, allowed create/modify/test files, frozen contracts, data/privacy/AI impact, observable behavior table, failing tests, verification commands, commit rule, delivery format and integration gates.

- [ ] **Step 2: Write the delivery-report template**

Include exact fields for branch, base and final commit, completed behavior, changed files, interfaces, RED evidence, GREEN evidence, full module regression, database impact, privacy/AI impact, omissions, risks, unexplained worktree changes and decisions requested from the Core Controller.

- [ ] **Step 3: Validate template completeness**

Run:

```bash
rg -n "Base commit|Allowed files|Frozen contracts|RED evidence|GREEN evidence|Known risks" docs/templates/v2-*.md
rg -n "TBD|TODO|适当处理|按现有方式" docs/templates/v2-*.md
git diff --check
```

Expected: required fields exist, the placeholder scan returns no matches, and `git diff --check` exits `0`.

- [ ] **Step 4: Commit**

```bash
git add docs/templates/v2-feature-task-card.md docs/templates/v2-agent-delivery-report.md
git commit -m "docs(governance): add V2 task handoff templates"
```

### Task 3: Register workstreams and publish the first dispatch-ready tasks

**Files:**
- Create: `docs/product/workstreams.md`
- Create: `docs/tasks/v2/ready/V2-F1-BE-01-pipeline-transition-history.md`
- Create: `docs/tasks/v2/ready/V2-F1-FE-01-pipeline-api-client.md`
- Create: `docs/tasks/v2/queued/V2-F1-FE-02-pipeline-store.md`

**Interfaces:**
- Consumes: current V2 commit, `/api/v2/pipelines`, the pipeline backend module and existing frontend `api/http.ts` conventions.
- Produces: one registry with file ownership/dependencies, two tasks safe to execute in parallel and one dependency-blocked follow-up task.

- [ ] **Step 1: Create the workstream registry**

Register `V2-F0-MIG-01` as `CORE_RESERVED`, `V2-F1-BE-01` and `V2-F1-FE-01` as `READY`, `V2-F1-FE-02` as `QUEUED`, and Pipeline page implementation as `DESIGN_REQUIRED`. Include base commit, branch name, owned files, dependencies and integration order for every entry.

- [ ] **Step 2: Create backend task `V2-F1-BE-01`**

Authorize only:

```text
backend/src/main/java/com/resumego/pipeline/CareerPipelineRepository.java
backend/src/main/java/com/resumego/pipeline/CareerPipelineService.java
backend/src/main/java/com/resumego/pipeline/CareerPipelineController.java
backend/src/main/java/com/resumego/pipeline/dto/PipelineStageTransitionResponse.java
backend/src/test/java/com/resumego/pipeline/CareerPipelineRepositoryTest.java
backend/src/test/java/com/resumego/pipeline/CareerPipelineServiceTest.java
backend/src/test/java/com/resumego/pipeline/CareerPipelineControllerTest.java
```

Freeze the endpoint as `GET /api/v2/pipelines/{id}/transitions`. Require ordered append-only history, current-user ownership, `404` for an unavailable Pipeline and no migration. Require targeted RED/GREEN tests plus `mvn test`.

- [ ] **Step 3: Create frontend task `V2-F1-FE-01`**

Authorize only:

```text
frontend/src/types/pipeline.ts
frontend/src/api/pipeline.ts
frontend/src/api/pipeline.test.ts
```

Freeze the client around current routes for list/get/create, stage add/rename/reorder/transition, archive/restore, schedule links and interview-plan links. Require typed `ApiResponse` unwrapping through existing HTTP conventions, no Pinia/UI changes, mocked HTTP tests, `npm test -- pipeline.test.ts`, full `npm test` and `npm run build`.

- [ ] **Step 4: Create queued frontend task `V2-F1-FE-02`**

Make it depend on the integrated final commit of `V2-F1-FE-01`. Restrict it to a new Pipeline store and its test. Do not authorize route or visual implementation. State that its base commit will be filled with a real integration commit only when promoted from `QUEUED` to `READY`.

- [ ] **Step 5: Validate ownership and readiness**

Run:

```bash
rg -n "CORE_RESERVED|READY|QUEUED|DESIGN_REQUIRED" docs/product/workstreams.md
rg -n "Base commit|Allowed files|Frozen contracts|Verification commands" docs/tasks/v2/ready/*.md
rg -n "TBD|TODO|适当处理|按现有方式" docs/product/workstreams.md docs/tasks/v2
git diff --check
```

Expected: two and only two tasks are `READY`; queued work has an explicit dependency and no fake base commit; no placeholder wording is present.

- [ ] **Step 6: Commit**

```bash
git add docs/product/workstreams.md docs/tasks/v2
git commit -m "docs(workstreams): dispatch first V2 feature tasks"
```

### Task 4: Publish integration review and final test matrices

**Files:**
- Create: `docs/testing/v2-integration-review-checklist.md`
- Create: `docs/testing/v2-final-test-matrix.md`

**Interfaces:**
- Consumes: V2 completion standards, desktop operations and the governance spec.
- Produces: repeatable accept/reject review and milestone/release verification procedures.

- [ ] **Step 1: Write the integration checklist**

Include gates for base-commit ancestry, file authorization, public-contract drift, domain ownership, transaction/deletion behavior, privacy, RED/GREEN evidence, module regression, build impact, migrations, documentation, worktree cleanliness and accept/conditional/reject outcome.

- [ ] **Step 2: Write the final test matrix**

Define commands and evidence for backend full tests, frontend full tests, Vite build, Electron build, empty-workspace startup, upgrade/reopen, V1 read-only import, backup/restore, Pipeline workflows, Knowledge workflows, Interview workflows, Workspace actions, secrets/log inspection and Windows/macOS package smoke tests. Mark unimplemented product flows as `NOT_RUN_UNTIL_IMPLEMENTED`, never as passing.

- [ ] **Step 3: Validate test responsibility coverage**

Run:

```bash
rg -n "mvn test|npm test|npm run build|build:electron|V1|backup|Windows|macOS|NOT_RUN_UNTIL_IMPLEMENTED" docs/testing/v2-*.md
rg -n "TBD|TODO|适当处理|按现有方式" docs/testing/v2-*.md
git diff --check
git status --short
```

Expected: every required verification class appears, the placeholder scan is empty, diff check exits `0`, and only this task's two files are uncommitted.

- [ ] **Step 4: Commit**

```bash
git add docs/testing/v2-integration-review-checklist.md docs/testing/v2-final-test-matrix.md
git commit -m "docs(testing): define V2 integration gates"
```

### Task 5: Verify the governance rollout as one system

**Files:**
- Verify: all files created in Tasks 1-4

**Interfaces:**
- Consumes: all governance assets.
- Produces: a clean V2 integration worktree with two dispatch-ready tasks and one authoritative final-test process.

- [ ] **Step 1: Check references and forbidden placeholders**

```bash
test -f docs/architecture/agent-collaboration.md
test -f docs/templates/v2-feature-task-card.md
test -f docs/templates/v2-agent-delivery-report.md
test -f docs/product/workstreams.md
test -f docs/testing/v2-integration-review-checklist.md
test -f docs/testing/v2-final-test-matrix.md
test "$(find docs/tasks/v2/ready -name '*.md' | wc -l | tr -d ' ')" = "2"
! rg -n "TBD|TODO|适当处理|按现有方式" docs/architecture/agent-collaboration.md docs/templates docs/product/workstreams.md docs/tasks/v2 docs/testing/v2-*.md
git diff --check
```

Expected: every command exits `0`.

- [ ] **Step 2: Confirm repository state**

```bash
git status --short
git log -5 --oneline
```

Expected: the worktree is clean and the governance rollout commits are visible.
