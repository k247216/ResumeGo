# V2-F2-BE-03：解析重试与派生数据彻底清理

## Identity

- Status: `QUEUED`，禁止开始
- Branch: `codex/v2-f2-be-03-recovery-delete`
- Base commit: `Assigned when promoted to READY`
- Depends on: integrated F2-BE-02

## User result

解析失败的资料可以安全重试；用户确认删除文档后，受管原文件、提取内容、分类标签关联、搜索派生数据和 job 全部清理，不留下可搜索幽灵记录。

## Planned scope

- 仅对 retryable failure 创建幂等重试。
- 正在运行/已删除/不可重试状态的确定拒绝。
- 删除影响摘要、明确确认 token 和完整派生清理。
- 文件删除失败、数据库失败和崩溃中断的恢复/补偿策略。

## Scope out

不提供无确认批量删除、回收站、云端恢复或 AI 修复解析内容。

## File ownership to freeze

Knowledge retry/delete service/API/repository、必要 job cleanup port 和对应测试。若需迁移或 Electron 文件删除能力，必须在 ARCH-01 中预先冻结并显式授权。

## Required tests

幂等重试、非 retryable 拒绝、并发重试、删除影响摘要、确认失效、跨用户、文件删除失败、数据库回滚、重启补偿、搜索不可见、其他文档不受影响。运行 `mvn test` 及相关 Electron tests。

