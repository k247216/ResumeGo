# 职达 v2.0 工作台与桌面发布实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将已确认的“面试临近提醒工作台”视觉稿落地到首页，保持现有模块入口可用，完成前端/后端验证和 Electron 发布准备。

**Architecture:** 保留 `WorkbenchView` 负责数据装配和路由动作，重构 `TargetDashboard` 为行动优先的桌面工作区。首页只消费现有目标、简历、面试计划和日程接口，不新增跨模块数据耦合；所有入口通过现有 `TargetDashboardAction` 向上交给路由层。发布阶段只整理版本元数据、README 和 Electron 构建产物，不改变本地优先存储边界。

**Tech Stack:** Vue 3 + TypeScript + Vite + Element Plus；Spring Boot + MyBatis-Plus + H2；Electron + electron-builder；Vitest；Maven。

**Spec:** `docs/product/productV2.md`、视觉基线 `/Users/kun/.codex/generated_images/01a018a8-a0af-7520-8951-205d4a367ec5/exec-2e88ebe5-239e-4f28-9275-1ad47f9cc974.png`。

## Global Constraints

- 首页回答“下一场面试是什么、现在该做什么”，不复刻 Career Pipeline 详情。
- 第一视觉为下一场真实面试与单一准备动作；未来 7 天面试为主要辅助上下文。
- 岗位、简历、知识库只作为当前面试上下文，不展示重复的完整管线。
- 保留简历、知识库、面试、日程、求职目标五个快捷入口；不新增伪功能。
- 白色/暖白画布、窄黑色工具栏、黑色主按钮、克制绿色状态；不得出现 AI 横幅、假指标或巨型卡片网格。
- 不删除或改写用户未授权的现有后端契约；不把密钥、回答或个人资料写入日志和仓库。
- 首页在常见桌面窗口内单屏呈现；小屏通过压缩间距和隐藏次要信息保持可用。

### Task 1: 重构首页工作区视觉与语义

**Files:**
- Modify: `frontend/src/components/workbench/TargetDashboard.vue`
- Modify: `frontend/src/views/workbench/WorkbenchView.vue`（仅补充首页所需派生数据和动作映射）
- Test: `frontend/src/components/workbench/TargetDashboard.test.ts`
- Test: `frontend/src/views/workbench/WorkbenchView.test.ts`

**Interfaces:**
- Consumes: 现有 `AgendaEventView`、`DetailView`、`RecentActivityItem` 和 `TargetDashboardAction`。
- Produces: `data-test="next-interview"`、`data-test="today-action"`、`data-test="upcoming-interviews"`、`data-test="tool-index"` 等稳定选择器；现有入口动作仍通过 `action` 事件向 `WorkbenchView` 冒泡。

- [ ] 写出首页核心结构测试：下一场面试、未来 7 天列表、最近复盘、工具索引和简历入口可见。
- [ ] 保留未关联事件、无目标、无记录等诚实空态，不伪造准备数据。
- [ ] 用统一网格替换旧的可拖拽 master-detail 日程主导布局；`agendaEvents` 仅用于近期面试上下文。
- [ ] 保留现有按钮动作的事件名和目标 ID，确保编辑简历、打开日程、开始面试、查看反馈仍能路由。
- [ ] 在 1440×1024 和低高度窗口下检查无页面滚动条、内容不裁切。

### Task 2: 首页交互与数据联动回归

**Files:**
- Modify: `frontend/src/views/workbench/WorkbenchView.vue`
- Test: `frontend/src/views/workbench/WorkbenchView.test.ts`

**Interfaces:**
- Consumes: `listScheduleEvents`、`listMyInterviewPlans`、`listResumes`、目标 store 及现有路由构建函数。
- Produces: 最近事件排序、最近一条复盘摘要、当前目标简历上下文和入口动作的可验证行为。

- [ ] 验证不同公司面试按时间排序，首页只展示最近三条辅助列表且不会覆盖主行动。
- [ ] 验证面试、简历、日程、知识库、求职目标入口分别指向已有路由。
- [ ] 验证无数据时首页仍能进入录入岗位/创建简历，不展示假面试或假反馈。
- [ ] 验证 API/数据加载错误显示可恢复错误态，不阻塞已有本地编辑功能。

### Task 3: 构建与真实运行验证

**Files:**
- Modify: `frontend/package.json`（仅在版本/发布脚本确有必要时）
- Modify: `README.md`
- Modify: `docs/design-qa.md`
- Create: `design-qa.md`（如根目录报告尚未包含本轮证据）

**Interfaces:**
- Consumes: 前端测试、`npm run build`、后端 `mvn test`、Electron 构建脚本。
- Produces: 可重复的验证命令、设计 QA 证据和 release 说明。

- [ ] 运行前端 Vitest、`vue-tsc`/Vite build 和后端 Maven 测试，记录完整结果。
- [ ] 启动本地前后端，检查首页、五个快捷入口、面试主流程和空态；检查浏览器控制台错误。
- [ ] 用同一 1440×1024 视口对照视觉稿，记录布局、字体、间距、颜色、内容和响应式差异，修复 P0/P1/P2。
- [ ] 运行 `npm run desktop:pack`（当前平台）确认 Electron 目录包可生成；Windows portable 仅在 Windows 环境构建。

### Task 4: v2.0 发布资料与版本标记

**Files:**
- Modify: `README.md`
- Modify: `frontend/package.json`
- Modify: `docs/operations/desktop-development.md`（如发布命令/平台限制需要补充）

**Interfaces:**
- Consumes: 已通过的构建与测试结果。
- Produces: `v2.0.0` 版本元数据、GitHub Release 文案和常见开源 README 结构。

- [ ] 将产品介绍、核心闭环、本地优先/隐私边界、功能模块、开发运行、桌面打包、限制和贡献方式整理成 README。
- [ ] 统一发布版本号为 `2.0.0`，不修改应用内部数据协议版本。
- [ ] 提交仅包含本轮明确修改和当前已纳入 v2 的工作树内容，创建带注释标签 `v2.0.0`。
- [ ] 推送当前远端并创建 GitHub Release；若网络或权限阻塞，保留可复制的 release 命令和明确阻塞说明。

## Verification Checklist

- [ ] `cd frontend && npm test -- --run`
- [ ] `cd frontend && npm run build`
- [ ] `cd backend && mvn test`
- [ ] `cd frontend && npm run desktop:pack`
- [ ] 浏览器/桌面运行：首页主行动、面试列表、简历入口、日程入口、工具索引和空态逐项可用。
- [ ] `design-qa.md` 以 `final result: passed` 结束，或在阻塞时如实写明 `blocked`。
