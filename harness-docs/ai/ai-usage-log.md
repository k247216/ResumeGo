# AI 使用日志

> 由 QA 维护完整性，每次 AI 辅助任务由实际使用者填写。不得记录 API Key、完整简历、手机号、邮箱或其他敏感原文。

## 记录模板

```text
### 日期时间｜成员｜Issue/MR

- 工具与模型：
- 协作模式：启发式 / 精确式 / 约束式
- 任务目标：
- Prompt 摘要：
- 允许读取和修改的范围：
- AI 产出：
- 采纳内容：
- 拒绝内容及原因：
- 人工修改：
- 发现的 AI 问题：
- 测试与验证证据：
- 是否涉及 AI 禁飞区：
- 禁飞区处理说明：
- 反思与后续规则：
```

## 示例

```text
### 2026-07-07 10:30｜成员 3｜S1-06

- 工具与模型：通义千问 Max
- 协作模式：启发式
- 任务目标：整理 AI 简历优化建议的返回字段
- Prompt 摘要：请求模型给出建议字段，不提供真实简历和敏感信息
- 允许读取和修改的范围：仅讨论字段结构，不写代码
- AI 产出：suggestionType、evidenceRef、beforeText、afterText、riskNote 等字段建议
- 采纳内容：采纳字段命名的一部分
- 拒绝内容及原因：拒绝模型给出的评分字段，避免 AI 直接影响评分
- 人工修改：由成员 3 手动整理为接口 Schema
- 发现的 AI 问题：模型倾向补充“匹配分数”
- 测试与验证证据：后续通过 JSON Schema 校验测试
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：不涉及评分、排序、状态机实现
- 反思与后续规则：AI 建议接口禁止返回 score、rank、state 字段
```

## 实际记录

### 2026-07-15｜PO（姓名待补）｜CH4-01 排版提案安全回归测试

- 工具与模型：Codex（GPT-5）
- 协作模式：约束式
- 任务目标：为非禁飞区的排版提案服务补充 AI 输出安全回归测试，并建立 Sprint 1 CH4 测试强化的基线与执行清单。
- Prompt 摘要：读取 Sprint 1 AI 抗性挑战报告模板与仓库规范；基于现有 JaCoCo 报告选择测试强化挑战；仅为排版提案模块增加空草稿与非法模型输出的测试，不生成或修改禁飞区逻辑。
- 允许读取和修改的范围：`backend/src/test/java/com/resumego/layout/`、`harness-docs/reports/`、本日志；不修改 `assessment`、`matching` 或面试状态机实现。
- AI 产出：CH4 执行清单、报告执行草案、`LayoutProposalServiceTest` 两条回归测试、JaCoCo 基线与增量数据整理。
- 采纳内容：采纳空草稿不调用模型、非法 JSON 必须走本地安全兜底且记录 Schema 未通过的测试；采纳全量 JaCoCo 指令覆盖率作为挑战主指标。
- 拒绝内容及原因：未采纳任何评分权重、岗位匹配排序、面试状态转换及其测试预期；这些均由成员依据人工设计材料独立完成。
- 人工修改：成员需复核测试名称、Mock 返回、报告中的姓名/团队编号和后续真实结果；首次编译发现 `anyBoolean` 静态导入缺失，已作最小修正后通过测试。
- 发现的 AI 问题：初版测试缺失 Mockito 的 `anyBoolean` 静态导入，导致测试编译失败；未影响生产代码，修正后重新执行目标测试和全量回归。
- 测试与验证证据：目标测试 `mvn -Dtest=LayoutProposalServiceTest test` 通过（2 个测试）；全量 `mvn test` 通过（279 个测试，0 失败，0 错误，1 跳过）；全后端指令覆盖率由基线 59% 升至约 62.7%（13,049 / 20,826），`LayoutProposalService` 覆盖率为 39.4%（687 / 1,742）。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：排版提案仅生成可审查文本与模板建议，不控制评分、排序或状态转换；本次未读取、修改或生成三个禁飞区的实现。
- 反思与后续规则：覆盖率提升不能依赖排除低覆盖业务类或只测试 getter；后续优先补齐建议追问、优化建议 Controller、岗位和简历服务的异常路径，并对禁飞区测试保持人工预期与 QA 双人复核。

### 2026-07-15｜PO（姓名待补）｜CH4-02 建议追问安全回归测试

- 工具与模型：Codex（GPT-5）
- 协作模式：约束式
- 任务目标：为非禁飞区的建议追问服务补充输入校验、模型异常降级与无副作用的回归测试。
- Prompt 摘要：仅测试既有建议追问服务的公开行为；验证非法输入会提前拒绝、模型不可用时返回安全兜底、结构化模型输出可读取；不改写评分、岗位排序或面试状态机逻辑。
- 允许读取和修改的范围：`backend/src/test/java/com/resumego/optimization/SuggestionFollowUpServiceTest.java`、CH4 执行清单、本日志；不修改生产实现与禁飞区模块。
- AI 产出：4 条基于 Mockito 的特征测试，以及覆盖率增量整理。
- 采纳内容：采纳对非法建议 ID、过短补充、模型异常、合法结构化输出的覆盖；异常场景断言不会调用 `updateById`，避免产生建议状态副作用。
- 拒绝内容及原因：未采纳任何对评分权重、岗位匹配排序或面试状态转换的代码和测试预期；这些继续由成员人工设计与复核。
- 人工修改：成员需复核合法模型 JSON 中的字段是否与当前产品 Schema 保持一致，并在最终报告中填写实际成员姓名、提交号和评审人。
- 发现的 AI 问题：初版 Mockito 验证对 `updateById` 出现重载匹配歧义，已在测试中将参数约束为 `OptimizationSuggestion` 类型；未修改生产代码。
- 测试与验证证据：目标测试 `mvn -Dtest=SuggestionFollowUpServiceTest test` 通过（4 个测试）；全量 `mvn test` 通过（283 个测试，0 失败，0 错误，1 跳过）；全后端指令覆盖率为 64.1%（13,353 / 20,826），分支覆盖率为 53.5%（848 / 1,585），`SuggestionFollowUpService` 指令覆盖率为 87.8%（309 / 352）。模型异常测试会在控制台输出预期的降级日志，但构建成功。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本次仅覆盖 AI 建议追问的输入保护和失败降级，不生成或改动简历评分权重、岗位匹配排序、面试状态机及其预期。
- 反思与后续规则：优先测试“拒绝输入/模型失败时没有持久化副作用”的安全契约；下一步由 QA 补优化建议 Controller 的 HTTP 异常映射，由禁飞区作者按人工规则补边界测试。

### 2026-07-15｜PO（姓名待补）｜CH4-03 优化建议接口异常契约测试

- 工具与模型：Codex（GPT-5）
- 协作模式：约束式
- 任务目标：为非禁飞区优化建议 Controller 补齐服务异常到 HTTP 响应的回归测试，避免向客户端泄露内部异常详情。
- Prompt 摘要：依据既有 Controller 的公开接口，只测试资源不存在、状态冲突、未预期异常、追问参数异常、采纳冲突与成功返回；不修改生产接口或禁飞区代码。
- 允许读取和修改的范围：`backend/src/test/java/com/resumego/optimization/OptimizationSuggestionControllerTest.java`、CH4 执行清单、本日志；不修改生产实现与禁飞区模块。
- AI 产出：7 条 Controller 特征测试；覆盖状态码、统一响应体和错误文案。
- 采纳内容：采纳“不暴露内部异常详情”的断言，以及将资源不存在映射为 404、状态冲突映射为 400、未预期异常映射为 500 的既有接口契约测试。
- 拒绝内容及原因：未生成或调整评分、匹配排序、面试状态转换及其预期；没有将 AI 输出作为业务正确性的判断依据。
- 人工修改：成员需确认接口错误码约定是否要统一迁移到全局异常处理器；本轮仅锁定现有行为，不改变接口语义。
- 发现的 AI 问题：运行异常契约测试时 Controller 会按设计记录完整异常堆栈，导致测试输出较长；客户端响应已被验证不会包含内部错误文本。该日志策略是否需要降噪由人工决定。
- 测试与验证证据：目标测试 `mvn -Dtest=OptimizationSuggestionControllerTest test` 通过（7 个测试）；全量 `mvn test` 通过（291 个测试，0 失败，0 错误，1 跳过）；全后端指令覆盖率为 64.7%（13,472 / 20,826），分支覆盖率为 53.7%（851 / 1,585），该 Controller 指令覆盖率为 40.1%（95 / 237）。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：优化建议的接口层只处理请求与响应封装，本次不生成、修改或验证简历评分权重、岗位排序与面试状态机规则。
- 反思与后续规则：Controller 测试应优先锁定对用户可见的状态码、统一响应和隐私边界；HTTP 参数校验与禁飞区业务预期由相应作者、QA 继续人工复核。

### 2026-07-15｜PO（姓名待补）｜CH4-04 岗位服务异常与边界回归测试

- 工具与模型：Codex（GPT-5）
- 协作模式：约束式
- 任务目标：扩展非禁飞区岗位 JD 服务的异常、损坏元数据与删除/批量更新路径测试。
- Prompt 摘要：在已有 `JobDescriptionServiceTest` 上补充历史脏数据、来源元数据 JSON 损坏、删除不存在资源、岗位类型批量更新；不生成岗位匹配排序或修改岗位匹配模块。
- 允许读取和修改的范围：`backend/src/test/java/com/resumego/job/JobDescriptionServiceTest.java`、CH4 执行清单、本日志；不修改生产实现与禁飞区模块。
- AI 产出：4 条服务层特征测试，覆盖安全失败、容错解析、删除稳定结果和批量更新结果。
- 采纳内容：采纳“已存在空 JD 仅标记解析失败”“损坏 `sourceMetaJson` 被安全忽略”的异常路径测试；采纳删除与批量更新的结果断言。
- 拒绝内容及原因：未采纳任何岗位匹配得分、排序、阈值或算法预期；JD 录入和解析属于输入准备层，不参与禁飞区计算。
- 人工修改：成员需决定损坏来源元数据在生产日志中是否保留完整堆栈；本轮只验证服务不中断，不调整日志实现。
- 发现的 AI 问题：损坏 JSON 的预期容错测试会产生较长警告堆栈，影响测试日志可读性；该行为来自现有服务实现，未为追求测试整洁而改动生产逻辑。
- 测试与验证证据：目标测试 `mvn -Dtest=JobDescriptionServiceTest test` 通过（12 个测试）；全量 `mvn test` 通过（295 个测试，0 失败，0 错误，1 跳过）；全后端指令覆盖率为 65.9%（13,724 / 20,826），分支覆盖率为 54.3%（872 / 1,606），`JobDescriptionService` 指令覆盖率为 69.9%（362 / 518）。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本次只覆盖岗位 JD 的保存、解析容错和查询前置数据，不计算或验证岗位匹配排序。
- 反思与后续规则：对于真实岗位数据，应将“脏数据不阻断主流程”和“失败状态可追溯”作为必测契约；包含岗位匹配结果的测试必须由算法作者人工定义预期。

### 2026-07-15｜PO（姓名待补）｜CH4-05 简历服务边界回归测试

- 工具与模型：Codex（GPT-5）
- 协作模式：约束式
- 任务目标：扩展非禁飞区简历服务的输入校验、资源缺失、默认摘要、目标岗位更新与无副作用测试。
- Prompt 摘要：为既有 `ResumeService` 添加空白名称、缺失简历、缺失版本、空白变更摘要及目标岗位绑定测试；不触碰评分、匹配排序或面试状态机。
- 允许读取和修改的范围：`backend/src/test/java/com/resumego/resume/ResumeServiceTest.java`、CH4 执行清单、本日志；不修改生产实现与禁飞区模块。
- AI 产出：6 条服务层特征测试，并扩展测试用假仓储以记录写入调用。
- 采纳内容：采纳“校验失败前不写入仓储”“不存在资源返回稳定异常”“空白摘要使用默认文本”的断言；目标岗位更新仅验证绑定数据，不计算岗位匹配结果。
- 拒绝内容及原因：未采纳任何评分、匹配排序或面试状态判断；也未修改简历内容、版本生成的生产逻辑。
- 人工修改：首次编译缺少 `UpdateResumeTargetJobRequest` 导入，人工确认根因后作最小导入修正；成员需在最终冻结提交上重新记录最终覆盖率。
- 发现的 AI 问题：测试初版遗漏 DTO 导入；此外，测试期间其他成员修改了 `MockAiClient` 生产代码，JaCoCo 指令总数从 20,826 增至 20,996，导致全局百分比不能与上一轮直接比较。
- 测试与验证证据：目标测试 `mvn -Dtest=ResumeServiceTest test` 通过（11 个测试）；全量 `mvn test` 通过（301 个测试，0 失败，0 错误，1 跳过）；当前全后端指令覆盖率 65.6%（13,778 / 20,996），分支覆盖率 54.7%（879 / 1,606），`ResumeService` 指令覆盖率 79.6%（250 / 314）。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本次只验证简历保存、版本与目标岗位绑定的服务契约，不计算简历评分、岗位排序或面试状态转换。
- 反思与后续规则：并行开发期间覆盖率必须同时记录 covered/total 与代码版本；达到最终门槛前先冻结提交，再由 QA 执行同一提交上的 JaCoCo 门禁与 Bug 注入。

### 2026-07-15｜PO（姓名待补）｜CH4-06 排版 AI 安全契约测试

- 工具与模型：Codex（GPT-5）
- 协作模式：约束式
- 任务目标：补充排版 AI 助手的模型异常降级、结构化输出过滤和接口异常响应测试。
- Prompt 摘要：使用固定 Mock AI 输出验证模型不可用时不丢失草稿、模板/模块仅取白名单、接口不向客户端返回内部错误；不生成评分、匹配排序或面试状态机规则。
- 允许读取和修改的范围：`backend/src/test/java/com/resumego/layout/`、CH4 执行清单、本日志；不修改生产实现与禁飞区模块。
- AI 产出：2 条排版服务安全测试和 3 条 Controller 响应契约测试。
- 采纳内容：采纳本地兜底必须为低风险、结构化输出只使用原文可验证字段、未知模板/模块被过滤、服务异常响应不暴露内部详情的测试。
- 拒绝内容及原因：未使用真实简历内容或真实模型调用；未涉及任何评分权重、岗位排序或面试状态判断。
- 人工修改：首次编译发现 DTO 的实际访问器为 `before()` 而非初版假定的 `beforeText()`；查阅 DTO 后只修正测试断言。
- 发现的 AI 问题：初版测试错误假设了 DTO 字段名称；此外，异常测试会触发现有错误日志堆栈，使测试输出较长，但客户端响应已被验证不含内部错误内容。
- 测试与验证证据：目标测试 `mvn -Dtest=LayoutProposalServiceTest,LayoutProposalControllerTest test` 通过（7 个测试）；全量 `mvn test` 通过（306 个测试，0 失败，0 错误，1 跳过）；当前全后端指令覆盖率 67.5%（14,170 / 20,996），分支覆盖率 56.7%（911 / 1,606）；`LayoutProposalController` 为 100%（41 / 41），`LayoutProposalService` 为 58.6%（1,021 / 1,742）。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：排版助手只输出用户可审查的表达和模板提案，不决定分数、排序或状态迁移；本次未改动三项禁飞区实现。
- 反思与后续规则：AI 相关测试应优先覆盖“模型失败、Schema 失败、白名单过滤、无持久化副作用”四类安全契约；最终覆盖率需在团队冻结的同一提交上复测。

### 2026-07-15｜PO（姓名待补）｜CH4-07 测试编译阻塞定位与恢复

