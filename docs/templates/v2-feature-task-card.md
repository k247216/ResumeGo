# Career OS V2 功能任务卡

> 本任务卡由 Core Controller 填写并冻结。Feature Agent 只能实施本卡明确授权的内容；与 `docs/architecture/agent-collaboration.md` 冲突时，必须停止并请求 Core Controller 裁决。

## 1. 任务身份与目标

| 字段 | 已冻结内容 |
| --- | --- |
| Task ID | [任务编号] |
| Task name | [简短、可区分的任务名称] |
| Owner | [Feature Agent 或负责人] |
| Milestone | [所属里程碑] |
| Status | [ready / in-progress / blocked / delivered / accepted / rejected] |
| Priority | [P0 / P1 / P2] |
| User result | [用户完成什么可观察结果；不得描述内部实现替代结果] |
| Acceptance owner | [Core Controller 姓名或标识] |

## 2. 分支、基线与依赖

| 字段 | 已冻结内容 |
| --- | --- |
| Base commit | [完整提交哈希] |
| Feature branch | `codex/v2-[任务编号]-[功能简称]` |
| Feature worktree | [独立 worktree 绝对路径] |
| V2 integration branch | `codex/v2-career-os` |
| Input dependencies | [已完成任务、模块、接口或夹具；无则写“无”] |
| Output consumed by | [下游任务、模块或接口；无则写“无”] |
| Dependency readiness proof | [提交哈希、接口版本或可复现命令] |

开始前必须确认：当前 HEAD 以 `Base commit` 为祖先；工作目录是本任务的独立 worktree；V1 仅为只读基线，不能作为修改、集成或迁移目标。

## 3. 范围

### Scope in

- [明确的行为、接口或测试目标]

### Scope out

- [明确不做的行为、重构、产品范围或数据迁移]

### Stop-and-escalate triggers

- [需要修改未授权文件、受保护工件或冻结契约时的停止条件]
- [需要新增依赖、迁移、遥测、云端传输或模型能力时的停止条件]

## 4. 文件所有权

未列出的文件默认禁止修改。每项必须给出精确相对路径；目录通配仅在 Core Controller 明确接受时使用。

### Allowed files

| 操作 | 文件路径 | 目的 | 所有权说明 |
| --- | --- | --- | --- |
| Create | [相对路径] | [新增内容] | [本任务独占 / 已协调] |
| Modify | [相对路径] | [变更原因] | [本任务独占 / 已协调] |
| Test | [相对路径或命令涉及的测试文件] | [验证行为] | [本任务独占 / 已协调] |

## 5. Frozen contracts

| 契约或受保护工件 | 冻结约束 | 允许的使用方式 | 变更授权 |
| --- | --- | --- | --- |
| [公共 DTO / API / Port / 领域规则 / 状态机 / 数据库迁移 / Electron 协议 / 构建配置] | [不得改变的名称、语义、字段、状态或行为] | [仅调用、仅消费或具体允许方式] | [无；需 Core Controller 新任务卡] |

## 6. 数据、隐私与 AI 影响

| 维度 | 已冻结内容 |
| --- | --- |
| Data ownership and storage | [创建、读取、更新、删除的数据及其本地存储边界；无则写“无”] |
| Database impact | [无迁移 / 已授权迁移及任务卡编号] |
| Privacy impact | [个人数据类型、传输边界、日志限制；不得记录真实简历、联系方式、回答、令牌或密钥] |
| AI impact | [模型输入/输出、结构化校验、人工决定点；AI 不得直接决定状态、权限或持久化] |
| Test fixtures | [虚构夹具说明；不得使用真实个人数据或模型服务] |

## 7. 可观察行为与验收

| 场景 | 前置条件与输入 | 用户可观察输出 | 错误/恢复边界 | 验收测试 |
| --- | --- | --- | --- | --- |
| 正常 | [精确输入] | [精确可见结果] | [不适用或恢复步骤] | [测试名称] |
| 空 | [空数据或缺失可选值] | [精确空态] | [不创建虚假数据] | [测试名称] |
| 失败 | [可预期失败条件] | [精确错误结果] | [回滚、保留输入或重试方式] | [测试名称] |
| 恢复 | [此前失败后的条件] | [精确恢复结果] | [幂等性或重复操作约束] | [测试名称] |
| 越权 | [未授权调用或文件/状态边界] | [拒绝结果] | [不得产生副作用] | [测试名称] |

## 8. 必须先失败的测试（RED）

在实施行为代码前，Feature Agent 必须记录下列测试的实际失败证据。每一项均须包含命令、退出码、测试数量和失败原因；不得以预期描述替代实际输出。

| RED test ID | 覆盖行为 | 测试文件或用例 | 执行命令 | 预期失败原因 | 证据要求 |
| --- | --- | --- | --- | --- |
| [RED-1] | [对应第 7 节场景] | [精确名称] | [可复制命令] | [尚未实现的可观察行为] | [退出非 0、测试总数、失败数] |

## 9. Verification commands

| 级别 | 命令 | 覆盖范围 | 通过条件 | 是否必须执行 |
| --- | --- | --- | --- | --- |
| GREEN local | [可复制命令] | [本任务测试] | [退出 0、测试数量] | 是 |
| Module regression | [可复制命令] | [所属模块] | [退出 0、测试数量] | 是 |
| Database / migration | [可复制命令或“不适用：无数据库影响”] | [迁移、事务或回滚] | [退出 0 或不适用依据] | [是 / 否] |
| Build | [可复制命令或“不适用：原因”] | [受影响构建目标] | [退出 0] | [是 / 否] |
| Manual observable check | [明确步骤] | [第 7 节场景] | [记录实际结果] | [是 / 否] |

未执行的命令必须在交付报告中逐项说明原因，且不得表述为已完成全量回归。

## 10. Commit rule

- 仅提交第 4 节 `Allowed files` 中的 Create、Modify、Test 文件。
- 提交前必须检查 `git diff --check`、已跟踪与未跟踪改动，以及提交相对 `Base commit` 的文件清单。
- 提交信息：`[约定的 type(scope): summary]`。
- 不得合并、变基、强制推送、覆盖其他分支，或修改 V2 集成 worktree。

## 11. Delivery format

Feature Agent 交付时提供：

1. 最终提交完整哈希；
2. 基于 `docs/templates/v2-agent-delivery-report.md` 的完整报告；
3. 每项 RED、GREEN、模块回归与未执行验证的实际命令、退出码、测试数量及证据；
4. 范围升级、未完成事项、风险和待 Core Controller 决策项。

## 12. Integration gates

Core Controller 仅在以下门禁全部满足或明确裁定条件通过时集成：

- `Base commit` 祖先关系、分支命名和独立 worktree 均可核验；
- 提交文件完全属于 `Allowed files`，额外变更已解释且已获重新授权；
- `Frozen contracts` 无漂移，或有对应的 Core Controller 决策和新授权；
- 第 7 节正常、空、失败、恢复、越权行为均有真实测试证据；
- RED 与 GREEN 证据真实、命令可复跑，且模块回归范围没有被夸大；
- 数据库、隐私、AI、构建与迁移影响已如实披露；
- 无敏感数据、密钥、真实个人数据、伪造联动或未经结构化校验的 AI 写入；
- Core Controller 已决定接受、条件接受或退回，并记录后续行动。
