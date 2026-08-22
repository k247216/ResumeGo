# Career OS V2 协作权限规范

Status: Normative policy

Date: 2026-08-22

本规范适用于 Career OS V2 的全部任务卡、功能分支和集成审查。任务卡与本规范冲突时，必须在编码前由 **Core Controller** 解决；Feature Agent 不得自行选择解释或继续实施。

## Core Controller

Core Controller 是 V2 的产品和技术决策中心，负责维护冻结的产品模型、路线图和模块边界；决定功能是否进入当前阶段；拆分任务卡并冻结基础提交、公共契约和文件所有权；审查数据模型、隐私、AI、失败与恢复路径及测试证据；负责跨分支集成、全量回归、桌面构建和发布前验证。

合入依据不是“代码可以运行”，而是用户结果、领域一致性、数据安全、可恢复性和可重复验证的证据。Core Controller 可退回任何不符合本规范或任务卡的交付。

## Feature Agent

Feature Agent 只实施任务卡明确授权的功能，不拥有产品范围、公共架构或公共契约的解释权。必须从指定基础提交建立独立 worktree 和短期分支，只修改授权文件，以最小变更交付；发现边界不足时停止并申请 Core Controller 决策。

Feature Agent 完成后只交付提交哈希和标准交付报告，不自行合并、变基、覆盖其他分支或进入 V2 集成 worktree 修改内容。

## Branch/worktree isolation

V2 集成分支为 `codex/v2-career-os`。功能分支命名为 `codex/v2-<任务编号>-<功能简称>`，每个 Feature Agent 必须使用独立 worktree。

Feature Agent 不得直接修改 `main`、V1 维护线或 V2 集成 worktree。Core Controller 在核验基础提交、文件范围和交付证据后，决定是否以及如何集成；公共契约冲突由其重新分派或调整任务顺序。

## Protected artifacts

任务卡必须枚举允许创建、修改和测试的文件；未列出的文件默认禁止修改。以下 protected artifacts 默认由 Core Controller 控制，只有任务卡明确授权才可修改：

- H2/MySQL 数据库迁移；
- 公共 DTO、API 类型和跨模块 Port；
- 领域枚举、状态机和确定性业务规则；
- Electron 应用身份、数据目录和升级协议；
- 全局依赖、构建与打包配置；
- 产品模型、路线图、架构、隐私和重大决策文档。

同一时间不得让两个进行中的任务拥有同一公共文件。

## Scope escalation

需要越过授权边界时，Feature Agent 必须停止实施并向 Core Controller 报告：原因、目标文件、公共契约影响、替代方案和依赖任务。未经重新授权，不得修改 protected artifacts、扩展产品范围或用临时实现绕开契约。

所有任务还必须保持本地优先、数据诚实和 AI 受约束：不得编造职业事实；不得让 AI 直接决定状态、权限或持久化；不得把真实个人数据、密钥或访问令牌写入仓库、日志或公开测试材料。

## TDD evidence

Feature Agent 必须先提交任务卡要求的失败测试证据（RED），再实施使该行为通过的最小变更（GREEN）。交付报告必须记录实际命令、退出码和测试数量，并区分局部验证、模块回归和未执行的验证；不得把部分测试写成全量通过。

测试使用虚构夹具，不依赖真实个人数据或模型服务。正常、空、失败、恢复和越权行为必须按任务卡验证。

## Delivery evidence

交付报告必须包含完成行为、修改文件、新增或改变的接口、RED 与 GREEN 证据、局部和模块级回归、数据库影响、隐私/AI 影响、未完成事项、风险，以及需要 Core Controller 决定的问题。

Feature Agent 只证明功能分支满足任务卡；Core Controller 负责证明集成后的产品仍可成立，并负责全量回归、桌面构建与发布前测试。

## Direct rejection conditions

以下情况直接退回：未解释的额外文件；公共契约漂移；缺失失败、恢复或越权路径；伪造联动数据；敏感数据或密钥进入仓库/日志/测试材料；未经结构化校验的 AI 写入；不完整或不真实的测试声明；违反任务卡或本规范的分支、worktree 或文件所有权。

“代码可以运行”本身不是接收条件。被退回的任务必须由 Core Controller 说明所缺的范围、证据或授权后，才能再次提交。