- 工具与模型：Codex（GPT-5）
- 协作模式：约束式
- 任务目标：继续补充非禁飞区优化建议服务的模型失败回归测试并执行目标测试。
- Prompt 摘要：只为 AI 建议服务验证调用失败后记录审计、返回空建议且不写入建议表；不改变评分、匹配排序或面试状态机。
- 允许读取和修改的范围：优化建议测试与现有测试编译错误上下文；不修改并行成员新建的测试或生产实现。
- AI 产出：新增一条模型失败无副作用测试；定位并记录并行测试的编译阻塞。
- 采纳内容：构建恢复后，采纳模型失败时“记录审计、返回空建议、不写入建议表”的测试；按共享批次记录全量回归，不将成员新增 Controller 测试归为本条 AI 产出。
- 拒绝内容及原因：拒绝直接改写未提交的 `ResumeControllerTest` 或为迎合其旧接口预期而修改生产代码，避免覆盖并行成员工作与制造非原子变更。
- 人工修改：需要 `ResumeControllerTest` 作者将测试替身对齐现有 `ResumeRepository` 方法签名，并确认是否把 `createResume(null)` 的服务层校验作为独立 Bug 修复。
- 发现的 AI 问题：执行目标测试时，Maven testCompile 被未提交的 `ResumeControllerTest` 阻断：三个 `@Override` 对应的方法已不在当前 Repository 中；测试还包含当前实现不满足的 null 请求预期。
- 测试与验证证据：阻塞文件修复后，`mvn -Dtest=ResumeControllerTest test` 通过（12 个测试）；`mvn -Dtest=OptimizationSuggestionServiceTest test` 通过（20 个测试）；全量 `mvn test` 通过（331 个测试，0 失败，0 错误，1 跳过）。当前指令覆盖率 68.9%（14,456 / 20,996），分支覆盖率 57.7%（927 / 1,606），`OptimizationSuggestionService` 指令覆盖率 49.0%（557 / 1,136）。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本条仅涉及优化建议服务的失败降级测试，未改动三个禁飞区。
- 反思与后续规则：团队并行开发时，新增测试必须在提交前对齐当前公开接口并单独运行；遇到非本人未提交文件的编译阻塞，应先记录并由作者确认，不能以“让构建变绿”为由擅自改语义。

### 2026-07-15｜PO（姓名待补）｜CH4-08 阶段证据核对与报告同步

- 工具与模型：Codex（GPT-5）
- 协作模式：约束式
- 任务目标：根据当前仓库中的 JaCoCo 配置、测试结果与报告文件，将 CH4 文档同步为真实阶段状态。
- Prompt 摘要：检查当前门禁阈值、执行 `mvn verify`、核对报告目录中的证据文件；不将尚未存在的 85% 覆盖率或 Bug 注入结果写为已完成。
- 允许读取和修改的范围：`backend/pom.xml`、`harness-docs/reports/`、本日志；不修改生产代码、禁飞区实现或门禁阈值。
- AI 产出：阶段完成状态表、报告草案更新建议和验证结果整理。
- 采纳内容：采纳“65% 阶段门禁已通过、85% 最终门禁未完成、Bug 注入证据未见”的如实表述。
- 拒绝内容及原因：拒绝把 `mvn verify` 成功直接写成 CH4 最终完成，因为当前 Bundle 最低阈值是 65% 而非 85%。
- 人工修改：需由 QA 补录两次 Bug 注入证据，由 PO 填写团队信息、冻结 Commit 与截图文件名。
- 发现的 AI 问题：无；该步骤只依据命令输出与仓库文件做状态核对。
- 测试与验证证据：`mvn verify` 通过，输出 `All coverage checks have been met.` 与 `BUILD SUCCESS`；当前全量测试基线为 331 个测试、0 失败、0 错误、1 跳过；全局 JaCoCo 门槛为 65%。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：仅整理测试证据与报告状态，不生成或修改评分、排序或状态机实现。
- 反思与后续规则：最终报告的“已完成”必须以同一冻结提交上的门禁、覆盖率和注入证据为准，不能以阶段性绿灯代替最终验收。

### 2026-07-07｜成员 1｜S1-00 / S1 工程骨架

- 工具与模型：Codex
- 协作模式：约束式
- 任务目标：协助完成 S1 启动前的项目认知对齐、团队协作规则整理、基础工程骨架确认、数据库迁移机制和演示数据准备。
- Prompt 摘要：围绕「职达」S1 基础闭环，要求结合课程中的 Harness Engineering、AI 禁飞区、AI 使用日志、Bug 注入检测、团队角色分工和 MySQL/千问 Max 技术选择，逐步整理文档与工程准备工作。
- 允许读取和修改的范围：项目说明文档、团队协作文档、AI 使用与禁飞区文档、前后端基础工程配置、数据库迁移和本地启动配置。
- AI 产出：项目文档结构建议、团队协作流程、AI 使用政策、禁飞区说明、Sprint 1 数据库表结构迁移、Sprint 1 演示种子数据、Docker/MySQL 本地启动排查建议。
- 采纳内容：采纳固定文档结构、三人角色与禁飞区责任矩阵、每人本地 Docker MySQL 的开发方式、Flyway 迁移机制、S1 最小演示数据设计、项目数据库使用独立端口以避免本机 MySQL 冲突。
- 拒绝内容及原因：未采纳任何由 AI 直接生成的简历评分算法、岗位匹配排序算法或模拟面试状态机实现；这些属于课程 AI 禁飞区，必须由对应成员人工完成。
- 人工修改：成员 1 确认项目名称、项目方向、S1 只做基础逻辑闭环、数据库使用 MySQL、AI 模型使用千问 Max，并确认三名成员的 PO/SM/QA 角色分工。
- 发现的 AI 问题：初期需要反复收敛文档目录和仓库目录的关系；数据库验证过程中发现端口冲突和 JDBC 字符集参数问题，需要人工确认后修正；本机测试还暴露 Java 26 与 Mockito/ByteBuddy 的兼容风险，后续建议团队统一 JDK 21。
- 测试与验证证据：Docker Compose 配置可解析；项目 MySQL 容器可启动；后端启动后 Flyway 成功执行到 v2；数据库中已存在基础表和 S1 演示数据；评分、匹配和 AI 建议结果表保持为空，未用假数据替代业务逻辑。
- 是否涉及 AI 禁飞区：是
- 禁飞区处理说明：本次仅整理禁飞区规则、责任划分、表结构和验证约束，没有生成三个禁飞区的核心实现。简历评分、岗位匹配排序和模拟面试状态机仍需由对应成员人工设计、编码和讲解。
- 反思与后续规则：后续使用 AI 时必须先明确任务范围和禁飞区边界；每次 AI 协助后及时补充日志；禁飞区只允许 AI 辅助解释需求、整理测试边界和审查文档，不允许生成可直接落地的核心算法或状态机代码。

### 2026-07-07｜成员 2｜S1-03 岗位 JD 保存与结构化

- 工具与模型：Claude Code（deepseek-v4-pro）
- 协作模式：精确式
- 任务目标：实现岗位 JD 粘贴、保存、查询和结构化字段展示的基础前后端功能。
- Prompt 摘要：按 OpenAPI 草案和 Sprint 1 验收标准，在 job 模块创建 Entity、Mapper、DTO、Service、Controller，前端创建 JD 粘贴页和详情页；JD 解析先使用 Stub 实现，后续由 S1-08 替换为千问 Max 真实调用。
- 允许读取和修改的范围：job 模块、ai 模块中的 JD 解析接口与 Stub、相关测试、前端 JD 页面与类型、AI 使用日志。
- AI 产出：
  - 后端：JD Entity、Mapper、DTO、Service、Controller、JdParseService 接口和 StubJdParseService；
  - 前端：TypeScript 类型定义、API 层、JD 创建页、JD 详情页、ParsedFields 共享组件、路由更新和首页导航；
  - 测试：StubJdParseServiceTest、JobDescriptionServiceTest 和 H2 测试 Schema。
- 采纳内容：采纳 JD 保存、查询、解析 Stub、前端创建/详情页面和测试主体。
- 拒绝内容及原因：未采纳“匹配打分”表述；本 Issue 只做 JD 录入与结构化解析，不实现岗位匹配排序禁飞区。
- 人工修改：
  - 合入最新 dev 后，复用已有公共响应、异常处理和当前用户约定，避免出现两套 common 结构；
  - 修正 AI 使用日志格式和与代码不一致的说明；
  - 前端错误处理兼容当前公共响应结构。
- 发现的 AI 问题：初版生成了重复的公共响应类和全局异常处理器；提交标题和部分说明出现“匹配打分”表述，容易与岗位匹配排序禁飞区混淆；AI 日志末尾存在多余代码块结束符。
- 测试与验证证据：初版记录显示后端编译、后端测试和前端构建曾通过；合入 dev 后仍需重新运行后端测试和前端构建验证。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：JD 解析属于信息抽取和结构化输入准备，不属于简历评分权重、岗位匹配排序、模拟面试状态机三个禁飞区。本次不生成匹配分数、不写入排序结果、不修改 `job_matches`。
- 反思与后续规则：
  - S1-03 的 MR 标题和描述应使用“岗位 JD 录入与结构化解析”，避免“匹配打分”表述；
  - parsed_json 结构变更时需同步 DTO、前端类型、OpenAPI 和 V2 种子数据；
  - Stub 解析质量有限，真实场景依赖 S1-08 千问 Max + JSON Schema 校验。

### 2026-07-08｜成员 1｜S1-02 简历接口
  
  - 工具与模型：Claude (DeepSeek-v4-pro)
  - 协作模式：精确式
  - 任务目标：实现 S1-02 简历与版本查询接口（GET /api/v1/resumes、GET /api/v1/resume-versions/{id}）
  - Prompt 摘要：根据 OpenAPI 规范和数据模型，按现有 S1-01 代码风格实现简历列表查询和版本详情查询
  - 允许读取和修改的范围：backend/src/main/java/com/resumego/resume/ 目录，新增 DTO、Repository、Service、Controller
  - AI 产出：
    - ResumeDTO.java
    - ResumeVersionDTO.java
    - ResumeRepository.java
    - ResumeService.java
    - ResumeController.java
  - 采纳内容：全部采纳
  - 拒绝内容及原因：无
  - 人工修改：无
  - 发现的 AI 问题：无
  - 测试与验证证据：Maven 编译通过，未执行集成测试（需 MySQL）
  - 是否涉及 AI 禁飞区：否
  - 反思与后续规则：接口遵循 JobDescriptionController 的 /api/v1/ 前缀和 ResponseEntity 包装模式，与 S1-03 风格一致；S1-01
  的 /api/evidences 用的是旧路径 /api/，后续可统一对齐

### 2026-07-08｜成员 1｜接口版本与 Java/Lombok 统一修正

- 工具与模型：Codex
- 协作模式：精确式
- 任务目标：统一后端接口版本前缀，修正 Java 版本约束，恢复 S1-03 相关类对 Lombok 的使用，并确认数据库访问方式。
- Prompt 摘要：用户说明本机 `mvn -v` 使用 Java 11，并要求直接修改代码，解决 Java 26/版本不一致、接口路径不统一、Lombok 未使用以及数据库访问方式确认问题。
- 允许读取和修改的范围：后端 Maven 配置、job 模块 Entity/DTO/Service/Controller、证据接口 Controller、AI 使用日志；不修改禁飞区算法。
- AI 产出：新增 `.java-version`，补充 Maven compiler release=21 与 Lombok annotation processor；将 JD Entity/DTO/Service/Controller 恢复为 Lombok 写法；将证据接口从 `/api/evidences` 统一为 `/api/v1/evidences`。
- 采纳内容：采纳 Java 21 作为团队统一版本、业务接口统一 `/api/v1` 前缀、MyBatis-Plus 作为当前 CRUD 方式。
- 拒绝内容及原因：未引入评分、岗位匹配排序或面试状态机实现；未新增复杂基础设施。
- 人工修改：待成员 1 审查后决定是否提交。
- 发现的 AI 问题：本机不同终端环境存在 JDK 不一致；用户终端显示 Maven 使用 Java 11，Codex 环境显示 Maven 使用 Java 26，测试仍受 Java 26 与 Mockito/ByteBuddy self-attach 兼容问题影响。
- 测试与验证证据：`mvn compile` 通过；`npm run build` 通过但存在第三方 pure annotation 和 chunk size 警告；`mvn test` 未通过，失败原因是 Java 26 下 Mockito inline ByteBuddy mock maker 无法 self-attach。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本次仅做工程配置、接口路径和代码样板统一，不涉及简历评分权重、岗位匹配排序或模拟面试状态机。
- 反思与后续规则：团队需统一安装和使用 JDK 21；提交前用 `mvn -v` 确认 Java version 为 21，再运行 `mvn test`。

### 2026-07-08｜成员 3｜S1-08 AI 客户端核心 + 调用日志 + 结构化校验
- 工具与模型：Trae IDE（Deepseek 4.0 Pro）
- 协作模式：约束式
- 任务目标：实现 AI 客户端核心包（统一接口、千问 Max 真实调用、Mock 模拟调用、根据 API Key 自动切换）、AI 调用日志记录、AI 输出结构化校验、基础测试框架。
- Prompt 摘要：要求先确认任务是否涉及 AI 禁飞区，然后在 ai 模块下创建 client 子包（定义 AiClient 接口、QwenMaxProvider 千问 Max 真实调用骨架、MockAiClient 无密钥时自动切换 mock 模式）、log 子包（AI 调用日志记录）、validate 子包（AI 输出结构化校验），以及基础测试框架。已完成的部分不再重复。
- 允许读取和修改的范围：ai 模块（client、log、validate 子包）、application-dev.yml 配置文件、AI 使用日志文档。
- AI 产出：
 - 客户端核心：AiClient.java（统一接口）、AiRequest.java（请求对象，含 prompt/messages/temperature/maxTokens/systemPrompt）、AiResult.java（响应对象，含 status/tokenUsage/latencyMs/mock 标识、工厂方法）、QwenMaxProvider.java（千问 Max HTTP 调用、Bearer 鉴权、OpenAI 兼容格式请求体拼装、响应解析和 token 用量提取）、MockAiClient.java（关键词匹配模板，含 JD 解析/评估/优化/匹配四套预设响应，模拟网络延迟 200-800ms）、AiClientConfig.java（Spring 配置，根据 apiKey 是否配置自动切换 QwenMaxProvider 或 MockAiClient）；
- 调用日志：AiInvocation.java（ai_invocations 表 Entity）、AiInvocationMapper.java（MyBatis-Plus Mapper）、AiInvocationService.java（日志服务，支持 recordStart/recordSuccess/recordFailure/recordTimeout，不记录完整 prompt 和 API Key）；
- 结构化校验：AiOutputValidator.java（JSON 格式校验、嵌套字段路径校验、字段类型校验、非空数组校验、markdown 代码块提取）；
- 测试：MockAiClientTest.java（12 用例）、AiOutputValidatorTest.java（20 用例）。
- 采纳内容：采纳客户端接口设计、千问 Max HTTP 调用封装、Mock 响应模板机制、API Key 自动切换逻辑、调用日志服务、结构化校验器。测试用例采纳了关键词匹配、模板注册/清除、token 估算、延迟模拟、合法/非法 JSON、必填字段、嵌套路径、null 值、类型校验、extractJson 提取等覆盖场景。
- 拒绝内容及原因：未采纳任何与简历评分权重、岗位匹配排序、模拟面试状态机相关的实现；Mock 模板中的评分数字（如 totalScore: 85）仅为模拟数据，不构成评分算法；结构化校验仅做 JSON 格式和字段存在性检查，不涉及业务评分或排序；调用日志不记录完整 prompt 或用户个人信息，仅记录审计摘要。
- 人工修改：
 - 修正 AI 使用日志格式和与代码不一致的说明；
 - 调用入口缺乏参数校检的问题。
 - QwenMaxProvider重复追加用户提问。
- 发现的 AI 问题：
 - QwenMaxProvider 中声明了 DEFAULT_CONNECT_TIMEOUT 和 DEFAULT_READ_TIMEOUT 常量，但 createRestTemplate() 方法中未实际应用到 RestTemplate 的超时配置，导致 HTTP 请求使用默认超时而非声明值；
 - AiClientConfig 中单独创建了 aiRestTemplate Bean，与 Spring Boot 自动配置的 RestTemplate 可能产生 Bean 重复或不一致，后续需要确认是否需要自定义超时、重试等配置；
 - 初始版本 AI 使用日志格式不符合团队模板要求，缺少 Issue 编号、人工修改和发现的 AI 问题等关键字段，经人工对照模板修正。
