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

## Interview room alignment pass (2026-08-27)

The interview room follows the approved desktop reference at `/Users/kun/.codex/generated_images/01a018a8-a0af-7520-8951-205d4a367ec5/exec-0273ced9-cf17-44ed-bdcf-ef2fd98fa145.png`.

- The top context strip always contains four mode-specific slots. Role-based sessions show 模式 / 公司 / 岗位 / 简历; knowledge sessions show 模式 / 资料库 / 训练重点 / 资料; experience sessions show 模式 / 题集 / 原题 / 来源. No context is silently omitted when the source changes.
- The left progress rail now keeps only the `n / total` summary, a thin vertical rule, and small state dots. Per-question labels are visually hidden while their accessible labels remain in the DOM.
- The lower-left context identifies the locked model/role and the selected strategy instead of rendering a generic fixed interviewer block.
- The right inspector keeps the reference order: 本次上下文, mode-specific source rows, 当前考察重点, and 系统状态. The model and API status are explicit and do not claim a connection when it has not been detected.
- Answer history now includes the persisted `interview_answers.created_at` timestamp as `submittedAt`; the room renders it beside “你已提交的回答”. After submission, the latest answered question remains visible with its evaluation while the backend advances the session, so experience-mode score and feedback do not disappear.

Verification: frontend production build passes; focused room tests pass (6 tests across 3 files); backend Maven compile passes. Existing Rolldown dependency annotations and chunk-size notices remain non-blocking build warnings.

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

## Round 18 — Resume 可读预览、绑定卡片与版本说明（2026-08-26）

- **固定预览**：简历正文使用可读工作区比例渲染；`.studio-preview` 独立承载滚轮和滚动条，正文滚动不会带动左侧更新栏、页面或右侧检查器；界面不再显示误导性的百分比控件。
- **绑定状态**：已绑定版本显示目标卡片、当前阶段、关联版本以及「查看目标 / 解除绑定 / 更换绑定」；未绑定版本显示「绑定到求职目标」，选择动作使用卡片式对话框，不再默认展示原生 select。
- **版本说明**：编辑台保存新版本前提供可选的用户说明；版本检查器可以进入内联编辑，保存通过 `PATCH /api/v1/resume-versions/{versionId}/summary` 持久化，不触碰简历正文。
- **资产收藏与删除**：收藏按简历资产 ID 保存在本地，和回收站状态分离；更多菜单提供重命名、创建岗位版本和删除到回收站/恢复入口，移除无效占位提示。

验证记录：简历库相关前端 34 项测试通过，`vue-tsc -b` 与生产构建通过；后端 ResumeController 定向测试通过。ResumeService 定向套件存在一项既有时间精度竞态（归档时间前后相差微秒），与本轮改动无关，需后续单独稳定化。

## Round 17 — 简历库中心区分栏与筛选反馈（2026-08-26）

- **中心阅读结构**：更新摘要与简历正文改为独立列，中间使用贯穿工作区的细分隔线；正文右侧贴齐检查器方向，缩放控件保持在独立工具栏层，不再与图例或正文抢占网格空间。
- **更新图例**：三种颜色图例改为更新摘要下方纵向排列，正文不再承载图例；高亮从弱下划线增强为琥珀/绿色/红色荧光带 + 章节色边，默认对比开启，关闭对比后同步消失。
- **资产导航**：移除基础/岗位资产行上的额外“基/岗”标签、彩色侧条和底色，仅保留顶部“基础/岗位版本”分栏及最近修改圆点。
- **筛选与回收站**：漏斗按钮打开真实筛选菜单（全部/基础/岗位版本/回收站）；删除到回收站的列表有明确分组，空列表显示“回收站为空”，回收站数量以当前真实软删除列表为准。

验证记录：简历库专项 24 项、全量前端 68 文件/355 项测试通过；生产构建通过；浏览器核验确认分隔线、纵向图例、无资产行额外标签、筛选菜单和回收站列表均可见/可交互。保留既有 Rolldown 注释、chunk size 和动态 import 警告。

## Round 16 — 简历库版本工作区复现与交互收敛（2026-08-26）

本轮以简历库目标图为视觉基线，严格限制在简历库内部，不改变跨页面全局工具栏。

- **壳层一致性**：移除简历页面专用的带文字深色 rail，恢复与工作台、知识库、日程一致的 56px 全局图标轨道；简历库自身不再改变全局导航语义。
- **顶部工具栏**：删除“简历库”重复标题和“查看原始文件”，搜索栏收紧为 320px 级别，导入/新建/历史/帮助/设置保持单行紧凑布局。
- **资产导航**：基础简历与岗位版本同时通过分组色点、行底色和标记区分；全部/基础/岗位版本仍调用真实 kind 过滤；回收站入口继续调用真实可恢复软删除 API。
- **版本工作区**：预览采用可读比例，版本轨道高度收敛为 42px，正文取消负 margin，更新摘要与正文顶部对齐；在窄视口隐藏常驻检查器并保留可读的资产列表/正文区域。

