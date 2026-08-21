# Workbench Design QA

- Source visual truth: `/Users/kun/.codex/generated_images/01a018a8-a0af-7520-8951-205d4a367ec5/exec-461b6ce7-c38e-485d-a524-87504606cc4f.png`
- Implementation screenshot: `/Users/kun/Projects/ResumeGo/workbench-implementation.png`
- Combined comparison: `/Users/kun/Projects/ResumeGo/workbench-design-comparison.png`
- Viewport: 1487 × 1058 CSS px
- Source pixels: 1487 × 1058
- Implementation pixels: 1487 × 1058
- Density normalization: source and implementation compared at 1:1 pixel dimensions; browser viewport override used device scale factor 1.
- State: light theme, existing local target and resume, no recorded real schedule events, no completed matching interview summary.

## Full-view comparison evidence

The combined comparison places the approved mock above the browser-rendered implementation. The implementation keeps the same major geometry: narrow black tool rail, relaxed header, full-width target strip, balanced two-column workspace, resume controls below feedback, compact interview row, and help in the bottom-right corner. The live state intentionally shows empty feedback and schedule content because the local data source contains no matching records; sample events and sample feedback from the visual mock were not copied into production.

## Focused region comparison evidence

- Header and target strip: the target strip begins at the same vertical position as the source and spans the available canvas. The add-target control is separated into the top-right desktop action area, while the date widget remains aligned with the welcome block.
- Main work area: the feedback and schedule panels begin within 3 px of the reference vertical position. The two columns use equal visual weight, and their primary panels finish at the same baseline.
- Resume and interview controls: the resume module remains directly below feedback and the two interview modes stay in one compact horizontal component, matching the source hierarchy.
- Tool rail: width, rounded shape, active white item, icon spacing, and bottom utility cluster match the selected desktop direction.

## Comparison history

### Pass 1 — blocked

- [P1] The dashboard retained a centered maximum width, creating too much empty space between the rail and content and unequal main columns.
- [P2] The add-target action sat inside the content header instead of the window-level top-right area.
- [P2] Feedback and schedule panels were shorter than the selected reference, which made the lower modules rise too high.

Fixes made:

- Removed the dashboard maximum-width constraint so content begins at the approved rail-to-canvas spacing.
- Positioned the add-target action at the top-right and kept the date summary aligned with the greeting.
- Changed the main grid to equal columns and aligned feedback/schedule heights and lower-module baselines.
- Normalized the displayed company label so a job title embedded in the stored company value is not repeated.

### Pass 2 — passed

Post-fix evidence in `workbench-design-comparison.png` shows no remaining P0/P1/P2 layout, hierarchy, spacing, color-token, typography, image-quality, copy, or interaction mismatch. The different card contents are expected real-data states, not design drift.

## Required fidelity surfaces

- Fonts and typography: system Chinese UI stack, weights, hierarchy, line height, and wrapping are consistent with the reference. No clipped text at the target viewport.
- Spacing and layout rhythm: rail gap, target strip, equal columns, section gaps, rounded corners, and lower-module alignment match the reference composition.
- Colors and visual tokens: warm-gray canvas, quiet white surfaces, black rail/actions, green progress accent, and subdued gray separators are retained. Dark theme also switches successfully.
- Image quality and asset fidelity: company presentation uses the existing local brand asset resolver; icons come from the installed Element Plus icon library. No placeholder boxes, emoji, CSS icon drawings, or newly handcrafted SVGs were introduced.
- Copy and content: approved labels and hierarchy are present. Missing interview and schedule records use explicit empty states rather than fabricated data.

## Primary interactions tested

- Light/dark theme toggle
- Help dialog open and close
- Schedule navigation and return to workbench
- Dialog interview entry with current target context
- Resume preview entry with current version context

Browser console warnings/errors checked: none.

## Follow-up polish

- [P3] A future company-brand asset pass can replace the existing generated local mark with official user-provided company icons.
- [P3] Once real schedule entry is implemented, populated event rows should receive a separate visual regression capture.

## Reopened after responsive audit

The 2026-08-20 responsive audit found actionable issues that the original single-viewport comparison missed: global CSS collisions break dark mode, and 1280 × 800 / 1024 × 768 desktop windows require excessive vertical scrolling. See `docs/audits/workbench-responsive-2026-08-20/audit.md`.

## Re-verification after responsive audit fixes

Re-measured the rendered workbench in the browser at 1487 × 1058, 1280 × 800, 1024 × 768, and dark mode. Audit findings and their current state:

- [P1] Global style pollution: workbench panels now use workbench-only `rg-*` class names; the legacy `.workbench-grid` / `.panel-heading` rules in `style.css` no longer reach this screen. Dark mode renders dark canvas (`#101112`), dark surfaces (`#1a1b1d`), light ink headings, and inverted primary actions — no white seams.
- [P1] Window height: 1487 × 1058, 1280 × 800, and 1024 × 768 all fit the whole dashboard in one viewport (`scrollHeight == innerHeight`); the `max-height: 899px` breakpoint compacts the header, target strip, panels, and spacing below 900 px tall windows.
- [P2] Tool rail: 62 px rail with 44 px entries and a 42–44 px touch target, matching the desktop direction.
- [P2] 1024 px stage track: the four stage items stay on a single line with no strip or document overflow.

Improvements applied on top of the audit fixes:

- Theme choice is now persisted in `localStorage` under `resumego:theme` and restored on startup (refresh keeps the chosen theme).
- `document.body[data-theme]` follows the shell theme so overscroll no longer reveals a light body background in dark mode.
- The workbench help dialog now uses the shell theme tokens (`--surface-solid`, `--ink`, `--copy`, `--line`) instead of a hard-coded white background, so it renders correctly in both themes.
- Removed dead CSS rules left behind in `TargetDashboard.vue` after the `rg-*` rename; `DesktopShell` gained tests for theme persistence and body synchronization.

Frontend test suite: 97 tests pass across 37 files. Production build passes.

final result: passed

## Dark-mode redo (2026-08-20)

User reported dark mode was unreadable ("深色字体根本就看不清"). Root cause: page and dialog styles hard-coded light backgrounds and dark text, so in dark mode text and surfaces both went dark. Redone the whole dark version by routing every reachable surface through the shell theme variables (`--canvas / --surface / --surface-solid / --line / --ink / --copy / --muted / --brand / --brand-soft / --danger / --danger-soft`), with the original hex kept as fallback so light mode is byte-identical.

Root causes fixed:

- Global `h1 { color:#111827 }` / `h3 { color:#111827 }` in `style.css` painted dark-on-dark headings inside the shell — switched to `var(--ink, #111827)`.
- Element Plus dialogs teleport outside `.desktop-shell`, so they never inherited the theme: added `body[data-theme='dark']` overrides in `style.css` for `.el-overlay`, `.el-dialog`, `.el-input__wrapper`, `.el-textarea__inner`, `.el-button--default`, and dialog close buttons.
- The immersive resume editor (`/editor`) renders outside the shell and had its own hard-coded light palette: it now defines the theme variables on `.focused-editor` (light defaults) plus a `body[data-theme='dark'] .focused-editor` dark block; `main.ts` now initializes `document.body.dataset.theme` from persisted storage so deep links and the editor respect the chosen theme.

Pages/components converted (shell routes): workbench (`TargetDashboard`, `LocalLibraryState`, `WorkspaceLoadingState`, `WorkspaceErrorState`, `FirstRunEmptyState`), targets list + 4 target dialogs, resumes library, evidence library, settings, schedule, interview view + `InterviewHistoryPanel`, `InterviewRoomSidebar`, `InterviewLobbyHero`, `InterviewPlanReviewDialog`, `AiSetupGuide`.

Editor components converted: `ResumeEditorView`, `EditorCanvas`, `EditorSidebar`, `SectionWrapper`, `EditorPreviewPanel` (chrome only), `AiCoachPanel`, `ParsedFields`, `CompanyProfileSignal`.

Deliberate exceptions (kept as-is, readable in both themes):

- The A4 resume paper and all `.a4-paper.template-*` styles stay white/dark-text — it is the export document and must match the printed output.
- Solid brand-green buttons, dark navy blocks (`#101a33` hero/buttons/avatar), blue/purple/amber accent chips, the amber weakness card, and the safety card keep their original colors.

