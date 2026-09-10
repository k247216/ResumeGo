# 移动端参考图风格重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将移动端四个主页面统一为参考图的浅色画布、白色工作区、柔和语义卡片和高留白产品风格，优先完成日程页。

**Architecture:** 保留现有 Vue Router、local-first store 和底部导航，只在 `app.css` 建立视觉 token/组件层，在各页面通过语义 class 组合新的卡片层级。日程页先完成可运行的参考图式布局，再将相同组件语言迁移到目标、简历和我的页面。

**Tech Stack:** Vue 3、TypeScript、Vue Router、Vite、Vitest、Capacitor 现有本地存储与通知能力。

**Spec:** `docs/superpowers/specs/2026-09-10-mobile-reference-ui-redesign-design.md`

## Global Constraints

- 保持本地优先，不新增云端同步、账号、岗位爬虫或假数据。
- 不改变现有路由、`store.ts` 数据结构和日程提醒/手机日历同步逻辑。
- 颜色表达状态：薄荷绿、粉色、淡黄、淡紫；黑色只用于核心文字和主操作。
- 手机竖屏单列；宽屏/平板可使用两列目标卡；底部导航固定且不遮挡内容。
- 所有核心动作保留加载、空态、成功、失败和删除确认反馈。
- 每个批次完成后运行相关 Vitest 测试、`npm run build` 和 `git diff --check`。

---

### Task 1: 建立参考图风格的全局视觉基线

**Files:**
- Modify: `mobile/src/styles/app.css`
- Modify: `mobile/src/components/TabBar.vue`
- Modify: `mobile/src/components/AppIcon.vue`（仅在现有图标映射缺失时补充，不新增依赖）
- Test: `mobile/src/data/theme.test.ts`

**Interfaces:**
- Consumes: 现有 `--canvas`、`--surface`、`--brand` token 与 `Theme` 主题切换。
- Produces: 统一的 `pastel-*` 语义色、`workspace-card`、`metric-card`、`action-arrow`、`page-head` 和新的 TabBar 视觉状态，页面任务只使用 class，不直接复制颜色值。

- [ ] **Step 1: Write the failing theme regression assertion**

在 `mobile/src/data/theme.test.ts` 增加：

```ts
it('保留浅色、薄荷绿、深色三种主题选项', () => {
  expect(THEME_OPTIONS.map((item) => item.value)).toEqual(['light', 'mint', 'dark'])
})
```

- [ ] **Step 2: Run the focused test**

Run: `npm test -- --run src/data/theme.test.ts`

Expected: PASS；如果主题选项顺序或数量已经漂移，先修正数据而不是在样式中兼容错误状态。

- [ ] **Step 3: Update the global tokens and reusable classes**

在 `mobile/src/styles/app.css` 中：

```css
:root {
  --canvas: #edf5f4;
  --surface: #ffffff;
  --surface-subtle: #f8faf9;
  --pastel-mint: #d9ece9;
  --pastel-pink: #f5d7e6;
  --pastel-yellow: #f6e5ad;
  --pastel-lilac: #dfd3ed;
  --ink: #111315;
  --copy: #60666a;
  --muted: #8c9397;
  --radius-panel: 22px;
  --shadow-card: 0 1px 0 rgba(17, 19, 21, .03);
}
.workspace-card { border: 0; border-radius: var(--radius-panel); background: var(--surface); box-shadow: var(--shadow-card); }
.metric-card { border: 0; border-radius: 18px; padding: 16px; }
.action-arrow { display: grid; place-items: center; width: 32px; height: 32px; border: 0; border-radius: 50%; background: var(--surface); color: var(--ink); }
.pastel-mint { background: var(--pastel-mint); }
.pastel-pink { background: var(--pastel-pink); }
.pastel-yellow { background: var(--pastel-yellow); }
.pastel-lilac { background: var(--pastel-lilac); }
```