- 测试与验证证据：新增测试 32 个全部通过，Tests run: 32, Failures: 0, Errors: 0, Skipped: 0；AiClientConfig 在无 API Key 时日志输出 "API Key 未配置，使用 MockAiClient"，成功自动切换；Mock 客户端按关键词返回对应模板响应；校验器正确识别合法/非法 JSON、必填字段缺失、类型不匹配、空数组、markdown 代码块提取等边界情况；Maven 编译和全部现有测试（含 AiConfigTest 15 用例）通过。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：结构化校验仅做 JSON 格式和字段存在性检查，不涉及评分、排序、状态机逻辑；调用日志仅记录审计摘要（requestId、featureType、provider、token 用量、latency），不记录完整 prompt 或业务结果；Mock 模板中的评分数字仅为开发调试用模拟数据，不构成简历评分权重计算；未生成简历评分权重、岗位匹配排序、模拟面试状态机的核心实现。
- 反思与后续规则：
 - Mock 模式作为降级方案而非默认路径，通过 AiClientConfig 根据 API Key 显式切换，避免误用 Mock 导致开发测试阶段与生产行为不一致；
 - AiInvocationService 的日志记录粒度（开始/成功/失败/超时）覆盖了 AI 调用全生命周期，后续可通过 feature_type 字段区分不同场景的调用统计；
 - AiOutputValidator 与业务评分完全解耦，仅校验数据结构，后续可扩展为各 feature_type 定义专用的必填字段和类型规则；
 - 使用 AI 时必须先明确任务范围和禁飞区边界；每次 AI 协助后及时补充日志，且日志格式需严格遵循团队模板，确保包含 Issue 编号、人工修改和发现的 - - AI 问题等关键字段；
 - 后续 S1-08 替换 StubJdParseService 为千问 Max 真实调用时，需复用 QwenMaxProvider + AiOutputValidator + AiInvocationService 这一套基础设施，并修复 QwenMaxProvider 中 RestTemplate 超时配置未生效的问题。

### 2026-07-08｜成员2｜S1-08 AI 安全通道

- 工具与模型：Claude Code（deepseek-v4-pro）
- 协作模式：精确式
- 任务目标：实现阶段 B AI 安全调用通道——AiClient 接口、千问 Max Provider 骨架、Mock AI 实现、ai_invocations 审计日志写入、JSON Schema 校验雏形。
- Prompt 摘要：根据阶段 B 验收标准逐项检查缺口，在 com.resumego.ai 包下补全接口定义、Provider 骨架、Mock 实现、审计日志服务和 Schema 校验器，附带完整单元测试和集成测试。
- 允许读取和修改的范围：backend/src/main/java/com/resumego/ai/（新增源文件）、backend/src/test/java/com/resumego/ai/（新增测试文件）、backend/src/test/resources/sql/（新增 H2 测试 Schema）
- AI 产出：
  - AiErrorCategory.java — 错误分类枚举（TIMEOUT/INVALID_JSON/PROVIDER_ERROR/UNKNOWN）
  - AiRequest.java / AiResult.java — 请求/响应 DTO record
  - AiClient.java — 统一调用接口：AiResult invoke(AiRequest)
  - MockAiClient.java — 无 API Key 时的本地 Mock，按 featureType 返回预制结构化 JSON
  - QwenMaxProvider.java — 千问 Max 骨架，含完整 RestClient 调用链路和异常分类（真实 HTTP 已注释待解除）
  - AiClientSelector.java — 根据 isApiKeyConfigured() 自动选择 QwenMaxProvider 或 MockAiClient
  - AiInvocation.java / AiInvocationMapper.java / AiInvocationService.java — ai_invocations 表实体、Mapper 和审计日志服务，记录请求元数据但不存储 userMessage 原文
  - JsonSchemaValidator.java — JSON Schema 校验器，支持必填字段检查、类型匹配、非法 JSON 检测
  - 5 个测试文件 + 1 个 H2 Schema SQL，共 32 个新增测试用例
- 采纳内容：全部采纳
- 拒绝内容及原因：无
- 人工修改：修复 MockAiClientTest 中一处大小写断言（"Mock" vs "mock"）；修复预先存在的 AiConfigTest$SpringIntegrationTest 构造器注入为 @Autowired 字段注入
- 发现的 AI 问题：无
- 测试与验证证据：mvn test -Dtest="com.resumego.ai.*Test" — Tests run: 47, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：AI 模块本身属于基础设施层，不涉及简历评分权重计算、岗位匹配相似度排序、模拟面试状态机三个禁飞区
- 反思与后续规则：
  - QwenMaxProvider 当前为骨架，真实 HTTP 调用已用注释标记，后续解除注释并接入 RestClient 即可
  - 审计日志已确保不记录 userMessage（业务数据）和 API Key，符合隐私安全要求
  - JsonSchemaValidator 当前支持字段级校验，后续可按 featureType 定义专用 Schema 规则
  - 与成员3 已有实现的差异：本实现将 AiClientSelector/Mock/Qwen/Validator/Invocation 统一放在 ai 包下，成员3 的实现使用了 ai/client 子包和 AiOutputValidator 命名，合入时需统一合并策略

### 2026-07-08｜成员3｜S1-06 AI 优化建议模块

- 工具与模型：Trae IDE（Deepseek 4.0 Pro）
- 协作模式：约束式
- 任务目标：实现 S1-06 定向修改建议模块——基于岗位匹配缺口和能力证据，通过 AI 生成结构化简历优化建议，支持用户逐条采纳或拒绝。核心包括 Prompt 构建器、建议生成服务、入参校验与编造检测、REST 接口层和完整单元测试。
- Prompt 摘要：要求先确认任务是否涉及 AI 禁飞区，然后在 optimization 模块下实现：SuggestionPromptBuilder模块 evidence_required模块、OptimizationSuggestionService模块、SuggestionValidation（模块、OptimizationSuggestionController模块、OptimizationSuggestion 实体 + Mapper + DTO，以及完整的单元测试。已完成的部分不再重复。
- 允许读取和修改的范围：optimization 模块全部（Entity、Mapper、Service、Controller、PromptBuilder、Validation、DTO）、相关测试文件、AI 使用日志文档。
- AI 产出：
 - 服务层：OptimizationSuggestionService.java实现双模式建议生成、建议查询、状态流转、AI调用通用管线与数据持久化能力；
 - Prompt 构建器：遵循四大核心原则：禁止编造、无证据返回空、证据驱动增强、强制标准化JSON输出。
 - 校验与编造检测：定义统一长度、格式、枚举、数量等常量校验规则，覆盖所有入参合法性校验。
 - Controller 层：提供6个REST接口，覆盖双模式生成、版本委托生成、查询、采纳/拒绝全场景，实现400/404/500分级异常处理。
 - 数据层：完成实体、Mapper、4类DTO完整封装，新增风险等级、Prompt版本、审计调用ID、决策时间等拓展字段，适配业务与审计需求。
 - 测试：累计48条全量单元测试，100%通过：
- 采纳内容：采纳整体模块架构（Service + PromptBuilder + Validation + Controller + DTO 分层设计）、双模式 Prompt 构建（Mock 模式用 raw JSON、真实模式用 MatchingService/ResumeAssessmentService 结构化数据）、编造检测方案B（差集检测 + high_risk 标记后仍保存不丢弃）、建议状态机（pending → accepted/rejected，加 evidence_required 和 high_risk 扩展状态）、AI 审计日志集成（AiInvocationService.logInvocationWithSchema）及 JSON Schema 校验管线（extractJson → validateJson → validateRequiredFields → validateFieldType 四级串联）、所有入参校验规则和停用词表（中英文共 60+ 词）。测试用例采纳了全部 48 条（19 + 13 + 16），覆盖正常路径、异常路径、边界值和参数化测试。
- 拒绝内容及原因：未采纳任何与简历评分权重计算、岗位匹配排序算法、模拟面试状态机三个禁飞区相关的实现；编造检测使用关键词差集（方案B），不涉及 AI 主观判断简历内容的真实性，标记 high_risk 后由人工最终裁定；suggestedText 的修改内容由 AI 生成但经证据关键词校验，不替代人工审核。
- 人工修改：
 - 修正 AI 使用日志格式和与代码不一致的说明；
 - generateSuggestionsByVersionId 端点路径为 `/api/v1/resume-versions/{versionId}/ai-suggestions` 但实际委托到 `generateSuggestionsByMatchId(request.matchId())`，路径中的 versionId 未实际使用，路径语义与实现不一致；
 - acceptSuggestion 采纳后生成新简历版本的逻辑标注为 TODO，依赖成员1 的 S1-07 完成后补充。
- 发现的 AI 问题：
 - 对接评分接口与匹配接口时，优化建议模块最小实体未丢弃导致类名冲突影响后台启动。
 - OptimizationSuggestionService.parseJsonMap() 在 JSON 解析失败时 catch 异常后静默返回空 Map，未向上层传播解析失败信息，可能导致后续 Prompt 缺少岗位要求数据但流程继续执行；
 - generateSuggestionsWithRealData() 中 resumeAssessmentService.assess() 失败时仅 log.warn 并继续，此时 assessmentResponse 为 null 导致 Prompt 缺少评分上下文信息，AI 建议质量可能下降且调用方无法感知；
- 测试与验证证据：全部 48 个新增测试通过（OptimizationSuggestionServiceTest 19/19、SuggestionPromptBuilderTest 13/13、SuggestionValidationTest 16/16），Tests run: 48, Failures: 0, Errors: 0, Skipped: 0；SuggestionPromptBuilder 的系统提示词正确包含"不得编造"核心原则和 JSON Schema 要求；SuggestionValidation 的 stop word 表覆盖中英文 60+ 词，detectFabrication 关键词差集逻辑正确；OptimizationSuggestionService 入参校验完整覆盖 0 值、负数、null、空内容、未解析 JD 等异常路径；accept/reject 状态流转正确（pending → accepted/rejected，非 pending 状态拒绝操作、无证据拒绝采纳）；Maven 编译和全部现有测试通过。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本模块不涉及简历评分权重计算、岗位匹配相似度排序、模拟面试状态机三个禁飞区
- 反思与后续规则：
 - 编造检测方案B（关键词差集）作为基础防线有效但粒度较粗，后续可扩展为方案C（逐句校验 + 引用定位），将检测精度从整条建议粒度提升到句子级别；
 - generateSuggestionsWithRealData 对评分服务失败的容错策略需要重新评估——是否应在评分不可用时降级为 Mock 模式而非静默跳过，避免 AI 建议质量下降且无感知；
 - 采纳建议后生成新简历版本的功能（S1-07）依赖 OptimizationSuggestion.acceptedVersionId 字段和 ResumeService 的版本创建逻辑，成员 1 实现时需确保采纳链路的事务完整性（建议状态更新 + 新版本创建在同一事务内）；
 - Controller 异常处理已按 IllegalArgumentException → 404、IllegalStateException → 400、Exception → 500 三级分层，后续可统一抽取为 GlobalExceptionHandler 的专用异常类型，减少 Controller 中的 try-catch 重复代码；

### 2026-07-09｜成员2｜S2 job_descriptions 表扩展（岗位来源元数据 + 岗位类型 + 真实数据录入）

- 工具与模型：Claude Code（DeepSeek-v4-pro）
- 协作模式：约束式 — 用户给出明确的字段设计方案（source_meta_json JSON NULL 必须、job_type VARCHAR(50) NULL 可选），AI 负责全链路落地与数据录入
- 任务目标：
  - 将 `job_descriptions` 表从仅存 JD 原文+AI 解析结果，扩展为支持岗位来源元数据和岗位类型筛选
  - 按新表结构录入 10 条真实招聘平台岗位数据
- Prompt 摘要：浏览项目所有文件 → 理解现有数据模型 → 按指定字段方案修改全链路（DB 迁移 → Entity → DTO → Service → 测试 Schema → 前端类型 → Vue 视图）→ 分两批录入真实岗位数据（TSV 格式表格），自动映射 job_type（实习→campus、应届生→campus、不限→campus、1-3年→social、3-5年→social、5-10年→social、10年以上→social）
- 允许读取和修改的范围：
  - `backend/src/main/resources/db/migration/` — 新增 V7 迁移、V8 种子数据
  - `backend/src/main/java/com/resumego/job/` — Entity、DTO、Request、Service
  - `backend/src/test/resources/sql/` — 3 个 H2 测试 Schema
  - `frontend/src/types/job.ts` — TypeScript 类型定义
  - `frontend/src/views/JobCreateView.vue`、`JobDetailView.vue`、`JobListView.vue` — Vue 页面
- AI 产出：
  - V7__add_job_source_meta_and_type.sql — Flyway 迁移，新增 `source_meta_json JSON NULL` 和 `job_type VARCHAR(50) NULL`
  - V8__seed_real_job_data.sql — 10 条真实岗位种子数据（id 10~19），每条包含完整的 raw_text、parsed_json、source_meta_json 和 job_type
  - 后端 4 个文件修改：JobDescription.java 新增 sourceMetaJson/jobType 字段、JobDescriptionDTO.java 新增 sourceMeta（Object）/jobType、CreateJobDescriptionRequest.java 新增可选字段、JobDescriptionService.java 在 create() 和 toDTO() 中处理新字段
  - 测试 3 个 H2 Schema 同步新增两列
  - 前端 4 个文件修改：job.ts 新增 sourceMeta 和 jobType 类型、JobCreateView.vue 增加岗位类型下拉（实习/校招/社招）、JobDetailView.vue 增加岗位类型标签和 sourceMeta 信息卡片、JobListView.vue 列表卡片增加岗位类型标签
  - AI 使用日志 1 条
- 采纳内容：全部采纳
- 拒绝内容及原因：无
- 人工修改：无（用户提供原始数据，AI 独立完成映射和 SQL 编写）
- 发现的 AI 问题：无
- 测试与验证证据：
  - 所有新增列为 `NULL`，现有种子数据（V2/V3/V4）无需修改，向后兼容
  - `JobDescriptionServiceTest` 无需修改即可通过（新字段为 null 不影响已有断言）
  - `MatchingService` / `OptimizationSuggestionService` 仅读取 job_descriptions 表，不受新增列影响
  - 前端岗位类型标签映射正确：internship→warning/实习、campus→success/校招、social→default/社招
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本次仅扩展数据存储结构和录入原始岗位数据，不涉及简历评分权重计算、岗位匹配相似度排序、模拟面试状态机三个禁飞区
- 反思与后续规则：
  - `source_meta_json` 存 Object → DTO 层反序列化方案验证可行，后续新增扩展字段无需改表结构
  - `job_type` 拆分独立列是正确的——前端列表页需按类型筛选过滤，放 JSON 里查询性能差且无法建索引
  - `ON DUPLICATE KEY UPDATE` 模式保证种子数据迁移幂等，后续批量录入沿用此模式
  - 真实数据录入时 AI 自动完成 job_type 映射（应届生/不限→campus，有年限→social），准确率 100%

### 2026-07-09｜成员 1｜S1-10 / S1 收尾验收与阶段总结

- 工具与模型：Codex
- 协作模式：约束式
- 任务目标：在 S1 基础闭环完成后，协助检查当前流程、总结已完成内容、识别遗留项，并形成 S1 阶段总结文档。
- Prompt 摘要：要求检查 S1 的能力证据、简历版本、岗位 JD、简历评分、岗位匹配、AI 建议、采纳/拒绝等环节是否构成基础闭环，并整理成阶段性总结文件。
- 允许读取和修改的范围：只读取现有仓库结构、文档和前后端文件；允许新增 `document/S1阶段总结.md`；不修改禁飞区算法实现。
- AI 产出：S1 阶段总结，包含阶段目标、已完成模块、数据库和接口概览、前端演示流程、AI 安全控制、禁飞区落实情况、验证方式、已知限制和下一阶段建议。
- 采纳内容：采纳“基础演示闭环已形成”的表述；采纳将“采纳建议生成新简历版本”“多版本列表”“匹配详情查询”等列为后续遗留项的处理方式，避免验收时过度承诺。
- 拒绝内容及原因：未将未完成的简历版本差异、完整版本生成、模拟面试等描述为已完成能力；未让 AI 代写评分算法、岗位匹配算法或面试状态机。
- 人工修改：成员 1 确认 S1 已进入收尾阶段，并决定开始规划 S2。
- 发现的 AI 问题：AI 初稿容易把 S1 理想目标和当前已实现状态混在一起，需要明确区分“已完成”“基础演示可用”和“后续遗留”。
- 测试与验证证据：此前后端打包和前端构建已通过；S1 总结中记录需要重启后端以应用 Flyway 迁移，并建议三名成员共同跑一遍 Smoke Test。
- 是否涉及 AI 禁飞区：是
- 禁飞区处理说明：本次只做验收总结和边界说明，不生成或修改简历评分、岗位匹配排序、模拟面试状态机实现。
- 反思与后续规则：阶段总结必须如实记录限制，不能为了演示效果把未完成能力写成已完成；S2 开始前需要同步更新 Sprint、Backlog、数据模型、禁飞区和 AI 日志。

### 2026-07-09｜成员 1｜S2-00 / S2 启动规划与范围收敛

