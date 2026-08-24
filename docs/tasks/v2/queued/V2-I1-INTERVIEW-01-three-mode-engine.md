# V2-I1-INTERVIEW-01：三模式模拟面试引擎

## Identity

- Status: `QUEUED`
- Owner: External Feature Agent after Resume task delivery
- Branch: `codex/v2-i1-three-mode-interview`
- Base commit: 由 Core Controller 在 Resume System 集成后冻结
- Spec: `docs/superpowers/specs/2026-08-25-resume-interview-workspace-contract.md`
- Execution plan: `docs/superpowers/plans/2026-08-25-interview-engine-v2.md`
- Depends on: `V2-R1-RESUME-01` 集成；当前 Pipeline/Schedule 契约修复集成
- Acceptance owner: Core Controller

## User result

用户可以明确选择岗位模拟、知识训练或面经模拟；每种模式只要求真实需要的材料，并在历史中保留当时使用的来源、版本和模式。面经原题、AI 生成练习题和 AI 追问不会混淆。

## Why queued

本任务需要新数据库迁移号、Resume Version 稳定引用和当前正在修复的 Pipeline/Schedule 契约。Core Controller 更新 Base commit 和迁移号后才可开始；不得从占位基线自行推断。

## Frozen contracts

- 三种且仅三种 mode；创建后不可修改。
- 岗位模式要求用户明确选择 Pipeline、Resume Version 和 persona。
- 知识模式只要求 Knowledge Document，不得强制岗位/简历。
- 面经模式只使用本地题集；AI 追问标为 `AI_FOLLOW_UP`。
- 开始上下文快照不可变且不包含正文、API Key 或绝对路径。
- 状态机仍由确定性代码控制；AI 不写状态和来源类型。
- 完成后只生成待处理 Feedback Event，不修改简历或推进 Pipeline。
- 不修改 Workspace，不实现语音面试。

## Planned file ownership

仅允许执行计划中列出的 Interview backend/frontend 文件、冻结后的双数据库迁移和交付报告。不得修改 Resume、Pipeline、Schedule、Knowledge 页面或 Workspace。若 Knowledge API 不足，停止并报告，不得跨模块临时补接口。

## Activation gate

Core Controller 必须填写：

1. 新 Base commit；
2. 无冲突的 MySQL/H2 迁移号；
3. Resume fork/版本响应最终字段；
4. Pipeline 与 Schedule 关系最终字段；
5. Knowledge 文档读取和来源定位接口版本。

五项齐全后把本卡移入 `ready/`，External Feature Agent 才能按计划 Task 1–7 开始。
