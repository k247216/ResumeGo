# ResumeGo 关键决策

这里只记录会长期影响产品方向或工程边界的决定。每条记录使用“日期、决定、原因、影响”，不维护复杂审批状态。

## 2026-08-19｜从课程 Web 原型转为个人桌面产品

**决定**

当前仓库作为 ResumeGo 的唯一产品主线，不继续维护课程阶段的计划、团队分工、答辩材料和过程日志。外部备份保留旧版本，仓库内直接建立新的产品文档基线。

**原因**

课程材料强调短期验收和功能展示，已经不适合指导长期个人产品。继续混用会让产品范围、开发方式和完成标准发生冲突。

**影响**

后续工作以真实用户结果、隐私、可信度、可安装性和可维护性排序。旧功能可以删除或重做，不要求保持原有页面和演示路径。

## 2026-08-19｜统一品牌为 ResumeGo

**决定**

正式产品名、桌面应用名和文档名统一为 ResumeGo；“职达”作为中文名称。公开仓库最终使用与 ResumeGo 一致的名称。

**原因**

ResumeGo 简短、易记，也能够覆盖简历、岗位准备和模拟面试。ResumeRefineHelper 过长，并把产品限制为简历润色辅助工具。

**影响**

界面文案、包名、安装包、发布说明和仓库说明逐步统一。正式公开前检查同名仓库、域名、应用商店名称和商标风险。

## 2026-08-19｜采用本地优先的桌面产品方向

**决定**

ResumeGo 默认在用户设备保存简历和求职数据，不要求注册或云端账户。第一阶段使用 Electron 包装现有 Vue 前端，Spring Boot 作为受控本地子进程，H2 文件数据库替代用户侧 MySQL。

**原因**

简历和面试信息高度敏感；本地优先能够降低使用门槛和隐私风险。Electron 可以复用现有前端，Spring Boot 可以保留已经完成并通过测试的业务能力，避免同时重写多个技术层。

**影响**

初期安装包会偏大，并需要处理 Java 运行时、子进程、数据目录和升级迁移。桌面壳稳定前不重写前端框架或后端语言。

## 2026-08-19｜以求职项目组织核心体验

**决定**

公司、目标岗位、简历版本、能力证据、模拟面试和反馈事项统一归属于一个求职项目。首页优先展示进行中的求职项目和下一步行动，不再以功能入口数量作为信息架构中心。

**原因**

真实用户想完成的是某个岗位的准备，而不是分别操作评分、匹配和面试模块。求职项目能够让上下文稳定，并形成反馈闭环。

**影响**

现有工作台、导航、岗位、简历和面试页面需要逐步重构；数据迁移必须保留旧版本和历史记录的可追溯关系。

## 2026-08-19｜公共岗位库退出核心范围

**决定**

在没有稳定、可授权、可追溯的数据源之前，不维护公共岗位市场。产品只保存用户主动创建或导入的目标岗位。

**原因**

不明来源、过期或模拟岗位会损害产品可信度，并引入持续采集和维护成本，但不能直接改善核心求职闭环。

**影响**

现有岗位列表功能降级或移除。未来重新引入岗位发现能力前，需要先说明数据来源、更新时间、授权方式和失败处理。

## 2026-08-19｜AI 是受约束的建议助手

**决定**

AI 可以抽取、改写、生成问题和提供解释，但最终评分、岗位排序、面试状态、数据权限和持久化决定由确定性程序控制。所有修改建议必须关联证据并由用户确认。

**原因**

求职内容需要真实、可解释和可恢复。模型输出具有不稳定性，不能成为改变用户数据和核心业务状态的唯一依据。

**影响**

模型输出需要 Schema、业务校验、降级和审计关系。简历修改使用版本而非覆盖，模型故障不能阻塞基础编辑、导出和删除。

## 2026-08-20｜日程采用自建本地月历，不接系统日历

**决定**

面试、笔试和跟进事项由产品内置的本地月历管理（`schedule_events` 表 + 自绘 CSS 月历界面），不读取或同步操作系统日历（macOS Calendar / iCloud / Outlook 等）。

**原因**

求职日程记录需要与岗位、简历和反馈形成闭环，并且全部保存在本地。接入系统日历会引入第三方同步、授权与遥测边界，与本地优先的产品约束冲突；用户选择的方案就是自建本地月历。

