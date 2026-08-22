# Career OS V2 Agent 交付报告

> Feature Agent 在完成任务卡授权范围后填写本报告。报告证明功能分支符合任务卡，不替代 Core Controller 的集成、全量回归、桌面构建或发布结论。

## 1. 交付身份

| 字段 | 实际内容 |
| --- | --- |
| Task ID | [任务编号] |
| Task name | [任务名称] |
| Feature Agent | [负责人] |
| Branch | [功能分支名] |
| Worktree | [独立 worktree 绝对路径] |
| Base commit | [任务卡冻结的完整提交哈希] |
| Final commit | [本次交付的完整提交哈希] |
| Delivery date | [日期与时区] |

## 2. Completed behavior

- [逐项说明已完成的用户可观察行为，并对应任务卡第 7 节场景。]
- [明确空、失败、恢复和越权路径的实际结果。]

## 3. Changed files

| 文件路径 | 操作 | 变更目的 | 是否在任务卡 `Allowed files` 中 |
| --- | --- | --- | --- |
| [相对路径] | [Create / Modify / Test] | [具体变更] | [是 / 否；否时必须在第 11 节解释] |

## 4. Interfaces

| 接口 | 变化类型 | 调用方/消费者 | 兼容性与契约核验 |
| --- | --- | --- | --- |
| [DTO、API、Port、事件、存储格式或“无新增/改变接口”] | [新增 / 修改 / 无] | [模块或任务] | [与 Frozen contracts 一致的证据] |

## 5. RED evidence

记录实施前真实失败的测试。不得补造、推测或只写预期。

| 用例 | 命令 | 退出码 | 测试总数 | 失败数 | 实际失败原因 |
| --- | --- | --- | --- | --- | --- |
| [RED-1] | [可复制命令] | [实际数值] | [实际数值] | [实际数值] | [测试输出摘要] |

## 6. GREEN evidence

记录最小实现后的通过证据。

| 用例 | 命令 | 退出码 | 测试总数 | 通过数 | 覆盖的可观察行为 |
| --- | --- | --- | --- | --- | --- |
| [GREEN-1] | [可复制命令] | [实际数值] | [实际数值] | [实际数值] | [对应任务卡场景] |

## 7. Local and Full module regression

| 验证级别 | 命令 | 退出码 | 测试数量或构建结果 | 实际覆盖范围 | 结论 |
| --- | --- | --- | --- | --- | --- |
| Local verification | [可复制命令] | [实际数值] | [实际数值] | [任务局部范围] | [通过 / 失败 / 未执行] |
| Full module regression | [可复制命令] | [实际数值] | [实际数值] | [模块完整范围] | [通过 / 失败 / 未执行] |
| Build or desktop check | [可复制命令或不适用依据] | [实际数值或不适用] | [实际结果] | [构建/运行范围] | [通过 / 失败 / 未执行] |

未执行项目：

- [命令、未执行原因、风险和建议由谁执行；没有则写“无”。]

## 8. Database impact

| 项目 | 实际内容 |
| --- | --- |
| Schema / migration | [无 / 已授权迁移文件与任务卡授权] |
| Data read/write/delete impact | [实际影响或无] |
| Transaction / rollback behavior | [实际验证结果或不适用依据] |
| Backup / upgrade compatibility | [实际验证结果或不适用依据] |

## 9. Privacy/AI impact

| 项目 | 实际内容 |
| --- | --- |
| Personal data and logging | [处理边界；确认未写入真实简历、联系方式、回答、令牌或密钥] |
| Network / third-party transfer | [无 / 实际目标、授权和隐私影响] |
| AI input/output | [无 / 实际输入输出与结构化校验] |
| Human decision boundary | [AI 未直接决定状态、权限或持久化的核验] |
| Test fixture provenance | [虚构夹具说明] |

## 10. Omissions

- [未完成的任务卡内事项、原因、用户影响和建议后续动作；没有则写“无”。]

## 11. Known risks

- [已知风险、触发条件、影响、缓解措施和剩余责任人；没有则写“无”。]

## 12. Unexplained worktree changes

| 工作区改动 | 是否纳入最终提交 | 与任务卡关系 | 处理或解释 |
| --- | --- | --- | --- |
| [通过 `git status --short` 发现的改动；没有则写“无”] | [是 / 否] | [授权 / 非授权 / 无] | [已停止、已移交或具体解释] |

## 13. Decisions requested from the Core Controller

- [需要裁决的范围、目标文件、公共契约影响、替代方案、依赖任务和建议；没有则写“无”。]

## 14. Agent declaration

- [ ] 最终提交相对 `Base commit` 的改动均在任务卡 `Allowed files` 内，或已在第 12 节如实说明。
- [ ] 未修改 V1、`main`、V2 集成 worktree 或未授权受保护工件。
- [ ] RED、GREEN、局部验证和模块回归均记录实际命令、退出码与数量；未执行项未被表述为通过。
- [ ] 未在仓库、日志或测试材料中加入真实个人数据、访问令牌、模型密钥或伪造联动数据。
- [ ] 本报告只主张功能分支符合任务卡；集成后的全量回归、桌面构建与发布结论由 Core Controller 作出。
