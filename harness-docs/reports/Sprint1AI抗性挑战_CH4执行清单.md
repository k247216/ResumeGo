# Sprint 1 AI 抗性挑战（CH4 测试强化）执行清单

> 当前阶段结论（2026-07-15）：阶段性门禁与回归已完成；最终 CH4 验收仍需达到 85% 全后端指令覆盖率，并补齐两次 Bug 注入证据。

## 已冻结基线（2026-07-15）

| 项目 | 结果 | 证据 |
| --- | ---: | --- |
| 全量后端测试 | 277 个，0 失败，0 错误，1 跳过 | `backend/target/surefire-reports/` |
| 测试耗时 | 17.352 秒 | Maven `mvn test` 输出 |
| 指令覆盖率 | 59%（12,338 / 20,826） | `backend/target/site/jacoco/index.html` |
| 分支覆盖率 | 46%（744 / 1,585） | `backend/target/site/jacoco/index.html` |
| JaCoCo 最低门槛 | 0% | `backend/pom.xml` |

## 执行记录

| 轮次 | 已完成内容 | 测试结果 | 覆盖率结果 | 证据 |
| --- | --- | --- | --- | --- |
| 0（基线） | 冻结全量后端测试与 JaCoCo 报告 | 277 个；0 失败、0 错误、1 跳过 | 指令 59%；分支 46% | 2026-07-15 `mvn test` 输出与 JaCoCo 报告 |
| 1（排版安全契约） | 新增 `LayoutProposalServiceTest`：空草稿不调用模型；非法 AI 输出走本地兜底 | 279 个；0 失败、0 错误、1 跳过 | 全后端指令约 62.7%（13,049 / 20,826）；该服务 39.4%（687 / 1,742） | `backend/src/test/java/com/resumego/layout/LayoutProposalServiceTest.java`；2026-07-15 `mvn test` 输出 |
| 2（建议追问安全契约） | 新增 `SuggestionFollowUpServiceTest`：非法建议 ID、过短补充、模型异常和合法结构化输出均可复核；异常路径不写入建议状态 | 283 个；0 失败、0 错误、1 跳过 | 全后端指令 64.1%（13,353 / 20,826）；分支 53.5%（848 / 1,585）；该服务 87.8%（309 / 352） | `backend/src/test/java/com/resumego/optimization/SuggestionFollowUpServiceTest.java`；2026-07-15 `mvn test` 输出 |
| 3（建议接口异常契约） | 新增 `OptimizationSuggestionControllerTest`：资源不存在、状态冲突、追问参数异常、服务异常与成功结果的 HTTP 响应契约 | 291 个；0 失败、0 错误、1 跳过 | 全后端指令 64.7%（13,472 / 20,826）；分支 53.7%（851 / 1,585）；该 Controller 40.1%（95 / 237） | `backend/src/test/java/com/resumego/optimization/OptimizationSuggestionControllerTest.java`；2026-07-15 `mvn test` 输出 |
| 4（岗位服务异常契约） | 扩展 `JobDescriptionServiceTest`：历史空 JD、损坏来源元数据、删除结果、批量岗位类型更新 | 295 个；0 失败、0 错误、1 跳过 | 全后端指令 65.9%（13,724 / 20,826）；分支 54.3%（872 / 1,606）；该服务 69.9%（362 / 518） | `backend/src/test/java/com/resumego/job/JobDescriptionServiceTest.java`；2026-07-15 `mvn test` 输出 |
| 5（简历服务边界契约） | 扩展 `ResumeServiceTest`：空白名称、缺失简历/版本、默认变更摘要、目标岗位更新与无写入副作用 | 301 个；0 失败、0 错误、1 跳过 | 全后端指令 65.6%（13,778 / 20,996）；分支 54.7%（879 / 1,606）；该服务 79.6%（250 / 314）。本轮并行合入的 `MockAiClient` 生产代码使全局指令总数增加，不能直接与第 4 轮百分比比较 | `backend/src/test/java/com/resumego/resume/ResumeServiceTest.java`；2026-07-15 `mvn test` 输出 |
| 6（排版 AI 安全契约） | 扩展 `LayoutProposalServiceTest` 并新增 Controller 测试：模型异常本地兜底、结构化输出白名单过滤、接口异常不泄露内部细节 | 306 个；0 失败、0 错误、1 跳过 | 全后端指令 67.5%（14,170 / 20,996）；分支 56.7%（911 / 1,606）；排版接口 100%（41 / 41），排版服务 58.6%（1,021 / 1,742） | `backend/src/test/java/com/resumego/layout/LayoutProposalServiceTest.java`、`LayoutProposalControllerTest.java`；2026-07-15 `mvn test` 输出 |
| 7（共享回归批次） | 解除并行测试编译阻塞后，验证优化建议服务的模型失败无副作用测试；本批还包含成员新增的简历/岗位 Controller 测试 | 331 个；0 失败、0 错误、1 跳过 | 全后端指令 68.9%（14,456 / 20,996）；分支 57.7%（927 / 1,606）；优化建议服务 49.0%（557 / 1,136） | `OptimizationSuggestionServiceTest.java`（本轮 1 条）及并行的 `ResumeControllerTest.java`、`JobDescriptionControllerTest.java`；2026-07-15 `mvn test` 输出 |

