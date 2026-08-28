# 面试模式页面与复盘工作区实施计划

> **执行状态：** 本计划已按任务顺序完成；复选框用于保留可审计的交付记录。

**Goal:** 将面试准备拆为三种来源隔离的页面，并把复盘改造成统一白色桌面工作区。

**Architecture:** 保留 `InterviewView.vue` 作为状态编排器，使用查询参数保存 `home/setup/room/review` 及当前准备模式；首页三个入口分别直达对应准备页，`InterviewComposer` 只渲染当前模式的配置组件，不提供模式合集切换。复盘继续读取 `sessionId` 对应的开始快照，由独立展示组件负责核心结论、逐题复盘和真实评分空态。

**Tech Stack:** Vue 3、TypeScript、Element Plus、Vitest、Vite。

**Spec:** `docs/superpowers/specs/2026-08-26-interview-mode-pages-and-review-design.md`

## Global Constraints

- 只有 `ROLE_BASED`、`KNOWLEDGE_TRAINING`、`EXPERIENCE_SIMULATION` 三种模式。
- 知识训练至少选择一份当前用户已完成处理的 Knowledge Document。
- 真题演练至少选择一份当前用户创建或导入且未归档的真实面经题集。
- 面试开始后模式、来源、岗位、简历和资料上下文不可修改。
- 历史复盘读取开始快照，不被当前对象名称覆盖。
- AI 不改变状态、岗位阶段、简历正文或题目来源。
- 不新增假分数、AI 宣传横幅、语音能力或自动投递功能。
- 页面使用白色桌面画布、炭黑文字、细分隔线和窄辅助栏。

---

### Task 1: 让准备状态带有明确模式上下文

**Files:**
- Modify: `frontend/src/utils/interviewEngineState.ts`
- Modify: `frontend/src/utils/interviewEngineState.test.ts`
- Modify: `frontend/src/views/InterviewView.vue`
- Modify: `frontend/src/views/InterviewView.mode.test.ts`

**Interfaces:**
- `InterviewEngineLocation` 增加 `mode: InterviewMode | null`。
- `interviewEngineStateFromQuery` 读取合法 `mode`，仅在 `view=setup` 时保留。
- `interviewEngineStateToQuery(state, sessionId, mode)` 在 setup 状态序列化模式。

- [x] **Step 1: 写模式查询契约测试**

```ts
expect(interviewEngineStateFromQuery({ view: 'setup', mode: 'KNOWLEDGE_TRAINING' })).toEqual({
  state: 'setup',
  sessionId: null,
  mode: 'KNOWLEDGE_TRAINING',
})
expect(interviewEngineStateFromQuery({ view: 'setup', mode: 'unknown' })).toEqual({
  state: 'setup',
  sessionId: null,
  mode: null,
})
expect(interviewEngineStateToQuery('setup', null, 'EXPERIENCE_SIMULATION')).toEqual({
  view: 'setup',
  mode: 'EXPERIENCE_SIMULATION',
})
```

- [x] **Step 2: 运行查询测试并确认旧协议失败**

```bash
cd frontend
npx vitest run src/utils/interviewEngineState.test.ts
```

- [x] **Step 3: 实现合法模式解析与序列化**

使用 `InterviewMode` 白名单解析 query；`home/room/review` 丢弃 mode，非法 mode 回退为 `null`，不把未知字符串传入 composer。

- [x] **Step 4: 将 InterviewView 的 setup 状态与 query 同步**

初始化 `setupMode` 使用解析结果；首页每个 `openPracticeSetup(mode)` 直接使用 `router.replace` 写入 `view=setup&mode=...`，返回主页清除 mode；刷新页面保留当前模式，并清除主页上的旧 mode 参数。

- [x] **Step 5: 运行测试（不在本轮提交，避免触碰其他工作区改动）**

```bash
cd frontend
npx vitest run src/utils/interviewEngineState.test.ts src/views/InterviewView.mode.test.ts
git add frontend/src/utils/interviewEngineState.ts frontend/src/utils/interviewEngineState.test.ts frontend/src/views/InterviewView.vue frontend/src/views/InterviewView.mode.test.ts
git commit -m "feat(interview): persist setup mode in route state"
```

### Task 2: 三种模式只展示自己的准备页面

**Files:**
- Modify: `frontend/src/components/interview/InterviewComposer.vue`
- Modify: `frontend/src/components/interview/RoleBasedSetup.vue`
- Modify: `frontend/src/components/interview/KnowledgeTrainingSetup.vue`
- Modify: `frontend/src/components/interview/ExperienceSimulationSetup.vue`
- Modify: `frontend/src/components/interview/InterviewComposer.test.ts`

**Interfaces:**
- `InterviewComposer` 接收必需的 `mode: InterviewMode`，仅发出 `started`；不再承担跨模式切换。
- `InterviewComposer` 通过 `mode` 条件只挂载一个模式配置组件。
- 三个 setup 组件保持各自 draft 类型，不共享不兼容字段。

- [x] **Step 1: 写隔离渲染测试**

```ts
const wrapper = mount(InterviewComposer, { props: { mode: 'KNOWLEDGE_TRAINING' } })
expect(wrapper.get('[data-test="knowledge-training-setup"]').exists()).toBe(true)
expect(wrapper.find('[data-test="role-based-setup"]').exists()).toBe(false)
expect(wrapper.find('[data-test="experience-simulation-setup"]').exists()).toBe(false)
```

