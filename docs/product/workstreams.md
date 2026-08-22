# Career OS V2 工作流登记表

Status: Active control register

Controller: Core Controller

Integration branch: `codex/v2-career-os`

Dispatch baseline: `d701ddc87aafc1a4f3d71043c4108c87fd8814d0`

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
| V2-F0-MIG-01 | 导入契约与 dry-run | `CORE_RESERVED` | 由 Core Controller 冻结 | `codex/v2-f0-mig-01-import-contract` | 迁移契约、manifest/result/receipt DTO、决策文档 | 无 | A 后可并行设计 |
| V2-F0-MIG-02 | V1 只读检查与事务导入 | `QUEUED` | MIG-01 集成后填写 | `codex/v2-f0-mig-02-import-engine` | migration service/repository/API、迁移测试 | V2-F0-MIG-01 | MIG-01 后 |
| V2-F0-MIG-03 | 桌面导入审阅流程 | `DESIGN_REQUIRED` | MIG-02 集成后填写 | `codex/v2-f0-mig-03-import-ui` | Electron IPC、导入页面与测试 | MIG-02；导入交互批准 | MIG-02 后 |
| V2-F1-BE-01 | Pipeline 阶段历史查询 | `INTEGRATED` | `9274a5e`、`7974b98` | `codex/v2-f1-be-01-transition-history` | Pipeline repository/service/controller、新响应 DTO、对应三层测试 | 无 | 已集成 |
| V2-F1-FE-01 | Pipeline 类型与 API client | `INTEGRATED` | `f4b8aee`、`fc98ad0` | `codex/v2-f1-fe-01-pipeline-api` | 新建 Pipeline 类型、API client 及其测试 | 无 | 已集成 |
| V2-F1-FE-02 | Pipeline Pinia store | `INTEGRATED` | `00a81a9`、`8d9a640` | `codex/v2-f1-fe-02-pipeline-store` | 仅新建 store 及其测试 | V2-F1-FE-01 已集成 | 已集成 |
| V2-F1-BE-02 | Pipeline 身份与材料更新 | `INTEGRATED` | `d33433f`、`42435cb` | `codex/v2-f1-be-02-pipeline-update` | Pipeline 三层、新 update DTO 与测试 | V2-F1-BE-01 已集成 | 已集成 |
| V2-F1-FE-02B | Pipeline update/history 前端契约补齐 | `INTEGRATED` | `b58115e` | `codex/v2-f1-fe-02b-pipeline-contract-completion` | Pipeline types/API/store 及现有对应测试 | BE-01、BE-02、FE-01、FE-02 已集成 | 已集成 |
| V2-F1-UX-01 | Pipeline 交互契约 | `INTEGRATED` | `docs/superpowers/specs/2026-08-22-v2-pipeline-interaction-design.md` | 不创建代码分支 | Pipeline 页面设计规范与验收场景 | F1 数据契约稳定 | 已批准 |
| V2-F1-FE-03 | V2 Pipeline 页面 | `INTEGRATED` | `2572d60`、`0e65947`、`681a37f` | `codex/v2-f1-fe-03-pipeline-page` | router、Pipeline view/components/tests | V2-F1-FE-02B、UX-01 | 已集成 |
| V2-F1-QA-01 | Pipeline 纵向验收 | `QUEUED` | FE-03 集成后填写 | `codex/v2-f1-qa-01-pipeline-acceptance` | 集成/E2E/桌面验收资产 | MIG-03、FE-03 | 4 |
| V2-F2-ARCH-01 | Knowledge 存储与生命周期契约 | `INTEGRATED` | `docs/superpowers/specs/2026-08-22-v2-knowledge-library-foundation.md` | `codex/v2-f2-arch-01-knowledge-contract` | 架构、决策、迁移设计 | F1 领域边界稳定 | 已冻结 |
| V2-F2-BE-01 | Knowledge 元数据基础 | `INTEGRATED` | `4946c0c`、`65c25c0` | `codex/v2-f2-be-01-metadata` | 迁移、domain/repository/service/controller/tests | V2-F2-ARCH-01 | 已集成 |
| V2-F2-IO-01 | 文件导入与可恢复解析 | `INTEGRATED` | `d218ed7`、`a8d4299` | `codex/v2-f2-io-01-import-extraction` | Knowledge 文件副本、文本提取、job/API/tests | V2-F2-BE-01 | 已集成 |
| V2-F2-FE-00 | Knowledge 可用前端切片 | `INTEGRATED` | `4979b04`、`fbe13e2` | `codex/v2-f2-fe-00-knowledge-usable-slice` | Knowledge types/API/store、基础页面、导航 | V2-F2-BE-01；并行采用冻结 IO-01 接口 | 已集成 |
| V2-F2-BE-02 | 分类、标签与关键词搜索 | `INTEGRATED` | `0d01cfd`、`b856a4b` | `codex/v2-f2-be-02-classification-search` | Knowledge backend API、迁移与测试 | V2-F2-IO-01、V2-F2-FE-00 | 已集成 |
| V2-F2-BE-03 | 重试与派生数据清理 | `INTEGRATED` | `2d1fa2a`、`60221fe`、`b5cc6a4` | `codex/v2-f2-be-03-recovery-delete` | job retry/delete service/API/tests | V2-F2-BE-02 | 已集成 |
| V2-F2-FE-01 | 分类搜索可用前端切片 | `INTEGRATED` | `8ff5829`、`8eb2b33` | `codex/v2-f2-fe-01-knowledge-client-store` | types/API/store、现有 Knowledge 页面与测试 | V2-F2-BE-02 | 已集成 |
| V2-F2-IO-02 | 安全打开与定位受管原文 | `INTEGRATED` | `d2204b1`、`d701ddc` | `codex/v2-f2-io-02-managed-source-open` | Knowledge 内部端点、Electron IPC/path guard、renderer adapter/tests | V2-F2-BE-03 | 已集成 |
| V2-F2-UX-01 | Knowledge Library 交互契约 | `INTEGRATED` | `docs/superpowers/specs/2026-08-22-v2-knowledge-library-interaction-design.md` | 不创建代码分支 | Knowledge 多栏页面、层级树、收起行为、检查器与响应式规范 | ARCH-01 | 已批准 |
| V2-F2-BE-04 | 层级资料库分类树 | `INTEGRATED` | `5d26fa1` | `codex/v2-f2-be-04-hierarchical-library` | V10/V30、Knowledge category tree API/service/repository/tests | V2-F2-IO-02 | 6 |
| V2-F2-BE-05 | 本地笔记正文保存 | `READY` | `5d26fa1` | `codex/v2-f2-be-05-note-content` | Knowledge NOTE content API/service/repository/tests | V2-F2-BE-04 | 7 |
| V2-F2-FE-02 | Knowledge Library 页面 | `QUEUED` | IO-02、BE-04、BE-05 集成后填写 | `codex/v2-f2-fe-02-library-ui` | Knowledge view/components/store/API/tests | V2-F2-IO-02、BE-04、BE-05、FE-01、UX-01 | 8 |
| V2-F2-QA-01 | Knowledge F2 纵向验收 | `QUEUED` | FE-02 集成后填写 | `codex/v2-f2-qa-01-library-acceptance` | 集成/E2E/桌面验收资产 | V2-F2-FE-02 | 6 |

## 并行与所有权规则

1. `V2-F2-BE-04` 已集成；当前发放 `V2-F2-BE-05`，补齐 NOTE 正文保存后进入完整 Knowledge Library 页面集成。
2. `V2-F1-QA-01` 仍等待 MIG-03，不得提前将迁移场景伪装为已验收。
3. BE-02 只实现分类、标签与关键词搜索；安全打开受管原文拆为后续 Electron capability，删除仍由 BE-03 负责。
4. 迁移和 Pipeline 页面不因“实现容易”而提前；它们的状态只能由 Core Controller 修改。
5. UX-01 已批准；视觉稿中的层级树、NOTE 正文和文件动作必须等待真实契约，不允许 FE-02 写死。
6. 外部 Agent 只提交功能分支和 `docs/templates/v2-agent-delivery-report.md` 对应的交付内容，不得合并。

## 用户分发步骤

1. 选择一个 `READY` 任务，把任务卡全文发送给一个外部 Agent。
2. 明确要求 Agent 先核验 `Base commit`，再创建任务卡指定分支和独立 worktree。
3. Agent 返回最终提交哈希与完整交付报告后，将二者交给 Core Controller 审查。
4. 未获 `APPROVED` 前，不要把分支合并到 `codex/v2-career-os`。