## Round 19 — 简历库比例、图例和删除语义修正（2026-08-26）

- 三色变化图例改为 `change-column` 底部对齐，左侧更新条目与图例之间保留自然留白。
- 删除了 `140%` 比例标签和无效缩放控件；正文改用 70% 工作区纸张比例，纸张居中并继续只在预览容器内滚动。
- 目标绑定卡片不再把数据库 `resumeVersionId` 当成用户可读的 `V20/V21`，只展示“已绑定简历版本”。
- 收藏是资产级星标；删除改为明确的“删除到回收站”，回收站仍支持恢复，界面不再使用“归档”概念。
- **对比表达**：对比开关默认开启；三色图例（修改/新增/删除）常驻，按钮可以显式隐藏/显示对比；正文由单纯下划线改为与图例一致的轻量荧光块和章节色边。
- **右侧版本工具**：复制只复制简历名称；操作按钮缩小；绿色提示改为中性文字层级；新增真实求职目标选择与绑定，绑定调用 `updateProjectLinks` 写入 `resumeVersionId`。
- **历史与帮助**：顶部历史按钮打开全部版本的动态时间轴；帮助按钮打开完整使用指南；设置入口保留为路由，不提前扩展个性化设置。

验证记录：目标简历库/版本检查器/列表 composable 测试 26 项通过；全量前端 68 文件、354 项测试通过；`vue-tsc -b` 与生产构建通过；浏览器核验确认全局 rail 56px、预览采用可读比例、原始文件入口不存在、三色图例常驻、历史/帮助弹层可打开。构建仅保留既有 Rolldown 注释、chunk size 和动态 import 警告。

## Round 20 — 版本轨道、历史活动与回收站收口（2026-08-26）

- **版本轨道**：从单一绿色圆点升级为带进度线、版本状态、来源标记、更新时间和变更摘要的版本演进轨道；当前版本、内容更新、岗位副本和起始版本使用不同的状态色与符号。
- **历史活动**：顶部历史按钮显示真实记录数；弹层增加最近一次活动摘要、状态徽章和动态节点；右侧历史活动同步显示当前使用、岗位副本、内容更新和初始记录状态。
- **对比开关**：关闭对比时同时关闭正文荧光高亮，但左下角三色图例保持常驻，不再被 `muted` 状态隐藏；高亮数据与章节标记共用同一对比开关。
- **回收站**：保留“归档”作为用户语义，归档进入回收站；支持恢复，并在回收站有真实的“清空回收站”确认流程。
- **目标绑定**：绑定弹窗复用求职计划的公司图标资源，确认按钮改为黑色，不再暴露数据库版本 ID。

验证记录：简历库相关专项测试与生产构建通过；其余构建警告仍为既有依赖注释、chunk size 和动态 import 提示。

## Round 21 — 版本轨道与对比标记视觉收口（2026-08-26）

- 版本轨道保留进度连线、状态徽章、来源符号、更新时间和变更摘要，当前/更新/岗位副本/起始四种状态不再依赖单一绿色圆点。
- 历史活动弹层与右侧活动列表使用同一套状态语义；最近活动有摘要卡，节点按顺序轻量入场，且遵循 `prefers-reduced-motion`。
- 正文对比统一为章节左边线 + 文字底部荧光线，避免整行色块和悬停依赖；关闭对比只关闭正文标记，左下角三色图例始终可见。

验证记录：全量前端 71 文件、367 项测试通过；生产构建通过（仅保留既有依赖注释、chunk size 与动态 import 警告）。

## Round 22 — 时间轴去标签（2026-08-26）

- 版本轨道去除状态胶囊和冗余变更说明，仅保留版本号、节点符号/颜色和日期；当前版本仍以实心节点和轻环强调。
- 历史修改弹层与右侧活动时间轴同步去除状态标签，状态只通过节点颜色、符号和当前记录的字重表达。

验证记录：简历库专项 26 项、全量前端 71 文件/367 项测试通过；生产构建通过。

## Round 23 — 时间轴、收藏与预览收口（2026-08-26）

