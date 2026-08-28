# Interview Source Selection and Mode Limits Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make knowledge training and experience simulation use real local sources, expose trustworthy metadata and previews, and enforce distinct question-count limits per interview mode.

**Architecture:** Keep Pipeline, Resume Version, Knowledge Document, and Interview Question Set as independent assets. The setup components receive already-loaded options from `InterviewComposer`; source-specific metadata is added to the API DTOs, while mode limits are centralized in the composer and validated by the backend request service. The UI remains a three-step desktop workspace with real source selection, a narrow preview pane, and mode-specific settings.

**Tech Stack:** Vue 3, TypeScript, Vitest, Spring Boot, Java 21, JdbcTemplate, Flyway (MySQL/H2).

**Spec:** `docs/superpowers/specs/2026-08-26-interview-mode-pages-and-review-design.md` and the approved user requirements in the current task.

## Global Constraints

- Free interview accepts 5–15 questions.
- Knowledge training accepts 1–20 questions.
- Experience simulation accepts the selected set's real size (minimum 1 for a small set; sets with at least 10 items default to a 10-question run), with no fabricated questions.
- Historical sessions keep immutable source/context snapshots.
- AI never fabricates user facts or real interview-source provenance.
- API keys and document contents are never written to logs.
- `main` remains the only long-lived branch; preserve unrelated dirty worktree changes.

### Task 1: Centralize mode limits and draft validation

**Files:**
- Modify: `frontend/src/composables/useInterviewComposer.ts`
- Modify: `frontend/src/components/interview/RoleBasedSetup.vue`
- Modify: `frontend/src/components/interview/KnowledgeTrainingSetup.vue`
- Modify: `frontend/src/components/interview/ExperienceSimulationSetup.vue`
- Test: `frontend/src/composables/useInterviewComposer.test.ts` (or nearest existing composer test)
- Test: the three setup component test files where present

**Interfaces:**
- Produce `INTERVIEW_QUESTION_LIMITS` with `{ ROLE_BASED: { min: 5, max: 15 }, KNOWLEDGE_TRAINING: { min: 1, max: 20 }, EXPERIENCE_SIMULATION: { min: 1, max: 100 } }`; when a real set is selected, its item count becomes the experience maximum and sets of at least 10 default to a 10-question minimum.
- Produce `clampQuestionCount(mode, value, availableCount?)` and use it from all three setup components.

- [x] Write failing tests proving each mode clamps to its own minimum/maximum and experience mode never exceeds the selected question set size.
- [x] Run the focused Vitest tests and confirm they fail for the current shared 3–10 behavior.
- [x] Implement the shared constants/helpers and update steppers, defaults, and request construction.
- [x] Run focused tests, then the complete frontend test suite.
- [ ] Commit as `feat(interview): enforce mode-specific question limits`.

### Task 2: Expose knowledge-document size and category-backed source selection

**Files:**
- Modify: `backend/src/main/java/com/resumego/knowledge/dto/KnowledgeDocumentResponse.java`
- Modify: `backend/src/main/java/com/resumego/knowledge/KnowledgeService.java`
- Modify: `backend/src/main/java/com/resumego/knowledge/KnowledgeRecoveryService.java`
- Modify: `backend/src/main/java/com/resumego/knowledge/KnowledgeClassificationService.java`
- Modify: `frontend/src/types/knowledge.ts`
- Modify: `frontend/src/components/interview/KnowledgeTrainingSetup.vue`
- Modify: `frontend/src/components/interview/InterviewComposer.vue`
- Test: relevant backend Knowledge service/controller tests
- Test: `frontend/src/components/interview/KnowledgeTrainingSetup.test.ts`

**Interfaces:**
- `KnowledgeDocumentResponse.sizeBytes: Long` and `KnowledgeDocument.sizeBytes: number | null`.
- `KnowledgeTrainingSetup` consumes category nodes and emits `open-knowledge-document` with the selected document id.

- [x] Write failing tests for size mapping and category-scoped file selection/search.
- [x] Run focused tests and confirm the missing field/folder behavior fails.
- [x] Map `KnowledgeSourceFile.sizeBytes` into every document response path, including search/classification/retry.
- [x] Load category tree and category-filtered documents; replace the hard-coded breadcrumb with a real folder popover and keep the selected-material strip.
- [x] Add the preview action that navigates through the existing Knowledge Base route contract (verify route/query names before wiring).
- [x] Run backend Knowledge tests, frontend component tests, and build.
- [ ] Commit as `feat(knowledge): connect training setup to local sources`.

### Task 3: Add structured question-set metadata and review mode

**Files:**
- Create: `backend/src/main/resources/db/migration/V43__add_question_set_context_metadata.sql`
- Create: `backend/src/main/resources/db/migration-h2/V23__add_question_set_context_metadata.sql`
- Modify: `backend/src/main/java/com/resumego/interview/dto/InterviewQuestionSetRequest.java`
- Modify: `backend/src/main/java/com/resumego/interview/dto/InterviewQuestionSetResponse.java`
- Modify: `backend/src/main/java/com/resumego/interview/repository/InterviewQuestionSetRepository.java`
- Modify: `backend/src/main/java/com/resumego/interview/service/InterviewQuestionSetService.java`
- Modify: `frontend/src/types/interview.ts`
- Modify: `frontend/src/components/interview/ExperienceSimulationSetup.vue`
- Modify: `frontend/src/composables/useInterviewComposer.ts`
- Test: `backend/src/test/java/com/resumego/interview/**`
- Test: `frontend/src/components/interview/ExperienceSimulationSetup.test.ts`

**Interfaces:**
- Add optional question-set metadata: `companyName`, `targetRole`, `companyIconKey`, plus a trustworthy `questionCount` derived from stored items.
- Add `reviewMode: 'PER_QUESTION' | 'END_OF_SESSION' | 'SOURCE_ONLY'` to experience draft/request and snapshot serialization.

- [x] Write failing tests for metadata round-trip, ownership isolation, and reviewMode request serialization.
- [x] Run focused backend/frontend tests and confirm fields are absent today.
- [x] Add nullable columns/mapping with backward-compatible empty values; never derive company/role from unstructured source notes.
- [x] Render company icon/role/source/updated time/item count, original-question preview, removable focus tags, and review-mode choices.
- [x] Validate that AI follow-ups remain visually distinct from original questions.
- [x] Run migration tests, interview tests, frontend tests, and build.
- [ ] Commit as `feat(interview): add question-set context and review mode`.

### Task 4: Integrate, document, and verify

**Files:**
- Modify: `docs/decisions.md`
- Modify: `docs/product/roadmap.md`
- Modify: `docs/design-qa.md`
- Modify: `frontend/src/views/InterviewView.vue` only if source-open navigation requires composition wiring
- Test: full frontend and backend suites

- [x] Add the approved limits and source-boundary decision to the decision log and roadmap.
- [x] Run `npm run build` in `frontend` and `mvn test` in `backend`.
- [x] Verify the real flows: folder → document preview → open in Knowledge Base; question set → original-question preview → review mode; each mode’s independent step state and limits.
- [x] Record only functional findings and final evidence in `docs/design-qa.md`.
- [ ] Commit as `chore(interview): verify source-bound setup flows`.
