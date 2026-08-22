# V2-F1-FE-03：V2 Career Pipeline 页面

## 1. 任务身份

| 字段 | 值 |
| --- | --- |
| Owner role | Frontend Feature Agent |
| Milestone | V2 F1 Career Pipeline |
| Status | `READY` |
| Base commit | `b58115e25d37f9b2a032c11d3ea15d7e27e74fa0` |
| Branch | `codex/v2-f1-fe-03-pipeline-page` |
| Integration target | `codex/v2-career-os`，仅由 Core Controller 集成 |

执行前完整阅读：

- `docs/superpowers/specs/2026-08-22-v2-pipeline-interaction-design.md`
- `docs/architecture/agent-collaboration.md`

必须从精确 Base commit 创建独立 worktree。

## 2. 用户结果

用户在独立桌面工作区管理多条真实求职管线：创建和切换机会、编辑公司/岗位/JD/简历关联、管理并推进阶段、查看历史、归档恢复，并管理现实日程与模拟面试关联。页面全部使用 V2 Pipeline 数据，不再显示旧 Job Project。

## 3. Scope in

- 将 `/targets` 路由组件替换为 V2 `PipelineView`，路由名保持 `targets`。
- 实现批准 UX 契约中的双栏页面与组件。
- 使用 `usePipelinesStore` 作为 Pipeline 唯一状态入口。
- 使用现有 Resume、JD、Schedule、Interview API 解析选择项和关联摘要。
- 创建、编辑身份/材料、阶段新增/重命名/排序/推进、历史抽屉、归档恢复、日程/面试计划关联管理。
- 覆盖核心视图和交互组件测试。

## 4. Scope out

- 不修改 Workspace、DesktopShell、全局导航结构、简历编辑台、日程页或模拟面试页。
- 不修改 Pipeline types/API/store；FE-02B 已冻结这些契约。
- 不使用 Legacy `useTargetsStore`、JobProject API、Target dialogs 或旧 TargetListView 作为数据源。
- 不增加阶段删除、Pipeline 删除、暂停/关闭命令、完整 JD 编辑、完整简历预览、成长分数或 AI 大卡。
- 不重构全局样式，不增加依赖。

## 5. Allowed files

仅允许：

```text
frontend/src/router/index.ts
frontend/src/router/index.test.ts
frontend/src/views/pipelines/**
frontend/src/components/pipeline/**
```

不得删除或修改旧 `views/targets/**`、`components/targets/**`；V1 文件保留用于维护与迁移参考。

## 6. 冻结交互

### 页面与选择

- 左栏 ACTIVE/PAUSED 在前，ARCHIVED/CLOSED 在后；同组保持 store/API 顺序。
- `/targets?pipelineId={id}` 预选有效 Pipeline；无效 id 使用 store fallback。
- 点击列表更新 store 选择并 `router.replace` query，不触发其他工作区联动。

### 创建与编辑

- 创建必填 name/companyName/roleTitle，可选 JD/resumeVersion；未选时发送 `null`。
- 编辑必须全量提交五字段；对话框成功后关闭，失败时保留输入与错误。
- 简历版本必须来自 `getResumeVersions` 的真实结果；失效 id 显示不可用，不猜测。

### 阶段

- 当前阶段最醒目；只有 PENDING 节点提供推进入口。
- 推进必须确认，可填写可空 note。
- 阶段管理仅新增、重命名、上移/下移排序；没有删除。
- 历史只在打开抽屉时调用 store load，使用服务端顺序。

### 关联与生命周期

- 日程与面试计划按真实 id 显示、关联和解除；缺失对象显示不可用。
- “查看全部”只跳转现有 `schedule` / `interview` 路由，不隐式设置上下文。
- ACTIVE/PAUSED 可编辑并显示归档；ARCHIVED 的详情只读但显示恢复；CLOSED 详情只读且不提供生命周期操作。

## 7. 组件建议

可以在授权目录内调整命名，但职责必须保持：

```text
PipelineView
PipelineListRail
PipelineIdentityPanel
PipelineStageTrack
PipelineMaterialsPanel
PipelineRelationsPanel
PipelineCreateDialog
PipelineEditDialog
PipelineStageManagerDialog
PipelineTransitionDialog
PipelineHistoryDrawer
PipelineRelationDialog
```

避免把整页实现塞进单个超大 Vue 文件。

## 8. 状态与布局验收

- 首次加载、空态、错误重试、已有数据刷新不闪回 Legacy 内容。
- 辅助 Resume/JD/Schedule/Interview 加载失败只降级对应区域。
- mutation 期间防重复提交；失败不关闭 dialog 或污染 store。
- 1080×720 可用且无页面水平滚动；1280+ 保持舒展留白。
- 浅色/深色均使用现有 CSS variables；主要按钮、列表和对话框键盘可达。

## 9. 关键测试

只覆盖功能主路径，不扩大为视觉快照测试：

1. 路由加载 V2 页面且不引用 Legacy store/view。
2. load/空/失败重试和 query 预选。
3. 创建第二条 Pipeline 并保持独立选择。
4. 编辑五字段、显式 null、失效关联修复。
5. 阶段推进与管理，历史按需加载。
6. 归档后保持选中并可恢复。
7. 日程/面试关联和跳转无隐式上下文切换。

验证：

```bash
cd frontend
npm test -- PipelineView.test.ts router/index.test.ts
npm test
npm run build
git diff --check
```

只在交付前运行一次全量测试与 build。

## 10. Commit 与交付

Commit：`feat(pipeline): add V2 management workspace`

通过 DSH delivery 返回最终提交、完成的用户流程、未完成项和已知风险。不得自行合并。

## 11. 直接退回条件

继续使用 JobProject/targets store、绕过 Pipeline store 自行维护领域副本、修改范围外页面或全局壳、目标切换联动简历/日程/面试、伪造材料/历史、实现后端不存在的状态命令时直接退回。