- **版本时间轴**：进一步收敛为细连线 + 8px 实心节点 + 日期；历史版本统一中性灰，当前版本统一品牌绿，不再显示节点版本标签、状态胶囊或只读徽标，历史版本仍可通过点击节点切换预览。
- **历史活动**：顶部历史入口保持纯图标，不显示记录数量；弹层与右侧活动列表沿用同一灰/绿节点语义，移除当前节点外圈与呼吸动画，避免时间轴视觉膨胀。
- **收藏**：收藏按资产 ID 保存在本地，与归档状态分离；标题栏星标可切换状态，左侧导航提供不带数字徽标的星标入口、收藏行标记和收藏空态，点击可只查看收藏资产。
- **副本模板隔离**：岗位副本创建时只复制一次来源简历的模板偏好，并写入副本自己的资产级键；之后任一方切换模板都不会影响另一方。
- **正文预览**：预览容器通过 `ResizeObserver` 按中心正文分栏宽度自适应，在可读下限与 100% 之间尽量填满可用空间；滚轮仍只作用于正文预览，不改变更新栏与检查器布局。
- **模板库**：模板目录扩展到 25 个选项，新增编辑叙事、经历时间线、单色网格、作品集式、纸张档案 5 个结构化模板；新增模板均改变排版结构，不以单纯换色冒充模板。

验证记录：简历库专项与全量前端测试通过；生产构建通过。保留既有依赖注释、chunk size 与动态 import 警告。

## Round 24 — 本地资料头像与月度使用记录（2026-08-26）

- **头像**：本地用户资料弹窗支持选择默认头像或上传 `image/*` 图片；图片先转为本地 Data URL，保存在 `resumego:local-profile`，桌面工具栏和资料弹窗同步展示，不上传云端；单张图片限制 2MB。
- **使用热力图**：从最近 12 周横向格子改为周一开始的月历网格，支持上/下月切换；日期节点收敛为 GitHub 风格的小方格，只用颜色层级表达强度，悬浮页面内提示显示日期与累计时长；无真实数据时不增加说明性卡片。
- **头像入口**：上传入口收敛为单个 `＋` 图标，不再显示“上传”文字。
- **每日详情**：真实记录由应用启动及每分钟会话计时写入本地，暂无记录的日期仅在悬浮提示中标注临时演示数据，避免占用资料弹窗空间。
- **时间语义**：开始使用、最近使用和连续使用均使用本地自然日计算；日期键不依赖 UTC 字符串，避免时区跨日偏移。

验证记录：`localProfile` 5 项、`DesktopShell` 7 项测试通过；全量前端 72 文件、378 项测试通过；`npm run build` 通过。保留既有 Rolldown 依赖注释、chunk size 与动态 import 警告。

## Round 25 — Interview Engine 四状态与来源可回放（2026-08-26）

- **面试主页**：进入面试先看到最近练习、最近一次复盘摘要和唯一主行动；三种模式不再以三个巨大卡片直接堆在主页。
- **练习准备**：顶部模式切换器保持可见，内容区按自由面试/知识训练/真题演练替换；切换不会串带不兼容的岗位、简历、知识资料或题集 ID。
- **面试房间与复盘**：开始后模式、来源、岗位/简历或资料快照锁定；结束后进入复盘报告，优先展示核心问题、证据和下一步，而不是 KPI 或总分墙。
- **来源可追溯**：来源会话的题目记录保存 `source`、来源引用和用户可见标签，房间题目可区分真实面经、练习题和资料依据缺失；历史回放读取当时快照。

验证记录：面试状态工具与主页/准备模式专项测试 9 项通过；前端全量 73 文件、385 项测试通过，生产构建通过；后端 `InterviewServiceTest`、`InterviewPlanServiceTest`、`InterviewQuestionSetServiceTest` 与 Controller 定向测试通过，`mvn -DskipTests compile` 通过。构建保留既有 Mockito agent、Rolldown 注释、chunk size 和动态 import 警告。

## Round 26 — 面试主页复盘时间线视觉方向（2026-08-26）

- **主页结构**：采用“复盘时间线”方向，去除主页英雄区和三张大模式卡；主区以最近练习时间线为视觉主体，记录按真实会话状态、来源模式和复盘重点展示。
- **右侧辅助栏**：收窄为复盘趋势与本周重点，趋势区保留清晰度、深度、结构、证据、相关性五个维度切换；仅在存在真实逐题评分时绘制折线，没有评分时保留坐标结构并使用诚实空状态，不生成假数据。
- **练习入口**：底部使用三条轻量入口承载自由面试、知识训练和真题演练，统一进入练习准备页，由模式切换器完成上下文配置。
- **响应式**：桌面端保持窄右栏和大留白；窄视口按主区、辅助栏、练习入口顺序纵向排列，时间线仍保持可读的节点与分隔线。
- **画布背景**：浅色模式整页使用纯白背景，时间线和右栏只保留细分隔线；深色模式仍切换为统一近黑画布。

