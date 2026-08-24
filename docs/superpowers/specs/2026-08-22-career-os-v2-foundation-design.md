# Career OS V2 Foundation Design

## Status

Approved product direction. This document defines the first executable V2 architecture slice.

## Product outcome

V2 establishes a local-first Career OS in which durable career knowledge can be reused inside a real job pipeline, an interview can produce traceable feedback, and the workspace can expose one explainable next action.

The first foundation release does not attempt to complete every V2 screen. It establishes stable domain ownership and one vertical path that later releases can extend.

## Version isolation

V1 and early V2 builds install side by side.

- V1 remains `ResumeGo` with application id `com.resumego.desktop`.
- V2 development builds use the display name `职达 Career OS Preview` and application id `com.resumego.careeros.preview`.
- V2 uses its own Electron `userData` directory and never opens the V1 database as its writable database.
- Importing V1 data is explicit. V2 copies or imports a V1 backup into a staging area, validates it, then writes converted records into the V2 workspace.
- Migration failure leaves both the V1 source and the current V2 workspace unchanged.
- V2 data is not downgraded to V1.

The preview identity may be replaced with the final V2 identity only after migration, packaging and rollback acceptance criteria pass.

## Canonical domain ownership

### Knowledge Base

Owns durable source documents, extracted content, topics, tags, notes, question sets and user-confirmed capability facts. A document does not prove that the user owns every skill mentioned in it. AI-derived facts remain proposals until the user confirms them.

### Career Pipeline

Owns one real opportunity: company, role, JD, lifecycle status, ordered stage instances, stage history, linked resume version, schedules, interviews, feedback references and next actions. It may reference Knowledge Base assets but never owns or copies them.

### Resume System

Owns a base career asset and independently evolving job-expression versions. A pipeline links to a selected version. Editing one job version must not silently modify another version.

### Interview Engine

Owns sessions and immutable start-context snapshots. It supports three distinct modes: role based, knowledge training and experience simulation. The modes share session infrastructure but use separate source contracts and evaluation rules.

### Growth Feedback

Owns normalized feedback events derived from interviews, learning and resume improvement. It is not a ranking or gamification system and does not own source truth.

### Workspace Action

Owns no source data. It computes an explainable action projection from deadlines, pipeline state, explicit tasks and feedback. AI may phrase or explain an action but cannot silently change priority or domain state.

## Foundation delivery sequence

### F0 — Product and runtime boundary

- Make the V2 product definition and roadmap authoritative.
- Establish the preview application identity and separate data directory.
- Define a versioned import manifest and dry-run migration result.
- Preserve the current V1 runtime as a read-only migration source.

### F1 — Career Pipeline foundation

- Introduce the V2 `pipeline` API while preserving V1 project records for migration.
- Add lifecycle status, company and role identity, ordered configurable stages and append-only stage history.
- Relate schedules, resume versions and interviews through validated identifiers.
- Support create, update, stage transition, archive and list workflows.
- Do not build Knowledge Base or redesign the workspace in this slice.

### F2 — Knowledge Base reliable library

- Import local files with explicit consent.
- Store source metadata separately from extracted text.
- Classify, tag, search, open the original source and delete all derived data.
- Treat extraction and indexing as recoverable jobs.
- Ship keyword search before optional semantic retrieval.

### F3 — Knowledge training vertical loop

- Select Knowledge Base sources and start a knowledge-training interview.
- Snapshot the selected sources and prompt/schema versions.
- Require every knowledge-specific evaluation claim to reference a source chunk or state that evidence is missing.
- Convert the final result into normalized feedback and one optional next action.

### F4 — Remaining interview modes

- Add role-based interview using pipeline + resume version + interviewer role.
- Add experience simulation using user-provided interview experience or question sets.
- Keep real-source provenance visible and never present generated questions as real interview experience.

### F5 — Workspace projection

- Compute one primary action using deterministic ordering.
- Show recent activity as an event projection.
- Show gentle growth feedback without scores, ranking or punitive streaks.
- Replace the existing V1-oriented workspace only when upstream projections contain real data.

## Pipeline foundation model

The first implementation slice introduces these concepts without renaming all legacy tables at once:

- `CareerPipeline`: stable opportunity identity and lifecycle.
- `PipelineStage`: ordered, user-editable stage instance.
- `PipelineStageTransition`: append-only transition history.
- `PipelineResumeLink`: currently selected job-expression version with change history.
- Existing schedule and interview records gain nullable pipeline links through additive migrations.

Legacy `job_projects` rows are migration inputs. V2 code does not reinterpret an `active` V1 project as a detailed pipeline without producing an explicit conversion result.

## Pipeline rules

- Lifecycle and stage are separate. A pipeline may be active while its current stage is `TECH_INTERVIEW`.
- Templates provide defaults but users may add, rename, reorder, skip and complete stage instances.
- Status transitions are deterministic application rules. AI cannot change them.
- Stage transitions are append-only and store actor, time, previous stage and next stage.
- Archive preserves relationships and history.
- Hard deletion requires an explicit impact summary and is outside the first slice.
- All identifiers are revalidated against the local user boundary.

## Migration contract

Migration is a staged, idempotent operation:

1. Inspect the selected V1 backup without modifying it.
2. Produce counts, detected schema version, unsupported records and warnings.
3. Ask the user to confirm import.
4. Import into a new V2 transaction or temporary workspace.
5. Validate record counts and required relationships.
6. Atomically activate the imported V2 workspace.
7. Record a migration receipt without sensitive content.

Repeated imports with the same source fingerprint must not create duplicate pipelines or resumes.

## Privacy and AI constraints

- Original files, extracted text, resumes and interview answers remain local by default.
- External model calls disclose what content will leave the device and apply minimum-necessary redaction.
- Logs store operational metadata only, not source text, resume content, interview answers or secrets.
- RAG output passes a versioned schema and includes source chunk identifiers.
- AI suggestions preserve source, rationale, draft and user decision; they never overwrite career assets silently.

## Error handling

- Additive database migrations must be covered by empty-workspace and V1-upgrade tests.
- Import and indexing operations expose pending, completed, partially failed and retryable states.
- A failed derived index may be rebuilt from the original file and metadata.
- Missing linked records produce visible repair states instead of fabricated fallback content.
- Provider failure never prevents local pipeline, resume or knowledge management.

## Verification gates

The foundation is accepted only when:

- V1 and V2 preview can be installed side by side with different data directories.
- A V1 backup can be dry-run inspected without modification.
- An existing V1 job project can be converted into a V2 pipeline with its JD and selected resume preserved.
- A user can create multiple pipelines and move one through custom stages without affecting another.
- Stage history is retained after restart and archive/restore.
- Frontend, backend, migration and packaging checks pass without real model calls or personal data.

## Explicit non-goals

- Full Knowledge Base and RAG implementation in the Pipeline slice.
- Agent, MCP or Skill orchestration.
- Cloud sync and accounts.
- Automatic job application submission.
- Public job marketplace or scraping.
- Gamified career scoring.
- A comprehensive V2 visual redesign before domain projections exist.
