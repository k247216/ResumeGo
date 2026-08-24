# DSH 交付：FE-03 审查修复完成

- From: DSH
- Type: delivery
- Related task: V2-F1-FE-03
- Date: 2026-08-22T19:05+08:00

已按审查意见在 FE-03 分支完成单一修复提交：
2df8371bf5a002c4d85bd6becdf5a5200b2326b9

## 三个阻断项修复

1. **阶段推进**：StageTrack 点击 PENDING 时传递 stage.id；PipelineView.openTransition(stageId) 保存明确 targetStageId；TransitionDialog 接收 target-stage-id 并展示对应阶段；confirm 后只调用一次 transitionStage({ targetStageId: 点击的阶段, note })，不再猜第一个 PENDING。测试证明：点击指定 PENDING → 只发一次且 targetStageId=12。

2. **失效关联**：RelationsPanel 摘要按 pipeline 的 scheduleEventIds/interviewPlanIds 数量计数（不再只数可解析对象）；RelationDialog 补出缺失 ID 行「关联不可用 #id」并允许解除。测试证明：缺失日程/面试 id 可见、可解除（toggle-schedule/toggle-interview 发出正确 id）。

3. **loadHistory 未处理拒绝**：调用层 void ... .catch(() => undefined) 消化 reject，store 的 historyErrorMessage 仍展示。测试证明：历史失败不产生未处理拒绝且错误可见。

## 验证
- 针对性测试：PipelineView 6 + RelationDialog 2 + router 4 = 12 全过；vue-tsc --force + build 全绿；diff-check clean。
- 未扩大全量测试（按约定）。

请审查 2df8371，批准后集成。集成后请释放下一张任务卡。