验证记录：`InterviewView.mode.test.ts` 2 项通过，`npm run build`（含 `vue-tsc -b`）通过；浏览器 DOM 核验确认主页包含时间线、真实复盘空状态、三种练习入口和窄辅助栏。构建仅保留既有 Rolldown 依赖注释、chunk size 与动态 import 警告。

## Round 27 — 面试主页固定工作区（2026-08-26）

- **一页完成**：主页改为继承桌面壳层的全高工作区，页面本身 `overflow:hidden`，不再把整页内容交给浏览器滚轮；时间线、右侧复盘栏和底部开始练习入口在同一视口内完成布局。
- **低高度收敛**：在较矮的桌面窗口中，仅按高度收紧上下留白、时间线行高、趋势图高度和练习入口间距，不改变信息顺序；移动/窄视口继续保留自然流式布局，避免内容被截断。
- **边界**：无真实练习记录时仍显示诚实空状态；固定工作区不改变面试记录、趋势评分或模式配置的数据契约。

验证记录：浏览器在 1280×720 与 1024×600 视口确认 `document/body.scrollHeight === clientHeight`，底部“开始练习”完整可见；`InterviewView.mode.test.ts` 2 项通过，`npm run build`（含 `vue-tsc -b`）通过。构建仅保留既有 Rolldown 依赖注释、chunk size 与动态 import 警告。

## Round 28 — 面试记录工作区与能力视图（2026-08-26）

- **主区层级**：近期练习改为目标图中的横向记录列表，明确显示模式类别、模式图标、岗位/资料来源、日期时间、复盘进度、核心问题和进入复盘入口；不加入岗位录入入口，顶部保留日历与记录导出。
- **开始练习**：三种入口仍沿用现有自由面试、知识训练、真题演练实现，移动到主记录区下方的工作区内，不再贴着整页底部固定。
- **能力变化**：右侧增加“趋势 / 五维”切换。折线图继续只读取真实逐题评分；五维视图保留清晰度、技术深度、回答结构、事实准确性和岗位相关性维度，数据不足时只显示空网格与诚实提示，不补造分数。
- **便签提醒**：右侧改为“需要关注的点 / 本周计划 / 下一步目标”三条轻量便签，内容来自最近一次真实复盘摘要；没有摘要时显示明确空状态。
- **一页完成**：主页高度按桌面壳层可用高度计算；1280×720、1024×600 以及低高度下切换五维图均保持 `document/body.scrollHeight === clientHeight`，不产生整页滚轮。

验证记录：浏览器核验记录列表、三种练习入口、趋势/五维切换和三条便签；`InterviewView.mode.test.ts` 2 项通过，`npm run build`（含 `vue-tsc -b`）通过。保留既有 Rolldown 依赖注释、chunk size 与动态 import 警告。

## Round 29 — 当前专注与回答文件导出（2026-08-26）

- **当前专注**：主页从本地求职目标列表按 `createdAt` 取最近创建的目标，展示公司图标、公司/岗位和当前阶段；仅作为上下文提示，不会把目标切换误当成面试配置。
- **开始练习**：入口保留在左侧主工作区的记录列表之后，并利用剩余空间靠近底部但保留底部留白，避免挤占近期记录的首屏阅读区域；顶部激励语保持可见。
- **回答导出**：独立“导出”菜单负责下载 Markdown 复盘（`.md`）或纯文本回答（`.txt`）；“导出记录”只查看本地导出历史，不触发下载。文件包含问题、用户回答、逐题反馈和可用的复盘摘要，不再导出原始 JSON；历史逐题数据加载失败时保留记录摘要并如实说明。
- **工具栏一致性**：导出入口按钮与日历、导出记录采用同一无边框文字按钮样式，格式菜单仅在点击后展开。
- **记录与五维图**：查看全部记录改为主区列表展开/收起，不再触发导出；加载期间不显示瞬时空状态；五维图按真实已评分维度绘制面积，缺失维度保留空轴并给出轻量说明，不阻断其他有效数据。

验证记录：`npm run build` 通过；`InterviewView.mode.test.ts` 2 项通过；浏览器在 1280×720 与 1024×600 视口确认主页无滚动，开始练习入口贴近左侧工作区底部，导出菜单显示两种回答文件格式，五维视图切换后仍保持一屏。构建仅保留既有 Rolldown 依赖注释、chunk size 与动态 import 警告。

## Round 30 — 开始练习入口比例微调（2026-08-26）

- **入口可用性**：三个练习模式入口在桌面端略微增加内边距、图标和文字尺寸，保持横向信息结构与轻量分隔线，点击目标更清晰但不升级为大卡片。
- **留白与位置**：练习入口组向上收敛约 8px，底部仍保留稳定留白；移动端纵向布局不变。