**影响**

后续若需要系统日历提醒，需先更新隐私文档并重新评估同步边界，不能静默接入。

## 2026-08-20｜简历导入先做 Markdown，PDF 留待后续

**决定**

简历导入能力本期只支持 Markdown 文本导入（解析预览 → 用户确认 → 创建简历与初始版本）；PDF 导入不在本期范围。

**原因**

Markdown 是结构清晰、可确定性解析的文本格式，能在不调用外部模型的情况下安全还原简历内容。PDF 版面解析复杂、需要外部依赖，且解析结果不可靠时容易静默破坏用户数据。

**影响**

PDF 导入作为后续独立事项评估，届时需明确解析失败的处理策略（提示而非写入半成品）。

## 2026-08-20｜首页 = 全局工作台（Dock | Agenda Workspace | Inspector），取消首页全局当前目标

**决定**

工作台首页是全局视角（Global Workspace），不再依赖全局 active target：首页展示所有求职目标聚合出的 upcoming 日程（Agenda timeline），选中事件或目标后由 Inspector 回答四个问题——这是什么（公司/岗位/事件类型/时间）、准备得怎样（简历/模拟面试/日程 readiness）、下一步做什么（一条上下文建议 + 一个主操作）、去哪里深入（目标详情 →）。最终空间模型为 `Dock | Agenda Workspace | Inspector`；「当前目标」概念只在 targets 页保留。

**原因**

用户确认首页应回答「接下来有什么安排、针对选中安排我该做什么」，而不是单目标的工作台。全局状态会让多个求职目标互相抢占首页上下文；把当前上下文改为由选中事件/目标驱动，首页才能真正服务多目标并行求职。

**影响**

首页删除 Current Target Banner、全局 active target、独立 Next Action section、dashboard 式准备卡片；选中事件切换时 Inspector 身份、准备状态、Next Action 全部跟随该事件对应的目标；未关联目标的事件如实显示「未关联求职目标」并跳转日程，不虚构准备数据。`targetsStore.activeTarget` 保留给 targets 页使用。

## 2026-08-20｜首页视觉系统：绿色=状态色、黑色=主操作色，空间模型 `Dock | Main Workspace | Inspector`

**决定**

工作台首页按用户确认的 37 节 UI 规范落实视觉系统，作为后续页面统一的视觉基线：页面 `100dvh`、无文档级滚动；空间模型严格为 `Dock | Main Workspace | Inspector` 三区（Dock 72px 常驻左侧，Inspector 右侧为空间区域而非卡片，`<1050px` 时 Inspector 变为 right drawer）；**绿色只表达状态（即将到来的最临近场次、已完成项），黑色/高对比反色表达主操作（主按钮）**，不再用绿色做主按钮；页内最大视觉元素是事件时间数字（40px/300），不用 30px+ 大标题；垂直节奏只用 4/8/12/16/20/24/28/32/40；radius 三档（8-9/11-12/16-18）；shadow 仅 Primary Event 与 Dock 两处例外；dark 模式只换 token 不重排布局（canvas `#111212`、主按钮反色为浅底深字）；禁止 KPI 卡、统计卡、渐变/毛玻璃/辉光、营销标语、多主按钮等 SaaS 网格装饰。

**原因**

此前的卡片化首页（居中网格、多个填充式模块、绿色主按钮）未能回答用户 3 秒测试（下一件事是什么/何时/与谁相关/缺什么准备/下一步能做什么），视觉重心被分散。用户以一份逐节规范明确了空间与层级，要求严格执行、不再重新解释方向。该规范与「绿色=进度/状态、黑色=操作」的颜色语义一致，也能避免首页与工作台页面因视觉语言漂移。

**影响**

首页实现与 QA 记录同步该规范（见 `design-qa.md` 第 6 轮）。其余页面（targets/resumes/interview/schedule/settings）本轮不动，但后续重构应复用同一套 token 与层级规则（radius、字重分布、无卡片倾向、状态色/操作色分离），避免每页各自发明视觉语言。

## 2026-08-20｜首页空间模型改为固定 Master–Detail（Dock | Agenda Rail | Focus Workspace）

**决定**

