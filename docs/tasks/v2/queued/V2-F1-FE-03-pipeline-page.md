# V2-F1-FE-03：V2 Pipeline 页面

## Identity

- Status: `QUEUED`，禁止开始
- Branch: `codex/v2-f1-fe-03-pipeline-page`
- Base commit: `Assigned when promoted to READY`
- Depends on: integrated F1-BE-02, F1-FE-02 and approved F1-UX-01

## User result

用户在独立 Pipeline 页面创建、选择和推进两条真实求职管线，编辑岗位材料、查看历史并归档/恢复；所有展示来自 V2 数据。

## Planned scope

- 将 `/targets` 的 V2 导航目标切换到批准的 Pipeline view。
- 组合已集成 Pipeline store/API，不在组件复制 fetch 和领域状态。
- 按 UX 契约拆分页面与任务型组件，并覆盖关键组件测试。
- 保留进入 Resume、Schedule、Interview 专业工作区的明确跳转。

## Scope out

不复用 Legacy Job Project store 作为数据源；不重做 Workspace；不加入 Knowledge、成长趋势、完整日历、完整面试或删除 Pipeline。

## File ownership to freeze

只授权 router、一个 Pipeline view、`components/pipeline/**` 和对应测试；全局 DesktopShell/style 只有 UX 契约明确需要时单独授权。

## Required tests

空/加载/失败/重试、创建、切换、推进、历史、编辑材料、无效关联、归档恢复、窄窗口和深浅主题。运行局部测试、`npm test`、`npm run build`。
