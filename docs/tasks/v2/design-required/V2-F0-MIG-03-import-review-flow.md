# V2-F0-MIG-03：桌面导入审阅与确认流程

## Identity

- Status: `DESIGN_REQUIRED`，禁止编码
- Planned branch: `codex/v2-f0-mig-03-import-ui`
- Depends on: integrated MIG-02 API and approved interaction design

## User result

首次启动或设置页中，用户主动选择 V1 备份，先看到 dry-run 数量、警告和不会修改 V1 的说明，再明确确认导入；成功后看到回执，失败后可以安全重试或退出。

## Design decisions required

- 首次启动入口与设置入口是否复用同一流程。
- Electron 文件/目录选择的最小 IPC capability，不向 renderer 暴露任意 Node API。
- dry-run、确认、执行、成功、部分不支持、失败和取消状态。
- 导入前 V2 当前数据非空时的阻止/新 workspace 策略。
- 恢复备份和导入失败后的用户文案。

## Scope out

不自动发现 V1、不后台导入、不用进度动画掩盖失败、不承诺未验证记录成功迁移。

## Acceptance design

需覆盖键盘操作、路径不可读、源文件在检查后变化、窗口关闭、后端退出、重试与已导入 fingerprint。设计通过后拆出精确 Electron/UI Allowed files，并转为 `QUEUED` 或 `READY`。