工作台首页废弃此前两轮确立的 Workspace + Inspector 空间模型（`Dock | Agenda Workspace | Inspector` 与 37 节规范轮），重新实现为固定的 Master–Detail Desktop Layout，页面只允许三个一级空间：**Dock | Agenda Pane | Detail Pane**。1440 基准：Dock 72px；Dock→应用 gap 32px；应用 max-width ≈1270px，左对齐 Dock 不居中；Agenda Rail 310px，为整块 subtle pane surface（非多个 Card）；Divider 1px；Focus Workspace `flex:1`。Agenda Rail 按日期分组展示 upcoming events，每个 event 是 navigation row（约 86px 高 / 12px radius / 极浅 selected 背景）；不再把最近 upcoming event 放进 100% 宽的大 Card。Focus Workspace 顶部直接渲染选中事件事实（`明天 / 09:50 / 字节跳动 · 笔试 / 还有 18 小时`，时间 48px/300、事件名 21px/600）。未关联 target 的事件只显示真实信息（`未关联求职目标 / 关联目标 → / 查看日程 →`），不虚构简历/模拟面试准备状态；已关联事件展示 Target identity → 准备 compact object row → contextual next action。最近活动移入 Detail Workspace 底部；删除首页「使用帮助」；不再创建独立 Inspector / Primary Event Card / 额外 Dashboard module。

**原因**

用户确认新的空间模型作废旧模型，要求严格按照 Master–Detail composition 实现——左侧导航（Agenda）+ 右侧焦点详情（Focus Detail），并明确本轮不要自行重新解释设计哲学。

**影响**

「全局工作台」与「37 节规范」两轮的首页空间模型被取代；其视觉 token（canvas `#F5F5F2`、`--surface-subtle`、`--brand-soft` 等）与「绿色=状态色、黑色=主操作色」语义继续沿用。`TargetDashboard.vue` 完全重写为 Master–Detail；`WorkbenchView` 派生 `DetailView` 状态机（event/target/empty）并新增「关联目标 →」对话框（对 schedule event 执行 PATCH `jobDescriptionId`）。QA 记录见 `design-qa.md` 第 7 轮，roadmap 进度记录同步更新。

## 2026-08-20｜语义 token 单一来源 + 其余页面 master-detail 化 + 面试绑定目标驱动

**决定**

全应用视觉系统收敛到 `src/style.css` 单一 token 来源（浅色 `:root` + 暗色 `body[data-theme='dark']`），桌面工作台之外的页面不再各自携带分歧调色板或浅色 fallback；目标/简历/日程/设置四页改用 `DesktopShell` 全高变体（路由 `meta.fill`，`height:100vh` 内部面板滚动）与 master-detail/Inspector 布局，共享 `PageHeader` 与 1219/1099/959 断点阶梯；面试大厅改为 Session Composer，会话房间仅做硬编码色 token 清理、不改布局与状态机。「开始模拟面试」统一为**目标驱动绑定**：大厅以求职目标实体的 `jobDescriptionId`/`resumeVersionId` 为准，无目标上下文或校验失败时明确显示未绑定态，不再静默回退到上次岗位/简历或列表第一项。

**原因**

各视图自行发明调色板导致暗色主题直接失效（近黑气泡 + 近黑底）、卡套卡与页面级文档滚动违背「工作空间不是网页」原则；「开始模拟面试」的绑定岗位与主界面不统一（主界面看目标、大厅看 jobId/versionId），会让用户误以为已针对目标准备。绑定必须由真实目标数据推导，不编造。

**影响**

暗色切换成为纯 token 替换，重构页面全部消费全局 token 与共享断点；四页文档滚动清零；面试页布局与状态机不变，仅色值 token 化（含三个子组件）；绑定文案/状态随目标实体变化，缺 JD/缺简历的目标如实进入「岗位待选」态。后续新增页面必须复用全局 token 与断点阶梯，不再允许页面级分歧调色板。QA 记录见 `design-qa.md` 第 13 轮。

## 2026-08-20｜页面三模板 + 收敛 token 表正式化，进入收敛阶段

**决定**

全应用页面从此只能从三种模板中选一，AI 后续改动不得再发明新布局：