Browser verification (dark mode, then light round-trip): canvas `#101112`, surfaces `#1a1b1d`, ink `#f4f4f2`, muted `#9da0a4`, brand `#2ea884`; all routes readable; the editor renders dark chrome with a white document sheet; light mode restores the original palette. Frontend test suite: 97 tests pass across 37 files. Production build passes.

## UI refinement pass (2026-08-20)

Follow-up design polish on top of the dark-mode redo, keeping the user's existing direction (warm-gray canvas, black rail, brand green) and converging toward Linear/Notion-style restraint: flatter surfaces, fewer shadows, calmer header chips, more breathing room. No redesign — the approved composition, rail, and color tokens are untouched.

Changes applied:

- Brand-color discipline: the workbench family had five greens in play (`#168866`, `#159447`, `#25af55`, `#10b981`, `#188d49`) that diverged between light/dark. All now resolve to the single `--brand` token (with the original hex as fallback), so every green accent tracks the theme: `#168866` light / `#2ea884` dark. Touched files: `TargetDashboard.vue` (stage current dot, welcome strong, feedback-next border), `WorkbenchView.vue`, `LocalLibraryState.vue`, `FirstRunEmptyState.vue`.
- First-run primary button: background now `var(--brand, #10b981)`; hover changed from a hard-coded darker green to `filter: brightness(0.94)` so it stays correct in both themes.
- Header: the welcome "chip" went from a heavy black pill (`background:#080909; color:#fff`) to a quiet surface capsule (`--surface-solid` background, `--muted` text, 999px radius) — matches the date widget language; h1 spacing tightened to `18px 0 14px`; the brand strong accent uses `--brand`.
- Target strip + panels: radius unified to 16 px, padding opened to `22px 26px` / `26px 28px`, and the near-invisible box-shadows removed (`0 8px 30px…` on the strip, `0 10px 34px…` on panels) for a flatter, more modern surface.
- Workspace grid: gap `38px → 40px`, top margin `28px → 34px` — one explicit rhythm step between header and work area.
- Deliberately kept: the black rail, black primary actions ("继续编辑简历" stays `#080808`/`#070808`), dark navy blocks, avatar gradients, A4 paper templates, and accent chips — these are the user's signature elements.

Browser verification (both themes, workbench dashboard):

