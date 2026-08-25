# Career OS V2 阶段与发布最终测试矩阵

本矩阵由 Core Controller 在里程碑集成和发布候选阶段执行。功能尚未实现时必须标记 `NOT_RUN_UNTIL_IMPLEMENTED`，不能以占位页面、mock 或旧 V1 行为记为通过。

## 1. 自动化基线

| 层级 | 命令 | 通过条件 | 证据 |
| --- | --- | --- | --- |
| Backend 全量 | `cd backend && mvn test` | 退出码 0；报告无失败/错误/跳过异常 | 命令、时间、测试数、报告路径 |
| Frontend 全量 | `cd frontend && npm test` | 退出码 0；全部 Vitest 文件和用例通过 | 命令、文件数、用例数 |
| Vite production | `cd frontend && npm run build` | Vue TS 与 Vite 均通过；无阻塞 warning | 命令、退出码、dist 结果 |
| Electron TypeScript | `cd frontend && npm run build:electron` | main/preload 编译通过 | 命令、退出码 |
| Desktop prepare | `cd frontend && npm run desktop:prepare` | backend jar、frontend dist、Electron 输出均生成 | 命令、退出码、产物路径 |
| Diff hygiene | `git diff --check && git status --short` | 无格式错误；工作区状态可解释 | 完整输出 |

自动化测试不得依赖真实模型服务、真实简历或访问密钥。

## 2. 桌面与数据生命周期

| ID | 场景 | 当前门禁 | 验收步骤与通过条件 |
| --- | --- | --- | --- |
| DESK-01 | 空工作区首次启动 | 必测 | 使用全新 V2 userData 启动；无 V1 文件时可进入引导/空态，不崩溃、不写 V1 路径 |
| DESK-02 | 关闭并重新打开 | 必测 | 创建本地 V2 记录后退出再打开；记录、选择和设置按契约恢复 |
| DESK-03 | V2 小版本升级重开 | 每个 release candidate | 用上一候选版本数据启动新版本；Schema 自动升级且记录可读 |
| DESK-04 | V1 只读导入 dry-run | `NOT_RUN_UNTIL_IMPLEMENTED` | 选择 V1 备份，只检查和生成报告；源文件 hash/mtime 不变，不写 V1 目录 |
| DESK-05 | V1 正式导入与失败回滚 | `NOT_RUN_UNTIL_IMPLEMENTED` | 从备份复制到 staging、校验、转换；失败时 V2 无半成品，V1 不变 |
| DESK-06 | 创建冷备份 | 必测 | 停止/安全刷新数据库后生成数据库与附件备份；列表可见，路径位于 V2 userData |
| DESK-07 | 恢复备份 | 必测 | 恢复前保留安全点；重启后数据一致，损坏/不存在备份明确失败且不破坏当前数据 |
| DESK-08 | V1/V2 并行安装 | Windows/macOS 候选必测 | 应用身份、userData、日志、备份、密钥路径互不覆盖；卸载/启动一方不影响另一方 |

## 3. Career Pipeline

| ID | 场景 | 当前门禁 | 通过条件 |
| --- | --- | --- | --- |
| PIPE-01 | 多 Pipeline 创建与读取 | 已实现后必测 | 公司、岗位、JD、可空简历关联与自定义阶段独立保存 |
| PIPE-02 | 阶段新增/重命名/排序 | 已实现后必测 | 重名/空值/非法排序被拒绝；重启后顺序一致 |
| PIPE-03 | 阶段推进历史 | API 集成后必测 | 每次推进追加记录；顺序稳定；不得编辑历史 |
| PIPE-04 | 跨用户/不存在资源 | 必测 | 不可读取或关联，返回统一不可用语义，不泄露存在性 |
| PIPE-05 | 日程与面试计划关联 | 已实现后必测 | 仅关联当前用户资产；重新绑定事务完整；解除不删除源资产 |
| PIPE-06 | 归档与恢复 | 必测 | 归档后受限写操作被拒绝；恢复后数据、关联和历史保留 |
| PIPE-07 | Pipeline 页面操作 | `NOT_RUN_UNTIL_IMPLEMENTED` | 页面只使用 V2 API/store；空、加载、失败、重试、归档均可操作 |

## 4. Knowledge Base

| ID | 场景 | 当前门禁 | 通过条件 |
| --- | --- | --- | --- |
| KB-01 | 文件导入与分类 | `NOT_RUN_UNTIL_IMPLEMENTED` | 支持已批准格式；重复、损坏、超限文件有明确处理；原文件本地保存策略清晰 |
| KB-02 | 内容提取与搜索 | `NOT_RUN_UNTIL_IMPLEMENTED` | 搜索结果可追溯到来源与片段；空库和无结果不伪造答案 |
| KB-03 | 基础 RAG | `NOT_RUN_UNTIL_IMPLEMENTED` | 回答引用本地来源；无依据时明确不足；模型输出结构化校验 |
| KB-04 | 知识资产边界 | `NOT_RUN_UNTIL_IMPLEMENTED` | 知识独立于单一 Pipeline；Pipeline 结束后资产仍可访问 |
| KB-05 | 删除与索引恢复 | `NOT_RUN_UNTIL_IMPLEMENTED` | 删除/重建索引行为确定；失败不留下来源与索引不一致 |

## 5. Resume System