- **A · Timeline/List → Workspace**：首页（接下来列表 + 焦点详情，沿时间/事件顺序组织）。
- **B · Library/Master → Inspector**：求职目标、简历库、日历、设置（左 master 列 + 右 320–360px Inspector/面板）。
- **C · Composer → Inspector**：模拟面试（左侧表单式 Composer + 右侧 Inspector 摘要/历史）。

配套收敛 token 表（Design QA 第 14 轮的实测基准）：Dock 72–76px；Dock→page gap 40–48px；Page top 32–40px；Master column 360–380px（首页 rail 例外，已按用户确认收窄至 290px）；Inspector 320–360px；Workspace 水平 gap 32–40px；Major section gap 32px；Minor gap 16–20px；Row height 56–64px；Icon container 32×32px；Hairline 1px；Selected surface radius 10–12px；body 14px；metadata 12–13px；section label 13–14px/500–600；object title 18–22px；hero time 44–48px/300；page title 28–30px。`src/style.css` 增加 `--space-1..8`（4/8/12/16/20/24/32/40px）spacing 刻度；绿色只用于 selected/success/primary action。

**原因**

第 13 轮完成后五页架构统一但视觉语法仍不齐（首页克制、模拟面试仍带上一代卡套卡与 SaaS 配置感）；用户要求进入「收敛阶段」：统一数字与表达，不再逐页大改。三模板 + token 表把「以后怎么改」锁死，避免 AI 每次重构重新发明布局。

**影响**

本轮只做表达层收敛（首页三个细修点、目标页 identity/meta、简历库卡缩容与绿色去装饰、日历密度与 selected 弱化、面试大厅 Composer→Inspector），数据流/状态机零改动。首页 rail 行高 86px 与 token 表 56–64px 的差异记为已知偏差，留待下一轮「统一五页 spacing/token」处理；未关联目标的事件绝不显示任何目标信息（数据诚实性守卫）。QA 记录见 `design-qa.md` 第 14 轮，roadmap 进度同步更新。

## 2026-08-20｜第 15 轮：首页可拖拽 Split Pane + Settings 配置流程 + Target 求职项目工作台

**决定**

首页左侧「接下来」栏从固定 290px 改为**用户可拖拽的 Split Pane**（覆盖第 14 轮「首页 rail 收窄至 290px」的决策）：默认 330px、最小 270、最大 420；分隔条 = 1px hairline + 4px 透明命中区，hover/focus 加宽变品牌绿并显示 `col-resize`，pointer 拖拽调整宽度，双击恢复 330px，`role="separator"` + 左右方向键 8px 微调；rail 宽 < 290px 时进入 compact 模式（隐藏日分组标签、行内联时间 `● 09:50 公司 · 标题`）。实现为组件内 pointer events，零新依赖；原 1219/1099/959 分档收窄断点删除（宽度改由用户控制）。

同时锁定三项产品语义：

- **Settings = 服务配置流程，不替用户填表**：空态不预填任何 Provider/模型；「添加 AI 服务」只问服务商 + API Key →「验证并继续」→「已连接」→ 模型下拉（来自 Provider 的 `/models`，为空则手动填 Model ID）→「高级设置」（协议/Base URL/Model ID）默认折叠；普通用户不接触高级字段。`defaultModel` 不再硬编码 `deepseek-chat`。
- **Target 页 = 求职项目状态工作台**：右侧从「这个对象有什么字段」改为「这个求职项目目前是什么状态」——identity → 准备进度三列（简历 / 模拟面试 / 日程）→ 下一场（真实日程 + 建议 + 开始准备）→ 岗位信息（JD 已录入/待解析）。
- **首页「关联目标」行**：显示 `公司 · 岗位` 单行 + 「目标详情 ›」；公司未知时回退目标名，若目标名已含岗位则不重复追加（数据诚实）。

**原因**

290px 静态左栏无法同时满足信息密度与用户控制权；「尚未配置」被默认值填满会让用户误以为已配置一半；Target 页「字段详情」式表达与「求职项目状态」的用户心智不符。

**影响**

首页 rail 宽度为本地视图状态（不持久化，刷新回默认 330）；compact 模式只是表达层切换，事件数据不变；Settings 后端校验与密钥存储（safeStorage/H2）零改动，仅前端口径与文案重写；Target 页沿用既有 API（schedule/interview plan/JD），新增展示全部来自真实数据。QA 记录见 `design-qa.md` 第 15 轮，roadmap 进度同步更新。

