# V2-F1-QA-01：Pipeline 纵向验收

## Identity

- Status: `QUEUED`，禁止开始
- Branch: `codex/v2-f1-qa-01-pipeline-acceptance`
- Base commit: `Assigned when promoted to READY`
- Depends on: integrated MIG-03 and F1-FE-03

## Outcome

以自动化和桌面实测证明 F1 的退出条件，而不是新增产品功能。

## Required scenarios

- 空 V2 workspace 创建腾讯、字节两条 Pipeline。
- 两条 Pipeline 使用不同阶段和材料关联，推进一条不影响另一条。
- 重启后当前阶段、历史、JD/Resume、日程和面试关联不丢失。
- 归档/恢复保留数据，非法/跨用户关联被拒绝。
- 从虚构 V1 备份 dry-run、确认导入、重复导入不重复，V1 hash/mtime 不变。
- 前端失败/重试、后端退出、备份恢复和 V1/V2 并行目录检查。

## Planned Allowed files

仅新增 F1 集成测试、桌面 smoke 脚本/记录和必要测试夹具；发现产品缺陷时退回原功能任务，不在 QA 卡中顺手修业务代码。

## Verification

执行后端全量、前端全量、Vite/Electron build、空工作区与重启验证，并按 `docs/testing/v2-final-test-matrix.md` 记录。未进行 Windows/macOS 实机项必须如实标为未验证。