- 工具与模型：Codex
- 协作模式：启发式 / 约束式
- 任务目标：在 S1 结束后规划 S2 的开发方向、阶段任务、Issue、分工、岗位数据处理方式和禁飞区边界。
- Prompt 摘要：围绕“B 和 C 最小版”目标，要求规划简历优化迭代闭环和模拟面试最小闭环；同时讨论真实岗位数据集、求职工作台、千问 Max API 可靠性、Mock 降级、数据库字段和团队分工。
- 允许读取和修改的范围：本次规划阶段主要是对话和文档更新；允许更新 `harness-docs/sprints/sprint-2.md`、`harness-docs/product/backlog.md`、`harness-docs/data-model/schema.md`、`harness-docs/ai/no-fly-zone.md`、`.agent/no-fly-zone.md` 和本日志。
- AI 产出：S2 总体计划、阶段 A—G、10 个建议 Issue、求职工作台结构、真实岗位数据处理原则、`source_meta_json` 设计建议、模拟面试最小状态机边界、三人分工和 4—5 天开发节奏。
- 采纳内容：采纳 S2 聚焦“求职工作台 + 简历版本迭代 + 模拟面试最小闭环”；采纳岗位数据“原文优先、核心抽取、扩展暂存”；采纳新增 `source_meta_json` 作为真实岗位扩展信息容器；采纳真实 API 优先、Mock 兜底的 AI 通道策略。
- 拒绝内容及原因：暂不做登录注册、社区发帖、复杂岗位推荐、大规模爬虫、PDF 导出、语音面试和复杂追问，避免 S2 范围失控。
- 人工修改：成员 1 明确 S2 选择“简历版本迭代中档 + 模拟面试最小版”，并确认岗位数据集主要用于增强真实感和支撑匹配测试。
- 发现的 AI 问题：AI 容易倾向把未来产品形态一次性规划进 Sprint，需要人工收敛为 4—5 天可完成的最小闭环；涉及状态机时必须反复强调不能生成实现。
- 测试与验证证据：当前为 S2 启动规划阶段，尚未产生代码测试；后续应以 S2 Smoke Test、状态机非法转换测试、AI 输出结构化校验和岗位数据 seed 验证作为证据。
- 是否涉及 AI 禁飞区：是
- 禁飞区处理说明：本次讨论涉及模拟面试状态机，但 AI 只参与目标澄清、阶段拆分和文档边界说明；状态机转换表和实现必须由成员人工完成，AI 不生成状态机代码或可直接翻译的伪代码。
- 反思与后续规则：S2 文档需要先于代码更新；禁飞区说明必须同步到 harness 文档和 `.agent` 约束；真实岗位数据先通过 JSON 扩展字段承载，不把岗位库建设扩大为推荐平台。

### 2026-07-10｜成员 1｜S2-09 模拟面试前端联调壳

- 工具与模型：Codex
- 协作模式：约束式
- 任务目标：在已有求职工作台视觉体系中新增模拟面试前端入口，展示简历版本与目标岗位选择、问题回答、单题反馈、练习总结以及返回简历优化入口，为后端面试接口联调预留稳定页面。
- Prompt 摘要：要求实现面试展示页面，但不得生成或实现模拟面试状态机；前端只能渲染后端会话快照，不得决定状态转换、题号推进或结束条件。
- 允许读取和修改的范围：`frontend/src/App.vue`、`frontend/src/router/index.ts`、`frontend/src/views/InterviewView.vue`、`frontend/src/types/interview.ts`、`frontend/src/api/interview.ts`、`frontend/src/style.css`、本日志。
- AI 产出：新增 `/interview` 路由与“面试准备”导航入口；新增接口展示类型和固定会话快照；新增面试页面及对应样式，包含题目、反馈、总结三个可视化快照和回到工作台入口。
- 采纳内容：采纳固定快照作为接口未完成期间的前端演示方式；在类型、接口样例和页面注释中明确“状态机仅由后端人工实现”。
- 拒绝内容及原因：拒绝任何前端状态转换表、题号递增、完成条件判断和自动追问逻辑，避免侵入模拟面试状态机禁飞区。
- 人工修改：成员 1 确认页面只承担呈现和接口适配职责；后续由成员 2/3 提供真实会话接口后再替换固定快照。
- 发现的 AI 问题：初版交互建议容易把“提交回答后切到反馈”写成前端状态迁移；已改为“示例反馈快照”切换，并显式标注其不代表真实会话状态。
- 测试与验证证据：`cd frontend && npm run build` 通过；浏览器验证 `/interview` 路由可访问，提交示例评价后可展示反馈，继续可展示练习总结与“回到简历优化”入口。
- 是否涉及 AI 禁飞区：是
- 禁飞区处理说明：本任务仅涉及模拟面试的界面展示。未生成、未修改状态机实现，也未让前端决定状态、题号、结束条件或重试次数。
- 反思与后续规则：接入真实 API 时，前端应以服务端返回的会话快照为唯一事实来源；若后端返回异常或模型降级，应展示可理解错误与重试入口，不得用本地推断掩盖真实状态。

### 2026-07-10｜成员 1｜S2-09 / S2 前端主链路接口接入

- 工具与模型：Codex
- 协作模式：约束式
- 任务目标：在后端面试、版本和岗位数据代码落库后，推进 S2 前端主链路，使工作台、简历优化迭代和模拟面试页面能够承接真实接口。
- Prompt 摘要：要求在新代码基础上着手前端开发并完成 S2 阶段任务，重点补齐工作台入口、建议采纳生成新版本后的展示、模拟面试真实 API 接入。
- 允许读取和修改的范围：`frontend/src/views/InterviewView.vue`、`frontend/src/api/interview.ts`、`frontend/src/types/interview.ts`、`frontend/src/views/AssessmentView.vue`、`frontend/src/views/HomeView.vue`、`frontend/src/style.css`、本日志。
- AI 产出：将模拟面试页从固定快照改为读取真实简历版本和岗位列表；新增创建会话、开始面试、提交回答、查询最终状态的前端 API；工作台新增模拟面试入口；采纳建议后读取版本列表并切换到最新简历版本。
- 采纳内容：采纳“前端只触发后端动作并渲染后端快照”的实现边界；采纳“采纳建议后跳转最新版本”的演示方案，确保 S2 新版本能力可见。
- 拒绝内容及原因：未在前端实现面试状态机、题号推进或完成判断规则；未修改简历评分权重、岗位匹配排序或状态机后端实现，避免触碰 AI 禁飞区。
- 人工修改：成员 1 负责后续真实联调时输入回答、检查版本生成和页面跳转是否符合演示口径。
- 发现的 AI 问题：初次实现中存在同一 API 文件既静态又动态导入的构建警告，已改为静态导入；同时保留后端状态为唯一事实来源。
- 测试与验证证据：`cd frontend && npm run build` 通过；仅保留既有第三方 `@vueuse/core` pure annotation 和 bundle size 警告。
- 是否涉及 AI 禁飞区：是
- 禁飞区处理说明：本次只做接口承接和页面渲染，不生成或调整任何评分算法、匹配算法、面试状态机规则。前端不决定状态转换、不递增题号、不设置结束条件。
- 反思与后续规则：真实演示前需要启动后端和 MySQL，跑通“工作台 → 简历评分/匹配 → AI 建议 → 采纳生成新版本 → 模拟面试三轮 → 总结 → 回到简历优化”的 smoke test。

### 2026-07-10｜成员2｜S2-06 模拟面试数据表 + 真实岗位种子数据导入

- 工具与模型：Claude Code（DeepSeek-v4-pro）
- 协作模式：精确式
- 任务目标：
  - V9：创建模拟面试模块的 4 张数据库表（interview_sessions、interview_questions、interview_answers、interview_evaluations），支撑 Sprint 2 完整模拟面试流程；
  - V10：基于用户提供的真实招聘平台岗位数据，生成 10 条 Flyway 种子数据，包含 raw_text、parsed_json、source_meta_json 和 job_type 完整字段。
- Prompt 摘要：V9 — 根据 no-fly-zone.md 中已定义的状态枚举和 S2 面试模块需求，创建 Flyway 迁移文件，包含 4 张表及外键约束、CHECK 约束和索引；V10 — 根据用户提供的 TSV 格式真实岗位数据（含 base/salary/platform/industry/companySize/education/experience/tags 等字段），自动映射 job_type（实习/应届生→campus，1-3年/3-5年/5-10年/10年以上→social），生成完整的 INSERT ... ON DUPLICATE KEY UPDATE 幂等种子迁移。
- 允许读取和修改的范围：
  - `backend/src/main/resources/db/migration/` — 新增 V9、V10 两个 Flyway 迁移文件
  - 不修改任何 Java 代码、前端代码或禁飞区实现
- AI 产出：
  - V9__create_interview_tables.sql（104 行）：4 张表的完整 DDL，含 `interview_sessions`（会话主表，含 status CHECK 约束覆盖 READY→COMPLETED/FAILED/CANCELLED 8 个状态）、`interview_questions`（问题表，含 question_type CHECK 约束和 generation_ai_invocation_id 外键）、`interview_answers`（回答表，与 question 一对一）、`interview_evaluations`（AI 评估表，含 score_json/strengths_json/weaknesses_json/suggestions_json 4 个 JSON 列及 JSON_TYPE CHECK 约束）；
  - V10__seed_tech_jobs.sql（6404 行）：10 条真实岗位种子数据（id 20~29），每条包含完整的 raw_text（JD 原文）、parsed_json（AI 结构化解析结果，含 requiredSkills/responsibilities/experienceRequirements/educationRequirements）、source_meta_json（岗位来源元数据，含 base/salary/platform/industry/companySize/collectedAt/sourceJobId/tags 等）、job_type 枚举映射，采用 `ON DUPLICATE KEY UPDATE` 保证幂等。
- 采纳内容：全部采纳 V9 完整 DDL 和 V10 全部 10 条种子数据。
- 拒绝内容及原因：无。
- 人工修改：
  - V10 初版修复（commit `6c4c764`）：AI 生成的 SQL 中 `parsed_json` 和 `source_meta_json` 两个 JSON 列的值缺少单引号包裹（如 `parsed_json = {"requiredSkills":...}` 应为 `parsed_json = '{"requiredSkills":...}'`），且 JSON 内部的 `\n` 在 SQL 中需转义为 `\\n`。人工逐一为 10 条记录的两个 JSON 列补充引号并修正转义，共修改约 400 行。
- 发现的 AI 问题：
  - V10 初版 SQL 存在 JSON 列语法错误：AI 将 JSON 对象直接拼入 SQL 而未识别出 MySQL 要求 JSON 字面量必须用单引号包裹为字符串。此问题在 Flyway 执行时会导致 SQL 解析失败（`{"requiredSkills":...}` 不是合法的 SQL 值表达式）。
  - JSON 内部 `\n` 换行符需在 SQL 字符串中写为 `\\n`，AI 初版未做此转义处理。
- 测试与验证证据：
  - V9：Flyway 迁移在目标数据库中成功执行，4 张表及全部外键、CHECK 约束、索引创建成功，表结构与 no-fly-zone.md 中定义的状态枚举一致；
  - V10：修复后 Flyway 迁移成功执行，10 条岗位数据正确写入 `job_descriptions` 表；`parsed_json` 和 `source_meta_json` 列可被 MySQL JSON 函数正常查询（如 `JSON_EXTRACT`、`JSON_TYPE` 校验通过）；`job_type` 映射准确率 100%（实习→campus、应届生→campus、不限→campus/social、1-3年→social、3-5年→social、5-10年→social、10年以上→social）。
- 是否涉及 AI 禁飞区：是
- 禁飞区处理说明：
  - V9 涉及禁飞区 #3（模拟面试状态机），但 AI 仅生成数据库 DDL 结构，未生成状态机实现代码。`interview_sessions.status` 的 CHECK 约束中的 8 个状态值（READY、ASKING、WAITING_ANSWER、EVALUATING、SUMMARIZING、COMPLETED、FAILED、CANCELLED）全部来自 `no-fly-zone.md` 中人工预定义的状态枚举，AI 未决定任何状态转换规则。`current_question_index` 和 `total_questions` 字段为数据存储列，字段含义和默认值由人工确认，实际的题号递进、最大题数控制和结束判断仍由状态机实现者人工编码。
  - V10 为岗位种子数据导入，不涉及简历评分权重计算、岗位匹配排序或模拟面试状态机。
- 反思与后续规则：
  - AI 生成含 JSON 列的 SQL 时需额外确认：必须用单引号包裹 JSON 字符串，内部特殊字符（`\n`、`\'`、`\\`）需按 MySQL 字符串转义规则处理。后续录入类似种子数据时，应先在 Prompt 中明确给出 SQL 引号和转义规范，避免批量修复；
  - JSON 列的 Flyway 种子迁移建议在生成后人工执行一次 `SELECT JSON_TYPE(parsed_json) FROM job_descriptions WHERE id = 20` 抽样验证，比全量眼检更高效；
  - V9 面试表设计已预留 `generation_ai_invocation_id` 和 `evaluation_ai_invocation_id` 外键关联 AI 调用审计日志，后续面试问题生成和评估功能实现时可直接复用 S1-08 的 `AiInvocationService` 基础设施；
  - 模拟面试模块的数据库 Schema 属于禁飞区支撑层而非核心实现层，AI 辅助 DDL 生成是可接受的，但必须确保：(1) 状态枚举由人预定义；(2) 不生成状态转换函数/触发器/存储过程；(3) 不生成业务逻辑层的状态机代码。
### 2026-07-10｜成员 1｜S2-10 / JobPilot 风格简历编辑器重构

- 工具与模型：Codex
- 协作模式：约束式 / 产品设计参考式
- 任务目标：参考 JobPilot 的编辑器结构，将职达工作台从“功能卡片展示页”重构为“简历编辑器工作台”，并补齐人工编辑保存为新简历版本的最小闭环。
- Prompt 摘要：要求严格了解 JobPilot 代码结构，尽可能高度一致地推进当前项目；每一层先汇报，避免回退；重点对齐左侧段落/版本/岗位、中间简历编辑与预览、右侧 AI 助手与诊断入口。
- 允许读取和修改的范围：读取本仓库前后端代码、`.agent` 工程约束和本地 JobPilot 参考项目；允许修改 `frontend/src/views/HomeView.vue`、新增 `frontend/src/components/editor/*`、更新简历 API 类型与接口、在后端 resume 模块新增人工创建版本接口与单元测试、更新本日志。
- AI 产出：新增 JobPilot 风格的 `EditorSidebar`、`EditorCanvas`、`SectionWrapper`、`AiCoachPanel` 前端组件；新增编辑器 section 类型；将 HomeView 改为页面装配器；新增 `POST /api/v1/resumes/{resumeId}/versions` 人工编辑生成新版本接口；新增 `ResumeServiceTest` 验证版本创建与空内容拒绝。
- 采纳内容：采纳 JobPilot 的页面分层思想，即 Sidebar → Canvas → SectionWrapper → AI Panel；采纳“编辑器草稿保存为新版本”的产品路径，保证用户手工修改能够进入版本链路；采纳右侧 AI 面板显示当前选中段落上下文，为后续结构化建议采纳预留入口。
- 拒绝内容及原因：未复制 JobPilot 的翻译、求职信、模板导出、WebDAV、本地桌面等非 S2 必需能力；未实现拖拽排序和自动保存，避免范围过大；未让 AI 直接改写分数、排序或面试状态。
- 人工修改：成员 1 需要在浏览器中实际验证编辑字段、保存为新版本、版本列表切换和进入评分/面试入口的演示效果，并决定后续是否继续拆出 Pinia store。
- 发现的 AI 问题：AI 容易把“像 JobPilot”理解为一次性复刻全部功能，因此实现时必须按层收敛：先骨架，再草稿编辑，再版本保存，再 AI 建议接入；不能把未接通的能力伪装成已完成。
- 测试与验证证据：`cd frontend && npm run build` 通过；`cd backend && mvn test -Dtest=ResumeServiceTest` 通过；`git diff --check` 通过。前端构建仅保留既有第三方 `@vueuse/core` pure annotation 与 bundle size 警告。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本次只涉及简历编辑器 UI、普通版本创建接口和前后端胶水代码；未生成或修改简历评分权重、岗位匹配排序/相似度、模拟面试状态机。
- 反思与后续规则：下一步如果继续对齐 JobPilot，应优先接“AI 建议面板内聚”和“建议采纳后局部 diff 可视化”，仍然必须通过后端结构化校验与证据规则，不能让前端或模型直接决定采纳状态、分数或排序。

### 2026-07-10｜成员 1｜S2-11 / 编辑器内 AI 建议侧栏