## 2026-08-21｜产品身份确认：向 Career OS（V2）彻底转向

**决定**

ResumeGo 的产品主线从「证据驱动的单目标求职工作台」升级为「职达 Career OS」——本地优先、以个人职业资产为核心的 AI 职业成长操作系统。仓库内已提交的 V2 文档（`docs/product/productV2.md`、`docs/architecture/architectureV2.md`、`docs/ai-architecture.md`、`docs/domain-map.md`、`docs/data-model.md`、`docs/architecture/architecture- freeze.md` 及 `roadmap.md` 后半段 V2 附录）从「未决草案」转为**正式批准的方向**。开发主线围绕五个核心系统组织：

```text
Career Pipeline（管理目标）→ Resume Version（管理表达）→ Knowledge Base（管理知识资产）→ Interview Engine（管理训练）→ Growth System（管理反馈）
```

**原因**

产品要回答的不再是「为某个岗位准备一次」，而是「围绕职业目标持续积累资产、训练能力、看到成长」。求职者真正的痛点不仅是写简历，而是信息分散、缺乏岗位针对性、没有训练闭环、AI 不可信；Career OS 以职业资产为长期主线，能同时解决这四类问题。现有桌面基础（本地优先架构、求职目标、简历版本、模拟面试、日程、master-detail 界面）正是 V2 的落地底座，转向是自然演进而非推倒重来。

**影响**

- V2 文档状态从「草案」转为「批准方向」，`docs/product/productV2.md` 等作为产品定义基准；
- 现有 V1 界面（工作台/目标/简历/面试/日程/设置）作为 Career OS 当前落地形态继续演进，不删除已有能力；
- 后续开发按 V2 阶段推进：V1.0 Core（Pipeline/Resume/Interview/Calendar）→ V1.5 知识增强（Knowledge Base/RAG/复盘/成长）→ V2.0 Agent（Skill/MCP、Career Agent）；
- 数据诚实、证据驱动、AI 受约束、本地优先等既有原则全部保留并继续适用；
- 仓库公开名与产品名统一为 ResumeGo，中文名职达；远端仓库迁移至 `github.com/k247216/ResumeGo`。
## 2026-08-21｜版本线修正：V1 产品落地 → V1.5 过渡 → V2 远期

**决定**

产品版本线调整为三个明确阶段，修正此前「向 Career OS（V2）彻底转向」的表述：

- **V1（当前）**：本地优先、证据驱动的求职工作台——目标/简历/证据/面试/日程 + 备份恢复。这是唯一可下载、可主推的产品版本，本阶段重点是真实用户跑通「无指导完成一次简历迭代」的闭环。
- **V1.5（过渡）**：V1 的补充与向 V2 过渡的基础——按需增强证据→版本→面试→反馈闭环，为 V2 沉淀数据与用户习惯，不引入 Career OS 全套概念。
- **V2（远期）**：Career OS——五个核心系统 + 知识库 / RAG / Agent。仍是最重要的下下阶段重点，但**不进入当前开发**。

**原因**

单人、预发布产品的第一优先级是把当前版本交付给真实用户并验证北极星指标，而不是扩展为「操作系统」。V1 尚未有用户验证就推进 V2 是范围膨胀；把 V2 保留为远期方向，用 V1.5 平滑过渡，既能保证当前版本聚焦落地，也不丢弃 V2 的长期价值。

**影响**

- 2026-08-21「彻底转向」条目的表述被本条修正：V2 文档（productV2/architectureV2/ai-architecture/domain-map/data-model/architecture-freeze）状态从「批准方向」改回「远期方向草案」，不再作为当前产品定义基准；
- 当前产品定义以 `docs/product/product.md` 为准，开发按 V1 → V1.5 → V2 顺序推进；
- 数据诚实、证据驱动、AI 受约束、本地优先原则不变，三个版本都适用。



## 2026-08-22｜Career Pipeline 阶段追踪：求职目标新增用户维护的阶段字段

**决定**

