# Career Pipeline Backend Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a versioned V2 backend API that creates multiple career pipelines, preserves configurable ordered stages, records deterministic stage transitions and archives pipelines without changing the V1 project API.

**Architecture:** Add a new `com.resumego.pipeline` module and additive H2/MySQL migrations. The V2 module owns pipeline lifecycle and stage-history rules; legacy `job_projects` remains untouched as a future migration source. JDBC repositories enforce the local-user boundary and the service performs all deterministic state transitions in one transaction.

**Tech Stack:** Java 21, Spring Boot 3.5, JdbcTemplate, H2 2.3, MySQL 8 migration syntax, JUnit 5, AssertJ, MockMvc

**Spec:** `docs/superpowers/specs/2026-08-22-career-os-v2-foundation-design.md`

## Global Constraints

- API base path is `/api/v2/pipelines`; `/api/v1/projects` behavior remains unchanged.
- Pipeline lifecycle values are `ACTIVE`, `PAUSED`, `CLOSED`, `ARCHIVED`.
- Closed outcomes are `OFFER`, `REJECTED`, `WITHDRAWN`, `OTHER`; only `CLOSED` may have an outcome.
- Stage state values are `PENDING`, `CURRENT`, `COMPLETED`, `SKIPPED`.
- New pipelines start `ACTIVE`; the first ordered stage starts `CURRENT`; remaining stages start `PENDING`.
- Stage transitions are append-only and use actor `USER`, `SYSTEM` or `MIGRATION`; AI never changes lifecycle or stage state.
- The first slice accepts custom stage names at creation but does not yet expose post-creation stage editing or hard deletion.
- All database changes are additive and V1 records remain readable.
- External model calls and real personal data are forbidden in tests.

---

### Task 1: Define deterministic pipeline domain rules

**Files:**
- Create: `backend/src/main/java/com/resumego/pipeline/PipelineLifecycle.java`
- Create: `backend/src/main/java/com/resumego/pipeline/PipelineOutcome.java`
- Create: `backend/src/main/java/com/resumego/pipeline/PipelineStageState.java`
- Create: `backend/src/main/java/com/resumego/pipeline/PipelineRules.java`
- Create: `backend/src/test/java/com/resumego/pipeline/PipelineRulesTest.java`

**Interfaces:**
- Produces: `PipelineRules.validateLifecycle(PipelineLifecycle, PipelineOutcome)` and `PipelineRules.validateStageTransition(long currentStageId, long targetStageId, PipelineLifecycle lifecycle)`.
- Consumers: `CareerPipelineService` in Task 3.

- [ ] **Step 1: Write failing rule tests**

Test these literal behaviors:

```java
assertThatCode(() -> rules.validateLifecycle(PipelineLifecycle.CLOSED, PipelineOutcome.OFFER))
        .doesNotThrowAnyException();
assertThatThrownBy(() -> rules.validateLifecycle(PipelineLifecycle.ACTIVE, PipelineOutcome.OFFER))
        .isInstanceOf(IllegalArgumentException.class);
assertThatThrownBy(() -> rules.validateStageTransition(11L, 11L, PipelineLifecycle.ACTIVE))
        .isInstanceOf(IllegalArgumentException.class);
assertThatThrownBy(() -> rules.validateStageTransition(11L, 12L, PipelineLifecycle.ARCHIVED))
        .isInstanceOf(IllegalStateException.class);
assertThatCode(() -> rules.validateStageTransition(11L, 12L, PipelineLifecycle.ACTIVE))
        .doesNotThrowAnyException();
```

- [ ] **Step 2: Run RED**

Run: `cd backend && mvn -Dtest=PipelineRulesTest test`

Expected: FAIL because the pipeline domain types do not exist.

- [ ] **Step 3: Implement minimal enums and rules**

Rules reject outcomes outside `CLOSED`, reject a missing outcome for `CLOSED`, reject same-stage transitions and reject transitions for `CLOSED` or `ARCHIVED` pipelines.

- [ ] **Step 4: Run GREEN**

Run: `cd backend && mvn -Dtest=PipelineRulesTest test`