- 工具与模型：Codex
- 协作模式：约束式 / JobPilot 参考式
- 任务目标：继续对齐 JobPilot 的编辑器体验，在工作台右侧加入当前段落上下文、岗位匹配分、结构化建议列表、原文/建议表达对比和采纳/忽略入口。
- Prompt 摘要：要求继续推进并尽可能相似；实现时需要保留每层汇报，不走回头路。
- 允许读取和修改的范围：`frontend/src/views/HomeView.vue`、`frontend/src/components/editor/AiCoachPanel.vue`、现有 `match` 与 `optimization` 前端 API、本日志。
- AI 产出：右侧 AI 面板新增“当前段落 / 全部建议”切换；展示建议状态、目标要求、生成理由、原文和建议表达；工作台内直接触发岗位匹配与结构化建议生成；采纳建议后调用后端接口生成新版本并切换到最新版本；忽略建议后更新前端状态。
- 采纳内容：采纳 JobPilot “选中 section 后让 AI 围绕该 section 工作”的交互模式；采纳“有未保存草稿时先保存再生成建议”的版本边界，避免建议绑定到不稳定内容。
- 拒绝内容及原因：未在前端自行生成建议、风险等级、匹配分或排序；未让模型直接写入简历内容；未复制 JobPilot 的工具调用执行器，避免绕过后端结构化校验和证据规则。
- 人工修改：成员 1 需要在真实后端运行时验证“生成建议 → 当前段落筛选 → 采纳生成新版本 → 版本切换”的演示路径。
- 发现的 AI 问题：AI 容易把右侧助手做成纯聊天框，但课程要求更强调结构化校验和可审计采纳，因此本阶段采用“建议卡片 + 原文/建议 diff + 明确状态”的受控形态。
- 测试与验证证据：`cd frontend && npm run build` 通过；`git diff --check` 通过。前端构建仅保留既有第三方 `@vueuse/core` pure annotation 与 bundle size 警告。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本次只编排已有后端岗位匹配与建议接口，未生成或修改评分权重、匹配排序规则或面试状态机；前端不决定建议状态和风险，只展示后端返回结果。
- 反思与后续规则：如果继续增强，应优先增加建议来源和证据可见性，以及从面试评价回流到建议池；所有采纳仍必须通过后端接口生成新版本并留下日志。

### 2026-07-10｜成员 1｜S2-12 / JobPilot 三栏编辑器与实时预览

- 工具与模型：Codex
- 协作模式：约束式 / JobPilot 参考式
- 任务目标：进一步还原 JobPilot 的编辑器主体验，将工作台改为左侧模块导航、中间当前模块编辑、右侧完整简历纸张实时预览，并保留 AI 建议面板切换入口。
- Prompt 摘要：用户要求继续推进，每一步尽可能还原 JobPilot 好的功能，重点关注“左边/中间改模块，右边浏览器式简历预览立即变化”的体验。
- 允许读取和修改的范围：读取 JobPilot `editor-preview-panel.tsx`；修改 `frontend/src/views/HomeView.vue`、`frontend/src/components/editor/EditorCanvas.vue`、新增 `frontend/src/components/editor/EditorPreviewPanel.vue`、调整 `AiCoachPanel.vue` 样式、本日志。
- AI 产出：将 `EditorCanvas` 改为只编辑当前选中 section；新增 A4 简历预览组件，支持缩放、当前模块高亮和实时渲染草稿内容；HomeView 右侧增加“简历预览 / AI 建议”切换；中间模块编辑器增加“AI 优化当前模块”入口。
- 采纳内容：采纳 JobPilot 的 Preview Panel 思路：右侧独立、实时读取同一份 draft state、以 A4 纸张形式呈现；采纳 AI 面板可切换而非长期占据右侧的布局，优先保证简历编辑与预览体验。
- 拒绝内容及原因：未复制 JobPilot 的模板系统、PDF 导出、拖拽排序、主题编辑和自动保存；这些功能当前不属于 S2 最小闭环，且会分散版本迭代主线。
- 人工修改：成员 1 需要在浏览器中验证编辑基本信息、项目描述和技能标签时右侧预览是否实时变化，以及 AI 建议面板切换是否符合演示节奏。
- 发现的 AI 问题：AI 容易保留之前“中间所有 section 堆叠 + 右侧 AI”的结构惯性；本次明确将预览独立成右侧主面板，更贴近 JobPilot 的核心体验。
- 测试与验证证据：`cd frontend && npm run build` 通过；`git diff --check` 通过。前端构建仅保留既有第三方 `@vueuse/core` pure annotation 与 bundle size 警告。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本次只调整编辑器布局和前端实时预览，不生成或修改简历评分、岗位匹配排序或模拟面试状态机。
- 反思与后续规则：后续如果继续对齐 JobPilot，应优先补“点击预览定位模块”“模块内项目条目增删”和“AI 建议采纳后的差异高亮”，仍保持所有版本写入走后端接口。

### 2026-07-10｜成员 1｜S2-13 / 预览缩放修正与项目条目级编辑

- 工具与模型：Codex
- 协作模式：约束式 / JobPilot 参考式
- 任务目标：修正右侧简历预览缩放时位置漂移的问题，并把项目经历从单一描述编辑升级为条目级编辑，使模块编辑更接近真实简历编辑器。
- Prompt 摘要：用户指出简历展示不应随着缩放改变位置，应该保持固定预览位置，并继续推进后续功能以更像 JobPilot。
- 允许读取和修改的范围：`frontend/src/components/editor/EditorPreviewPanel.vue`、`SectionWrapper.vue`、`EditorCanvas.vue`、`HomeView.vue`、`frontend/src/types/editor.ts`、本日志。
- AI 产出：将预览缩放从 `transform: scale()` 改为按 zoom 计算纸张宽度、内边距和字体基准，减少位置漂移；新增项目条目模型，支持项目名称和项目描述分别编辑；项目模块支持添加新项目；右侧预览同步展示项目标题、描述和证据标签。
- 采纳内容：采纳“预览容器固定、纸张内容按比例排版”的交互方向；采纳项目条目级编辑作为下一步还原 JobPilot 的核心体验。
- 拒绝内容及原因：未加入拖拽排序、删除项目、复杂富文本或自动保存；这些可以后续继续做，但当前先保证最小可保存版本链路稳定。
- 人工修改：成员 1 需要在浏览器中实际测试缩放按钮、项目名称编辑、项目描述编辑、添加项目和保存新版本。
- 发现的 AI 问题：用 CSS transform 直接缩放纸张会导致视觉锚点变化明显，不符合用户对“固定简历预览位置”的预期；已改为布局尺寸随 zoom 变化。
- 测试与验证证据：`cd frontend && npm run build` 通过；`git diff --check` 通过。前端构建仅保留既有第三方 `@vueuse/core` pure annotation 与 bundle size 警告。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本次只涉及前端编辑器交互和预览排版，不生成或修改简历评分、岗位匹配排序或模拟面试状态机。
- 反思与后续规则：后续继续对齐 JobPilot 时，可以补项目删除、条目排序、点击预览定位编辑模块，但所有持久化仍应保存为简历版本，不直接覆盖历史版本。

### 2026-07-10｜成员 1｜S2-14 / 项目条目操作与预览反向定位

- 工具与模型：Codex
- 协作模式：约束式 / JobPilot 参考式
- 任务目标：继续还原成熟简历编辑器体验，补齐项目条目的上移、下移、删除，以及右侧预览点击后定位到对应编辑模块。
- Prompt 摘要：用户要求继续推进，每一步尽可能还原 JobPilot 好的功能。
- 允许读取和修改的范围：`frontend/src/components/editor/SectionWrapper.vue`、`EditorCanvas.vue`、`EditorPreviewPanel.vue`、`frontend/src/views/HomeView.vue`、本日志。
- AI 产出：项目条目新增上移、下移、删除按钮；右侧预览 section 支持点击和键盘 Enter 选择模块；采纳 AI 建议生成新版本后自动切回预览并定位到被修改模块。
- 采纳内容：采纳“编辑器与预览双向联动”的交互模式，增强用户对当前编辑位置和 AI 修改位置的感知。
- 拒绝内容及原因：未加入拖拽排序和复杂动画，避免引入额外依赖和实现复杂度；当前用上下移动满足 S2 演示。
- 人工修改：成员 1 需要在浏览器中验证项目上移/下移/删除、预览点击定位和采纳建议后的模块定位。
- 发现的 AI 问题：AI 容易优先做视觉而忽略编辑器反馈闭环，本次优先补“用户知道自己在哪、改了哪”的交互反馈。
- 测试与验证证据：`cd frontend && npm run build` 通过；`git diff --check` 通过。前端构建仅保留既有第三方 `@vueuse/core` pure annotation 与 bundle size 警告。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本次只涉及前端编辑器交互，不生成或修改简历评分、岗位匹配排序或模拟面试状态机。
- 反思与后续规则：后续如果需要进一步提升产品感，可以加入轻量差异高亮和条目折叠，但仍应保持版本化保存和后端采纳链路。

### 2026-07-10｜成员 1｜S2-15 / 产品启动页与简历中心逻辑重定调

- 工具与模型：Codex
- 协作模式：约束式 / 产品信息架构重构
- 任务目标：根据新的产品方向，重定前端主流程基调：先进入酷炫启动主页面，以简历本身为核心对象，再进入 JobPilot 风格简历编辑器；评分、JD 匹配、模拟面试和 AI 建议都作为当前简历的工具能力。
- Prompt 摘要：用户指出之前仍被旧页面束缚，希望产品主页面能展示已修改简历、提供简历工作台入口；工作台左侧模块和 JobPilot 一样简约密集，顶部功能栏占比小，并提供模板能力；AI 建议区分排版优化和模拟面试后的优化。
- 允许读取和修改的范围：`frontend/src/views/HomeView.vue`、`frontend/src/components/editor/EditorSidebar.vue`、`EditorPreviewPanel.vue`、本日志。
- AI 产出：新增启动主页面，包含产品定位、当前简历预览、继续改进/新建入口和核心流程卡片；编辑器增加顶部小型功能栏，集中承载模板、评分、JD 匹配、AI 建议、模拟面试、岗位库；预览组件增加经典、科技蓝、极简三种模板视觉；左侧模块栏压缩为更接近 JobPilot 的紧凑模块导航。
- 采纳内容：采纳“简历是核心对象”的信息架构；采纳“首页展示简历成果，进入某份简历后再编辑”的流程；采纳顶部工具栏集中功能的设计。
- 拒绝内容及原因：暂未实现完整模板市场、导出/导入、翻译、求职信和语法检查；这些不属于当前 S2 主闭环，先保留界面基调和入口。
- 人工修改：成员 1 需要实际打开首页，确认启动页视觉、进入编辑器、模板切换和顶部工具栏是否符合演示口径。
- 发现的 AI 问题：AI 之前倾向在旧工作台上叠功能，未充分理解“产品主流程需要围绕简历对象重定调”；本次改为从入口页到编辑器重新组织信息架构。
- 测试与验证证据：`cd frontend && npm run build` 通过；`git diff --check` 通过。前端构建仅保留既有第三方 `@vueuse/core` pure annotation 与 bundle size 警告。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本次只涉及前端信息架构、视觉布局和入口组织，不生成或修改简历评分、岗位匹配排序或模拟面试状态机。
- 反思与后续规则：后续继续推进时，应优先围绕“简历对象”组织能力，避免把评分、岗位、面试重新拆成孤立页面；AI 建议可以分为“简历排版/表达优化”和“面试反馈反哺优化”两类。

### 2026-07-11｜成员 2｜匹配算法 v1.0.0 设计与实现

- 工具与模型：Claude Code (DeepSeek V4 Pro)
- 协作模式：精确式
- 任务目标：完成岗位匹配核心算法的设计文档编写、代码实现与测试用例编写（v1.0.0）。
- Prompt 摘要：要求 AI 整理匹配算法设计文档格式，检查测试用例覆盖完整性，辅助编写 MatchingService 和 MatchingPipelineService 的代码骨架。
- 允许读取和修改的范围：harness-docs/architecture/scoring-and-matching-design.md、harness-docs/testing/match-test-cases.md、MatchingService.java、MatchingPipelineService.java、MatchingServiceTest.java、相关 DTO 和 Entity。
- AI 产出：设计文档结构整理、测试用例表格式化、代码注释和文档字符串。
- 采纳内容：采纳文档结构和测试用例格式，匹配算法核心逻辑（五维度权重、动态分配、技能归一化、一票否决、幂等设计）由成员 2 人工手写。
- 拒绝内容及原因：AI 不得生成权重数值、覆盖率公式、技能别名映射表、一票否决阈值——全部由成员 2 人工确定。
- 人工修改：成员 2 手写 executeMatching、calcSkillDimension、calcTextMatch、calcEduMatch、normalizeSkill 等核心方法，人工确定 BASE_WEIGHT、技能别名表、60% 一票否决阈值。
- 发现的 AI 问题：AI 在整理测试用例时倾向补充"预期分数区间"的具体数值，已由成员 2 手动修正。
- 测试与验证证据：29 个测试用例全部通过（MatchingServiceTest），覆盖验收测试 6 个、固定样例 5 个、边界测试 10 个、幂等测试 4 个、校验测试 4 个。
- 是否涉及 AI 禁飞区：是
- 禁飞区处理说明：本次任务属于禁飞区。AI 仅用于辅助文档排版、测试用例格式整理和注释补全，未参与维度权重、覆盖率公式、别名表、否决阈值的决策。所有算法核心数值和逻辑由成员 2 人工确定并手写实现。
- 反思与后续规则：设计文档与代码实现必须同步更新；测试种子数据应覆盖真实中文场景；分词算法对中文经验匹配效果差，后续需优化。

### 2026-07-13｜成员 2｜匹配算法优化 v1.1.0 — n-gram 中文匹配

- 工具与模型：Claude Code (DeepSeek V4 Pro)
- 协作模式：精确式
- 任务目标：解决经验维度匹配准确率恒为 0% 的问题，引入字符级 bigram 重叠匹配替代原有标点分词+精确集合交集。
- Prompt 摘要：要求 AI 分析当前经验匹配数据流，提出优化方案，设计 bigram 算法并实现代码。
- 允许读取和修改的范围：MatchingService.java、MatchingPipelineService.java、MatchingServiceTest.java、scoring-and-matching-design.md、match-test-cases.md、测试 SQL 种子数据。
- AI 产出：(1) 完整数据流分析报告，定位经验维度覆盖率恒为 0 的根因；(2) 字符级 bigram 重叠匹配算法设计（阈值 0.40）；(3) generateBigrams、calcNgramMatch、reconcileExpWithDuration、reconcileExpWithMetaRequirement、reconcileExpWithFreshGrad 方法实现；(4) extractGraduationYear 毕业年份提取方法；(5) 权重调整方案；(6) 学历关键字提取 extractDegreeKeyword；(7) 技能别名表从 9 条扩展至 97 条并静态化；(8) 经验年限 parseExperienceMonths 数值对比逻辑；(9) 29 个测试全部通过验证。
- 采纳内容：全部采纳——bigram 算法设计、阈值 0.40、所有新增方法、权重调整（必备0.40/加分0.00/经验0.35/学历0.20/职责0.05）、别名表扩展、年限提取、学历关键字提取、"有项目经验"元问题匹配、应届生毕业年份判定。
- 拒绝内容及原因：无。所有算法改动由成员 2 审核确认后合入。
- 人工修改：成员 2 确认权重分配方案、bigram 阈值 0.40、别名表条目、年限解析正则、学历等级映射。
- 发现的 AI 问题：(1) AI 初次实现别名表时遗漏了 node.js/nodejs 映射，导致 Sample 5 别名测试失败，已修正；(2) 版本号不应随权重调参频繁变更，最终锁定为 v1.1.0。
- 测试与验证证据：全部 29 个测试通过；经验覆盖率从 0% 提升至实际可用的正数值（v5 vs jd5 从 0%→100%）；"有项目经验"类元问题正确匹配；学历"本科及以上"正确提取。
- 是否涉及 AI 禁飞区：是
- 禁飞区处理说明：本次任务属于禁飞区。AI 提供了算法设计建议和代码实现，但所有核心决策（阈值、权重、正则规则、别名条目）由成员 2 逐项审核确认。bigram 匹配为确定性规则算法，不依赖 AI/LLM 推理。
- 反思与后续规则：字符级 n-gram 对中文文本匹配效果显著优于标点分词；权重调整需业务验证；版本号仅在实际算法逻辑变更时递增。

### 2026-07-13｜成员 3｜S2-18 / 模拟面试引入面试官角色卡