在 `job_projects` 上新增 `stage` 字段（默认 `preparing`），取值为固定的六阶段集合：`preparing 准备中 / applied 已投递 / exam 笔试 / interviewing 面试中 / offer 已拿 Offer / closed 已结束`，并记录 `stage_updated_at`。阶段由用户手动推进，是用户声明的事实记录；AI 不推断、不自动流转。API 为 `PATCH /api/v1/projects/{id}/stage`，后端白名单校验；前端目标页详情新增「求职阶段」步进条（点击任一阶段直接切换），左列行与 identity 状态行展示当前阶段。数据库通过 Flyway 双基线迁移（MySQL V24 / H2 V4）落地，含 CHECK 约束。

**原因**

Career OS V1.0 Core 的 Career Pipeline 完成标准是「用户可以同时管理多个求职目标，并清晰了解每个目标当前状态」。此前 `status` 只有 active/archived（项目生命周期），无法表达求职进展。阶段模型保持最小可信：只记录用户主动声明的状态，不做投递平台集成，不引入自动推断，与「不得编造事实」「模型输出不控制流程状态」的长期约束一致。

**影响**

- `JobProjectStatus` 与 `TargetStage` 是两个正交概念，前者管理生命周期，后者表达求职进展；
- 前端类型新增 `TargetStage`/`TARGET_STAGE_LABELS`/`normalizeTargetStage`（缺失值回退 `preparing`），store 新增 `setStage`；
- 后续如需阶段历史（每次流转的时间线），应另立事件表而不是扩展本字段；
- 测试基准更新：后端 449 项、前端 138+ 项（本机 localStorage 环境性失败 13 项为存量问题，CI 全绿）。



## 2026-08-22｜V2.0 进入当前开发，早期与 V1 并行安装

**决定**

职达 Career OS V2.0 从远期草案转为当前开发线。V1 由独立维护流程继续负责 Release、缺陷修复和现有功能优化；V2 不再承接 V1 的界面优化任务，而围绕 Knowledge Base、Career Pipeline、Resume Version、Interview Engine、Growth Feedback 和 Workspace Action 分阶段演进。

V2 早期版本与 V1 并行安装：使用独立应用标识、产品显示名和本地数据目录。V2 只通过显式迁移流程读取 V1 备份或工作区副本，迁移前创建备份，不原地修改 V1 数据。V2 稳定前不支持将已经升级的数据降级回 V1。

**原因**

V1 已经形成可下载、可在 Windows 使用的发行基线，同时 V2 将重构核心业务对象和数据关系。并行安装可以让 V1 持续维护，也允许 V2 在不危及现有用户数据的前提下进行架构级迭代。

**影响**

- `docs/product/productV2.md` 成为当前产品定义，`docs/product/product.md` 降级为 V1 历史与迁移参考；
- `docs/architecture/architectureV2.md` 成为 V2 目标架构，现有 `docs/architecture/architecture.md` 继续描述当前代码基线；
- V2 开发使用隔离 worktree 和短期 `codex/` 分支，`main` 仍是唯一长期分支；
- V2 第一阶段是 Career OS Foundation，不同时全面建设全部系统；
- V1 数据兼容通过可验证、可重试的迁移层实现，不通过长期保留错误领域模型实现。



## 2026-08-22｜V2 采用中央总控、多分支隔离与统一集成

**决定**

Career OS V2 采用中央总控、隔离功能分支与统一集成的协作模式。Core Controller 负责产品模型、架构边界、任务授权、公共契约、集成审查、全量回归和最终测试；Feature Agent 只在独立 worktree 的短期分支内实施已授权任务，并以提交哈希和可重复的交付证据交回。

**原因**

可运行的代码不足以证明 V2 可以合入：它可能偏离职业资产模型、破坏数据所有权或恢复路径、泄露隐私、改变公共契约，或只在功能分支局部测试中成立。多 Agent 并行时，若没有唯一的决策与集成责任，也无法保证各项实现形成一个可测试、可发布的产品。

**影响**

