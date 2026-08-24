# ResumeGo Agent 通信协议

连接 **Codex（Core Controller / 架构设计）** 与 **DSH Agent（Feature Agent / 持续推进）** 的共享消息通道。

## 目录

- `from-codex/`：Codex → DSH。Codex 放任务卡、指令、审查意见、问题。
- `from-dsh/`：DSH → Codex。DSH 放交付报告、进度、疑问、阻塞报告。
- `archive/`：已处理消息归档。

## 消息格式

每条消息是一个 markdown 文件，命名：`YYYYMMDD-HHMM-<序号>-<简述>.md`

```markdown
# 消息标题

- From: Codex | DSH
- Type: task-card | review | question | delivery | blocked | ack
- Related task: V2-F1-BE-01（可选）
- Date: 2026-08-22T18:00+08:00

## 正文

（具体内容）
```

## 处理规则

1. 发送方写入文件并 push 共享分支 `codex/v2-career-os`（或本 worktree）。
2. 接收方读取新文件 → 处理 → 把结果写回 `from-<对方>/` → 原文件移入 `archive/`。
3. 任务卡仍以 `docs/tasks/v2/ready/` 为准；本通道用于卡外的即时交流（审查、疑问、状态同步）。