Expected: all `PipelineRulesTest` tests pass.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/resumego/pipeline backend/src/test/java/com/resumego/pipeline/PipelineRulesTest.java
git commit -m "feat(pipeline): define lifecycle rules"
```

### Task 2: Add additive Pipeline persistence schema

**Files:**
- Create: `backend/src/main/resources/db/migration-h2/V4__career_pipeline_foundation.sql`
- Create: `backend/src/main/resources/db/migration/V24__career_pipeline_foundation.sql`
- Modify: `backend/src/test/java/com/resumego/migration/H2FileWorkspaceMigrationTest.java`

**Interfaces:**
- Produces tables `career_pipelines`, `pipeline_stages`, `pipeline_stage_transitions`.
- Foreign keys reference `users`, `job_descriptions`, `resume_versions` and pipeline records without altering V1 tables.

- [ ] **Step 1: Extend the migration test first**

After migrating an empty H2 workspace, assert that all three tables exist. After closing and reopening the same file database, assert Flyway remains at version `4` and the tables are still readable.

- [ ] **Step 2: Run RED**

Run: `cd backend && mvn -Dtest=H2FileWorkspaceMigrationTest test`

Expected: FAIL because migration version 4 and its tables do not exist.

- [ ] **Step 3: Add the H2 and MySQL migrations**

`career_pipelines` stores user, name, company, role, JD, selected resume version, lifecycle, outcome, current stage and timestamps. `pipeline_stages` stores pipeline, name, zero-based position and state. `pipeline_stage_transitions` stores from/to stage, actor, optional note and occurrence time. Add ownership/listing indexes and checks for all frozen enum values.

- [ ] **Step 4: Run GREEN**

Run: `cd backend && mvn -Dtest=H2FileWorkspaceMigrationTest test`

Expected: migration and reopen tests pass at schema version 4.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/db/migration-h2/V4__career_pipeline_foundation.sql backend/src/main/resources/db/migration/V24__career_pipeline_foundation.sql backend/src/test/java/com/resumego/migration/H2FileWorkspaceMigrationTest.java
git commit -m "feat(pipeline): add persistence schema"
```

### Task 3: Implement pipeline creation, reads and transitions

**Files:**
- Create: `backend/src/main/java/com/resumego/pipeline/CareerPipeline.java`
- Create: `backend/src/main/java/com/resumego/pipeline/PipelineStage.java`
- Create: `backend/src/main/java/com/resumego/pipeline/PipelineStageTransition.java`
- Create: `backend/src/main/java/com/resumego/pipeline/CareerPipelineRepository.java`
- Create: `backend/src/main/java/com/resumego/pipeline/CareerPipelineService.java`
- Create: `backend/src/main/java/com/resumego/pipeline/dto/CreateCareerPipelineRequest.java`
- Create: `backend/src/main/java/com/resumego/pipeline/dto/TransitionPipelineStageRequest.java`
- Create: `backend/src/main/java/com/resumego/pipeline/dto/CareerPipelineResponse.java`
- Create: `backend/src/main/java/com/resumego/pipeline/dto/PipelineStageResponse.java`
- Create: `backend/src/test/resources/sql/career_pipeline_schema.sql`
- Create: `backend/src/test/java/com/resumego/pipeline/CareerPipelineRepositoryTest.java`
- Create: `backend/src/test/java/com/resumego/pipeline/CareerPipelineServiceTest.java`

**Interfaces:**
- Produces: `list()`, `get(long)`, `create(CreateCareerPipelineRequest)`, `transition(long, TransitionPipelineStageRequest)`, `archive(long)` and `restore(long)`.
- The response includes ordered stages and current stage id; transition history remains internal until a later history endpoint.

- [ ] **Step 1: Write failing repository tests**

Prove user-scoped reads, creation of two independent pipelines, ordered stages, current-stage persistence and append-only transition rows using a real H2 JDBC test schema.

- [ ] **Step 2: Run repository RED**

Run: `cd backend && mvn -Dtest=CareerPipelineRepositoryTest test`