> 阻塞已解除（2026-07-15）：`ResumeControllerTest` 已由其作者对齐当前 Repository/Service 合约，目标测试 12 条通过；随后全量测试恢复通过。该问题及修复前后的证据应保留为并行协作与构建恢复记录。

## 当前完成状态（2026-07-15）

| 项目 | 当前真实状态 | 证据/说明 |
| --- | --- | --- |
| 非禁飞区异常与降级测试 | 已完成一批 | 排版、建议追问、优化建议、岗位、简历及 Controller 测试已进入当前回归集 |
| 全量回归 | 已完成 | `mvn test`：331 个测试，0 失败、0 错误、1 跳过 |
| 阶段性 JaCoCo 门禁 | 已完成 | `backend/pom.xml` 全后端 Bundle 指令门槛为 65%；`mvn verify` 输出 `All coverage checks have been met.` |
| 禁飞区包门禁 | 已生效 | 评分服务 80%，岗位匹配服务 85%，面试包 85%；具体业务预期仍需人工作者与 QA 复核 |
| 最终 85% 全局门禁 | 未完成 | 当前指令覆盖率 68.9%（14,456 / 20,996），不应把 65% 阶段门禁写成 85% 验收通过 |
| 两次 Bug 注入 | 未见仓库证据 | 需要补失败输出、恢复后 `mvn test`/`mvn verify` 输出及 QA 记录 |
| 最终报告元数据 | 待人工填写 | 团队编号、姓名、提交日期、冻结 Commit ID、截图文件名和自评 |

## 验收口径

1. 使用 `cd backend && mvn test` 生成同一份 JaCoCo 报告。
2. 以 JaCoCo **全后端 Bundle 的 instruction covered ratio** 为主指标，目标不低于 85%；报告同时保留分支覆盖率和测试数量，不能通过删除业务类或随意排除低覆盖包凑数。
3. 至少为 AI 输出异常、非法输入/资源不存在、并发或重复请求、禁飞区边界各保留一类人工设计用例。
4. 先使用当前 65% 阶段门禁持续集成；达到 85% 后再将全后端 Bundle 门槛调整为 85%，使 `mvn verify` 在覆盖率下降时失败。
5. 至少完成 2 个小型 Bug 注入：先证明测试失败，再恢复正确实现并证明回归通过。

> 并行开发说明：若测试期间生产代码发生合入或未提交变更，必须同时记录覆盖指令分子/分母，不能只比较百分比；最终挑战数据应在冻结的同一提交上重新执行 `mvn test` 和 `mvn verify`。

## 分工与顺序

| 阶段 | 负责人 | 具体产出 | 完成证据 |
| --- | --- | --- | --- |
| A. 测试矩阵 | PO | 按模块列出正常、空值、异常、边界、回归场景；确认验收口径 | 测试矩阵与评审记录 |
| B. 非禁飞区补测 | SM | AI、优化建议、简历、岗位、Controller 的成功/失败/降级路径测试 | 新测试、JaCoCo 增量 |
| C. 禁飞区测试 | 三人 | 人工设计评分、匹配、状态机的边界与非法路径；不得由 AI 生成预期规则 | 作者说明、QA Review、测试输出 |
| D. 门禁与注入 | QA | 85% JaCoCo 门禁、两次 Bug 注入、回归记录 | 失败/恢复截图、构建输出 |
| E. 报告验收 | PO + QA | 填写模板 3–6 节、核对证据位置和真实数据 | 最终 DOCX + 证据包 |

## 第二阶段：按覆盖率缺口拆分的测试矩阵

> 这些是对既有行为的**特征测试/回归测试**，不修改评分权重、岗位排序或面试状态机规则。所有禁飞区测试预期由成员独立填写；本表不提供其实现或可翻译的伪代码。