验证记录：浏览器在 1280×720 与 1024×600 视口确认三个入口完整可见且 `document.documentElement.scrollHeight === clientHeight`；生产构建与 `InterviewView.mode.test.ts` 2 项测试通过。

## Round 31 — 三模式准备页与独立复盘工作区（2026-08-26）

- **模式准备隔离**：开始练习先进入模式选择状态；选择后只渲染对应的岗位模拟、知识训练或真题演练配置，不混入其他来源的字段。模式同步到路由，刷新后仍能回到同一准备页。
- **来源边界**：知识训练必须至少选择一份已完成解析的知识库资料；真题演练必须选择本地真实面经题集，原题与 AI 追问沿用来源标签区分；岗位模拟继续显式选择求职计划、简历版本和面试官。
- **复盘工作区**：复盘从主页和历史记录进入独立的白色桌面工作区，优先展示模式与上下文快照、核心问题、回答证据和下一步行动；逐题回答折叠在时间线中，右侧窄栏只显示真实上下文和实际评分，缺少评分时使用诚实空状态。
- **交互连续性**：支持准备页返回面试主页、复盘返回主页、从复盘使用相同上下文再次练习；面试开始后模式和来源保持锁定，不把当前简历或知识资料变化回写到历史快照。

验证记录：面试引擎专项 22 项测试通过；浏览器核验了 1280×720 主页、知识训练和真题演练准备页的配置隔离，并验证历史记录可进入复盘工作区；`npm run build`（含 `vue-tsc -b`）通过。构建仅保留既有 Rolldown 依赖注释、chunk size 与动态 import 警告。

## Round 32 — 独立模式配置页与演示空状态（2026-08-26）

- **直达入口**：首页的自由面试、知识训练、真题演练分别直达自己的 `setup` 页面，不再进入可切换的配置合集；模式写入路由，刷新后仍保持当前配置页。
- **配置工作区**：三种页面均提供来源上下文、可选重点、题量和补充说明。知识训练使用多选资料条目；真题演练使用真实题集条目并明确原题/AI 追问边界；岗位模拟保留求职目标、简历版本和面试官顺序。
- **诚实演示**：无真实项目、简历、资料或面经时显示禁用的“演示数据”条目，只用于说明页面结构，不能被选中或提交给后端。
- **面经边界**：面经在知识库中作为独立的真实面经题集资产管理，真题演练不把普通知识笔记或 AI 生成题混入真实来源。

验证记录：`InterviewComposer.test.ts` 与 `InterviewView.mode.test.ts` 共 9 项通过；浏览器核验三个入口分别挂载唯一配置组件、模式选择器不存在、知识/真题空状态显示演示数据；`npm run build`（含 `vue-tsc -b`）通过。测试环境仍会输出既有本地 3000 端口 EPERM 连接日志，但不影响测试退出码。

## Round 33 — 三模式独立配置工作区视觉落地（2026-08-26）

- **统一工作区**：三种模式采用同一套白色桌面画布、顶部三步进度和细分隔线，但分别进入独立配置页；不再渲染模式合集或重复的产品说明标题。
- **知识训练**：左侧提供资料类型筛选，中间以文件列表显示 Markdown/PDF/TXT 文件类型、修改时间和选择状态，右侧显示真实正文预览与训练深度、题量、重点设置；多选资料通过真实 `KnowledgeDocument` API 形成上下文。
- **岗位模拟**：三列工作区分别承载求职计划、简历版本快照和面试官顺序；面试官可加入、移除、上下调整，开始前的岗位/简历/角色选择保持独立且在开始后锁定。
- **真题演练**：从知识库的真实面经题集筛选手动录入/导入来源，右侧预览原始题目并单独配置追问强度；不提供面试官选择，不把 AI 生成练习题伪装成真实面经。
- **诚实空状态**：没有后端真实资料或题集时展示禁用演示项，所有开始按钮继续由真实 ID 约束，不提交演示数据。

验证记录：三条 setup 路由浏览器核验分别只挂载对应组件；岗位模拟真实目标、简历和面试官可选择且开始按钮随必填项就绪；知识/真题无数据时展示来源边界和禁用演示项；`InterviewComposer.test.ts`、`InterviewView.mode.test.ts` 共 9 项通过；前端全量 74 文件、392 项测试通过；`npm run build`（含 `vue-tsc -b`）通过。构建保留既有 Rolldown 依赖注释、chunk size 与动态 import 警告。

## Round 34 — 三模式配置页像素收敛与整页适配（2026-08-27）