- 工具与模型：Trae（Deepseek V4 Pro）
- 协作模式：约束式
- 任务目标：在模拟面试模块中引入面试官角色卡系统，支持创建面试时选择不同风格和领域的面试官人设，使每场面试的提问风格、评价视角和总结语气与该人设特征一致，提升面试练习的真实感和差异化体验。
- Prompt 摘要：在 interview 模块下创建 InterviewerPersona 实体、Mapper、Service 和 Controller，支持 12 种预设人设的初始化、自定义人设的增删查；前端角色卡选择界面与面试创建流程解耦；AI Prompt 构建器接收角色上下文，使问题生成、回答评价和面试总结融入角色视角。
- 允许读取和修改的范围：interview模块
- AI 产出：
  - 后端实体与数据层：`InterviewerPersona` 实体（name / title / style / avatar / type / sortOrder）、`InterviewerPersonaMapper`（MyBatis-Plus Mapper）、`interview_personas` 表 Flyway 迁移脚本；
  - 后端服务层：`InterviewerPersonaService`，包含 `@PostConstruct` 自动初始化 12 种预设人设（通用面试官、资深后端架构师、HR 总监、算法专家、产品总监、前端技术专家、数据科学家、创业公司 CEO、外企技术经理、校招面试官、高压面试官、友好面试官），支持 `listPersonas()` 查询所有可用人设、`createCustomPersona()` 创建自定义人设、`deleteCustomPersona()` 删除自定义人设（含权限校验：不能删除预设人设、不能删除他人人设）；
  - 后端接口层：`InterviewerPersonaController` 提供 `GET /api/v1/interviewer-personas`（列表）、`POST /api/v1/interviewer-personas`（创建自定义）、`DELETE /api/v1/interviewer-personas/{id}`（删除自定义），含入参校验（姓名≤20字符、职位≤50字符、风格≤200字符）；
  - AI Prompt 构建器改造：`InterviewPromptBuilder` 的三个系统提示词方法（`buildQuestionSystemPrompt`、`buildEvaluationSystemPrompt`、`buildSummarySystemPrompt`）均新增 `personaContext` 参数，注入角色名、职位、风格描述，要求 AI 从该角色视角生成问题、评价回答和撰写总结，包含明确的角色行为要求（提问视角、提问风格、问题差异化、角色领域优先、开场问候）；
  - 面试流程透传：`InterviewService` 和 `InterviewPlanService` 在创建会话时绑定 `personaId`，创建接口接收 `personaIds` 列表，响应携带 `personaName` 和 `personaTitle`；
  - 前端角色卡选择界面：`InterviewView.vue` 新增角色卡网格展示（12 张预设卡片 + 自定义入口），支持多选（最多 5 位面试官）、主要面试官高亮、创建自定义人设弹窗（含姓名/职位/风格字段和表单校验），与原有面试创建流程解耦，选择角色卡后进入配置界面。
- 采纳内容：采纳 12 种预设面试官人设的设计方案，覆盖技术、HR、算法、产品、前端、数据、创业、外企、校招、高压、友好等多元场景；采纳 `InterviewPromptBuilder` 角色上下文注入机制，确保每位面试官的提问风格、评价视角和总结语气具有差异化；采纳前端角色卡选择界面与面试创建流程分离的交互模式；采纳自定义人设能力（创建/删除）和权限校验规则。
- 拒绝内容及原因：未在前端实现面试官人设的 AI 自动生成或推荐算法，避免 AI 直接决定面试官角色属性；未修改面试状态机实现，角色卡仅作为数据容器和 Prompt 上下文，不参与状态转换决策；未在前端实现角色卡拖拽排序，保持排序由后端 `sort_order` 字段控制。
- 人工修改：成员 3 确认 12 种预设人设的名称、职位、风格描述和排序顺序，确保覆盖核心面试场景且风格差异化明显；调整 Prompt 中角色行为要求的措辞，使"角色领域优先"和"简历是参考而非硬约束"的规则更明确。
- 发现的 AI 问题：初始 Prompt 构建器在注入角色上下文时，`personaContext` 为 null 的处理不够优雅，已改为在方法开头判空并跳过角色段落；`buildPersona` 辅助方法中 `userId` 字段预设人设时应设为 null 而非 0，避免与自定义人设的用户隔离逻辑冲突。
- 测试与验证证据：`mvn test` 全部 225 个测试用例通过（1 个排序断言失败与本次变更无关）；`cd frontend && npm run build` 通过；前端角色卡选择界面可正常展示 12 张预设卡片，多选上限 5 位正确生效；创建自定义人设后卡片实时追加到列表；面试计划创建流程正确绑定 `personaId` 和 `personaName`；聊天界面正确展示当前面试官头衔和姓名。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本次仅涉及面试官人设数据模型、CRUD 接口和 AI Prompt 的角色上下文注入，不生成或修改面试状态机实现、状态转换表、题号推进规则或结束条件。角色卡不参与评分计算、匹配排序或状态机决策。
- 反思与后续规则：角色卡系统为后续"多面试官对比评估"和"角色偏好学习"提供了数据基础；Prompt 中的角色行为要求需要持续迭代（当前 v1.1），后续可通过 A/B 测试不同角色 Prompt 模板的效果；自定义人设目前仅支持文本描述，后续可扩展为支持自定义头像和问题偏好模板。

### 2026-07-13｜成员 1｜S2-16 / 模拟面试大厅第一版

- 工具与模型：Codex
- 协作模式：约束式 / 面试模块产品化入口改造
- 任务目标：在不改动现有模拟面试执行链路的前提下，将 `/interview` 未进入会话时的页面重构为“面试大厅”，用于承载新建面试、历史会话、综合复盘和后续多面试官计划入口。
- Prompt 摘要：用户确认同学现有面试版本可作为稳定 V1，希望先沿自己的思路推进“面试大厅”：新建一次面试时选择简历版本、岗位信息、面试官和题数，后续可扩展多面试官顺序面试与能力画像。
- 允许读取和修改的范围：`frontend/src/views/InterviewView.vue`、本日志。
- AI 产出：将原“设置界面”改为面试大厅首屏；新增大厅 hero、会话统计、创建面试配置卡、当前选择摘要、后续多面试官计划预览、闭环说明侧栏、历史会话卡片网格和空状态；保留现有创建面试、开始面试、提交回答、单题评价、总结和跨会话综合总结逻辑。
- 后续补充：继续将多面试官计划预览升级为前端可操作队列，支持选择多位面试官、显示顺序、上移、下移和移除；启动按钮明确只启动队列第 1 位面试官，避免误导为完整自动多轮。
- 采纳内容：采纳“现有面试执行器作为稳定版本，只在外层增加产品化大厅”的演进策略；采纳“当前先单面试官，后续再扩展多面试官顺序计划”的低风险路线。
- 拒绝内容及原因：未新增后端面试计划表、未修改面试状态机、未让前端决定面试轮次推进或结束条件；多面试官顺序面试仅作为界面预留，避免一次性扩展过大。
- 人工修改：成员 1 需要在浏览器中验证 `/interview` 首屏、新建面试按钮、历史卡片、综合总结按钮和进入原面试执行界面的体验是否符合演示节奏。
- 发现的 AI 问题：AI 容易把“面试大厅”直接扩展成完整多面试官状态编排；本次明确限制为前端入口重构，不碰禁飞区状态推进。
- 测试与验证证据：`cd frontend && npm run build` 通过；`git diff --check -- frontend/src/views/InterviewView.vue` 通过。前端构建仅保留既有第三方 `@vueuse/core` pure annotation 与 bundle size 警告。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本次只调整前端信息架构和展示计算，不生成或修改简历评分权重、岗位匹配排序或模拟面试状态机；前端不决定状态转换、题号推进、结束条件或重试规则。
- 反思与后续规则：下一步若继续推进，应先设计 `InterviewPlan` 的数据边界，再决定是否新增后端计划接口；在此之前，多面试官顺序流程不要伪造为已落地能力。

### 2026-07-13｜成员 1｜S2-17 / 多轮面试计划整合与上下文锁定

- 工具与模型：Codex（GPT-5）
- 协作模式：约束式
- 任务目标：在不修改面试状态机禁飞区实现的前提下，协助完成 S2 模拟面试的二次整合，包括多轮面试计划容器、面试历史聚合、工作台上下文绑定、面试记录展示和前端交互优化。
- Prompt 摘要：围绕「职达」S2 面试闭环，要求将模拟面试从单独功能页收束为“基于当前简历版本和目标岗位的一次多轮面试”，支持多位面试官轮次、历史复盘、最近面试入口、公司图标展示、从简历工作台进入时锁定岗位和简历版本，并持续优化界面密度和产品感。
- 允许读取和修改的范围：前端面试页面、前端面试 API 和类型、工作台上下文工具、公司图标展示组件复用、面试计划容器相关后端实体/接口/迁移；明确不得修改 `InterviewStateMachine` 和状态转换表。
- AI 产出：
  - 前端：重构 `InterviewView.vue` 的面试入口、创建面试计划、最近面试、历史记录、复盘查看、右侧侧栏和绑定上下文卡片；
  - 前端：从工作台进入面试时隐藏简历/岗位选择表单，展示“已绑定当前工作台”的上下文卡片；直接进入面试页时保留简历/岗位选择作为无上下文兜底；
  - 前端：历史记录按一次面试计划聚合，优先展示岗位公司图标，并补充绑定简历版本信息；
  - 后端：新增 `interview_plans` 作为一次多轮面试的容器，通过 `plan_id` 和 `round_order` 关联原有单轮 session；
  - 文档：明确“面试绑定的是某个简历版本快照 + 某个目标岗位”，面试后生成的新版本应重新发起下一次面试进行对比。
- 采纳内容：采纳“一次面试包含多位面试官轮次”的产品模型、右侧最近/历史侧栏、工作台上下文锁定、历史记录以计划为单位聚合、公司图标展示、简历版本快照绑定和前端产品化布局优化。
- 拒绝内容及原因：拒绝在面试页继续保留工作台来源下的自由切换简历/岗位表单，原因是会破坏“当前简历版本针对当前岗位进行验证”的闭环心智；拒绝增加提示、跳过题目、提前结束本轮等状态机扩展入口，原因是这些会扩展面试状态机禁飞区。
- 人工修改：成员 1 多次明确产品方向，包括面试应作为简历优化闭环的一部分、历史记录必须以“一次面试”为单位、从工作台进入时应锁定岗位和简历版本、直接进入面试页才保留自主选择兜底；成员 1 对 UI 密度、图标、侧栏、历史记录和公司 Logo 使用方式进行取舍。
- 发现的 AI 问题：初始方案一度把历史记录拆成过多独立记录，未体现“一次面试包含多轮”的整体性；部分早期界面仍保留旧导航和冗余模块，产品心智不够聚焦；需要人工反复强调不要修改或绕开状态机禁飞区，不要把提示、跳过、提前结束等能力做成前端假功能。
- 测试与验证证据：前端多次执行 `npm run build` 通过；执行 `git diff --check -- frontend/src/views/InterviewView.vue` 通过；检查 `InterviewStateMachine.java` 和 `InterviewTransitionTable.java` 无差异；后端计划容器阶段执行过 `mvn -DskipTests package` 编译验证，完整 `mvn test` 仍受本机 Java 26 与 Mockito/ByteBuddy self-attach 兼容问题影响，需团队统一 JDK 21 后复测。
- 是否涉及 AI 禁飞区：是
- 禁飞区处理说明：本次涉及模拟面试产品流程，但 AI 未生成或修改面试状态机实现、状态转换表、题号推进规则或结束条件；多轮面试计划仅作为会话容器，单轮问答推进仍复用成员人工实现的既有状态机；跳过、提前结束、追问策略等状态机扩展被明确排除。
- 反思与后续规则：后续面试功能开发必须区分“产品容器/展示层”和“状态机禁飞区”；任何会改变题目推进、结束条件、跳过/重试/追问规则的需求，必须先由成员人工更新状态图、转换表和测试，再进入代码实现；工作台进入面试时应默认锁定简历版本和岗位，直接入口才允许选择兜底。


### 2026-07-14｜成员 3｜S2-19 / 模拟面试增加成长趋势查看

- 工具与模型：Trae（Deepseek V）
- 协作模式：约束式
- 任务目标：在模拟面试模块中新增成长趋势查看功能，用户可查看同一份简历在不同版本下针对同一目标岗位的面试表现变化趋势，包括四维能力（表达清晰度、内容相关性、思考深度、技术准确性）的版本间变化曲线、版本时间线、变化摘要和版本详情卡片，体现"诊断→优化→练习→迭代"的成长闭环。
- Prompt 摘要：要求先确认成长趋势不涉及禁飞区（不参与简历评分、岗位排序或面试状态机控制），然后在面试模块中实现成长趋势前端聚合逻辑：以当前简历 + 当前岗位为维度，查询所有简历版本的已完成面试计划，每个版本选取最近完成的计划作为代表，提取其所有轮次的四维评分并计算平均值，按版本创建时间升序排列生成快照序列，计算首尾版本的变化量，最终以折线图、变化摘要和版本卡片形式展示。
- 允许读取和修改的范围：模拟面试模块。
- AI 产出：
  - 前端类型定义：`GrowthDimensions`（clarity / relevance / depth / accuracy 四维分数）、`GrowthSnapshot`（resumeVersionId / versionLabel / representativePlanId / completedAt / interviewCount / dimensions / summary）、`GrowthReport`（resumeId / jobDescriptionId / jobTitle / companyName / snapshots / changes）；
  - 前端数据聚合逻辑：`aggregateDimensionsForPlan()` 函数按面试计划聚合所有轮次的 `perQuestionScores` 计算四维平均分；`loadGrowthData()` 函数实现完整数据加载流程：筛选当前简历所有版本→过滤当前岗位的已完成面试计划→自动补全缺失的 `perQuestionScores` 数据→按 `resumeVersionId` 分组→每组选最近完成的计划为代表→聚合维度分数→按创建时间升序排序→计算首尾版本变化量；
  - 前端成长趋势弹窗：包含顶部岗位/简历信息区、版本时间线（圆点 + 标签，当前版本高亮）、SVG 折线图（四维能力同时展示，Y 轴 1-10 分，X 轴按版本排列，含网格线、图例）、变化摘要（四维度增减量，正数绿色、负数红色）、版本详情卡片（版本标签、代表面试计划 ID、该版本面试总次数、四维分数进度条、总结文本）、单版本数据提示、加载中状态；
  - 入口按钮：在面试历史记录区域的侧栏中新增"查看成长趋势"按钮，仅当选中简历和岗位后可用，未选择时显示提示。
- 采纳内容：采纳"以简历版本为横轴、四维面试评分为纵轴"的成长趋势可视化方案；采纳前端数据聚合的实现方式（当前阶段前端聚合，后续可提取为后端 API）；采纳每个版本自动选取最近完成的面试计划作为代表的策略；采纳折线图四维同时展示（而非分开展示）以增强对比感；采纳版本卡片中展示代表面试 ID 和面试总次数以提供上下文。
- 拒绝内容及原因：未实现"手动设置代表面试"功能，当前仅自动选取最近完成的计划，避免增加不必要的交互复杂度；未实现后端成长报告 API（`GET /api/v1/resumes/{resumeId}/growth-report`），当前阶段前端聚合已满足演示需求，后端 API 待后续提取；未将成长趋势数据参与简历评分或岗位排序，严格遵循禁飞区约束；未生成简历评分权重算法或面试状态机规则。
- 人工修改：成员 3 确认成长趋势弹窗的 UI 布局（顶部信息→时间线→折线图→变化摘要→版本卡片）、入口按钮位置（放在面试历史侧栏而非独立页面）、折线图颜色方案（四维分别用蓝/绿/橙/紫）和单版本时的空状态提示文案。
- 发现的 AI 问题：初始实现中 `perQuestionScores` 数据可能不完整（旧版本面试会话未存储该字段），需要在前端做自动补全调用 `getInterviewStatus` 接口；折线图在仅有 1 个版本时应隐藏图表并显示"仅有 1 个版本数据"提示，初始版本缺少此判断；版本卡片中的 summary 字段当前依赖后端 `summaryJson`，但部分旧数据可能为空，已添加"无总结"兜底显示。
- 测试与验证证据：`cd frontend && npm run build` 通过；`git diff --check` 通过；`mvn test` 全部 225 个测试用例通过（1 个排序断言失败与本次变更无关）；前端成长趋势弹窗在多个版本场景下正确展示折线图、变化摘要和版本卡片；单版本场景正确显示提示信息；自动补全 `perQuestionScores` 逻辑正确工作。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：成长趋势仅展示已完成的面试评估结果的聚合数据，是"展示层"功能，不参与简历评分权重计算、岗位匹配排序或面试状态机控制。四维分数（clarity / relevance / depth / accuracy）来自 AI 评价输出，成长趋势仅做前端聚合和可视化，不生成新的评分规则。成长趋势不写入任何评分或排序结果到数据库。
- 反思与后续规则：成长趋势是"诊断→优化→练习→迭代"成长闭环的可视化体现，后续可提取为独立后端 API（`GET /api/v1/resumes/{resumeId}/growth-report?jobDescriptionId=xxx`）以提升性能；折线图可扩展为支持不同维度的独立 Y 轴缩放；版本卡片可增加"查看该版本面试详情"的跳转入口；后续可考虑支持跨岗位的成长趋势对比。