Expected: FAIL because repository and records do not exist.

- [ ] **Step 3: Implement the minimal JDBC repository**

Use generated keys and one transaction owned by the service. Repository methods accept `userId` for every pipeline lookup. Creation inserts the pipeline, stages and initial `NULL → first stage` transition with actor `USER`.

- [ ] **Step 4: Run repository GREEN**

Run: `cd backend && mvn -Dtest=CareerPipelineRepositoryTest test`

Expected: repository tests pass.

- [ ] **Step 5: Write failing service tests**

Prove normalization, duplicate/blank stage rejection, default stage creation, foreign-asset ownership validation, deterministic transition, archive blocking transitions and restore returning an active pipeline.

- [ ] **Step 6: Run service RED**

Run: `cd backend && mvn -Dtest=CareerPipelineServiceTest test`

Expected: FAIL because service behavior is incomplete.

- [ ] **Step 7: Implement the minimal service and DTOs**

Use `CurrentUser.DEMO_USER_ID`, normalize text at the boundary, validate JD/resume ownership through repository queries, call `PipelineRules` before writes and return ordered stage projections.

- [ ] **Step 8: Run service and repository GREEN**

Run: `cd backend && mvn -Dtest=CareerPipelineRepositoryTest,CareerPipelineServiceTest test`

Expected: both test classes pass.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/resumego/pipeline backend/src/test/java/com/resumego/pipeline backend/src/test/resources/sql/career_pipeline_schema.sql
git commit -m "feat(pipeline): add core application service"
```

### Task 4: Expose the versioned V2 API

**Files:**
- Create: `backend/src/main/java/com/resumego/pipeline/CareerPipelineController.java`
- Create: `backend/src/test/java/com/resumego/pipeline/CareerPipelineControllerTest.java`

**Interfaces:**
- `GET /api/v2/pipelines`
- `GET /api/v2/pipelines/{id}`
- `POST /api/v2/pipelines`
- `POST /api/v2/pipelines/{id}/transitions`
- `POST /api/v2/pipelines/{id}/archive`
- `POST /api/v2/pipelines/{id}/restore`

- [ ] **Step 1: Write failing MockMvc tests**

Cover list, create, transition, archive, restore, blank company/role/stage validation, missing pipeline 404 and invalid transition 400. Assertions use returned JSON behavior, not mocked call counts.

- [ ] **Step 2: Run RED**

Run: `cd backend && mvn -Dtest=CareerPipelineControllerTest test`

Expected: FAIL because the V2 routes do not exist.

- [ ] **Step 3: Implement the controller**

Use `ApiResponse`, `@Valid`, HTTP 201 for creation and stable 400/404 responses matching existing API conventions.

- [ ] **Step 4: Run GREEN and backend regression**

Run: `cd backend && mvn -Dtest=CareerPipelineControllerTest test && mvn test`

Expected: controller tests and the complete backend suite pass.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/resumego/pipeline/CareerPipelineController.java backend/src/test/java/com/resumego/pipeline/CareerPipelineControllerTest.java
git commit -m "feat(pipeline): expose V2 API"
```

### Task 5: Record the delivered backend boundary

**Files:**
- Modify: `docs/product/roadmap.md`
- Modify: `docs/architecture/architectureV2.md`

**Interfaces:**
- Produces: verified F1 backend progress and documents `/api/v2/pipelines` as the new boundary while retaining `/api/v1/projects` for V1 migration.

- [ ] **Step 1: Run final verification**

Run: `cd backend && mvn test`

Run: `cd frontend && npm run build:electron && npm test && npm run build`

Expected: all tests and builds pass.

- [ ] **Step 2: Update documentation with verified facts**

Record the API, schema version, supported lifecycle/transition behavior and remaining F1 work: post-creation stage editing, schedule/interview links, V1 migration and Pipeline UI.

- [ ] **Step 3: Check and commit**

Run: `git diff --check && git status --short`

```bash
git add docs/product/roadmap.md docs/architecture/architectureV2.md
git commit -m "docs(pipeline): record backend foundation"
```