- **目标图结构对齐**：知识训练页固定为“资料表格 + 提问策略”主区与“正文预览 + 已选资料”窄侧栏；真题演练页保留“来源筛选 + 题集列表 + 原题预览”三段结构；自由面试页维持“目标 / 简历 / 面试官顺序”三列结构，三页共用顶部三步进度和右上返回入口。
- **右侧操作区**：开始按钮与右侧检查器同列对齐，保持黑色主操作；知识资料预览增加“预览 / 提取大纲”轻量切换，真题预览明确原始题目来源，不把 AI 生成内容混入来源列表。
- **桌面整页适配**：面试路由改为填充式桌面壳层；在 1280×720 低高度窗口中页面自身不滚动，开始按钮保持完整可见，资料、简历和设置列在各自工作区内独立滚动，避免按钮覆盖内容；窄视口仍回退为自然流式布局。
- **数据边界**：配置页仍只使用真实 API 返回的岗位、简历、知识资料和真实面经题集；无数据时仅显示不可提交的演示条目，开始按钮继续由真实 ID 约束。

验证记录：浏览器核验知识训练、自由面试、真题演练三条 setup 路由在 1280×720 下 `document.documentElement.scrollHeight === clientHeight`，右侧开始按钮完整可见且无整页滚动；`InterviewComposer.test.ts`、`InterviewView.mode.test.ts` 共 9 项通过；`npm run build`（含 `vue-tsc -b`）通过。保留既有 Rolldown 依赖注释、chunk size 与动态 import 警告。

## Round 35 — 自由面试步骤状态与简历来源摘要（2026-08-27）

- **步骤提示**：顶部三步不再固定高亮第一步；自由面试按真实草稿状态推进，岗位与简历确认后进入面试官顺序，至少一位面试官加入后进入开始步骤，已完成节点显示为黑色完成态，当前节点保持黑色强调。
- **简历来源**：简历预览不再写入固定技术栈。选项从当前简历版本的 `ResumeContent` 提取标题、摘要、技能分类以及项目/工作经历中的明确技术栈；没有对应字段时显示诚实空状态，不补造事实。
- **布局比例**：配置页顶部提示收紧垂直间距，主操作按钮限制为窄桌面按钮（最大 320px），保持右侧对齐与整页无滚轮布局。

验证记录：`InterviewComposer.test.ts` 8 项通过；浏览器在 1280×720 下验证步骤由“目标与简历”推进至“面试官顺序”和“开始”，按钮尺寸为 320×40，页面 `scrollHeight === clientHeight`，选中简历预览仅展示接口返回的实际摘要/技能；浏览器控制台无 error。`npm run build`（含 `vue-tsc -b`）通过，保留既有 Rolldown 依赖注释、chunk size 与动态 import 警告。

## Round 36 — 来源题量边界与真题配置元数据（2026-08-27）

- 自由面试题量步进器限制为 5–15；知识训练限制为 1–20；真题演练按所选题集实际原题数动态限制，10 道以上默认从 10 道开始，小题集从 1 道开始。
- 知识训练文件列表保留文件类型、更新时间和大小，文件夹选择会重新加载真实分类下的资料，预览可打开知识库原文。
- 真题演练列表展示显式公司/岗位上下文、来源、题数和更新时间；右侧提供原题预览、追问强度、可移除考察重点和三种答题回顾方式。
- 回顾方式和题集上下文进入开始快照；服务端再次校验题集原题数量，拒绝超过真实题数的请求。

验证记录：前端面试组件专项 22 项通过（含题量、来源、进度与复盘）；前端全量 76 个测试文件、401 项测试通过，`npm run build`（含 `vue-tsc -b`）通过；后端 `mvn -q test` 全量 557 项测试通过（failures=0、errors=0、skipped=0），`mvn -DskipTests compile` 通过。构建仅保留既有 Rolldown 依赖注释、chunk size 与动态 import 警告。

## Round 37 — 移除面试配置演示数据（2026-08-27）

- 面试准备页不再为岗位、简历、面试官、知识资料或真实面经题集注入占位演示项；列表只展示当前用户 API 返回的真实资产。
- 知识库没有真实面经题集时，真题演练显示可操作的空状态并保持开始按钮禁用，不再出现无法删除或修改的“演示题集”。
- 知识训练无资料时同样保持真实空状态；真实资料和真实题集的读取、预览、题量校验链路不变。

验证记录：新增 `InterviewComposer` 空题集断言，确认空知识库不会生成 `isDemo` 题集；面试专项 15 项通过，前端全量 76 个测试文件、402 项测试通过，`npm run build`（含 `vue-tsc -b`）通过。测试环境仍会输出既有本地 3000 端口 EPERM 连接日志，但不影响退出码。

## Round 38 — 未配置 AI 的面试启动引导（2026-08-27）