- V1 是本 V2 工作流的只读基线：任何 V2 任务、Feature Agent 与 Core Controller 集成操作都不得修改 V1 代码、数据、构建、维护线或发布物，也不得以 V1 作为任务目标、集成目标或迁移捷径。独立 V1 维护在本治理流程之外，必须取得单独授权。
- V2 功能分支和 worktree 归 Feature Agent 所有，但 `main` 与 `codex/v2-career-os` 集成 worktree 由 Core Controller 保护；功能 Agent 不得自行合并、变基或覆盖其他分支。
- 数据库迁移、公共 DTO/API/Port、领域规则、Electron 身份与升级协议、全局构建配置及产品/架构/隐私/决策文档属于受保护的公共契约，除非任务卡明确授权，否则不得修改。
- 功能交付必须包含 RED/GREEN、局部与模块回归及风险证据；Core Controller 必须在统一集成后执行全量测试、桌面构建和发布前验证，才能决定接收或退回。



## 2026-08-23｜求职计划页重构为「公司计划卡」视图，阶段集合按真实流程修订

**决定**

根据产品反馈对 2026-08-22 的阶段模型做一次修订，并重设计求职计划页（不动其他页面）：

1. 阶段集合改为贴近真实校招流程的六段：`applied 投递中 / exam 笔试 / interview 面试 / hr HR面 / offer 已拿 Offer / closed 已结束`（原 preparing/interviewing 经迁移 V25/V5 映射合并），管线展示前五段、已结束以中性态呈现；
2. 求职目标页从 master-detail 改为公司计划卡网格：每张卡片包含本地品牌色公司标识、可点击推进的阶段管线、「{公司}修改版 · V{n}」简历徽章、近三次面试记录（点击弹窗查看逐题题目与评分）、创建时间与最近动态；页面不再提供模拟面试入口；
3. 创建目标弹窗升级为四要素录入：公司 / 岗位 / JD 原文 / 绑定简历，公司信息驱动品牌标识与后续修改版命名；
4. 页面画布为纯白底色，卡片含 hover 浮起、入场 stagger、当前阶段呼吸环等克制动效，并尊重 `prefers-reduced-motion`。

**原因**

用户反馈原六阶段（准备中/已投递/…）与真实招聘流程不匹配（缺 HR 面），且 master-detail 视图信息密度不足；成熟求职管理工具（Huntr/Teal 类）均以公司为单位组织进度。公司真实 logo 属于外部资源，违反本地优先约束，故采用本地品牌色映射 + 字母标识的方案离线渲染。

**影响**

- 数据库新增 Flyway 双基线迁移 MySQL V25 / H2 V5（旧值自动映射，CHECK 约束同步替换）；
- 前端 `PIPELINE_STAGES` 与阶段标签成为该页单一事实来源；面试记录弹窗复用既有 `getSessionHistory` 接口，无新增后端面；
- 「修改版」当前为展示层语义（绑定版本即该公司修改版）；简历 fork 工作流仍归 Resume Version System 阶段实现。


## 2026-08-23｜Knowledge Library 采用受管副本编辑模型

**决定**

Knowledge Library 保持全局黑色工具栏常驻，只允许资料库导航、资料列表和来源检查器完整收起到零宽度。页面以批准的桌面多栏目标图为视觉基线，取消外层大卡片和残留图标轨。新建笔记改为一次点击创建真实 ID、自动选中并进入行内标题与正文编辑。

本地 NOTE 和导入的 Markdown 受管副本允许编辑；TXT 及其他未授权格式只读。Markdown 保存只修改 ResumeGo 数据目录中的受管副本，同时更新提取正文、大小、哈希和更新时间，不覆盖用户原位置文件。文档响应显式提供安全扩展名，前端不得依赖标题猜测类型。

**原因**

现有页面虽然具备导入、分类和阅读能力，但收起后遗留长条、文件类型不明确、新建笔记依赖多余对话框，并把所有 FILE 一律设为只读，无法形成接近 Obsidian 的流畅本地知识工作流。直接修改用户原文件风险过高，只修改数据库正文又会造成受管来源与展示内容不一致。

**影响**

- Knowledge 公共 DTO、标题更新和内容保存契约需要显式扩展；
- 受管 Markdown 写入必须具备所有权校验、原子替换、失败回滚和哈希冲突处理；
- 资料库内部三个面板的可见性属于本机 UI 状态，不改变业务数据和选择状态；
- 全局工具栏不新增收起功能，避免影响其他 V2 页面；
- 详细边界与验收见 `docs/superpowers/specs/2026-08-23-v2-knowledge-library-editor-redesign.md`。

