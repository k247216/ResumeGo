# Career OS V2 工作流登记表

Status: Active control register  
Controller: Core Controller  
Integration branch: `codex/v2-career-os`  
Dispatch baseline: `1f83b1ac02e62793813d174a2f451a42fc5c3e57`

本文件是 V2 外部 Agent 的唯一任务状态入口。只有位于 `docs/tasks/v2/ready/` 且状态为 `READY` 的任务可以开始。执行者领取任务时，将对应任务卡全文交给该 Agent；不得只转述标题。

## 状态定义

- `CORE_RESERVED`：涉及迁移、共享契约或产品决策，只能由 Core Controller 设计并另行授权。
- `READY`：基线、文件范围、契约和验收均已冻结，可以交给一个外部 Agent。
- `IN_PROGRESS`：用户已将任务卡交给一个 Agent；开始后应在本表登记负责人和分支，避免重复领取。
- `REVIEW`：Agent 已按模板交付，等待 Core Controller 审查。
- `QUEUED`：依赖未满足，禁止开始。
- `DESIGN_REQUIRED`：交互或产品契约未冻结，禁止编码。
- `INTEGRATED`：已通过审查并进入 V2 集成分支。

## 当前登记

| Task ID | 模块 | 状态 | 基线/生成规则 | 功能分支 | 文件所有权摘要 | 依赖 | 集成顺序 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| V2-F0-MIG-01 | V1 只读导入 | `CORE_RESERVED` | 由迁移设计冻结 | `codex/v2-f0-mig-01-v1-import` | 迁移器、迁移 DTO、迁移测试；禁止写 V1 数据 | 迁移设计与恢复策略 | 单独审查后才可进入 F1 |
| V2-F1-BE-01 | Pipeline 阶段历史查询 | `READY` | `1f83b1ac02e62793813d174a2f451a42fc5c3e57` | `codex/v2-f1-be-01-transition-history` | Pipeline repository/service/controller、新响应 DTO、对应三层测试 | 无 | 1 |
| V2-F1-FE-01 | Pipeline 类型与 API client | `READY` | `1f83b1ac02e62793813d174a2f451a42fc5c3e57` | `codex/v2-f1-fe-01-pipeline-api` | 新建 Pipeline 类型、API client 及其测试 | 只依赖已冻结的现有 `/api/v2/pipelines` 契约 | 1，可与 BE-01 并行 |
| V2-F1-FE-02 | Pipeline Pinia store | `QUEUED` | FE-01 集成后填写真实提交 | `codex/v2-f1-fe-02-pipeline-store` | 新建 store 及其测试 | V2-F1-FE-01 已集成 | 2 |
| V2-F1-UI-01 | Pipeline 页面 | `DESIGN_REQUIRED` | 不适用 | 未分配 | 路由、页面、组件、样式、视图测试 | Pipeline 信息架构与交互稿确认；FE-02 已集成 | 3 |

## 并行与所有权规则

1. 当前仅 `V2-F1-BE-01` 与 `V2-F1-FE-01` 可分发，且必须交给不同分支和独立 worktree。
2. 两个任务没有重叠文件；任一 Agent 请求共享类型、迁移、路由或 UI 文件时必须停止，不能自行扩大范围。
3. `V2-F1-FE-02` 只有在 FE-01 的最终提交被 Core Controller 集成后，才能移入 `ready/` 并写入新的完整基线哈希。
4. 迁移和 Pipeline 页面不因“实现容易”而提前；它们的状态只能由 Core Controller 修改。
5. 外部 Agent 只提交功能分支和 `docs/templates/v2-agent-delivery-report.md` 对应的交付内容，不得合并。

## 用户分发步骤

1. 选择一个 `READY` 任务，把任务卡全文发送给一个外部 Agent。
2. 明确要求 Agent 先核验 `Base commit`，再创建任务卡指定分支和独立 worktree。
3. Agent 返回最终提交哈希与完整交付报告后，将二者交给 Core Controller 审查。
4. 未获 `APPROVED` 前，不要把分支合并到 `codex/v2-career-os`。