- 三种面试模式均要求真实 AI Provider 运行时；未配置时后端拒绝启动，不生成空题目或伪造结果。
- 知识训练在创建题目阶段即返回“尚未配置 AI 模型服务，请先在设置页添加模型服务”，与自由面试、真题演练进入房间时的提示统一；前端全局轻量引导可跳转设置或暂不使用。

验证记录：后端面试题源与计划 Controller 定向测试通过，后端全量 559 项测试通过；前端 `AiSetupGuide` 与 `InterviewComposer` 11 项测试通过。

## Round 39 — 真实面经资料桥接与标签弹层（2026-08-27）

- **真实来源**：真题演练配置页读取“真实面经”分类及后代中的 `COMPLETED` 知识资料；选择资料后通过后端登记为题集，未使用演示题目或标题推断公司。
- **来源可追溯**：题集写入 `source_document_id`，同一资料只能登记一次；解析只接受 Markdown front matter 的公司/岗位/图标和正文中的明确列表题目。
- **元数据展示**：有明确公司名时使用构建期本地公司图标，缺失时显示首字母；更新时间来自知识资料/题集真实字段，面试发生时间仍由 Pipeline/Schedule 管理，不以更新时间冒充。
- **标签弹层**：打开新建标签前清除上一次目录错误，弹层打开后自动聚焦名称输入；创建成功后标签实时进入当前用户目录。标签仍是独立目录资产，需在文档检查器中显式关联到当前笔记。

验证记录：`InterviewExperienceParserTest`、`InterviewQuestionSetKnowledgeBridgeTest`、`InterviewModeRepositoryTest`、`H2FileWorkspaceMigrationTest` 定向通过；前端面试组件 13 项通过，`npm run build`（含 `vue-tsc -b`）通过；后端全量 562 项测试通过（failures=0、errors=0、skipped=0）。

## Round 40 — 真实面经格式引导与解析反馈（2026-08-27）

- 知识库命令栏增加“面经格式”入口，弹窗提供可复制的 Markdown front matter + 编号题目模板；真题演练页不增加第二个导入入口。
- 保存或导入后，知识库对归入“真实面经”的资料调用无副作用预览接口，显示已识别题目数量、文本提取等待状态或具体格式错误。
- 解析预览不会创建题集；只有在真题演练中选择资料时才物化题集，题集仍与原文档保持来源关系。

验证记录：新增格式预览服务测试、格式引导与题目数量展示测试；知识库专项测试 11 项通过，后端桥接/控制器测试通过，`npm run build`（含 `vue-tsc -b`）通过。

## Round 41 — 知识库单一来源与选择可撤销（2026-08-27）

- 真题演练不再提供“手动录入 / 导入面经 / 待整理”三种来源筛选；列表统一来自知识库“真实面经”资料，并用轻量来源标签区分形成方式。
- 已关联知识文档的活动题集在列表或详情读取时同步正文和 front matter，题集 ID 不变，题目数量、公司、岗位与图标使用最新值；开始过的面试仍读取快照。
- 知识训练右侧已选资料改为“预览 + 独立取消”两个操作，取消只改变本次草稿，不删除知识库原文。

验证记录：真题演练、知识训练与题集服务的相关专项测试通过；`npm run build`（含 `vue-tsc -b`）通过。

## Round 42 — V2 Workspace 首页发布前视觉验收（2026-08-28）

### 目标与证据

- 目标参考：`docs/superpowers/plans/2026-08-28-v2-workspace-release.md` 中冻结的 Workspace 方向及本轮确认的白色桌面稿。
- 实际页面：`/`，桌面视口 1440×1024，使用本地 API 返回的目标、简历和复盘数据；截图留档于 `output/playwright/homepage-v2.png`（本地 QA 产物，不进入源码包）。

### 检查结果

- **布局**：窄黑色全局工具栏与主画布保持稳定间距；主区与右侧近期安排栏有清晰分隔，右栏宽度可通过键盘/指针调整，窄视口会回退为纵向流式布局。
- **层级**：首页不再渲染“工作台”页面大标题；日期/引导语为弱层级，下一场面试与下一步行动为主层级，最近复盘、简历操作和职达工具为辅助层级。
- **内容**：右栏最多显示接下来 7 天的三场真实安排；无安排、无简历或无复盘时显示诚实空状态，不伪造岗位、面试或评分。
- **颜色**：浅色模式使用白色画布、炭黑文字、浅灰分隔线和绿色状态点；黑色按钮只用于主行动，未引入 AI 宣传横幅或无意义 KPI。
- **交互**：日程、简历、知识库、面试练习和职业目标快捷入口均路由到对应独立页面；选择安排会同步主区上下文，关联目标/简历操作继续沿用现有数据契约。
- **响应式**：在 1440×1024 实测页面内容完整落在单个视口内；前端测试覆盖空状态、真实活动、安排选择、分栏键盘调整和入口路由。