- [x] **Step 2: 运行测试确认单模式入口行为**

```bash
cd frontend
npx vitest run src/components/interview/InterviewComposer.test.ts src/views/InterviewView.mode.test.ts
```

- [x] **Step 3: 实现单模式准备页**

首页练习入口直接传入 `mode`，准备页只显示该模式的配置字段；没有模式的旧 setup 链接回到主页。不同模式通过返回主页重新选择，避免把不兼容草稿引用串带到另一模式。

- [x] **Step 4: 统一准备页白色工作区样式**

移除配置主体的卡片阴影和厚边框，改为白色背景、细分隔线、模式图标和单一“开始训练”行动；知识资料下拉只接收 `COMPLETED` 文档，面经下拉只接收未归档的 `USER_MANUAL` / `IMPORTED_EXPERIENCE` 真实题集。

- [x] **Step 5: 运行组件测试（不在本轮提交，避免触碰其他工作区改动）**

```bash
cd frontend
npx vitest run src/components/interview/InterviewComposer.test.ts src/views/InterviewView.mode.test.ts
git add frontend/src/components/interview
git commit -m "feat(interview): isolate mode-specific setup pages"
```

### Task 3: 重做白色复盘工作区

**Files:**
- Create: `frontend/src/components/interview/InterviewReviewPage.vue`
- Create: `frontend/src/components/interview/InterviewReviewPage.test.ts`
- Modify: `frontend/src/views/InterviewView.vue`

**Interfaces:**
- Props: `session: InterviewStatusResponse`, `plan: InterviewPlanResponse | null`, `history: SessionHistoryItem[]`, `scores: PerQuestionScore[]`。
- Emits: `back-home`, `re-practice`。
- 展示函数只读取传入快照和历史，不修改 session/plan。

- [x] **Step 1: 写复盘核心结论测试**

```ts
const wrapper = mount(InterviewReviewPage, { props: { session, plan, history, scores } })
expect(wrapper.get('[data-test="review-core-issue"]').text()).toContain('缓存一致性')
expect(wrapper.get('[data-test="review-next-action"]').text()).toContain('再次练习')
expect(wrapper.find('[data-test="review-question-row"]').exists()).toBe(true)
```

- [x] **Step 2: 写空状态测试**

```ts
const wrapper = mount(InterviewReviewPage, { props: { session: emptySession, plan: null, history: [], scores: [] } })
expect(wrapper.get('[data-test="review-empty-score"]').text()).toContain('暂无真实评分')
expect(wrapper.get('[data-test="review-score-card"]').exists()).toBe(true)
```

- [x] **Step 3: 实现复盘页面内容层级**

按“模式/来源快照 → 核心问题 → 回答证据 → 下一步行动 → 可展开逐题时间线”渲染；默认突出核心结论，逐题内容折叠；上下文显示快照中的岗位、简历、资料或题集名称。

- [x] **Step 4: 实现窄右栏和白色桌面布局**

主区占剩余宽度，右侧固定 220–260px；仅有真实评分时显示维度，评分为空则显示诚实空态；不使用大面积阴影、渐变、KPI 卡片或“AI 已连接”提示。

- [x] **Step 5: 在 InterviewView 中接入并保持路由行为**

将原 review 区替换为 `InterviewReviewPage`；历史记录、完成会话和再次练习均通过事件回到既有状态机；再次练习清除当前 session，进入 setup 但不修改历史快照。

- [x] **Step 6: 运行复盘测试（不在本轮提交，避免触碰其他工作区改动）**

```bash
cd frontend
npx vitest run src/components/interview/InterviewReviewPage.test.ts src/views/InterviewView.mode.test.ts
git add frontend/src/components/interview/InterviewReviewPage.vue frontend/src/components/interview/InterviewReviewPage.test.ts frontend/src/views/InterviewView.vue
git commit -m "feat(interview): add white review workspace"
```

### Task 4: 端到端视觉与数据边界验收

**Files:**
- Modify: `frontend/src/views/InterviewView.mode.test.ts`
- Modify: `docs/design-qa.md`

- [x] **Step 1: 增加关键流程断言**

覆盖首页 → 各模式直达配置页 → 知识训练缺资料不可开始 → 资料选择后可开始；首页 → 真题演练无题集不可开始 → 真实题集选择后可开始；完成后进入 review；复盘再次练习不修改旧 session。

- [x] **Step 2: 运行前端专项测试与构建**

```bash
cd frontend
npx vitest run src/components/interview src/utils/interviewEngineState.test.ts src/views/InterviewView.mode.test.ts
npm run build
```

- [x] **Step 3: 浏览器核验白色桌面布局**

在 1280×720、1024×600 视口检查 setup 三种模式单独渲染、review 主区/窄右栏对齐、无页面滚动；检查知识资料和真题题集的空态文案。

- [x] **Step 4: 更新设计 QA 记录**

记录模式页面隔离、知识/面经来源门槛、复盘空态、白色布局和尺寸核验结果。

- [x] **Step 5: 汇总变更（不在本轮提交，避免触碰其他工作区改动）**

```bash
git add frontend/src/views/InterviewView.mode.test.ts docs/design-qa.md
git commit -m "test(interview): verify isolated modes and review flow"
```