保留现有语义 token 的暗色和薄荷主题覆盖，确保这些 class 在三种主题下仍有足够对比度。将 TabBar 的 active 状态改为轻量图标与文字强调，不使用大面积胶囊。

- [ ] **Step 4: Run the full frontend checks**

Run: `npm test -- --run && npm run build && git diff --check`

Expected: 现有测试全部通过，构建成功且无 diff whitespace 错误。

- [ ] **Step 5: Commit**

```bash
git add mobile/src/styles/app.css mobile/src/components/TabBar.vue mobile/src/components/AppIcon.vue mobile/src/data/theme.test.ts
git commit -m "feat(mobile): 建立参考图风格视觉基线"
```

### Task 2: 重构日程页为参考图式工作区

**Files:**
- Modify: `mobile/src/views/ScheduleView.vue`
- Modify: `mobile/src/styles/app.css`
- Test: `mobile/src/data/timeline.test.ts`, `mobile/src/data/calendar.test.ts`

**Interfaces:**
- Consumes: `listSchedules()`、`listTargets()`、`companyMark()`、提醒和手机日历同步函数。
- Produces: 日程页的“当前时间 → 今日活动/下一场 → 月历 → 真实面试计划”结构；所有标题、公司、岗位、时间、提醒来自 store。

- [ ] **Step 1: Preserve and test timeline/calendar data contracts**

Run: `npm test -- --run src/data/timeline.test.ts src/data/calendar.test.ts`

Expected: 现有时间轴 31 天、月历选中日期和事件点测试通过；不为视觉改造新增演示事件。

- [ ] **Step 2: Replace the page composition without changing business handlers**

在 `ScheduleView.vue` 只调整模板层级：

```vue
<header class="page-head schedule-head workspace-head">...</header>
<section class="schedule-focus workspace-card pastel-mint">下一场真实面试 + 开始准备/面试详情</section>
<section class="schedule-calendar workspace-card">月历或横向月时间轴</section>
<section class="schedule-list workspace-card">真实面试计划列表</section>
```

保留 `openCreate`、`openEdit`、`submit`、`remove`、`syncToCalendar` 和提醒权限处理；没有事件时只渲染空态和添加入口。

- [ ] **Step 3: Match the reference layout at mobile widths**

新增 `.schedule-focus`、`.schedule-calendar`、`.schedule-list`、`.schedule-activity` 样式：

- 下一场面试使用薄荷或粉色卡片，图标、公司、阶段、精确到分钟时间对齐；
- 月历保留月份切换与事件圆点，选中日期仅使用一个克制的黑色或薄荷色圆点；
- 列表行使用统一公司图标、标题、时间、提醒状态和右箭头；
- 控制字号和间距，确保手机竖屏首屏能看到下一场与月历入口，不产生横向滚动。

- [ ] **Step 4: Verify the schedule flow**

通过浏览器检查：打开 `/schedule` → 点击下一场 → 修改时间/提醒 → 保存 → 打开月历 → 添加到手机日历入口。

Run: `npm test -- --run && npm run build && git diff --check`

Expected: 日程内容只来自本地 store；修改后列表和时间轴同步；空态、错误 toast 和删除确认可见。

- [ ] **Step 5: Commit**

```bash
git add mobile/src/views/ScheduleView.vue mobile/src/styles/app.css
git commit -m "feat(mobile): 重构参考图风格日程工作区"
```

### Task 3: 将同一视觉语言迁移到目标、简历和我的

**Files:**
- Modify: `mobile/src/views/TargetsView.vue`
- Modify: `mobile/src/components/TargetCard.vue`
- Modify: `mobile/src/views/ResumesView.vue`
- Modify: `mobile/src/views/ResumeDetailView.vue`
- Modify: `mobile/src/views/MeView.vue`
- Modify: `mobile/src/styles/app.css`
- Test: `mobile/src/data/store.test.ts`, `mobile/src/data/resumeFile.test.ts`, `mobile/src/data/resumeMark.test.ts`