### 结论

Workspace 首页达到 v2.0 发布候选的视觉和功能基线。剩余发布工作只包括目标系统的 Electron 打包、安装包验证和发布资产整理，不再在本轮扩张首页职责。

## Round 43 — Workspace 首页密度与低高度适配（2026-08-28）

- **单页布局**：进一步收紧日期、引导语、下一步、复盘、简历操作和工具入口之间的垂直间距；标准桌面视口 1440×1024 保持完整留白，不产生滚动。
- **视觉平衡**：降低顶部引导语和主标题字号，缩小主操作按钮；提高底部“职达工具”入口的字号与图标存在感，让行动入口不再过小。
- **近期面试栏**：保持窄栏比例，事件行中的公司图标、公司名、日期时间和状态采用稳定的网格布局，避免名称被图标或状态挤压截断。
- **低高度窗口**：新增 820px 以下桌面窗口的紧凑规则，在 1440×760 实测时工具入口和简历操作仍完整可见，无需滚轮。

验证记录：`TargetDashboard.test.ts` 与 `WorkbenchView.test.ts` 定向 20 项通过；前端全量 79 个测试文件、428 项测试通过；`npm run build`（含 `vue-tsc -b`）通过；Playwright 在 1440×1024、1440×760 截图检查通过，控制台 0 error。构建保留既有 Rolldown 注释、chunk size 与动态 import 警告。

## Round 44 — Workspace 首页目标图版式复刻（2026-08-28）

- **版式骨架**：按确认的目标图重排为“顶部引导 → 左侧下一场面试 / 右侧未来 7 天 → 全宽最近练习 → 职达工具 → 底部建议”，移除页面级大标题和额外简历操作条，简历入口保留在职达工具与上下文行中。
- **下一场面试**：真实安排存在时显示公司标识、公司与面试阶段、岗位、时间、主操作和详情入口；无安排时保留诚实空状态，不伪造事件。
- **近期安排**：取消重复的时间轴里程碑，统一为公司图标、公司/阶段、日期时间、状态和箭头的一行结构；默认右栏宽度调整为 440px，左右比例更接近设计稿。
- **信息层级**：最近练习改为横向信息带，工具入口使用更明确的图标与说明；底部建议只使用真实 `nextAction`，不生成假趋势数据。
- **响应式**：1440×1024 和 1440×760 均保持单页，不出现垂直滚动；窄屏继续回退为纵向布局。

验证记录：`TargetDashboard.test.ts`、`WorkbenchView.test.ts` 定向 20 项通过；前端全量 79 个测试文件、428 项测试通过；`npm run build`（含 `vue-tsc -b`）通过；Playwright 截图 `output/playwright/homepage-target-final.png` 与低高度截图通过人工对比，未引入浏览器错误。保留既有 Rolldown 依赖注释、chunk size 与动态 import 警告。

## Round 45 — 下一场面试信息层级（2026-08-28）

- **时间精度**：下一场面试与右侧近期安排统一从时间戳解析 `HH:mm`，保留分钟并兼容 ISO 与本地 SQL 时间格式。
- **公司图标**：优先使用求职计划关联 JD 的公司名；旧日程缺少绑定时从事件标题或目标名称识别公司，再复用同一套本地品牌资源，不伪造岗位关系。
- **辅助操作**：`查看日程`、`面试详情`、`查看全部日程`、`查看复盘`、`更换/切换`统一为 12px 灰色次级操作，箭头与图标同步降级；`开始准备`保留黑色主行动。

验证记录：工作台定向 21 项测试通过；`npm run build`（含 `vue-tsc -b`）通过；桌面视口实测显示 `8月29日 22:30`，真实字节跳动本地图标正常加载，辅助操作字号与颜色符合层级要求。

## Round 46 — Workspace 单一网格重排（2026-08-28）

- **统一骨架**：首页改为单一桌面网格：紧凑头部、左右主工作区、全宽最近练习和职达工具；分隔线仅承担对齐，不再参与拖拽。
- **视觉层级**：日期与引导语归为同一信息组；“下一场面试 / 接下来 7 天”降为功能标签；公司与岗位、时间、主行动按同一纵向节奏排列。
- **下半区**：最近练习摘要限制为单行核心信息，工具入口共用同一内容边界，减少模块之间的拼接感。
- **窗口适配**：桌面标准窗口与 1280×720 低高度窗口均保持单页，无垂直滚动。

验证记录：工作台定向 21 项测试通过；`npm run build`（含 `vue-tsc -b`）通过；浏览器实测真实日程、品牌图标、辅助操作与 `HH:mm` 时间均正常。