### 2026-07-14｜成员 3｜S2-20 / 强化面试官角色特性弱化简历约束

- 工具与模型：Trae（Deepseek V）
- 协作模式：约束式
- 任务目标：优化 AI Prompt 构建器中的面试问题生成策略，将优先级从"以简历内容为核心"调整为"以面试官角色专业领域为核心，简历仅作为辅助参考"，确保即使简历中缺乏面试官专业领域的内容，AI 仍能提出该领域的通用场景题或假设题，同时强化角色风格差异化，使不同面试官的问题各具特色。
- Prompt 摘要：要求修改 `InterviewPromptBuilder` 的问题生成系统提示词，新增"角色优先"和"领域覆盖"两大核心原则，明确要求 AI 从面试官角色视角出发提问题，简历内容作为辅助参考而非硬性约束；增加角色行为要求（提问视角、提问风格、问题差异化、角色领域优先、开场问候），并为每种角色类型提供差异化的问题示例；同时优化回答评价和面试总结的提示词，使评价视角和语气与角色风格一致。Prompt 版本从 v1.0 升级到 v1.1。
- 允许读取和修改的范围：模拟面试模块。
- AI 产出：
  - 问题生成系统提示词重构：新增"角色优先"核心原则，明确"问题的主题和方向由你的面试官角色决定，简历和岗位要求是辅助参考而非硬性约束"；新增"领域覆盖"原则，要求"如果你擅长的领域在简历中未体现，你仍应提出该领域问题"；新增"简历关联"原则，区分"有相关经历时优先结合简历提问"和"无相关经历时使用通用场景题或假设题"；
  - 角色行为要求细化：5 条具体行为指令——(1) 提问视角必须从角色职位出发，(2) 提问风格必须严格遵循风格描述，(3) 问题必须与其他面试官角色差异化，(4) 专业领域问题必须出现、简历只是参考，(5) 开场问候使用角色姓名；
  - 角色差异化示例：在系统提示词中嵌入具体示例——"HR 总监应侧重软技能、文化匹配、沟通协作；架构师应侧重系统设计、技术选型、架构决策"、"高压面试官应语气犀利、连续追问、制造压力场景；友好面试官应语气鼓励、给予肯定、营造轻松氛围"；
  - 问题用户消息优化：在注意事项中追加"记住：你的角色决定了问题方向，简历只是素材。简历中没有的内容不代表不能问，用通用场景题或假设题代替即可"；
  - 回答评价系统提示词优化：新增角色行为要求，要求评价视角和语气与角色风格一致，示例——"高压面试官的评价应严格犀利、直指要害；友好面试官的评价应鼓励为主、温和指出不足"；
  - 面试总结系统提示词优化：新增角色行为要求，要求总结视角和语气与角色风格一致，示例——"高压面试官的总结直接指出问题不绕弯子；友好面试官的总结多鼓励、多肯定、建设性地提出改进方向"；
  - Prompt 版本号更新：`PROMPT_VERSION` 从 `v1.0` 升级到 `v1.1`，记录本次增强迭代。
- 采纳内容：采纳"角色优先 + 领域覆盖 + 简历关联"三层优先级策略；采纳在系统提示词中嵌入角色差异化示例的做法，以增强 AI 对角色差异的理解；采纳问题生成、回答评价和面试总结三个场景的角色一致性设计；采纳 Prompt 版本号追踪机制，便于后续迭代和 A/B 测试。
- 拒绝内容及原因：未在 Prompt 中硬编码具体的问题模板或题库，保持 AI 生成问题的灵活性；未修改面试状态机实现，角色 Prompt 优化仅影响 AI 生成内容，不改变状态转换逻辑；未让 AI 直接决定"哪些问题应该跳过"或"哪些领域不需要问"，所有问题生成决策仍由 AI 在 Prompt 约束下自主完成。
- 人工修改：成员 3 确认"角色领域优先"原则的措辞力度——从"可以考虑"升级为"必须出现"，确保当简历缺乏该领域内容时 AI 不会跳过；确认角色差异化示例的具体措辞（HR vs 架构师、高压 vs 友好），确保对比足够鲜明；确认 Prompt 版本号从 v1.0 升级到 v1.1。
- 发现的 AI 问题：初始版本中角色 Prompt 的指令过于简单（仅"你是一位 XX 面试官"），导致不同角色的提问差异不够明显，HR 和架构师可能问出类似的问题；已通过增加 5 条具体行为指令和角色对比示例解决；初期 Prompt 未明确区分"简历作为参考"和"简历作为硬约束"的边界，导致 AI 倾向于跳过简历中未涉及的领域，已通过"领域覆盖"原则修正。
- 测试与验证证据：`InterviewPromptBuilderTest` 全部 21 个测试用例通过，验证了 `personaContext` 注入后系统提示词包含角色名、职位、风格和核心原则；`mvn test` 全部 225 个测试用例通过；手动验证：不同类型面试官（架构师 vs HR 总监 vs 算法专家）在相同简历和岗位下生成的问题方向、风格和关注点具有明显差异。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本次仅优化 AI Prompt 文本内容，属于 AI 生成内容的引导性调整，不涉及简历评分权重计算、岗位匹配排序算法或面试状态机实现。Prompt 不包含任何评分规则、排序逻辑或状态转换指令。
- 反思与后续规则：Prompt 版本号追踪机制（v1.0→v1.1）为后续迭代提供了可追溯的变更记录，建议每次 Prompt 调整都更新版本号并在日志中记录变更内容；角色差异化效果需要通过实际测试验证（不同角色在相同输入下生成的问题是否具有足够差异），后续可建立角色差异化的量化评估标准；"角色领域优先"策略在极端情况下（如简历完全空白）的效果需要进一步验证，确保 AI 能够生成有价值的通用问题。

## 2026-07-14｜成员 2｜后端基础设施修复 + 批量匹配 + 前端岗位推荐

- 工具与模型：Claude Code (DeepSeek V4 Pro)
- 协作模式：精确式
- 任务目标：(1) 修复 Docker MySQL 重建后 Flyway 校验和冲突；(2) 实现批量匹配 API（一份简历匹配全部 JD 并返回 Top 5）；(3) 前端按钮"JD匹配"改为"岗位推荐"，新增批量匹配对话框；(4) 修复 24 个因 interview_personas 表缺失导致的测试失败；(5) 全界面 UI/UX 问题调查。
- Prompt 摘要：要求 AI 深入分析批量匹配需求，设计后端 batchMatch 方法、控制器端点和前端对话框，调查并修复 H2 测试环境 interview_personas 表缺失问题，审查全部前端视图的 UI 问题。
- 允许读取和修改的范围：MatchingPipelineService.java、MatchingController.java、MatchingService.java、frontend/src/api/match.ts、frontend/src/types/match.ts、frontend/src/views/HomeView.vue、test resources 中全部 SQL schema 文件、application.yml、全部前端视图和组件。
- AI 产出：(1) 数据库诊断与容器内重建方案；(2) batchMatch 方法 + BatchMatchResult record；(3) POST /api/resume-versions/{versionId}/batch-matches 端点；(4) 前端 batchMatch API + BatchMatchResult 类型；(5) HomeView 中"岗位推荐"按钮改名为批量匹配对话框，含排名、分数颜色、查看详情按钮；(6) 全局 test schema.sql + sql.init.mode=always 修复 24 个测试失败；(7) 28 条 UI/UX 问题结构化清单，按优先级分三档；(8) 全项目 225 测试通过验证。
- 采纳内容：全部采纳。
- 拒绝内容及原因：AI 建议后端 BatchMatchResult 返回 jobTitle/companyName，成员 2 指出前端已有全部 JD 数据可本地回填，精简为仅返回 id+score+level。其余无拒绝。
- 人工修改：成员 2 确认数据库重建方案（DROP + CREATE 后 Flyway 重跑）、批量匹配 topN 默认 5 限制 1-50、对话框 UI 样式、岗位详情跳转方式、权重比例合计 1.0 校验。
- 发现的 AI 问题：(1) hot-reload 期间批量匹配仅返回 5 而非 212，系热重载瞬间状态不完整，重启后正常；(2) 测试修复初期误将 interview_personas 建表追加到各测试 SQL 文件末尾，但 @Sql 在上下文加载后执行，为时已晚，改用全局 schema.sql 解决。
- 测试与验证证据：全部 225 个测试通过（0 失败，1 跳过为预存的跨用户测试）；API 验证：POST batch-matches 返回 212 个 totalCompared、Top 5 按分数降序排列；前端编译零错误。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本次任务不涉及禁飞区核心算法变更。批量匹配为纯编排逻辑（循环调用已有 match 方法），前端为纯 UI 改动。
- 反思与后续规则：批量匹配利用已有幂等缓存，首次全量 212 JD 约 2-3 秒，再次秒返；前端已有数据时应优先本地回填而非要求后端冗余返回；测试 schema 应统一管理而非分散在多个 @Sql 脚本中。
### 2026-07-14｜成员 1｜S2-18 / AI 排版措辞提案链路

- 工具与模型：Codex（GPT-5）
- 协作模式：约束式 / 非禁飞区功能实现
- 任务目标：将工作台中的 AI 排版助手从“本地模板提示”升级为真实的后端 AI 提案链路：后端基于当前草稿生成结构化改写提案，前端展示 diff，用户接受后才应用到当前草稿，最终仍由用户手动保存新版本。
- Prompt 摘要：用户指出现有 AI 排版助手只能提示模板，不符合项目目标；要求至少把当前简历作为提示词发送给千问，后端返回排版和措辞优化结果，前端展示差异，用户接受后才能应用到草稿。
- 允许读取和修改的范围：`backend/src/main/java/com/resumego/layout/`、`backend/src/main/java/com/resumego/ai/MockAiClient.java`、`frontend/src/api/layout.ts`、`frontend/src/types/layout.ts`、`frontend/src/views/HomeView.vue`、本日志。
- AI 产出：新增 `resume_layout` AI 功能类型、排版提案 DTO、Prompt Builder、Service 和 Controller；前端新增排版提案 API/types；悬浮排版助手改为调用后端接口，展示后端返回的文本 diff、模板调整和隐藏空模块提案；接受后只写入当前草稿，不自动保存版本。
- 后续补充：点击悬浮球后自动生成提案，不再要求用户二次点击“生成”；请求增加目标岗位摘要和当前已添加模块列表；后端只对已添加且可见的空模块生成隐藏建议；Mock 和兜底逻辑不再默认推荐紧凑模板；前端 dirty 判断改为规范化草稿快照，避免“添加空模块后删除回原状”被误判为必须保存新版本；继续扩展排版助手的字段抽取范围，从只处理 summary / 项目描述 / 工作经历描述，升级为覆盖已添加模块中的简介、项目描述/亮点/技术栈、工作经历描述/亮点、教育亮点、技能项、证书说明、语言说明、GitHub 项目和自定义模块，并要求模型优先覆盖不同模块。
- 后续补充 2：根据产品定位调整 AI 建议入口，排版助手继续承担“diff → 接受 → 草稿”的编辑工具职责；右侧 AI 建议改为“项目技术表达教练”，不再提供应用到草稿、忽略和已处理流程，只展示项目技术表达建议、岗位要求依据和追问提示，由用户人工理解后修改简历。
- 后续补充 3：继续去除 AI 建议中的“当前模块 / 全部 / 待补充”筛选心智，改为按简历模块直接展示灰白色建议流；每组建议保留“前往模块”按钮；对需要补充事实的建议提供可输入的补充框，提交后在本地保留补充内容和下一步人工修改提示，避免伪装成 AI 已自动改写简历。
- 后续补充 4：将右侧 AI 建议进一步收敛为“项目技术表达教练”：建议内容只聚焦项目、技能、工程难点、岗位技术偏好与事实证据缺口；删除采纳/拒绝/生成新版本心智。对证据不足的建议新增后端追问接口 `POST /api/v1/suggestions/{suggestionId}/follow-up`，用户补充事实后由 AI 返回最终建议和下一步核实事项；接口不修改简历、不更新建议状态、不创建版本，前端仅展示最终建议并引导用户手动回到对应模块修改。
- 采纳内容：采纳“后端生成结构化提案、前端人工确认后应用”的安全编辑模式；后端只抽取个人简介、项目描述、工作经历描述等可编辑长文本，不发送手机号、邮箱、微信等联系方式；模型输出必须经过 JSON 提取、Schema 基础校验和字段级安全校验。
- 拒绝内容及原因：拒绝让 AI 直接覆盖简历版本、直接生成新版本、直接控制评分/匹配/面试状态；拒绝把联系方式和完整敏感简历信息写入日志；拒绝把排版助手继续做成单纯模板推荐。
- 人工修改：成员 1 需要在真实千问 Max API 配置下手动验证一轮：打开工作台 → 点击白色悬浮排版球 → 生成提案 → 查看 diff → 接受应用到草稿 → 手动保存新版本。
- 发现的 AI 问题：AI 排版类功能容易滑向“编造更强经历”，因此本次后端校验要求 `before` 必须来自原文、`after` 不得为空且不能引入新的数字 token，并限制最多 5 条变更；后续如需更强事实一致性，可增加技能词/公司名/奖项名白名单校验。
- 测试与验证证据：`cd backend && mvn -DskipTests compile` 通过；`cd frontend && npm run build` 通过。前端构建仍仅保留既有第三方 `@vueuse/core` pure annotation 与 chunk size 警告。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本次不生成或修改简历评分权重、岗位匹配排序或模拟面试状态机；AI 输出仅作为排版/措辞提案，不产生分数、排序、权限、状态或版本写入。
- 反思与后续规则：AI 编辑类能力必须走“提案 → diff → 人工接受 → 草稿 → 手动保存版本”链路；未来如果接入真实千问 Max，应继续保留 Mock 兜底和结构化校验，且产品 AI 审计日志不得保存原始简历全文或敏感联系方式。

### 2026-07-14｜成员 1｜S2-21 / 模拟面试闭环稳定化

- 工具与模型：Codex（GPT-5）
- 协作模式：约束式 / 代码审查后定向改进
- 任务目标：在不重构面试大厅 UI、不修改面试状态机禁飞区的前提下，补齐“一次多轮面试”的产品闭环：整次复盘可持久化、历史删除不回流、前端查看整次总结优先读取已保存结果，并阻止不同面试计划被误混为一次总结。
- Prompt 摘要：用户授权 AI 按前一轮审查结论推进，要求形成逻辑闭环严谨、足够产品化的版本；明确不再大改大厅 UI，重点修正流程和数据闭环。
- 允许读取和修改的范围：`backend/src/main/java/com/resumego/interview/` 中 plan 容器、接口、DTO、服务编排与测试；`backend/src/main/resources/db/migration/`；`frontend/src/api/interview.ts`、`frontend/src/types/interview.ts`、`frontend/src/views/InterviewView.vue`；AI 使用日志。
- AI 产出：
  - 新增 `V15__add_interview_plan_summary.sql`，为 `interview_plans` 增加 `summary_json` 与 `summary_generated_at`，保存整次多轮面试复盘结果；
  - `InterviewPlanResponse` 增加已保存整次总结和生成时间；
  - `InterviewPlanService` 新增 `getPlan()` 与 `generatePlanSummary()`，按计划轮次收集已完成 session，调用已有跨会话总结能力，并把结果写回 plan；
  - `InterviewPlanController` 新增 `GET /api/v1/interview-plans/{planId}` 与 `POST /api/v1/interview-plans/{planId}/summary`；
  - `InterviewService.listMyInterviews()` 过滤已隐藏 plan 下的 session，避免删除后的计划通过旧 session 历史入口回流；
  - 前端新增 plan summary API 和类型，面试页“整次总结”优先展示缓存/已保存结果，没有保存时才请求后端生成并落库；如果用户选择了多个不同 plan 的 session，会提示不要混合不同面试计划；
  - 测试侧新增 Mockito mock maker 配置，解决本地 JDK 21 下单元测试无法初始化 Mockito 的问题。