**Interfaces:**
- Consumes: 现有目标阶段/结果、简历版本/身份图标、主题、备份恢复和删除接口。
- Produces: 四页一致的 pastel 卡片、轻量列表、圆形动作入口和真实数据空态。

- [ ] **Step 1: Run existing domain regressions before visual changes**

Run: `npm test -- --run src/data/store.test.ts src/data/resumeFile.test.ts src/data/resumeMark.test.ts`

Expected: 阶段推进、结果标记、简历删除和身份图标测试通过。

- [ ] **Step 2: Restyle target cards without changing target actions**

在 `TargetCard.vue` 保留阶段推进、轮次选择、菜单、归档和绑定简历事件；仅将卡片拆为公司/岗位头部、进度节点、结果状态和右上角动作，使用 `pastel-pink` 或 `pastel-mint`。

- [ ] **Step 3: Restyle resume cards and detail header**

继续使用 `ResumeMark`，将上传、版本、删除和详情入口放入白色工作区卡；文件格式只在版本元信息显示，不能重新成为主视觉徽标。

- [ ] **Step 4: Restyle the “我的” workspace**

保留真实目标数、日程数、简历数、下一场安排、主题选择和备份恢复；统计使用淡黄/淡紫小卡，不添加假成长数据。

- [ ] **Step 5: Verify all primary actions**

浏览器检查：目标一列/两列切换、阶段与结果编辑、简历上传/删除/查看版本、主题切换、备份恢复。

Run: `npm test -- --run && npm run build && git diff --check`

- [ ] **Step 6: Commit**

```bash
git add mobile/src/views/TargetsView.vue mobile/src/components/TargetCard.vue mobile/src/views/ResumesView.vue mobile/src/views/ResumeDetailView.vue mobile/src/views/MeView.vue mobile/src/styles/app.css
git commit -m "feat(mobile): 统一目标简历与个人空间视觉"
```

### Task 4: 视觉验收与移动端发布基线

**Files:**
- Modify: `docs/design-qa.md`
- Modify: `mobile/package.json`
- Modify: `mobile/package-lock.json`

**Interfaces:**
- Consumes: Task 1–3 的页面和视觉 token。
- Produces: 手机竖屏、宽屏/平板两组截图验收记录和可构建的移动端 UI 批次。

- [ ] **Step 1: Run the complete automated checks**

Run: `cd mobile && npm test -- --run && npm run build`

Expected: 全部 Vitest 测试通过，Vite 生产构建成功。

- [ ] **Step 2: Capture the four primary pages**

使用本地浏览器分别检查 `/schedule`、`/targets`、`/resumes`、`/me`，至少覆盖手机竖屏和宽屏/平板宽度；记录横向溢出、底部遮挡、文字截断和主题对比度。

- [ ] **Step 3: Update design QA evidence**

在 `docs/design-qa.md` 增加本批次日期、截图路径、检查尺寸、核心交互结果和未解决的 P3 视觉问题；不得把“构建通过”当作视觉验收替代品。

- [ ] **Step 4: Bump the mobile patch version**

将 `mobile/package.json` 与 `mobile/package-lock.json` 版本从 `0.2.4` 提升到 `0.2.5`，仅在所有检查通过后执行。

- [ ] **Step 5: Commit the release baseline**

```bash
git add docs/design-qa.md mobile/package.json mobile/package-lock.json
git commit -m "chore(mobile): 发布参考图风格 UI 批次 0.2.5"
```

## Self-review

- Spec coverage: 全局 token/组件对应 Task 1；日程第一优先级对应 Task 2；目标、简历、我的对应 Task 3；响应式、交互和构建验收对应 Task 4。
- Placeholder scan: 未发现占位说明或未定义的实现任务。
- Type consistency: 计划只复用现有 store、组件和路由接口，没有引入未定义的数据类型或 API。