- Light: canvas `rgb(246,245,242)`; welcome chip `rgb(255,255,255)` pill with muted text; stage current dot + welcome strong `rgb(22,136,102)` (`#168866`); strip radius 16 px, padding `22px 26px`, no shadow; grid gap 40 px / top margin 34 px; panel radius 16 px, no shadow, padding `26px 28px`.
- Dark (round-trip back, left in the user's preferred dark state): chip `rgb(26,27,29)`, accents `rgb(46,168,132)` (`#2ea884`), primary action `rgb(7,8,8)`.

Frontend test suite: 97 tests pass across 37 files (no style assertions affected). Production build passes.

## Workbench polish + real schedule + resume workspace batch (2026-08-20)

Six feedback points turned into verifiable changes: workbench breathing room, calendar tile icon fix, interview lobby cleanup, a self-built local calendar with real data, an open resume workspace with Markdown import, and target-entry quick picks. Key QA findings:

- **Calendar tile icon was being crushed into a 4px gradient bar.** Root cause: the raw `<i>` progress bar was styled via `.calendar-tile i`, which also matched the Element Plus `el-icon` wrapper (rendered as `<i class="el-icon">`). The icon ended up 27×4px. Fix: scope the bar to `.calendar-tile .tile-progress`. Verified in browser: grid rows `17px 29px 4px`, icon 17×17px, bar 27×4px.
- **Vue nested-ref unwrap trap on the schedule page.** `useSchedule` returns a plain object; the template was checking `v-if="schedule.errorMessage"` without `.value`. Templates only auto-unwrap top-level refs, so the check saw a truthy Ref object while interpolation rendered an empty string — producing an empty `<p role="alert">`. Fixed to `.errorMessage.value`; browser now reports 0 alerts.
- **Schedule page `data-test` contract fix.** `:data-test="`day-cell-${cell.date}`"` interpolated a `Date` object into `day-cell-Thu Aug 20 2026 00:00:00 GMT+0800`. Now uses the local `YYYY-MM-DD` key (`day-cell-2026-08-20`).
- **End-to-end schedule flow verified in browser (dark theme):** create event via dialog → save → dialog closes → event card appears in the day panel (笔试·腾讯 · 技术面 14:30), type-colored dot on the day cell, month summary counts it; survives navigation away and back (persisted through the real backend). The workbench "即将到来的面试" panel lists the real event with "今天" relative label.
- **Resume workspace verified:** shelf renders the paper-card thumbnails for existing resumes with version badges, plus the dashed Markdown-import card; detail section shows version history.
- **Target dialog quick picks verified:** 15 hot-company chips and 8 hot-job chips render; typing "腾" filters chips to 腾讯 (label flips to 匹配), clicking the chip fills the company field.
- **Theme coverage:** dark theme browser-verified across workbench/schedule/resumes; light/dark toggle, persistence, and restore are covered by unit tests (DesktopShell.test.ts), and the light palette is the base `.desktop-shell` rule.

Verification: frontend 111 tests across 38 files + production build pass; backend 445 tests, 0 failures, 1 AI-dependent skip. New tests this batch: `ScheduleEventServiceTest` (6), `parseMarkdownResume.test` (4), `ResumeLibraryView` import flow, `TargetJobDialog` quick-pick behavior (4), `WorkbenchView` schedule linkage.

## Workspace home redesign — desktop-first (2026-08-20)

Redesigned the workbench home from a centered SaaS card-grid into a desktop-first personal career workspace per the design brief: quiet metadata header (no hero/slogan replacement), identity/progress split into two clear layers, asymmetric primary/context-rail composition, one restrained surface (upcoming interview) and everything else expressed through hierarchy/spacing/alignment. Business logic, events, and all `data-test` contracts were preserved (WorkbenchView emits unchanged; TargetDashboard tests updated to the new markup but same contracts).

Layout system:

- `DesktopShell` gains a route-scoped fluid mode (`workspace-fluid`) for the workbench route: `justify-self: stretch; padding-inline: clamp(28px,4vw,64px)`; every other page keeps the 1224 px centered column.
- Workspace body grid: `grid-template-columns: minmax(0,1fr) clamp(300px,24vw,340px)` with `gap: clamp(28px,4vw,64px)` — the primary column takes the remaining space fluidly, no hard-coded width. Reading content (`feedback`) is capped at `max-inline-size: 42rem`.
- Identity layer (`target-identity`) and progress layer (`当前求职阶段`) are separate stacked rows with a quiet divider, not a compressed toolbar.
- Context rail (right) hosts the tertiary tools: resume module and the dialog/voice mock-interview entry group, separated by `border-left` (removed below 1120 px when the body collapses to one column).
- Upcoming interview is the only retained restrained surface (`--surface` + 1px `--line` + 14px radius); it is capped at `max-inline-size: 56rem` so it never degrades into a wide empty box on ultrawide monitors.
- Future rule preserved in a code comment: when there is no upcoming interview, the "开始模拟面试" entry may be promoted from the context rail to the left column's next-action slot.

Browser verification (dark theme, real local data):

- 1440×900: grid `751px 340px`, no document scroll, slogan removed.
- 1280×800: grid `676px 307px` (24 vw clamp active), identity and progress on separate rows, no document scroll.
- 1600×900: grid `890px 340px`, feedback width 672 (42 rem cap), no document scroll.
- 1920×1080: grid `1210px 340px`; upcoming surface now caps at 896 px (56 rem) with breathing room before the rail divider — earlier measure showed a 1210 px surface whose content only filled the left ~200 px (empty-box regression), fixed by the cap.
- 1100×800: body collapses to one column (rows 216/146 px), context rail becomes a 2-column grid with `border-left` removed, no document scroll.
- All five viewports: `scrollHeight == innerHeight`, no horizontal overflow; home does not depend on document-level scrolling in normal content state.

Verification: frontend 111 tests across 38 files + production build pass.

## Workbench home — task-oriented workspace (round 2, 2026-08-20)

Second pass on the home page, per the follow-up brief: stop subtracting visuals, reframe the page as a task-oriented workspace (Task-oriented / Contextual / Dense but quiet / State-aware / Application-like), keep every round-1 structural decision (no 2×2 grid, no centered container, no marketing title, no extra cards, no gradient/glow, no document scroll). The page is now organized around one contextual Next Action, with an object-identity context band and a preparation-state right column instead of a sidebar.

What changed from round 1:

- **Context band gives the current target back its object identity.** The identity row and progress row now share one faint surface (`--surface` = `rgba(255,255,255,.78)` light / `rgba(26,27,29,.78)` dark), radius 16 px, **no border, no shadow, no card chrome**. The identity (avatar + `腾讯 · Java后端实习` + 地点 + 切换目标) and the four-segment stage stepper read as one object, not as two floating toolbars.
- **Upcoming is now a pure typographic block.** No box, no card: zone label `即将到来 · 今天`, then a text-anchored primary block — `14:30` at 44 px / weight 200 / `-0.88px` tracking is the visual anchor, followed by `腾讯 · 技术面` (17 px semibold) and quiet metadata (static countdown `还有 X 分钟`, date, 查看日程). The whole main block is one quiet button with no fill.
- **Right column redefined as a Preparation Panel** (`准备`), three state rows: 简历 (name/version/updated + `已就绪`), 模拟面试 (`未准备` + 对话/语音面试 entries), 日程 (今天 14:30 + `已安排`). Rows are status-driven from real props only; no invented capability rows. Panel is transparent on the canvas — no `border-left`, no box — so it reads as preparation state, not a sidebar.
- **Next Action drives the page.** `建议下一步` + one short action sentence + a single filled button (the only filled button in the dashboard). Rules, in priority order around the current preparation gap: no resume → 先选择简历; upcoming + no recent feedback → 面试前完成一次针对当前岗位的模拟面试; upcoming + recent feedback → 复看最近反馈的薄弱点; no upcoming → 完成一次模拟面试保持状态. No AI-recommendation phrasing.
- **Quieting details:** `工作台` pill deleted, `录入岗位` demoted to a ghost header button, the 70×66 calendar tile replaced by a text `date-summary` control, progress width now content-driven (stage track no longer spans the full band), right column divider removed, avatar stays aligned with the first stage dot.
- **Countdown kept as static secondary metadata** (user constraint): computed from the real event `startTime`, shown once, never ticking, never animated, never promoted above the 14:30 anchor.

Single-column scroll regression found and fixed:

- At 1100×800 the stacked single column was 76 px too tall (`scrollHeight` 876 > 800). The compact `max-height:899px` breakpoint alone could not absorb it. Fix: reordered the media queries so the `max-width:1120px` single-column block is applied **after** the compact block (its tighter rhythm wins in the overlap), and compressed the single-column layout: dashboard padding `12/8`, body gap 20, primary-zone gap 20, band `14px` margin / `16px 20px` padding, upcoming time 30 px, prep panel 2-col at `gap:16px` with zero row padding, next button height 34. Prep panel went from 252 → 184 px tall.

Browser verification (both themes, real local data):

- 1440×900: grid `751px 340px`, band bottom 267, upcoming anchor 44 px, scrollHeight == 900.
- 1280×800: grid `678px 307px`, scrollHeight == 800; first stage dot x = 217.
- 1600×900: grid `890px 340px`, scrollHeight == 900.
- 1920×1080: grid `1210px 340px`, upcoming still capped at 896 px (56 rem), scrollHeight == 1080.
- 1100×800: single column 853 px, prep 2-col rows `84/66/40` with schedule spanning full width, **scrollHeight == 800** (was 876).
- Style audit: context band `rgba(255,255,255,.78)` + radius 16 + no border/shadow; prep panel transparent, no left border; `.next-button` is the only filled button in the dashboard (`rgb(7,8,8)` light / inverted dark); `录入岗位`/`继续编辑简历`/`⋯` are hairline or ghost; upcoming time 44 px / 200 / `-0.88px`.
- Next Action rule hit the expected branch with real data: upcoming exists + no recent feedback → `面试前完成一次针对腾讯 Java后端实习的模拟面试` → `开始模拟面试`.

Verification: frontend 114 tests across 38 files + production build pass.

## Workbench home — compact workspace refinement (round 3, 2026-08-20)

Refinement pass on top of round 2, per the brief: stop adding whitespace, fix over-spacing / under-grouping, keep the round-2 information architecture (Current Context → Primary Task → Preparation State → Next Action). Theme of the round: dense information + low visual noise; whitespace belongs between concepts, not inside a workflow.

Changes applied:

- **Context band compressed to a compact workspace context.** Same faint `--surface`, same no-border/no-shadow rule, but padding `26px 28px → 18px 24px`, identity→progress gap `22 → 12`, identity title 19→18px, progress track is now content-width (`inline-flex; width:max-content`) instead of stretching, stage-item spacing 40→28. Measured height: **122 px** at 1440/1600/1920 and **110 px** at 1280/1100 — inside the 110–125 px target, down from ~156 px. No longer a hero card.
- **Empty upcoming state collapsed from a full-width dashed box to one quiet line** (`近期没有面试安排 · 添加日程 →`, 12px muted, text-style action). No `empty data → preserve full module footprint`.
- **State A / State B primary composition.** With upcoming events the primary column keeps 即将到来 (44px typographic anchor) → 建议下一步 → 最近反馈. With no upcoming events the upcoming region collapses to the inline line and the Next Action section re-labels itself **当前准备** with an object line (简历/模拟面试) + one-line description + the single filled button, so the page keeps an explicit focus without an empty rectangle. Rule logic unchanged (no resume → 选择简历; upcoming no feedback → 模拟面试; upcoming + feedback → 查看完整报告; no upcoming → 当前准备·模拟面试).
- **Preparation panel rebuilt as three compact object rows.** Each row: restrained 32px icon container, title, metadata, semantic status, and exactly ONE contextual action (编辑 → / 开始 → / 查看·添加 →). The old full-width hairline button row is gone; secondary actions moved behind `⋯` progressive disclosure (resume: 预览/更换版本; interview: 语音面试 now lives in the interview row's `⋯` menu instead of being expanded next to 对话面试). Measured row heights 86/83/70 px at 1440 (was ~110+), panel 265 px (was ~340).
- **Vertical rhythm per the spacing hierarchy** (within object 8–12 / related objects 16–24 / different sections 28–36 / major regions 40–48 max): primary-zone gap 38→32, upcoming padding 22/24→16/20 and inner gaps 10/16→8/12, next-action text/button margins 8/14→6/12, feedback inner gaps 20/24/22→16/18/18, prep rows 18→16, dashboard footer 24→20, header→band 28→22, column gap capped `clamp(28px,3.5vw,48px)` (was 64). No consecutive 40px+ gaps remain inside the workflow.
- **Nothing new was added to fill space** — no statistics, no cards, no tips; page simply ends (~669 px at 1440×900) and leaves canvas below.

Verified empty-state copy stays honest: `尚未进行模拟面试 · 完成后将在这里显示关键问题`, `近期没有面试安排 · 添加日程 →` (the round-2 copy `还没有模拟反馈 / 还没有添加近期面试` was replaced together with the collapsing surfaces).

Browser verification (real local data, State A active — a today 14:30 event exists; the static countdown correctly disappears once the event time has passed):

- Context band 122 px (1440/1600/1920) / 110 px (1280/1100); prep panel 265 px (1440) / 237 px (1280) / 144 px (1100 single-column); all five viewports `scrollHeight == innerHeight`.
- Prep panel DOM audit: three rows with buttons `[编辑, ⋯]`, `[开始, ⋯]`, `[查看]` — one contextual action each; `⋯ → 语音面试` navigates to `/interview?...&mode=voice` (progressive disclosure works end-to-end).
- State B composition (no upcoming) is covered by unit tests: `当前准备` label + 模拟面试 object line + 开始模拟面试, compact `近期没有面试安排` line, no-resume rule still first.

Verification: frontend 114 tests across 38 files + production build pass.

## Workbench home — global workspace with Agenda + Inspector (round 5, 2026-08-20)

Per the confirmed brief, the home page is now a Global Workspace: no global active target, no "今日工作区" title, no Current Target Banner, no separate Next Action section, no dashboard-style preparation cards. Spatial model: `Dock | Agenda Workspace | Inspector`.

Layout system:

- Toolbar = `8月20日 星期四` (explicit `X月X日 星期X`, passed in as a prop so the child never re-derives locale-sensitive text) + `录入岗位` ghost action + `日程 →` text control. Workspace zone label: `接下来 · N 场安排`.
- Agenda = timeline grouped by day (今天/明天). Rows use a `20px auto minmax(0,1fr)` grid: rail + dot (timeline rule), time, then title/role. Selection is expressed by the semantic indicator (solid brand-green dot vs transparent), title weight 600, and one restrained row tint — no card chrome, no size change.
- Inspector (right, `border-left`, 300–340px) answers four questions: identity (这是什么), readiness rows 简历/模拟面试/日程 (准备得怎样), one contextual next action + single primary button (下一步做什么), and a footer link 目标详情 → / 查看日程 → (去哪里深入). Three kinds: linked event (`inspector-readiness` + `inspector-next`), unlinked event (`inspector-unlinked`, **no fabricated readiness/next**), target fallback / empty.
- Recent activity stays bottom-left (3 latest completed feedback summaries, `查看报告` →).
- Empty states are compact single lines (`近期没有安排  添加日程 →`, `还没有求职目标  录入岗位 →`), no large empty-state rectangles.

Data honesty verified live: with real local data the only two upcoming events are unlinked to any target, and the Inspector shows `未关联求职目标` + `查看日程` with zero readiness/next rows for both — no invented prep state.

Browser verification (real local data, light + dark):

- All five viewports `scrollHeight == innerHeight` (no document scroll): 1280×800, 1440×900, 1600×900, 1920×1080, 1100×800 (single-column breakpoint puts the inspector below the agenda at ≤1120px; dark theme also audited with scroll 0).
- Selection switching (click row 1 → row 0 → back): inspector identity title/meta/line follows the selected event (`字节跳动 · 笔试` ↔ `阿里巴巴 · 技术二面`; `8月21日 09:50` ↔ `8月22日 14:30`; `笔试` ↔ `面试`); the semantic dot fill toggles `rgba(0,0,0,0)` ↔ `rgb(46,168,132)` (dark brand) exactly with the selected row; both events stay `未关联求职目标` with no readiness/next rows.
- Linked-target switching (identity/prep/next sync across two targets, next-action routing with selected target context) is covered by WorkbenchView unit tests, since the current real dataset has no linked events.

Screenshots stored in `docs/audits/round5-workbench-light-1440.png` and `round5-workbench-dark-1440.png`.

Verification: frontend 116 tests across 38 files + production build pass.

## Workbench home — strict 37-section spec (round 6, 2026-08-20)

Executed the confirmed UI specification verbatim: `Dock | Main Workspace | Inspector`, page `100dvh` with no document-level scroll, canvas `#F5F5F2`. Implemented exactly as specified — no new compositions, no reinterpreted direction.

Layout system:

- Dock 72px wide, x=18/y=16, height `calc(100vh - 32px)`, radius 28px, bg `#0A0A0B`, shadow `0 8px 24px rgba(0,0,0,.10)`, no glow. Icons 22px on 48×48 hit areas, 8–10px gap; selected state = 44×44 white surface radius 13px; bottom settings/theme/avatar form two clear groups.
- Workspace left-anchored (not centered): 1440 start-x≈124, toolbar top 28px, max-width 1280 (1600+ → 1320, inspector 336). Toolbar 52px high, no card/no fill/no page title: `8月20日 星期四` (14px/450/secondary) + `录入岗位` ghost action + `日程 →`. Content starts ~y=102–110.
- Primary Event — the single allowed major surface (not a SaaS card): 176–190px tall, padding 24/28, radius 18, bg white, 1px hairline border, near-flat shadow. Order: `明天` (13/600 accent, no pill) → `09:50` (40/300/lh 1/letter-spacing −.03em, the page's largest visual element, not bold) → `字节跳动 · 笔试` (18/600) → 后端开发实习 (14 secondary) → footer `还有 18 小时 31 分` + `查看日程 →`. Event type as plain text top-right (12 secondary, no badge). Hover only `#FCFCFB` + deeper border; no scale/translate/shadow/grow.
- 稍后 agenda below (margin-top 28): rows 58–64px, left 76px time column (15/500 + 12px date), middle title 15/550 + target 13 secondary, right relative `2天后` (12 tertiary), 1px subtle dividers, no rounded card. Timeline indicator = per-row 6–8px dot (nearest/selected brand green, others neutral gray) — no full-height rail.
- Recent activity (margin-top 34): `最近活动` 15/600 + single most recent record (date/title/summary 14/lh 1.6 + `查看完整报告 →`, max-width 620). Empty = two quiet lines ≤70px.
- Inspector: 328px (1440), full height, `#F9F9F7`, `border-left`, padding 26/20/22, a spatial region not a card. Company 13 secondary → Role 18/600 → `技术面 · 明天 14:30` 13 secondary. 准备 = three 68–74px object rows (32px icon surface/16 icon/radius 9, hairline dividers, one contextual action each): 简历 (`算法工程师简历 · V4` + `7/15 更新` + ✓ + 编辑 →), 模拟面试 (尚未完成 + 开始 →), 日程 (明天 09:50 + 查看 →). 下一步 (28 below): label + 15/550 body + the single primary button (40px, radius 11, bg `#111`/white text, hover `#222`). `目标详情 →` quiet link below. **Green = status color, black = action color** (no green primary button).
- Unlinked events: identity + time + countdown + `未关联求职目标` + `查看日程 →` only — no resume/mock/schedule/next rows (no data to back them); the Inspector region keeps its tinted background so the app space stays stable.
- Vertical rhythm only 4/8/12/16/20/24/28/32/40; typography system stack, max number 40/300, page title ≤18/600; weights mostly 400/500/600. Radius three tiers 8–9 / 11–12 / 16–18 (Dock excepted); borders 1px; shadows only Primary Event (near-flat) + Dock.
- Dark mode = token swap only, layout untouched: canvas `#111212`, surface `#191A1A`, subtle `#161717`, hover `#202121`, text `#F2F2EF`, copy `#A3A39E`, muted `#73736F`, accent `#47B58E`; primary button inverts to light bg + dark text.
- Responsive: 1600+ inspector 336/max-width 1320; 1440 inspector 328/main 922/gap 30 (baseline); 1280 inspector 300/gap 24/primary padding 22; 1100–1180 inspector 280. `<1050px` the Inspector becomes a right drawer (fixed, translateX(105%) → `.is-open` 0), workspace fills the main region, clicking an event slides the drawer in; close button returns it off-canvas.
- 900px height: no scroll, Primary 176–190, agenda shows ≤3–4 upcoming + `查看全部日程 →`, recent single record. 800px height: compact — Primary 156–168/36px time, section gaps 28→22/34→26, object rows 68→60.
- Forbidden items absent: KPI/statistic cards, giant empty states, colored avatars, gradient/glass/blur/glow, marketing slogans, oversized page titles, feature grids, nested cards, large badges, AI sparkle/recommendation cards, multiple primary buttons, full-width button everywhere.

Browser verification (real local data, light + dark):

- Geometry at 1440×900: scroll 0; Dock 72/`rgb(10,10,11)`/radius 28/top 16/height 868; toolbar top 28/height 52/width 1280 (right edge 1404); primary top 144/height 181/width 922/radius 18/white; inspector 328 wide/`rgb(249,249,247)` full height 102→884.
- Five-viewport matrix, all `scrollHeight == innerHeight`: 1280×800, 1440×900, 1600×900 (inspector 336, toolbar 1320), 1920×1080 (inspector 336, toolbar 1320 left-anchored), 1100×800 (inspector 280). Compact at 800px height: primary 169px/`20px 26px`, time 36/300, later margin 22, activity margin 26.
- Selection switching: row click flips the per-row dot `rgba(20,20,20,.1)` → `rgb(22,139,104)` (brand green, matching the primary's nearest-event semantics); inspector identity/countdown follows the selected event; both real events are unlinked → `未关联求职目标`, zero readiness/next rows (no fabricated prep).
- Dark theme: canvas `rgb(17,18,18)`, primary `rgb(25,26,26)` + white hairline, inspector `rgb(22,23,23)`, time/title `rgb(242,242,239)` 40/300, scroll 0. Dock stays `#0A0A0B` in both themes.
- Drawer at 1024×768: default off-canvas (`matrix(1,0,0,1,399,0)` = translateX 105%); clicking an event row → `.is-open`, transform identity, right edge flush at 1024; close button returns it to off-canvas. Workspace fills the main region (primary width 868).

Screenshots stored in `docs/audits/round6-home-light-1440.png` and `round6-home-dark-1440.png`.

Verification: frontend 117 tests across 38 files + production build pass.

## Workbench home — Master–Detail desktop layout (round 7, 2026-08-20)

Executed the confirmed brief verbatim: the previous Workspace + Inspector spatial model is void; the home page is now a fixed Master–Detail desktop layout with exactly three first-level spaces — `Dock | Agenda Pane | Detail Pane`. No reinterpreted design philosophy, no new compositions (no Inspector, no Primary Event Card, no extra dashboard modules, no 使用帮助).

Layout system:

- 1440 px baseline: Dock 72 px; Dock→app gap 32 px; app max-width ≈1270 px, **left-anchored to the Dock, not centered**; Agenda Rail 310 px; divider 1 px; Focus Workspace `flex: 1`.
- Agenda Rail is one whole subtle pane surface (`--surface-subtle`, `#F9F9F7`), not multiple cards. Upcoming events are grouped by date (`今天 / 明天 / N 天后`); each event is a navigation row (~86 px tall, radius 12 px, extremely light selected background `--brand-soft`). No 100%-wide Primary Event Card anywhere.
- Focus Workspace renders the selected event detail. Header straight at the top: `明天` → `09:50` (48 px / weight 300 / `line-height:1` / `letter-spacing:-.03em`, the page's largest element) → `字节跳动 · 笔试` (21 px / 600) → static countdown `还有 18 小时` (compact floor-hours format, no live ticker).
- **Unlinked event** (no target): the right side shows only true information — `未关联求职目标` + `关联目标 →` (opens a target-link dialog listing real linkable targets) + `查看日程 →`. No fabricated resume/mock-interview preparation state.
- **Linked event**: Target identity block → preparation rows → one contextual next action. Resume and Mock Interview are compact object rows (32 px icon surface + label/meta + one action), not cards. `目标详情` footer link.
- Recent activity sits at the bottom of the Detail Workspace (`margin-top:auto`, max-width 620), no longer floating on the home page.
- `使用帮助` removed from the page.

Browser verification (real local data, light + dark):

- Geometry at 1440×900: scroll 0; Dock 72 × 868 at x=18/y=16, radius 28, bg `rgb(10,10,11)`; app content x=122 (= 18 + 72 + 32 gap), max-width capped 1270; rail 310 at x=122 full height `rgb(249,249,247)`; divider 1 px at x=432 `rgba(20,20,20,.067)`; detail flex 951 at x=433 (`overflow:auto`).
- Five-viewport matrix, all `scrollHeight == innerHeight`, no horizontal overflow: 1280×800 (detail 815), 1600×900 (content capped 1270, detail 951), 1920×1080 (content still capped 1270, **Dock stays left-anchored at x=18**), 1100×800 (detail 635), 1440×900.
- Selected row: 86 px tall, radius 12, background `rgba(22,139,104,.09)` (light brand-soft); hover `--surface-hover`.
- Selection switching (row click): header facts follow the selected event; the two real events are 字节跳动·笔试 09:50 (明天) and 阿里巴巴·技术二面 14:30 (后天) — the latter is unlinked → `未关联求职目标` + `关联目标 →` + `查看日程 →`, zero readiness rows.
- **Link flow end-to-end in the browser**: clicking 关联目标 → opens the dialog with the real linkable target list (腾讯Java后端实习) → choosing it PATCHes the event (`updateScheduleEvent` full-field, backend-validated) → the schedule reloads → the detail pane switches to the linked branch: identity, preparation rows, `开始模拟面试` next action, `目标详情`. The rail row meta now shows `· Java后端实习`.
- Dark theme tokens: canvas `rgb(17,18,18)`, rail `rgb(22,23,23)`, divider `rgba(255,255,255,.07)`, selected `rgba(71,181,142,.11)`, text `rgb(242,242,239)`, countdown muted `rgb(115,115,111)`; scroll 0; Dock stays `#0A0A0B`.

Screenshots stored in `docs/audits/round7-home-light-1440.png` and `round7-home-dark-1440.png`.

Verification: frontend 118 tests across 38 files + production build pass.

## Workbench home — desktop refinement (round 8, 2026-08-20)

Follow-up on the round-7 Master–Detail layout per user feedback: narrower Dock, a product-like preparation timeline, deduplicated role text, an estimated-duration hint next to the primary action, and a balanced two-column detail pane that fits on one screen.

Changes applied:

- **Dock narrowed 72 → 56 px.** Rail width 56, padding `40px 8px 12px`, radius 22, nav items 40×40 (radius 11, icons 20px), user avatar 36, tooltip offset 44. Dock-to-app gap unchanged (32 px workspace / 34 px base), so app content starts at x=106 at 1440 (was 122).
- **Preparation block is now a vertical timeline.** Each readiness row is a track column with an icon dot (26 px circle, brand-soft fill + brand icon when ready, surface fill + muted icon otherwise) connected by a 1 px hairline that terminates at the last row. The old icon container + ✓/— status columns are gone — node state carries the status. `data-test="readiness-*"` contracts unchanged.
- **Deduplicated role text.** The rail row meta is now time-only (`09:50`, was `09:50 · Java后端实习`); the linked-event identity block drops its role line (the header already shows the role once); the next-action text no longer recomposes company+jobTitle (`面试前完成一次针对当前目标的模拟面试`). The role word now appears once in the header and once in the target object name — no redundant repetition.
- **Estimated duration hint.** The `开始模拟面试` next action carries `预计 20 分钟` beside the primary button (`NextAction.hint`), shown only for the open-interview branch.
- **Detail pane balanced into two columns.** Linked/target bodies use `grid-template-columns: minmax(0,1.25fr) minmax(0,1fr)` with a 44 px gap: left = identity + preparation timeline, right = next action + 目标详情. Recent activity stays full-width pinned to the bottom. This halves the body's vertical footprint so everything fits on screen at 800 px heights, and distributes information across the detail width.

Browser verification (real local data, light + dark):

- Dock 56 × 868 at x=18/y=16, content x=106 (= 18 + 56 + 32 gap), app max-width still capped 1270.
- Rail rows: `字节跳动 · 笔试 / 09:50`, `阿里巴巴 · 技术二面 / 14:30`, `拼多多- HR面 / 15:30` — meta is time-only.
- Linked event identity: `腾讯Java后端实习` single line (no role line); header keeps `Java后端实习` once.
- Timeline: 3 icon dots, resume ready (brand-soft + brand icon), mock not ready, schedule ready; `.last` row has no connector line.
- Two-column split at 1440: main 475 / side 380 (899 body); at 1280: 415/332; at 1100: 315/252 — scales proportionally.
- `预计 20 分钟` hint visible beside 开始模拟面试.
- Five-viewport matrix (1280×800, 1440×900, 1600×900, 1920×1080, 1100×800): all `scrollHeight == innerHeight`, and `detail-pane` internal scroll 0 — the whole page (header + two-column body + recent activity) fits on one screen, including 800 px-tall windows (compact: time 40 px, timeline rows 71/56/71).
- Unlinked branch unchanged: single-column body, `未关联求职目标 / 关联目标 → / 查看日程 →`, zero fabricated readiness.
- Dark tokens: canvas `rgb(17,18,18)`, rail `rgb(22,23,23)`, selected + ready dot `rgba(71,181,142,.11)` / brand icon `rgb(71,181,142)`, hint muted `rgb(115,115,111)`, scroll 0.

Screenshots stored in `docs/audits/round8-home-light-1440.png` and `round8-home-dark-1440.png`.

Verification: frontend 118 tests across 38 files + production build pass.

## Workbench home — timeline rail + single-column detail (round 9, 2026-08-20)

User feedback on the round-8 split: the "next action" column felt cramped and out of place on the right, and the timeline they actually wanted was for the left-hand agenda rows. Two changes:

- **Detail back to a single vertical column.** The two-column split is gone. Content flows top-down again: identity → 准备 timeline → 下一步 (with `预计 20 分钟` hint beside the primary button) → 目标详情 → recent activity pinned at the bottom. `.detail-body` is capped at `max-width: 640px` so long text rows stay readable without spreading across the 900 px pane. The round-8 deduplication (time-only rail meta, identity without a repeated role line, "针对当前目标" next text) and the preparation timeline are kept.
- **Agenda Rail rows are now a timeline.** Each date group's event rows get a node column on the far left: an 8 px dot (10 px brand-green solid when selected, gray otherwise) with a 1 px hairline connector through the group; the first row's line starts at the node (no dangling stub above), the last row has no line. Groups with a single event show just the node. This is the "multiple-interview timeline" the user asked for, in the same visual language as the preparation timeline (brand green = selected/ready).

Browser verification (real local data, light + dark):

- Rail timeline: 3 event rows each render a node; selected row dot is 10 px brand solid, others 8 px `--line`; current data puts each event in its own date group (each row `last`, no connector needed); same-day multi-event groups will render the connector.
- Detail is single-column: sections 准备 → 下一步 at y≈519, 目标详情 at y≈650, body max-width 640, no split classes.
- 1440×900 and 1280×800: document scroll 0 and detail internal scroll 0 — identity, preparation, next action, and recent activity all visible on one screen.
- Dark tokens unchanged and verified (canvas `rgb(17,18,18)`, rail `rgb(22,23,23)`, selected brand-soft, ready dots brand icon).

Screenshots stored in `docs/audits/round9-home-light-1440.png` and `round9-home-dark-1440.png`.

Verification: frontend 118 tests across 38 files + production build pass.

## Workbench home — real timeline + text/action pairing (round 10, 2026-08-20)

User feedback: the rail timeline was "just a dot" (isolated nodes with no visible axis), and the prep rows separated the label text from its action button across the whole pane width. Full-page review first, then two changes:

- **Agenda Rail is now a real timeline, not dots.** Each date group is a **milestone row** (small dot + day label like `明天` / `2 天后` / `3 天后`) followed by its event rows; every event node is now a 28 px circle carrying an event-type icon (`ChatDotRound` interview, `EditPen` exam, `Clock` followup). A 2 px axis runs through every element's box (`::before` on `.agenda-row` / `.tl-milestone`, left-aligned to the node column center), starting at the first milestone's node and ending at the last event's node — so single-event groups, multi-event groups, and the gaps between groups all stay connected. Selected node = brand-green solid fill with the icon inverted (`--action-fg`), matching the selected row's brand-soft background. `.agenda-group` becomes `display:contents` so the axis is one continuous line across all groups instead of restarting per group.
- **Prep rows: text and action button are no longer split.** The button moved from the far-right `auto` grid column into the copy block, sitting right beside the label on the same line (`object-head` flex row: label + compact bordered action pill, 24 px). Sub-meta merges into a single meta line (`算法工程师简历 · V4 · 7/15 更新`) instead of a separate third line. The timeline connectors on the prep rows were also upgraded to 2 px, starting at the first dot and ending at the last (was: 1 px hairline hidden on the last row).

Browser verification (real local data, light + dark):

- Rail axis geometry at 1440×900: axis x = 148 px (column center); all 4 event nodes (28 px circles) sit centered on it; first milestone dot y≈74 and last node y≈482 are the axis endpoints; milestones `明天` / `2 天后` / `3 天后` render with their labels and dots on the axis.
- 模拟面试 row: label `模拟面试` (x=481) and `开始` button (x=543) are adjacent on the same line — previously the button sat ~555 px away at the pane's far right.
- Selection switching works: clicking a rail row moves the brand-solid selected node, the row background, and the detail header together.
- Unlinked event branch unchanged: `拼多多- HR面` → `未关联求职目标 / 关联目标 / 查看日程`, zero fabricated readiness.
- Five-viewport matrix (1280×800, 1440×900, 1600×900, 1920×1080, 1100×800): all `scrollHeight == innerHeight`; rail and detail internal scroll both 0; node rows align to the same axis at every width.
- Dark tokens unchanged and verified (canvas `rgb(17,18,18)`, rail `rgb(22,23,23)`, selected node brand `rgb(71,181,142)` with inverted icon).

Screenshots stored in `docs/audits/round10-home-light-1440.png` and `round10-home-dark-1440.png`.

Verification: frontend 118 tests across 38 files + production build pass.

## Workbench home — restore old action buttons + left-aligned next action (round 11, 2026-08-20)

User feedback: the timeline is fine now (keep it); the functional buttons after 简历/模拟面试/日程 should go back to the **previous version's style** (right-side text links, not the round-10 bordered pill); and the `开始模拟面试 · 预计 20 分钟` primary button should sit **below the sentence, left-aligned**, not pushed to the right. Two changes:

- **Prep rows: action buttons back to old text-link style.** The round-10 pill (bordered, 24 px, inline with the label) is removed. `.object-row` is a 3-column grid again (`28px | minmax(0,1fr) | auto`); the button is the third column, aligned to the label line (`margin-top:19px`, matching `.object-copy`'s padding), styled as a plain text link (`padding:0;border:0;background:transparent;color:var(--copy);font-size:13px;font-weight:500`, hover `--ink`). The timeline, nodes, axis, and meta line are unchanged.
- **Next action: explicit left alignment.** `.next-actions` gains `justify-content:flex-start` in the component, so the button + `预计 20 分钟` hint follow the `面试前完成一次针对当前目标的模拟面试` sentence from the left edge instead of flex-end.

Root-cause find worth recording: `src/style.css:707` has a **global (unscoped) `.next-actions{justify-content:flex-end}`** rule; the component's scoped rule previously didn't set `justify-content`, so the global rule silently won and the button drifted right even after reloads — it was not a stale dev server. Also fixed en route: `.workbench-dashboard button{font:inherit}` (specificity 0,2,1) was resetting `font-size`/`font-weight` on every workbench button, overriding `.object-action`'s 13px/500 and `.primary-button`'s 14px/600 to the browser default 16px/400 — changed to `font-family:inherit` so buttons inherit the family but keep their declared sizes.

Browser verification (real local data, light + dark):

- Prep rows at 1440×900: labels at x=481, action buttons at x=1040 on the same baseline (y 308/372/436); buttons are borderless text links in `--copy` (13px/500).
- Next action: sentence at (441, 532); `.next-actions` at (441, 568) with `justify-content:flex-start`; primary button at x=441 directly under the text, hint `预计 20 分钟` immediately after (x=569).
- Five-viewport matrix (1280×800, 1440×900, 1600×900, 1920×1080, 1100×800): all `scrollHeight == innerHeight`; `.next-actions` is flex-start and the button stays at x=441 in every viewport.
- Dark tokens verified: text-link button `--copy` rgb(163,163,158), primary button reversed to light bg / dark fg, no scroll.
- Selection switching and the unlinked branch (`拼多多- HR面` → 未关联/关联/查看日程) re-verified as unchanged.

Screenshots stored in `docs/audits/round11-home-light-1440.png` and `round11-home-dark-1440.png`.

Verification: frontend 118 tests across 38 files + production build pass.

## Workbench home — responsive rail + unified content column (round 12, 2026-08-20)

User feedback: in small windows the left timeline's to-do section takes too much share and its text is too large, and this gets more obvious when shrinking the window; in fullscreen the ratio is fine but the right side feels loose. Request: make it responsive so the left side doesn't keep growing in share and the overall proportion stays coordinated.

Measured before the change (real local data):

- 1440×900: workspace capped at 1270, rail fixed 310 (24.6% share); detail pane 951 wide but content column only 640 (`detail-body`) / 620 (`activity-zone`) while `.detail-header` stretched to 899 — content ended at x=1081 with ~300 px of empty pane on the right.
- 1000×800: the rail stayed 310 → **35.6%** of the 870-wide workspace; detail pane squeezed to 551; 15 px rail titles now look oversized next to the small pane.

Changes (all in `TargetDashboard.vue`):

- **Rail steps down with window width** (discrete tiers, not continuous scaling): default 310 → `max-width:1219px` 285 → `max-width:1099px` 260 plus rail typography downgrade (`.rail-title` 15→14, `.row-title` 15→14, `.row-meta` 13→12, milestone label 13→12, tighter milestone/row padding) → `max-width:959px` 240. The width-tier blocks are placed **before** the existing `max-height:899px` compact block so compact heights win when both match.
- **Right content column unified at 720 px**: `.detail-header`, `.detail-body`, `.activity-zone` all `max-width:720px` (was none / 640 / 620), so the header no longer stretches to 899 and the three blocks share one right edge; in wide panes the column fills ~80% instead of ending mid-pane.

Verified geometry (light, real data):

- 1440×900: rail 310 / 24.6%; header = body = activity = 720 (x=441..1161); prep action buttons now sit at the 720-column right edge (x=1120), still on the label baseline.
- Width curve: 1280 → 310 / 27.1%; 1200 → 285 / 26.8%; 1100 → 285 / 29.6%; 1000 → 260 / 30.2% with 14/12 px text (was 35.6% / 15 px); 960 → 260 / 31.6%; 920 → 240 / 30.7%; 850 → 240 / 33.5%.
- All eight viewports: `scrollWidth == clientWidth` (no horizontal scroll) and zero `.row-title` ellipsis truncation.
- Dark theme 1440 re-captured and theme restored to light after capture.

Screenshots stored in `docs/audits/round12-home-light-1440.png`, `round12-home-light-1000.png`, `round12-home-dark-1440.png`.

Verification: frontend 118 tests across 38 files + production build pass.

---

## Round 13 — App-wide semantic tokens + master-detail pages + interview binding fix (2026-08-20)

Goal: take the workbench's visual language (flat surfaces, semantic tokens, 1px separators, one primary action per page) to the remaining pages, and fix the mismatch between the workbench's bound job and the interview lobby's bound job.

### Token single source

- `src/style.css` is now the only token source: light `:root` + dark `body[data-theme='dark']` define canvas/surface/elevated/subtle/hover/selected, ink/copy/muted/text-tertiary, border subtle/default/strong, brand/accent-soft/accent-hover, danger(+soft), warning(+soft), and `--radius-control:10px / --radius-panel:16px / --radius-large:22px`. Refactored views dropped their divergent fallbacks (`var(--brand,#168866)` → `var(--brand)`).
- Filled elements (buttons, bubbles, badges, slider values, score pills) map the old `#101a33`/`#10b981` to `var(--brand)` — not `var(--ink)`, which resolves near-white in dark theme and would produce white-on-white.
- Interview session room + three subcomponents (`InterviewHistoryPanel`, `InterviewRoomSidebar`, `InterviewPlanReviewDialog`) fully tokenized; amber retry UI → `--warning` tokens; all gradients flattened.

### Pages

- **Targets → master-detail** (`TargetListView.vue`): 300 px target rail + right detail pane (identity → linked materials → real-data readiness → actions). The dead `route.query.targetId` preselect is wired up; detail falls back to `activeTargetId` then first target. "新建求职目标" entry added.
- **Resumes → shelf + Inspector** (`ResumeLibraryView.vue`): shelf grid kept (product identity), bottom 1+3+1 card stack replaced by a 360 px Inspector: 继续编辑 (primary), 概况 stat rows, 版本历史 selectable rows. Paper click selects into the Inspector instead of opening the editor.
- **Schedule** (`ScheduleView.vue`): flat calendar pane + 316 px day panel with 1 px divider, no floating cards/shadows; event rows use type dots + 1 px separators.
- **Settings → desktop settings** (`SettingsView.vue`): 200 px section nav (常规 / AI 模型服务) + inspector-form, no big cards; `v-show` keeps both sections in the DOM for tests.
- **Interview lobby → Session Composer** (`InterviewView.vue` lobby branch): page bg flattened to `--canvas`, context cards become section-title + divider + restrained rows, persona avatars neutral single-color with brand border when selected, one primary action 「开始本次练习」.

### Binding fix

`WorkbenchView.openTargetInterview` and `InterviewView.loadOptions` are now consistent: entering from a target binds that target's resume version + JD; missing JD/resume still carries the target context into the lobby (explicit "job pending" state) instead of dropping the query; `open-feedback` no longer routes into the create lobby. No more silent fallback to last-used job/resume.

### Verified geometry (real local data)

- Targets / Resumes / Schedule / Settings at 1440×900, 1100×850, 900×800: `scrollHeight == clientHeight` and `scrollWidth == clientWidth` — zero document scroll, panels scroll internally.
- Interview lobby is a long composer form and scrolls as a document (immersive page, out of scope for fill-mode this round).
- Theme toggle flips `body[data-theme]` on every shell page; screenshots pixel-scanned with a stdlib-only PNG decoder: light captures sample canvas `rgb(245,245,242)`, dark captures `rgb(34,35,35)` (toolbar surface), and **zero pixels match any legacy light-theme hex** (`#101a33 #10b981 #047857 #059669 #0f172a #f59e0b #fef3c7 #92400e #fffbeb #fde68a #fcd34d #bbf7d0 #dbeafe #6ee7b7 #1e3a5f #3b82f6 #8b5cf6`) across all 20 screenshots.

Screenshots stored in `docs/audits/round13-{page}-{light,dark}-{1440,1100}.png` for targets / resumes / schedule / settings / interview (20 files).

Verification: frontend 118 tests across 38 files + production build pass.

---

## Round 14 — 收敛阶段：三页面模板 + token 表 + 表达层收敛（2026-08-20）

Goal: 不再逐页大改。把五页统一到三种模板（A Timeline/List→Workspace / B Library/Master→Inspector / C Composer→Inspector），把「统一数字」写成 token 表，只做表达层收敛。唯一结构性改动是面试大厅 Composer→Inspector。

### 设计系统

- `src/style.css` 新增 `--space-1..8` spacing 刻度（4/8/12/16/20/24/32/40px）；`PageHeader .page-title` 26→28px（page title token 28–30）。绿色语义约束：只用于 selected/success/primary action。
- 三模板 + token 表写入 `docs/decisions.md`（2026-08-20 条目）。

### 首页（只细修三个点位）

- **rail 290px**：`.master-grid` 310→290（实测 `detail.x - master.x = 291` = 290 + 1px divider）；响应式档 270/248/228。1440 与 1100 两档实测全部 rail 行 `scrollWidth == clientWidth`，零截断。
- **顶部去信息重复 + 数据诚实**：已关联事件头部不再重复岗位行（`.detail-role` 仅未关联事件渲染）；新增「关联目标」行（`data-test="detail-linked-target"`）——目标公司 · 岗位 + 「目标详情 ›」内联按钮（`data-test="linked-target-open"`）触发 `open-target`，删除该分支底部重复的 `.target-detail` 按钮。目标回退分支（`kind==='target'`）的 identity-block 与底部按钮保留。
- **准备区**：`.tl-track::after` 贯穿竖线删除；`.tl-dot` 28→32×32、圆改 10px radius 中性容器；`.tl-dot.ready` 右下角 13px 绿色圆 + 白色 ✓（实测 `::after content == "✓"`）。`.object-row` 首列 28→32px。
- **已知偏差（本轮不改，留待统一 spacing 轮）**：首页 rail 行 `min-height:86px` 高于 token 表 Row height 56–64px；首页 Page top 16px 低于 32–40px 档。用户明确本轮首页只动三个点位。

### 求职目标

- 标题下新增 identity/meta 层（全部真实数据，缺则省略）：岗位行 `v-if=roleLine`、状态行 `进行中 · 当前目标 · 2 场近期安排 · 简历 V4`（实测真实数据四段齐全）、`最近更新 2026年8月19日`。
- 「已是当前目标 / 设为当前目标」按钮删除（`activeTargetId` 数据兼容保留，仅 UI 弱化）。实测页面无该文案。

### 简历库

- `.resume-shelf` max-width 980→780、`minmax(190px,1fr)`→`minmax(200px,1fr)`。实测 1440：3 列、卡宽 247px（230–250 区间内）、`.paper` 247×329（aspect-ratio 3/4，落在 310–330 区间；卡总高 358 含下方名字/日期 caption）。
- 绿色去装饰化：`.paper-band` 顶部渐变 → 1px 平线（`--line-subtle`）；`.paper-version` V 徽章 → `--bg-subtle` 底 + `--copy` 字（暗色实测 `rgb(22,23,23)/rgb(163,163,158)`，中性）；`.inspector-kicker`、import 卡 hover 与弹窗装饰绿 → 中性。保留 `.paper-card.selected .paper` 绿边（实测 brand 色）、`.state-primary`、`.btn-primary`、`.version-row.active`。

### 日历

- 密度：`.day-cell` min-height 70→58、grid gap 6→4（实测 42 cells 均高 58px，≈ −17%）；959px 档 54→46。day-panel 316→320px。
- selected day 弱化：`.day-cell.selected` 无品牌绿边，仅 `--bg-selected` 极浅表面（实测暗色 `rgba(71,181,142,0.11)`）；日期数字套 26px 绿色圆 + 白字（与 today 同款）。

### 模拟面试大厅（唯一结构性改动，功能/状态机零改动）

- **左列 Composer**（`.lobby-create-card` 重写）：标题「新建一次面试演练」；简历/目标两行（未锁定时紧凑 el-select，来自目标绑定时静态值 + 「从工作台更换 ›」）；题目 slider 3–10 行内；面试官从 12 卡网格改为水平队列（头像 + 名字/角色 + 前移/后移/移除，单选队列时前后移禁用合理）；「更多面试官…/自定义」展开/收起原网格；重点考察方向 + 开始主按钮保留。删 `.lobby-selected-strip`/`.bound-context-card`/`.context-selectors` 及其 dead CSS。
- **右列 Inspector**（`InterviewHistoryPanel.vue` 重写）：顶部「本次面试」摘要（`currentPlan` prop，四行：目标/简历/题目/面试官，无完整计划不渲染）；「最近面试」扁平记录行（状态圆点 + 标题/简历·副标题 + `X/X 轮` + 查看复盘/继续面试 + 删除）。`history-open-${id}`/`history-delete-${id}`/`history-filter-${key}` 钩子与空态文案保留。QUICK ACCESS/HISTORY 英文 eyebrow、卡片、pill、进度条全删。
- 实测 1440：composer x=168/w=812，inspector x=996/w=360（Inspector token 320–360 内），plan-summary 4 行，全页无英文 eyebrow 残留。

### 数据诚实性

- 首页「关联目标」行：`detail.companyLabel || detail.targetName` + `· roleLabel`。当岗位记录 companyName 为空时回退显示目标名（实测「腾讯Java后端实习 · Java后端实习」），属真实数据回退而非编造；岗位信息完整的岗位会显示「公司 · 岗位」。未关联事件绝不渲染目标信息。

Screenshots stored in `docs/audits/round14-{home,targets,resumes,schedule,interview}-{light,dark}-{1440,1100}.png`（20 files）。

Verification: frontend 120 tests across 38 files + `vue-tsc --noEmit` 无错 + production build pass。

## Round 15 — 产品逻辑补全 + 空间密度 + 绿色语义（2026-08-20）

按用户 P0/P1/P2 三点评审执行；只做评审点名的改动，新增展示一律只用真实数据（无「技术面」标签、无简历页数——两者无数据源，如实省略）。

### P0 产品逻辑

- **Settings 配置流程重写**：删硬编码 DeepSeek 预设（`deepseek-chat` 不再默认填充）。空态（无 provider）时中间 240px 配置列表栏 collapse，页面为两栏；文案按用户规定：「AI 模型 / 使用你自己的 API / 尚未配置任何服务。AI 功能只会在你主动使用时调用已配置的服务。/ ＋ 添加 AI 服务」。点击后进入「服务商 select + 配置名称 + API Key + [验证并继续]」，不出现任何模型/协议/URL 字段；验证成功后才经 `fetchAiProviderModels` 填充模型下拉（空则高级设置手动填 Model ID），「高级设置 展开 ›」默认折叠（协议/Base URL/Model ID）。有 provider 后恢复三栏（providers 列表带 ● 已连接 + 配置表单 + 保存并设为默认）。
- **求职目标页：字段详情 → 求职项目工作台**。右栏四段：① identity（公司/岗位/`进行中 · 当前目标 · 2 场近期安排 · 简历 V4`/最近更新）② 准备进度三列（简历 `V4 已就绪 · 打开简历 ›` / 模拟面试 `1 次完成 · 查看复盘 ›` / 日程 `2 场安排 · 查看日程 ›`）③ 下一场（`明天 09:50 · 字节跳动 · 笔试` + 反馈驱动建议行 + `开始准备 ›`）④ 岗位信息（岗位名 + `JD 已录入，待解析` + `查看岗位 ›`）；操作区（重命名/归档/删除）为底部克制行。左列行 `岗位 + 场次 · 简历版本 · 下一场摘要`。全部来自真实 API（`listMyInterviewPlans`/`listScheduleEvents`/`listJobDescriptions`）。
- **面试大厅 Session Composer 化**：「针对」区保留未锁定 el-select（目标绑定态为静态值 + 双「更换」按钮回工作台）；「面试形式」N 道题 slider + 分钟说明，无「技术面」标签；「面试官」小卡（头像/名字/角色 + 擅长方向 style 行）+ 添加/更多；右栏 Inspector「本次练习」上下文卡：目标/简历/面试官/题目/`预计 15–20 分钟`（`estimateSessionMinutes` 3n~4n）；「最近面试」记录行升级：日期 + 标题 + `张老师 → 李架构`（persona 顺序）+ `2/2 轮` + 查看复盘。

### P1 空间与信息密度

- **首页 Split Pane**：默认 330 / 最小 270 / 最大 420，1px hairline + 4px 透明命中区（hover 加宽 + col-resize），pointer 拖拽 + 双击恢复 330 + 键盘 ±8。实测：拖拽钳制 270/420、双击复位 330、键盘微调 282 生效；rail <290 时 compact（隐藏日里程碑，`8月21日 字节跳动 · 笔试` 行内联时间）。覆盖第 14 轮 290px 决策（见 `docs/decisions.md`）。
- **「关联目标」行修复**：单行 `腾讯Java后端实习 目标详情 ›`——目标名已含岗位时不重复追加（数据诚实回退，JD 无公司名时保留目标名公司前缀）。实测无「腾讯Java后端实习 · Java后端实习」重复。
- **简历库**：shelf `minmax(210px,1fr)` + gap 24px（用户直接规定，不再由 AI 判断）；Inspector 上下文化：identity → `继续编辑`（唯一实心绿）→ `用于`（`1 个求职目标 · 腾讯Java后端实习`，`resumeVersionId` 反查 + JD 标签，JD 无公司名时保留目标名公司前缀）→ `内容`（项目经历 2 条/技能 7 项，如实省略页数）→ `最近版本`（5 个版本默认 3 + `查看全部版本 ›`）。
- **日程事件目标上下文**：已关联事件（jobDescriptionId 有值）追加目标名 + `目标详情 ›`（跳 `/targets?targetId=X`）；未关联事件不变。实测 21 日两个事件只有关联的那条（字节跳动 · 笔试）有上下文行。
- **首次启动引导**：4 步自然顺序（① 录入一个目标岗位 ② 关联或创建简历 ③ 添加面试/笔试日程 ④ 开始针对性模拟），CTA「录入第一个岗位」，完成第一步后消失。

### P2 绿色语义

- 每视口最多一个 solid-green Primary CTA，其余为中性/浅文字式。实测：面试房间「提交回答」为唯一绿色（发送/进入下一位/回到简历优化/重试评价均中性）；简历库「继续编辑」唯一绿色（创建空白简历为中性 link）；证据库「保存证据」绿色、「新增证据」中性；目标页「新建求职目标」、日程「添加日程」、设置流程主操作各自唯一。
- 删除全局死 CSS `.primary-green`（`style.css`）。「查看/编辑/详情/更多」类交互全部保持浅文字式。

### 浏览器验证（双主题 × 1440/1100）

- 首页：rail 330 默认；拖拽左/右分别钳制 270/420；双击复位 330；键盘微调 ±8；compact 态 4 行行内时间（8月21日×2 / 8月22日 / 8月23日）；关联目标行无岗位重复；1100 宽零横向溢出。
- 简历库：选择张逸凡简历后 Inspector「用于」显示 `1 个求职目标 · 腾讯Java后端实习`；内容 `项目经历 2 条 / 技能项 7 项`；最近版本 3 + 查看全部。
- 日程：21 日 2 事件，仅关联事件有 `腾讯Java后端实习 目标详情 ›` 上下文行（未关联事件无）。
- 设置：空态两栏（中间栏 collapse）；添加服务表单只有服务商/配置名/API Key + 验证并继续，无模型/Base URL 字段。
- 暗色：body canvas `rgb(16,17,18)`、简历纸面 `rgb(26,27,29)`，无白色缝隙。
- 截图存档：`docs/audits/round15-{home,targets,resumes,schedule,interview,settings-empty}-{light,dark}-{1440,1100}.png`（12 files）+ compact 态一张。

Verification: frontend 136 tests across 40 files + `vue-tsc -b` 无错 + production build pass。