| 优先级 | 模块与当前缺口 | 人工测试目标 | 建议负责人 | 验收方式 |
| --- | --- | --- | --- | --- |
| P0 | `layout/LayoutProposalService`：1,685 条未覆盖指令 | 空草稿不调用模型；无可编辑文本返回安全提示；非法 JSON/Schema 失败走本地兜底；模型改写新增数字、超长或改错原文时被拒绝；仅白名单模板与模块可输出 | PO | 新增 `LayoutProposalServiceTest`；每个分支有独立断言与 AI 审计调用验证 |
| P0 | `optimization/SuggestionFollowUpService`：329 条未覆盖指令 | 非法建议 ID、建议不存在、空补充、模型异常、结构化字段缺失、模型内容非法时均不修改简历版本和建议状态 | SM | 新增服务测试；验证返回降级结果与无副作用 |
| P0 | `optimization/OptimizationSuggestionController`：233 条未覆盖指令 | 参数错误、资源不存在、状态冲突、未预期异常分别映射为正确 HTTP 响应 | QA | `@WebMvcTest` 或 MockMvc 测试；每个错误码单独可复现 |
| P1 | `job/JobDescriptionService`：253 条未覆盖指令；Controller：89 条未覆盖指令 | 解析器异常会标记失败；失效的 `sourceMetaJson` 被安全忽略；批量重解析跳过空 JD；删除/查询不存在资源有稳定结果 | PO | 扩展现有 `JobDescriptionServiceTest` 与新增 Controller 测试 |
| P1 | `resume` 服务、仓储与 Controller：合计约 669 条未覆盖指令 | 新建/更新目标岗位/版本历史的成功与失败路径；非法 ID、空数据、持久化失败不写入半成品数据 | SM | 服务测试 + 接口测试；验证返回值和持久化副作用 |
| P1 | `layout` Controller 与 Prompt Builder：约 85 条未覆盖指令 | 请求体不合法返回 400；服务异常不泄露堆栈；目标岗位文本作为数据处理而非指令 | QA | MockMvc + 固定输入；检查错误响应不含敏感数据 |
| P2 | 面试编排服务：893 条未覆盖指令 | 仅由面试模块作者和 QA 人工补充：模型失败、重复提交、恢复、总结失败等编排路径；状态转换预期来自人工状态表 | 面试模块作者 + QA | 独立 Review，提交标记 `No-Fly-Zone: interview` |
| P2 | 评分/匹配余量 | 仅由作者补齐人工设计的边界测试与异常测试；不调整算法，只证明已有人工规则可重复 | 评分/匹配作者 + QA | 固定样例、边界记录、双人 Review |

## 本轮先做什么

1. **PO** 创建 `LayoutProposalServiceTest`，先覆盖“空草稿不调用模型”和“非法 AI 输出走本地兜底”两条非禁飞区路径。
2. **SM** 创建 `SuggestionFollowUpServiceTest`，先覆盖“非法建议 ID”和“模型异常不产生副作用”。
3. **QA** 创建优化建议 Controller 的 MockMvc 测试，先覆盖“资源不存在”和“状态冲突”两个 HTTP 分支。
4. 三人各自运行对应测试；完成后再合并执行 `mvn test`，记录新的测试总数和 JaCoCo 指标。

### 本轮完成判定

- 新增测试都能独立执行且通过。
- 所有 AI 返回均使用固定 Mock；不访问真实千问 API。
- 禁飞区没有新增或修改 AI 生成的实现代码。
- `mvn test` 通过，JaCoCo 报告生成；报告中记录实际覆盖率，不能预填目标值。

## 每日执行步骤

1. PO 先建立模块测试矩阵；每条用例明确输入、人工预期、关联风险和作者。
2. 各成员从 `dev` 拉取独立功能分支，小步提交测试；禁飞区测试提交正文标注 `No-Fly-Zone` 和人工作者。
3. 每合入一批测试，运行 `mvn test` 并记录：测试总数、失败数、指令覆盖率、分支覆盖率。
4. 当前 65% 门禁已通过 `mvn verify`；覆盖率达到 85% 后，再把 JaCoCo 全局配置门槛调为 `0.85`，重新运行 `mvn verify`。
5. QA 人工注入 Bug，记录“注入后失败 → 恢复后通过”；不修改评分权重、排序规则或状态机规则本身。
6. PO 将真实完成时间、提交记录、截图和最终指标写回报告草案，删除所有 `【待填】` 与 `待执行`。

## 禁飞区约束

- AI 仅可帮助整理测试文档、解释非禁飞区失败日志、检查报告措辞。
- 评分权重、岗位排序预期、面试状态转换及其测试预期必须由成员依据人工设计材料确定。
- 所有 AI 使用记录进入 `harness-docs/ai/ai-usage-log.md`，不得包含简历、面试回答、密钥或原始敏感文本。

## 最终证据包

- JaCoCo 挑战前、后首页截图或 HTML/PDF。
- Maven 测试与 `mvn verify` 成功输出。
- Bug 注入前后的失败/恢复输出。
- 关键测试源码、覆盖率门禁配置 diff、相关 Commit ID。
- 成员人工设计记录与 QA Review 记录。