| ID | 场景 | 当前门禁 | 通过条件 |
| --- | --- | --- | --- |
| RES-01 | 基础简历与岗位版本 | V2-R1 集成后必测（已实现：分支 codex/v2-r1-resume-asset-library） | 岗位表达副本独立演进；修改副本不污染其他版本或基础资产 |
| RES-05 | 版本谱系与线性历史 | V2-R1 集成后必测（已实现，API 流程已验证） | fork 记录 forkedFromVersionId 且副本 V1 独立；普通保存只能从当前版本推进；历史版本只读 |
| RES-06 | 归档与恢复 | V2-R1 集成后必测（已实现，API 流程已验证） | 归档后默认列表隐藏、恢复后重现；历史引用仍有效；跨用户按不存在处理（404） |
| RES-02 | 导入与手工编辑互通 | 进入 V2 Resume 后必测 | 导入内容转为可编辑结构；手工修改可保存、预览和再次编辑 |
| RES-03 | AI 建议审阅 | 进入 V2 Resume 后必测 | 保留原文、依据、建议稿和接受/拒绝决定；不静默覆盖 |
| RES-04 | 数据诚实 | 必测 | 证据不足明确提示；不编造经历、技能、数字或荣誉 |

## 6. Interview Engine

| ID | 场景 | 当前门禁 | 通过条件 |
| --- | --- | --- | --- |
| INT-01 | Role Based Interview | `NOT_RUN_UNTIL_IMPLEMENTED` | 进入面试工作区后手动绑定 Pipeline、Resume version 与面试官角色 |
| INT-02 | Knowledge Training | `NOT_RUN_UNTIL_IMPLEMENTED` | 从选定 Knowledge 来源生成基础/深入/场景问题并保留来源 |
| INT-03 | Experience Simulation | `NOT_RUN_UNTIL_IMPLEMENTED` | 使用真实面经/用户题库主持提问、追问和评价，不冒充来源 |
| INT-04 | 中断与恢复 | `NOT_RUN_UNTIL_IMPLEMENTED` | 会话中断、模型错误、用户退出时可恢复或明确结束，无假完成记录 |
| INT-05 | 反馈生成 | `NOT_RUN_UNTIL_IMPLEMENTED` | 反馈关联实际 Pipeline/简历/模式，提炼核心问题和下一步，不直接改状态 |

## 7. Workspace 与 Growth

| ID | 场景 | 当前门禁 | 通过条件 |
| --- | --- | --- | --- |
| WS-01 | 今日主要行动 | `NOT_RUN_UNTIL_IMPLEMENTED` | 首页约 70% 权重回答“现在最应该做什么”，来源为真实日程/训练/反馈 |
| WS-02 | 最近活动 | `NOT_RUN_UNTIL_IMPLEMENTED` | 只显示已发生的本地事件，不使用写死通知或演示话术 |
| WS-03 | 弱成长反馈 | `NOT_RUN_UNTIL_IMPLEMENTED` | 趋势、学习时长、连续准备来自真实记录；无数据时保持诚实空态 |
| WS-04 | 行动闭环 | `NOT_RUN_UNTIL_IMPLEMENTED` | Workspace 行动进入正确专业工作区；完成结果反向更新活动/下一步 |

## 8. 安全、密钥与日志

| ID | 场景 | 门禁 | 通过条件 |
| --- | --- | --- | --- |
| SEC-01 | API key 保存/切换/删除 | 桌面候选必测 | 密钥使用安全存储或明确 session fallback；不进入 localStorage、数据库、仓库 |
| SEC-02 | 未配置模型 | 必测 | 可跳过配置使用非 AI 功能；调用 AI 时提供明确引导，不用机械式全屏阻塞 |
| SEC-03 | 日志脱敏 | 必测 | 搜索运行日志，确认无密钥、token、联系方式、简历正文和面试回答 |
| SEC-04 | 网络边界 | 每个新增连接必测 | 仅向用户配置的 provider/获批服务发送必要数据；失败时不静默转发到其他服务 |

日志核验要记录被检查的目录、文件范围和搜索规则；不得上传真实日志作为公开 CI artifact。

## 9. Windows/macOS 包验证

| 平台 | 构建/产物 | Smoke test |
| --- | --- | --- |
| Windows | 在 Windows 执行 `cd frontend && npm run desktop:pack:win`；记录 portable 产物 hash | 全新启动、V1/V2 并行、创建/重开、中文路径、备份恢复、API key、卸载后数据策略 |
| macOS | 在 macOS 执行 `cd frontend && npm run desktop:pack`；记录 `.app` 目录或最终包 hash | 全新启动、V1/V2 并行、创建/重开、备份恢复、API key、Gatekeeper/签名状态说明 |

不能用 macOS 生成的 Windows runtime 代替 Windows 实机打包验证，反之亦然。没有目标平台实机结果时，发布状态必须写“未验证”，不得写通过。

## 10. 测试记录格式

每次阶段验收复制以下字段：

```text
Version / candidate:
Integration commit:
Platform and OS:
Executed at and timezone:
Tester:
Automated commands and exact results:
Manual scenario IDs passed:
Failed scenario IDs and evidence:
NOT_RUN_UNTIL_IMPLEMENTED items:
Known risks:
Decision: APPROVED / CONDITIONAL / REJECT
Artifact path and SHA-256 (when packaged):
```
