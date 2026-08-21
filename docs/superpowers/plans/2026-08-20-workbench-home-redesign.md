# Workbench Home Redesign Implementation Plan

> **For Codex:** Follow strict test-driven development and verify the rendered page against the selected visual reference before handoff.

**Goal:** Replace the web-style workbench with the approved desktop companion dashboard while preserving existing resume, target, and interview workflows.

**Architecture:** Keep `WorkbenchView` as the data-loading/container layer, make `TargetDashboard` a presentational desktop dashboard, and make `DesktopShell` own the compact icon rail and theme state. Existing backend APIs and route builders remain the source of truth; missing schedule and feedback data render honest empty states rather than demo records.

**Tech Stack:** Vue 3, TypeScript, Vue Router, Pinia, Element Plus icons, Vitest, Vite.

**Visual truth:** `/Users/kun/.codex/generated_images/01a018a8-a0af-7520-8951-205d4a367ec5/exec-461b6ce7-c38e-485d-a524-87504606cc4f.png`

---

### Task 1: Lock the desktop shell behavior

**Files:**
- Modify: `frontend/src/layouts/DesktopShell.test.ts`
- Modify: `frontend/src/layouts/DesktopShell.vue`

1. Add a failing component test for the five icon-first primary entries, bottom settings/theme/local status controls, and removal of the top-level ability-evidence entry.
2. Run the focused test and confirm the old text sidebar fails the new contract.
3. Implement the narrow black rail with Element Plus icons, accessible labels/tooltips, active state, theme toggle, and local-state indicator.
4. Re-run the focused test until green.

### Task 2: Lock the workbench dashboard contract

**Files:**
- Modify: `frontend/src/components/workbench/TargetDashboard.test.ts`
- Modify: `frontend/src/components/workbench/TargetDashboard.vue`

1. Add failing tests for the current target identity/stage strip, honest feedback and schedule states, the three resume actions, two compact interview actions, and absence of notification/score content.
2. Run the focused test and confirm the legacy four-card dashboard fails.
3. Implement the approved two-column desktop composition and responsive layout using existing project/resume/job data.
4. Ensure incomplete target materials expose helpful actions without inventing company, schedule, or feedback content.
5. Re-run the component tests until green.

### Task 3: Wire real routes and container data

**Files:**
- Modify: `frontend/src/views/workbench/WorkbenchView.test.ts`
- Modify: `frontend/src/views/workbench/WorkbenchView.vue`
- Modify: `frontend/src/router/index.ts`
- Create: `frontend/src/views/schedule/ScheduleView.vue`

1. Add failing tests for edit, preview/version, dialog interview, voice-interview handoff, target switching, and schedule navigation.
2. Run the focused tests and confirm missing action wiring fails.
3. Map existing target/job/resume values into the dashboard; keep target and resume persistence behavior unchanged in this visual phase.
4. Add a lightweight local schedule destination and route so the visible calendar entry is not dead.
5. Re-run the focused tests until green.

### Task 4: Verify build and visual fidelity

**Files:**
- Create or update: `design-qa.md`

1. Run all frontend tests and production build.
2. Start or reuse the local preview and open the workbench at the selected desktop viewport.
3. Exercise target switch, resume actions, interview actions, schedule, help, and theme toggle; check console errors.
4. Capture the rendered workbench and compare it together with the source visual at a normalized viewport.
5. Fix every P0/P1/P2 mismatch, recapture, and repeat.
6. Save the final comparison record with `final result: passed` in `design-qa.md`.
