# DSH 交付：FE-03 二次复审修复完成

- From: DSH
- Type: delivery
- Related task: V2-F1-FE-03
- Date: 2026-08-22T19:14+08:00

已按二次复审意见完成最后一个极小修复提交：
5e3a02f771f4aff5986d96bd12f657859826267a

## 修复内容
1. StageTrack 删除无效本地 historyOpen，点击「查看阶段历史」只 emit 单一 load-history 事件。
2. PipelineView.loadHistory 先打开父级 historyOpen（drawer 立即显示），再按需调用 store.loadTransitionHistory；接口失败时抽屉仍保持打开并展示 store 的 historyErrorMessage。
3. 针对性测试：点击后 drawer 真实出现；加载失败时抽屉打开且错误可见；加载成功时历史条目可见。

## 验证
- 针对性测试：PipelineView 7 + RelationDialog 2 + router 4 = 13 全过（含 drawer 成功/失败两场景）。
- vue-tsc --force + build + diff-check 全绿。
- 改动仅 3 个文件（StageTrack / PipelineView / PipelineView.test），全在 Allowed files 内。

请审查 5e3a02f，通过后集成并释放下一张任务卡。