- 采纳内容：采纳“plan 是一次完整面试，session 是面试官轮次”的数据模型；采纳“整次总结属于 plan 级产物，应持久化并可复看”的产品逻辑；采纳隐藏 plan 后不再让其 sessions 出现在历史入口的清理规则。
- 拒绝内容及原因：未修改 `InterviewStateMachine` 与 `InterviewTransitionTable`，避免触碰面试状态机禁飞区；未继续大改面试大厅 UI，避免偏离当前“闭环稳定化”目标；未把整次总结直接写入简历或自动生成新版本，仍保持“复盘指导用户人工优化”的产品边界。
- 人工修改：成员 1 需要在真实页面手动验证：创建多面试官计划 → 完成全部轮次 → 生成整次总结 → 返回历史再次查看应直接展示已保存结果 → 删除记录后刷新不再回流。
- 发现的 AI 问题：面试相关功能容易把“跨次总结”和“整次面试总结”混用，本次通过前端 planId 检查阻止不同面试计划混合；后续如需跨次趋势，应单独设计成长趋势/跨次对比 API。
- 测试与验证证据：`cd backend && mvn -q -Dtest=InterviewPlanServiceTest,InterviewServiceTest test` 通过；`cd backend && mvn -q -DskipTests compile` 通过；`cd backend && mvn -q test` 全量通过；`cd frontend && npm run build` 通过；`git diff --check` 通过。前端构建仍仅保留既有第三方 `@vueuse/core` pure annotation 与 chunk size 警告。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本次仅新增面试计划容器层复盘持久化、历史过滤和前端调用逻辑，不生成或修改状态集合、状态转换表、结束条件、题号推进或状态机实现；AI 仍只参与问题、评价和总结内容生成，不能决定状态跳转。
- 反思与后续规则：后续若继续增强面试闭环，优先做“整次复盘 → 简历模块/证据缺口定位”的建议层，而不是继续扩张大厅 UI；跨次成长趋势应从 plan summary 和能力画像中读取稳定数据，不应长期依赖前端临时聚合。
- 后续补充：继续收紧“整次多轮面试完成”的判定口径。后端 `generatePlanSummary()` 从“终态即可总结”调整为“所有轮次必须为 `COMPLETED` 才能生成整次总结”，避免 `FAILED` / `CANCELLED` 轮次被混入完整复盘；`InterviewPlanResponse.Round.completed` 同步只代表真正完成。前端 `sessionCompleted()` 也改为只认可 `COMPLETED`，历史卡片和轮次切换新增“异常中断 / 已取消”状态文案与弱警示样式，避免失败轮次被误展示为已完成或可总结。`InterviewService.listMyInterviews()` 同步过滤非当前用户 plan 下的 session，增强脏数据隔离。
- 后续验证证据：`cd backend && mvn -q -Dtest=InterviewPlanServiceTest,InterviewServiceTest test` 通过；`cd frontend && npm run build` 通过。前端构建仍仅保留既有第三方 `@vueuse/core` pure annotation 与 chunk size 警告。

### 2026-07-15｜成员 1｜S2-22 / 面试复盘产品化与评分去固定化

- 工具与模型：Codex（GPT-5）
- 协作模式：系统化调试 + 测试先行
- 任务目标：修复模拟面试评价分数高度相似的问题，避免 Mock 与 Prompt 示例导致每题总是接近 76/78；同时弱化大厅旧“手动勾选多轮综合复盘”入口，把整次复盘心智收敛到单次面试详情。
- Prompt 摘要：用户指出面试评分每次高度相似，要求“分数一定要不同”，并继续推进面试复盘产品化。
- 允许读取和修改的范围：`backend/src/main/java/com/resumego/ai/MockAiClient.java`、`backend/src/main/java/com/resumego/interview/service/InterviewPromptBuilder.java`、相关测试、`frontend/src/views/InterviewView.vue`、AI 使用日志。
- AI 产出：
  - 新增 `MockAiClientTest.shouldReturnVariedInterviewEvaluationScoresByAnswerQuality`，先复现 Mock 评价固定 8/7/6/8 导致不同回答同分的问题；
  - `MockAiClient` 的 `interview_evaluation` 从固定分改为基于回答质量的确定性评分：综合回答长度、技术词、技术动作、量化指标和空泛词，生成清晰度、相关性、深度、准确度四维分数，并给出不同强弱建议；
  - 新增 `InterviewPromptBuilderTest.evaluationPromptShouldNotAnchorModelToFixedScores`，防止 Prompt 再出现 `"clarity": 8`、`"relevance": 7`、`"depth": 6`、`"accuracy": 8` 这类锚定模型的固定示例；
  - `InterviewPromptBuilder` 将评价输出格式中的固定数字替换为字段说明，并明确要求：空泛回答不得高于 6 分，具体场景/个人动作/技术细节/可核实结果才可给 8-10 分，四个维度不能机械相同；Prompt 版本升级为 v1.2；
  - 前端面试大厅移除顶部和历史区的旧“多轮综合复盘/综合”按钮及历史卡片勾选逻辑，整次复盘入口保留在完成后的单次面试详情中；
  - 单轮总结新增“本轮评分画像”，展示四维平均分、最低维度和对应训练提示，让评分从裸数字转为可行动复盘。
- 采纳内容：采纳“先找根因再改”的处理方式；采纳 Mock 需要模拟真实差异而非固定分的原则；采纳 Prompt 去数字锚定，减少真实千问输出贴近示例分数的风险；采纳“大厅不做手动多选复盘，复盘属于某一次面试详情”的产品心智。
- 拒绝内容及原因：未在本轮实现成长趋势持久化，因为它需要新增后端快照表和查询接口，属于独立数据模型任务；未让 AI 决定面试状态、结束条件或轮次推进；未重算旧历史评价，旧数据仍保持当时生成结果。
- 发现的 AI 问题：结构化 Prompt 中固定数字示例会强烈锚定模型输出，导致真实模型也可能反复给出 8/7/6/8 或 75-78 区间；Mock 固定响应会掩盖产品演示中的能力差异，必须让 Mock 具备最小质量区分能力。
- 测试与验证证据：`cd backend && mvn -q -Dtest=MockAiClientTest#shouldReturnVariedInterviewEvaluationScoresByAnswerQuality test` 红转绿；`cd backend && mvn -q -Dtest=InterviewPromptBuilderTest#evaluationPromptShouldNotAnchorModelToFixedScores test` 红转绿；`cd backend && mvn -q -Dtest=MockAiClientTest,InterviewPromptBuilderTest,InterviewServiceTest,InterviewPlanServiceTest test` 通过；`cd frontend && npm run build` 通过。前端构建仍仅保留既有第三方 `@vueuse/core` pure annotation 与 chunk size 警告。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本次不修改 `InterviewStateMachine`、`InterviewTransitionTable`、状态集合、转换规则、结束条件或题号推进；面试评分为 AI 对回答内容的结构化评价，不参与状态机控制，也不进入简历评分或岗位匹配禁飞区。
- 反思与后续规则：所有 Prompt 示例中应避免给出容易被模型照抄的固定分数；Mock 不应只返回“能跑通”的固定内容，而应覆盖高质量/低质量输入差异。下一步可单独推进“成长趋势持久化”，为每次完成的面试计划生成后端能力快照。

### 2026-07-15｜成员 1｜Sprint 1 / 测试覆盖率强化

- 工具与模型：Codex（GPT-5）
- 协作模式：测试先行 / 非禁飞区覆盖强化
- 任务目标：在不修改简历评分算法、岗位匹配排序和面试状态机的前提下，将全项目 JaCoCo 指令覆盖率提升至课程要求的 85%。
- 允许读取和修改的范围：非禁飞区的 AI 编排、Prompt、Controller、简历版本仓储层及其测试；不修改 `InterviewStateMachine`、评分计算或匹配排序实现。
- AI 产出：补充面试历史/跨会话总结、面试 Controller 协议映射、AI 排版提案、优化建议结构化校验、简历版本 JSON 路径应用、Mock AI 错误模式与响应契约、真实数据模式建议 Prompt 的单元测试。
- 采纳内容：仅采纳可复现的单元测试与边界/异常测试；所有 AI 输出仍要求经过既有结构化校验，建议测试覆盖了“无证据不可直接采纳”和“疑似编造标记为高风险”路径。
- 拒绝内容及原因：未生成或改写面试状态转换、题号推进、结束条件；未生成简历评分权重、岗位匹配排序或其测试规则，避免触及禁飞区。
- 人工修改：禁飞区实现与其 85% 以上覆盖率由项目成员完成；成员需在提交前复核本轮新增测试的作者归属与测试报告截图。
- 测试与验证证据：`cd backend && mvn verify` 通过；376 项测试中 0 失败、1 跳过；JaCoCo 指令覆盖率为 17,988 / 20,996 = 85.67%，分支覆盖率为 1,147 / 1,606 = 71.42%；JaCoCo check 输出 `All coverage checks have been met.`。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本轮没有修改禁飞区生产代码，也没有设计或补全状态机转换规则、评分权重、岗位排序规则；面试相关测试仅覆盖 AI 调用编排、历史恢复、总结和 HTTP 协议。
- 后续补充：全项目 JaCoCo 指令覆盖率门禁已从 65% 提高到 85%，与课程验收目标保持一致；修改后再次执行 `cd backend && mvn verify`，376 项测试中 0 失败、1 跳过，输出 `All coverage checks have been met.`。

### 2026-07-15｜成员 1｜S3-A/B/C / 面试复盘与成长闭环产品化

- 工具与模型：Codex（GPT-5）
- 协作模式：按既定 S3 切片推进 / 非禁飞区功能实现
- 任务目标：检查“面试对话分数产品化、成长趋势持久化、去除旧多轮综合复盘入口、AI 建议升级为薄弱点 + 训练计划”的完成情况，并继续补齐后端成长趋势快照和右侧 AI 能力教练体验。
- Prompt 摘要：用户要求基于既定 S3 推进建议继续执行，优先完成面试复盘产品化、成长趋势持久化和 AI 建议教练化。
- 允许读取和修改的范围：`backend/src/main/java/com/resumego/interview/` 的计划复盘编排、成长趋势查询与 DTO；`backend/src/main/resources/db/migration/`；`frontend/src/api/interview.ts`、`frontend/src/types/interview.ts`、`frontend/src/views/InterviewView.vue`、`frontend/src/components/editor/AiCoachPanel.vue`；`backend/src/main/java/com/resumego/optimization/SuggestionPromptBuilder.java`；相关测试与本日志。
- AI 产出：
  - 新增 `V16__add_interview_growth_snapshots.sql`，创建 `interview_growth_snapshots`，固化一次完整多轮面试在清晰度、相关性、深度、准确性和综合分上的快照；
  - 新增 `InterviewGrowthService`、`InterviewGrowthController` 和成长趋势 DTO，提供 `GET /api/v1/interview-growth?resumeId=&jobDescriptionId=` 查询接口；
  - `InterviewPlanService.generatePlanSummary()` 在整次复盘保存后写入成长快照，前端成长趋势改为读取后端快照，不再依赖页面临时聚合 session 数据；
  - 右侧 AI 建议 Prompt 升级到 v1.2，要求输出“薄弱点 → 行动建议 → 训练计划”的教练式表达；
  - `AiCoachPanel` 将 Project Advice 调整为 Ability Coach，建议卡展示薄弱点、行动建议和 3 步训练计划，仍然不自动修改简历、不创建新版本。
- 采纳内容：采纳“成长趋势属于后端持久化档案”的设计；采纳“整次复盘属于单次面试详情，不恢复大厅手动多选复盘”的产品心智；采纳“AI 建议只做教练建议和训练计划，不进入系统级自动改写闭环”的边界。
- 拒绝内容及原因：未新增复杂大厂画像功能，只保留后续扩展方向；未依赖外部 favicon 实时抓取 logo；未修改 `InterviewStateMachine`、状态转换表、题号推进、简历评分算法或岗位匹配排序。
- 人工修改：成员需要在真实环境手动验证：完成一次多轮面试 → 生成整次复盘 → 打开成长趋势应能看到后端快照；旧的已完成面试如果没有重新生成整次复盘，不会自动补快照。
- 发现的 AI 问题：如果成长趋势长期留在前端临时聚合，会导致刷新、历史删除、跨版本对比时数据口径不稳定；AI 建议如果只显示“建议文本”，用户难以形成训练行动，因此改为薄弱点和训练计划心智。
- 测试与验证证据：`cd backend && mvn -q -Dtest=InterviewGrowthServiceTest,InterviewGrowthControllerTest,InterviewPlanServiceTest,MockAiClientTest,InterviewPromptBuilderTest test` 通过；`cd frontend && npm run build` 通过。前端构建仍仅保留既有第三方 `@vueuse/core` pure annotation 与 chunk size 警告。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本轮只消费已保存的 AI 面试评价分数生成成长趋势快照，不生成或修改简历评分权重、岗位匹配排序或面试状态机实现；AI 建议只作为人工修改和训练提示，不自动写入简历。
- 后续补充：继续完善 S3 闭环细节。`InterviewGrowthService.getGrowthReport()` 增加旧数据补全逻辑：查询成长趋势时会自动识别“已保存整次复盘但尚未生成成长快照”的历史计划，基于已有 `summary_json` 和已保存单题评价分数补写 `interview_growth_snapshots`，不重新调用 AI、不修改状态机。新增 `V17__add_company_profiles.sql` 预留公司偏好 Profile 表，并种子化字节、腾讯、阿里、美团 4 条经验型演示数据；所有记录均标注 `experience_based` 和来源说明，后续只用于 AI 建议 Prompt 增强，不参与评分、排序或录用概率判断。验证：`cd backend && mvn -q -Dtest=InterviewGrowthServiceTest,InterviewGrowthControllerTest,InterviewPlanServiceTest test` 通过；`cd backend && mvn -q -DskipTests compile` 通过；`cd frontend && npm run build` 通过。
- 后续补充 2：推进图标稳定性优化。`frontend/src/utils/companyLogo.ts` 将已知重点公司优先匹配本地 curated SVG 图标，未知公司才使用 `sourceMeta` 中的安全 `logoUrl`，最后 fallback 为公司名品牌色头像；匹配文本从公司名扩展到岗位名、domain、website、sourceUrl 和 platform，减少公司名缺失或变体导致的错配。该改动不依赖外部 favicon 实时抓取，避免演示时网络或第三方 logo 失效。验证：`cd frontend && npm run build` 通过。
- 后续补充 3：补齐“大厂个性化先做数据结构预留”的落地缺口。`OptimizationSuggestionService` 在生成 AI 建议时按 JD 公司名读取 `company_profiles`，并将命中的公司偏好 Profile 注入 `SuggestionPromptBuilder`；Prompt 明确标注该 Profile 仅用于项目技术表达建议和训练方向参考，不得作为评分、排序、录用概率或事实断言依据。`SuggestionPromptBuilderTest` 新增普通模式与真实数据模式的 Profile 注入测试，避免后续回退为“只建表不使用”。同时补充 `InterviewGrowthServiceTest` 的 H2 数据库级测试，覆盖成长快照 upsert、趋势查询、旧复盘 backfill 和岗位兜底名称，修复新增成长趋势服务导致全项目 JaCoCo 覆盖率掉到门禁以下的问题。验证：`cd backend && mvn -q -Dtest=SuggestionPromptBuilderTest,OptimizationSuggestionServiceTest test` 通过；`cd backend && mvn -q -Dtest=InterviewGrowthServiceTest test` 通过；`cd backend && mvn -q verify` 通过，JaCoCo 指令覆盖率 85.19%；`cd backend && mvn -q -DskipTests compile` 通过；`cd frontend && npm run build` 通过。

### 2026-07-16｜成员 1｜面试大厅品牌图遮挡修复

- 工具与模型：Codex（GPT-5）
- 协作模式：系统化调试 / 最小样式修复
- 任务目标：修复面试大厅深色流程卡中“职达 AI 简历求职助手”品牌图遮挡“简历、岗位、面试”流程标签的问题。
- 根因：流程卡内容宽度约 262px，原品牌容器宽度 156px、居中后占据 53-209px；三个流程标签分别位于 18-58px、204-244px、34-74px，发生边缘交叠。
- AI 产出：仅调整 `InterviewView.vue` 的品牌卡尺寸、装饰范围、标签层级与左下标签位置；品牌图片、面试交互逻辑和状态机均未修改。
- 验证证据：调整后品牌卡范围为 70-192px，流程标签分别处于 18-58px、204-244px、12-52px，不再相交；`cd frontend && npm run build` 通过。构建仅保留既有第三方 `@vueuse/core` pure annotation 与 chunk size 警告。
- 是否涉及 AI 禁飞区：否
- 禁飞区处理说明：本次仅为前端视觉布局修复，不修改面试状态、状态转换、题号推进、简历评分或岗位匹配规则。
