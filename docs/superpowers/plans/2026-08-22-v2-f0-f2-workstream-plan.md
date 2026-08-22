# Career OS V2 F0-F2 Workstream Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the remaining V1-to-V2 import and Career Pipeline foundation work, then deliver the first reliable local Knowledge Base without starting the Interview Engine redesign.

**Architecture:** Work proceeds through contract-first waves. Core-owned migration and Knowledge storage decisions establish durable boundaries; backend and frontend agents then implement isolated slices from exact integrated commits. UI work begins only after an approved interaction contract and real domain projections exist.

**Tech Stack:** Java 21, Spring Boot, JdbcTemplate, Flyway H2/MySQL, Vue 3, TypeScript, Pinia, Vitest, Electron, local filesystem

**Spec:** `docs/product/productV2.md`, `docs/product/roadmap.md`, `docs/superpowers/specs/2026-08-22-career-os-v2-foundation-design.md`

## Global Constraints

- V1 is read-only migration input; V2 never opens the V1 database as writable storage.
- Every executable task receives a real integrated Base commit when promoted to `READY`.
- Migrations, import activation, file-path capability and shared public contracts remain Core Controller-owned until explicitly frozen.
- Knowledge documents do not prove user capability; inferred facts remain proposals until user confirmation.
- Original files, extracted text, resumes, interview answers and secrets never enter ordinary logs or fixtures.
- Keyword search ships before semantic retrieval; F2 does not include embeddings, RAG, Agent, MCP or interview training.
- A task may change only its `Allowed files`; overlapping owners run sequentially.
- Every code task uses real RED/GREEN evidence and full affected-module regression.

---

## Wave A — Complete current independent Pipeline tasks

- [x] `V2-F1-BE-01`: expose append-only stage history.
- [x] `V2-F1-FE-01`: create Pipeline types and API client.
- [x] Review and integrate both independently.
- [x] Promote `V2-F1-FE-02` and `V2-F1-BE-02` from the shared post-integration baseline.
- [x] Integrate `V2-F1-FE-02` Pipeline store after functional review.

Wave exit: Pipeline history is readable and the frontend has one typed state-management foundation without UI replacement.

## Wave B — Finish Pipeline and V1 import

- [x] `V2-F1-BE-02`: add deterministic Pipeline identity/material update after BE-01 integration.
- [x] `V2-F1-FE-02B`: complete frontend update and transition-history contracts before the page task.
- [ ] `V2-F0-MIG-01`: freeze import manifest, dry-run result, fingerprint and receipt contracts.
- [ ] `V2-F0-MIG-02`: implement read-only inspection and transactional idempotent import.
- [ ] `V2-F0-MIG-03`: expose the import review/confirmation desktop flow.
- [ ] `V2-F1-UX-01`: approve Pipeline list/detail/stage interaction contract.
- [ ] `V2-F1-FE-03`: implement the approved V2 Pipeline page.
- [ ] `V2-F1-QA-01`: verify two independent Pipelines, restart persistence and V1 import.

Wave exit: the F1 roadmap exit condition is demonstrable without the legacy Job Project UI.

## Wave C — Freeze and implement the reliable Knowledge library

- [ ] `V2-F2-ARCH-01`: freeze storage, lifecycle, parser, source-location and deletion contracts.
- [ ] `V2-F2-BE-01`: create schema, domain records and metadata/list/detail API.
- [ ] `V2-F2-IO-01`: implement consented file import, safe local storage and recoverable extraction jobs.
- [ ] `V2-F2-BE-02`: implement categories, tags, keyword search and source location.
- [ ] `V2-F2-BE-03`: implement retry and complete derived-data cleanup.
- [ ] `V2-F2-FE-01`: create typed Knowledge API client and Pinia store.

Wave exit: all non-visual F2 contracts are testable with local fictional fixtures and no model calls.

## Wave D — Knowledge UI and milestone acceptance

- [ ] `V2-F2-UX-01`: approve the Knowledge Library desktop interaction contract.
- [ ] `V2-F2-FE-02`: implement the approved library UI with empty/loading/failure/retry states.
- [ ] `V2-F2-QA-01`: verify import, search, source opening, failure recovery, deletion and desktop persistence.

Wave exit: the F2 roadmap exit condition passes on a packaged desktop candidate. F3 remains out of scope until F2 evidence is approved.

## Integration order

```text
BE-01 ──→ F1-BE-02 ───────────────┐
FE-01 ──→ FE-02 ──────────────────┼─→ F1-FE-03 → F1-QA-01
                 F1-UX-01 ────────┘
F0-MIG-01 → F0-MIG-02 → F0-MIG-03 ┘

F2-ARCH-01 → F2-BE-01 → F2-IO-01 → F2-BE-02 → F2-BE-03
                                      │
                                      └→ F2-FE-01
F2-UX-01 + F2-FE-01 ───────────────────→ F2-FE-02 → F2-QA-01
```

## Plan verification

Before dispatching any card:

```bash
rg -n "READY|QUEUED|CORE_RESERVED|DESIGN_REQUIRED" docs/product/workstreams.md
find docs/tasks/v2 -name '*.md' -print | sort
git diff --check
```

Only cards physically under `docs/tasks/v2/ready/` may be executed.
