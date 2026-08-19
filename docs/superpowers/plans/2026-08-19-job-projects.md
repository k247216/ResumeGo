# Job Projects Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the minimal local-user job project model and APIs for create, read, rename, link, archive, restore, and soft delete.

**Architecture:** Add a focused `project` backend module backed by one `job_projects` table. The service owns lifecycle and ownership validation; the repository owns SQL and always scopes reads to `user_id` plus `deleted_at IS NULL`. Frontend work in this slice is limited to shared API types and functions; project-centered screens remain RG-004.

**Tech Stack:** Java 21, Spring Boot, JdbcTemplate, Flyway, JUnit 5, Mockito, Vue 3 TypeScript, Axios.

**Spec:** `docs/features/job-projects.md`

## Global Constraints

- Do not implement or change resume score weights, job ranking, or interview state transitions.
- Do not cascade project deletion into resumes, jobs, evidence, or interviews.
- Every project, job, and resume-version lookup must be scoped to the fixed local user.
- Use soft deletion for projects and explicit deterministic archive/restore commands.
- Do not send project data to an external model.

---

### Task 1: Database migration and repository

**Files:**
- Create: `backend/src/main/resources/db/migration/V21__create_job_projects.sql`
- Create: `backend/src/main/java/com/resumego/project/JobProjectRepository.java`
- Create: `backend/src/main/java/com/resumego/project/JobProject.java`
- Create: `backend/src/test/resources/sql/job_projects_schema.sql`
- Create: `backend/src/test/java/com/resumego/project/JobProjectRepositoryTest.java`

**Interfaces:**
- Produces: repository methods `create`, `findAll`, `findById`, `rename`, `updateLinks`, `archive`, `restore`, `softDelete`, `ownsJobDescription`, and `ownsResumeVersion`.

- [ ] Write repository tests proving user scoping, link ownership, lifecycle changes, and preservation of linked rows after soft delete.
- [ ] Run `mvn -Dtest=JobProjectRepositoryTest test` and verify failure because the project repository and schema do not exist.
- [ ] Add the V21 table with nullable `job_description_id` and `resume_version_id` foreign keys, lifecycle timestamps, indexes, and a status check.
- [ ] Implement the minimal JdbcTemplate repository with every project query scoped by user and `deleted_at IS NULL`.
- [ ] Run `mvn -Dtest=JobProjectRepositoryTest test` and verify all repository tests pass.

### Task 2: Service, validation, and REST API

**Files:**
- Create: `backend/src/main/java/com/resumego/project/JobProjectService.java`
- Create: `backend/src/main/java/com/resumego/project/JobProjectController.java`
- Create: `backend/src/main/java/com/resumego/project/dto/CreateJobProjectRequest.java`
- Create: `backend/src/main/java/com/resumego/project/dto/RenameJobProjectRequest.java`
- Create: `backend/src/main/java/com/resumego/project/dto/UpdateJobProjectLinksRequest.java`
- Create: `backend/src/main/java/com/resumego/project/dto/JobProjectResponse.java`
- Create: `backend/src/test/java/com/resumego/project/JobProjectServiceTest.java`
- Create: `backend/src/test/java/com/resumego/project/JobProjectControllerTest.java`

**Interfaces:**
- Consumes: Task 1 repository and entity.
- Produces: `/api/v1/projects` endpoints specified in `docs/features/job-projects.md`.

- [ ] Write service tests for name normalization, ownership rejection, explicit archive/restore, missing projects, and soft deletion.
- [ ] Write controller tests for status codes and request validation on every endpoint family.
- [ ] Run the two test classes and verify they fail because service/controller types do not exist.
- [ ] Implement request records with Jakarta Validation, response mapping, transactional service commands, and thin controller handlers.
- [ ] Run the service and controller tests and verify they pass.

### Task 3: Frontend contract and roadmap closure

**Files:**
- Create: `frontend/src/types/project.ts`
- Create: `frontend/src/api/project.ts`
- Modify: `docs/roadmap.md`

**Interfaces:**
- Consumes: Task 2 REST paths and JSON fields.
- Produces: typed functions `listProjects`, `getProject`, `createProject`, `renameProject`, `updateProjectLinks`, `archiveProject`, `restoreProject`, and `deleteProject`.

- [ ] Add exact TypeScript request/response types and Axios API functions matching the backend contract.
- [ ] Run `npm run build` to prove the frontend contract type-checks.
- [ ] Mark RG-002 complete and record that project-centered UI remains RG-004.
- [ ] Run `npm test`, `npm run build`, `mvn test`, and `git diff --check`; verify zero failures before creating the RG-002 checkpoint commit.
