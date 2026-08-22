# V2-F1-FE-02：Pipeline Pinia Store

## 1. 任务身份

| 字段 | 值 |
| --- | --- |
| Owner role | Frontend Feature Agent |
| Milestone | V2 F1 Career Pipeline |
| Status | `QUEUED`，禁止开始 |
| Base commit | `Assigned when promoted to READY` |
| Branch | `codex/v2-f1-fe-02-pipeline-store` |

## 2. 阻塞条件

必须先满足全部条件：

1. `V2-F1-FE-01` 已由 Core Controller 审查并集成到 `codex/v2-career-os`。
2. 本卡 `Base commit` 被替换为该集成分支的真实完整提交哈希。
3. 本卡移动到 `docs/tasks/v2/ready/`，登记表状态改为 `READY`。

在此之前，任何 Agent 都不得创建分支或编写代码。

## 3. 预定用户结果

为未来 Pipeline 页面提供可预测的加载、选择、刷新和错误恢复状态；目标切换不应隐式改变独立简历资产。具体状态结构将在升为 `READY` 时依据已集成 client 冻结。

## 4. 预定范围

仅计划授权新建：

```text
frontend/src/stores/pipelines.ts
frontend/src/stores/pipelines.test.ts
```

不授权路由、页面、组件、样式、工作台、简历 store 或 API client 修改。

## 5. 升级为 READY 时必须补齐

- 精确 Base commit 与已集成 API 类型名称。
- store 的公开 state/actions 契约。
- 空列表、持久选择失效、并发加载、失败重试与归档后选择行为。
- 真实 RED/GREEN 命令和完整前端回归要求。
- 提交信息、交付格式与直接退回条件。

