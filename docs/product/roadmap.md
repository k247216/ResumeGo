# ResumeGo 产品路线图

路线图按可交付的用户结果推进。每个版本都必须能够独立运行、回归和发布，不以页面数量作为完成标准。

## 当前主线：职达 Career OS（V2，2026-08-21 批准）

产品主线已从「单目标求职工作台」升级为「以个人职业资产为核心的 AI 职业成长操作系统」（见 `docs/decisions.md` 2026-08-21 决策）。下方 V1 路线图记录产品重置阶段的完成过程与退出条件；后续开发按文件后半段 **Career OS Evolution Roadmap（V2.0）** 的阶段推进：

```text
V1.0 Core（Pipeline / Resume / Interview / Calendar）→ V1.5 知识增强（Knowledge Base / RAG / 复盘 / 成长）→ V2.0 Agent（Skill/MCP、Career Agent）
```

现有界面（工作台/求职目标/简历/面试/日程/设置）是 Career OS 的当前落地形态，继续演进不删除。

## 当前阶段：产品重置

目标：把现有 Web 原型转换为清晰、可信、可继续演进的产品基线。

- 统一 ResumeGo 品牌和产品语言；
- 删除演示项目、课程流程和不可信数据入口；
- 建立以求职项目为中心的信息架构；
- 拆分超大页面和后端服务的职责；
- 为桌面化建立进程、存储和迁移边界；
- 补充关键前端流程测试。

退出条件：仓库文档、界面导航和核心数据对象使用同一套产品模型，且现有构建与后端测试保持通过。

## 起始任务包

起始任务包负责把“产品重置”从文档方向转化为可验证的工程起点。任务按编号顺序推进；前一项没有达到完成条件时，不同时展开依赖它的大范围界面重构。

### RG-001｜清除默认个人数据与建立首次启动空状态

- **优先级：** P0
- **依赖：** 无
- **状态：** 核心任务已完成（2026-08-19）；示例项目导入在 RG-002 建立求职项目模型后补齐。
- **交付物：** 默认数据清单、清理后的初始化逻辑、首次启动页、显式的示例项目入口。
- **完成条件：** 全新数据目录启动后不出现姓名、联系方式、简历、岗位或面试记录；用户可以进入第一个真实目标的创建流程。示例数据不再随初始化生成；待 RG-002 完成后，只允许通过用户主动选择、带有明显标识的示例项目导入。

### RG-002｜定义求职项目最小数据模型

- **优先级：** P0
- **依赖：** RG-001
- **状态：** 已完成（2026-08-19）；项目中心界面在 RG-004 接入。
- **交付物：** 求职项目字段说明、数据库迁移、后端接口、前端类型和基础测试。
- **完成条件：** 用户可以创建、重命名、归档和删除求职项目；一个项目能够关联目标岗位和当前简历版本；删除行为明确处理关联数据且不会跨项目读取内容。

### RG-003｜补齐前端测试基础设施

- **优先级：** P0
- **依赖：** 无
- **状态：** 已完成（2026-08-19）；启动状态、求职目标、专注编辑器、路由边界和失败恢复已有组件测试。
- **交付物：** 组件测试配置、统一测试命令、首批核心状态测试和持续集成入口。
- **完成条件：** 首次启动空状态、求职项目创建以及异步失败反馈至少各有一条自动化测试；本地和持续集成使用同一命令运行；测试不连接真实模型服务。

### RG-004｜拆分工作台页面并收束导航

- **优先级：** P0
- **依赖：** RG-002、RG-003
- **状态：** 已完成（2026-08-19）；桌面式外壳、三种启动状态、目标岗位录入、当前简历选择、专注编辑器、本地简历库和能力证据库已接入；目标驾驶舱可读取当前版本的真实证据引用，并以锁定的岗位和简历版本进入模拟面试。面试大厅、历史记录、会话状态、进行中侧栏和整次复盘已形成独立组件或可测试模块，页面只负责流程编排。
- **交付物：** 以求职项目为入口的导航、职责单一的页面组件、对应 store 或 composable、回归测试。
- **完成条件：** 首页优先展示进行中的求职项目和下一步行动；简历、岗位、建议和面试都从项目上下文进入；页面不再同时承担数据请求、领域规则、复杂编辑和弹窗编排。

### RG-005｜验证 MySQL 到 H2 的兼容性

- **优先级：** P1
- **依赖：** RG-002
- **状态：** 已完成（2026-08-19）；新增独立 H2 文件基线和 `local` 配置，完整应用可在空白个人工作区启动，文件重开与冷备份恢复均有自动化验证；MySQL 历史迁移保持不变。
- **交付物：** 文件模式 H2 配置、Flyway 迁移验证、数据库兼容性记录、备份恢复样例。
- **完成条件：** 核心后端测试在 H2 文件模式通过；应用重启后数据仍存在；MySQL 专用 SQL 和字段差异均有明确处理；失败迁移不会覆盖原数据。

### RG-006｜验证 Electron 最小桌面链路

- **优先级：** P1
- **依赖：** RG-005
- **状态：** 已完成（2026-08-19）；Electron 主进程可在随机回环端口启动内置 Spring Boot 与精简 Java 运行时，健康检查后显示 Vue 窗口，退出时停止子进程；preload 仅暴露受应用来源约束的运行配置和密钥白名单，API Key 优先使用系统安全存储，不可用时明确降级为单次会话。模型配置支持三类协议、常用预设、连接测试、模型发现和首次使用引导。macOS arm64 未签名开发包已实际启动验证，Windows 构建配置已建立、待 Windows 环境验收。
- **交付物：** Electron 主进程、受控 preload、Vue 窗口、Spring Boot 子进程启动与退出、开发启动命令。
- **完成条件：** 应用可以启动本地后端、等待健康检查后打开界面，并在退出时停止子进程；渲染进程没有任意 Node.js 权限；启动失败时显示可理解的错误且不损坏数据。

### 执行顺序

```text
RG-001 ──→ RG-002 ──→ RG-004
              │
              └────→ RG-005 ──→ RG-006

RG-003 ─────────────→ RG-004
```

RG-001 至 RG-006 的桌面基础主线已完成。下一阶段进入 v0.1 产品闭环：补齐 Markdown 导入、可编辑解析结果、PDF 导出与面向真实用户的安装体验；视觉细化随真实工作流逐步推进。

### 2026-08-20 进度记录

- **日程真实功能已接入（自建本地月历）**：新增 `schedule_events` 表（Flyway 双写 MySQL/H2）与 `schedule` 后端模块（查询/新增/编辑/软删，按用户与时间范围），`ScheduleView` 重写为自绘 CSS 月历（月份切换、今天高亮、类型色点、选中日期事件面板、编辑对话框），工作台「即将到来的面试」面板读取真实日程。v0.1 的日程事项不再停留在空壳。
- **简历 Markdown 导入已落地**：`parseMarkdownResume` 确定性解析（章节、基本信息、经历/项目条目、技能、证书、语言），导入流程为「文件选择 → 解析预览 → 确认创建」，解析结果不可识别时只提示不创建。PDF 导入留待后续版本。
- **简历库改为开放式简历工作台**：左侧功能坞「简历」页展示最近简历的 CSS 拟 A4 纸缩略图（标题、角色、版本徽章、最近更新），点击直达最新版本；「导入 Markdown」入口常驻。
- **目标岗位录入优化**：岗位名称与公司字段新增热门 chips（腾讯、字节跳动、美团等 15 家公司 + 8 类常见岗位），输入时按关键词过滤建议，点击即填入；纯本地静态数据，不引入岗位库或爬虫。
- **主工作台布局与日历 tile 精修**：头部与岗位条间距拉开、日历 tile 图标恢复正常渲染，模拟面试介绍区移除、「查看成长趋势」按钮移至右下角。
- **工作台首页改为 desktop-first 个人求职工作台**：去掉居中的 SaaS 卡片网格感，按设计简报重做空间构成与层级——安静元数据头部（不引入新大标题）、当前目标拆为 identity/progress 两层、主列 + 右侧工具栏的不对称布局、仅「即将到来的面试」保留克制表面（56rem 上限防超宽空盒）、反馈文本限宽 42rem；工作台路由启用流体内容列，其余页面保持 1224px 居中。1280/1440/1600/1920/1100 五档视口验证均无文档级滚动。
- **工作台首页第二轮：任务导向工作台**（`TargetDashboard.vue` 全面重构）：围绕唯一上下文「下一步」组织首页——当前目标对象身份（极淡 `--surface` 上下文带，无边框无阴影）、纯排版式「即将到来」（14:30 为 44px 视觉锚点 + 静态倒计时）、右栏重定义为「准备」状态面板（简历/模拟面试/日程三行，状态由真实数据推导）、Next Action 按准备缺口四档规则切换且为唯一填充按钮。删除「工作台」pill、收敛日历 tile 为文字控件、弱化「录入岗位」、右栏去分隔线。单列断点（≤1120px）纵向压缩并调整 media query 顺序，1100×800 恢复无文档滚动。
- **工作台首页第三轮：紧凑工作台精修**：修复 over-spacing / under-grouping——上下文带压缩到 110–125px（progress 改为内容宽度）、upcoming 空态从整栏虚线大盒收敛为一行 `近期没有面试安排 · 添加日程 →`、无 upcoming 时 Next Action 自动升级为「当前准备」主焦点（无大空壳、页面始终有明确视觉焦点）、准备面板改为三个紧凑 object row（图标 + 标题 + 元数据 + 语义状态 + 单动作，次入口走 ⋯ 渐进披露，语音面试收进菜单）、垂直节奏按 8-12/16-24/28-36/40-48 层级收紧。无新增填充性模块；五档视口仍无文档级滚动。
- **工作台首页第四轮：全局工作台（Dock | Agenda Workspace | Inspector）**：首页改为全局视角，取消首页的 global active target。toolbar 为 `8月20日 星期四 · 录入岗位 · 日程 →`（无「今日工作区」标题）；主区是 Agenda timeline——按日期分组（今天/明天），选中事件通过语义指示器（实心/空心圆点）、字重与克制的行内着色表达，不做大尺寸卡片；Inspector 回答四问（这是什么 / 准备得怎样 / 下一步做什么 / 去哪里深入），Next Action 移入 Inspector。删除 Current Target Banner、全局当前目标、独立 Next Action section 与 dashboard 式准备卡片；最近活动保留在左下。选中事件切换时 Inspector 身份、准备状态（简历/模拟面试/日程 readiness）、Next Action 与该事件对应目标同步；未关联目标的日程如实显示「未关联求职目标」并只给「查看日程」，不虚构准备数据。空态为紧凑单行（`近期没有安排 · 添加日程 →`），无大型 empty-state 矩形。五档视口（1280/1440/1600/1920/1100）light 与 dark 均无文档级滚动。
- **工作台首页第五轮：严格执行 37 节 UI 规范（`Dock | Main Workspace | Inspector` 空间模型）**：按确认的规范逐节实现，不再自行解释方向。Dock 收窄到 72px（x=18/y=16/高 `calc(100vh-32px)`/radius 28/`#0A0A0B`，选中态 44×44 白面 radius 13）；Workspace 左侧锚定（1440 起 x≈124、toolbar 28px、max-width 1280，1600+ 1320）；toolbar 52px 高无卡片无标题；Primary Event 为唯一主 Surface（40px/300 的 `09:50` 全页最大视觉元素 → `字节跳动 · 笔试` → 倒计时 + 查看日程，hover 仅微变底）；「稍后」agenda 行 + 每行语义圆点（最临近场次品牌绿）；最近活动单条（≤70px 空态两行）；Inspector 328px 为空间区域（`#F9F9F7` + border-left），三行准备 object row（简历/模拟面试/日程各一 contextual action）→ 唯一黑底主按钮 → `目标详情 →`；绿色=状态色、黑色=主操作色。垂直节奏只用 4/8/12/16/20/24/28/32/40；radius 三档 8-9/11-12/16-18；shadow 仅 Primary + Dock。Dark 只换 token（canvas `#111212`、主按钮反色为浅底黑字）。响应式：1280→inspector 300、1100-1180→280、`<1050` Inspector 变 right drawer（点击事件从右滑入）；900px 高度不滚动、800px 高度 compact（Primary 156-168、section 间距 28→22/34→26）。五档视口 light/dark 均无文档级滚动；未关联事件不虚构准备数据。
- **工作台首页 Master–Detail 桌面布局（`Dock | Agenda Rail | Focus Workspace`）**：废弃 Workspace + Inspector 空间模型，按确认简报严格执行，页面只保留三个一级空间。1440 基准：Dock 72px、Dock→应用 gap 32px、应用 max-width ≈1270px **左对齐 Dock 不居中**、Agenda Rail 310px、Divider 1px、Focus Workspace `flex:1`。Agenda Rail 为整块 subtle pane surface（非多个 Card），upcoming 按日期分组（今天/明天/N 天后），每个 event 为 navigation row（约 86px 高 / 12px radius / 极浅 selected 背景）；删除 100% 宽 Primary Event Card。Focus Workspace 展示选中事件详情，顶部直接 `明天 / 09:50 / 字节跳动 · 笔试 / 还有 18 小时`（时间 48px/300、事件名 21px/600、静态倒计时）。未关联 target 只显示真实信息（`未关联求职目标` / `关联目标 →` / `查看日程 →`），不虚构简历/模拟面试准备状态；「关联目标 →」打开真实目标列表对话框，选择后 PATCH 关联、详情切换关联分支（identity → 准备 compact object row → contextual next action + `目标详情`）。最近活动移入 Detail Workspace 底部；删除首页「使用帮助」；不再创建 Inspector / Primary Event Card / 额外 Dashboard module。浏览器验证：五档视口（1280/1440/1600/1920/1100）均无文档滚动、1600/1920 下应用保持 1270 封顶且 Dock 仍左对齐；「关联目标 →」端到端真实 PATCH 验证通过；dark tokens 审计通过（canvas `#111212`/rail `#161717`/selected brand-soft `rgba(71,181,142,.11)`）；截图存档 `docs/audits/round7-home-{light,dark}-1440.png`。
- **工作台首页桌面精修（第 8 轮反馈）**：Dock 收窄到 56px（图标 20px/40px 命中区/radius 22，内容起始 x=106）；准备区改为垂直时间轴（图标节点 + 串联线，ready 节点品牌色填充、末行无线）；去重岗位词（rail 行 meta 只保留时间、linked identity 不再重复角色行、next 文案改为「针对当前目标」）；「开始模拟面试」旁新增 `预计 20 分钟` 耗时提示；Detail 改为两列平衡布局（左列 identity + 准备时间轴、右列 下一步 + 目标详情），垂直足迹减半保证 800px 高度窗口一屏展示。五档视口（1280/1440/1600/1920/1100）doc 与 detail 均零滚动；unlinked 分支保持单列与真实信息；dark tokens 审计通过；截图存档 `docs/audits/round8-home-{light,dark}-1440.png`。
- **工作台首页时间轴收尾（第 9 轮反馈）**：Detail 从两列改回单列纵向——用户反馈「下一步」单独放右边很挤、要求顺延在下面；内容流恢复 identity → 准备时间轴 → 下一步（含 `预计 20 分钟` 提示）→ 目标详情 → 最近活动，body 限宽 640px 保持克制不空旷。最左侧 Agenda Rail 的事件列表改为时间轴：每组事件行左侧 8px 节点圆点（选中 10px 品牌实心、未选中灰）+ 1px 串联线（首行线从节点起、末行无线），同日期多场次串联呈现「多场面试时间轴」。1440×900 与 1280×800 均一屏零滚动；第 8 轮的去重、准备时间轴、Dock 56px 全部保留；截图存档 `docs/audits/round9-home-{light,dark}-1440.png`。

## v0.1：可信的本地简历工具

目标：即使不调用 AI，也是一款可以日常使用的本地简历工具。

- 无默认个人数据的首次启动流程；
- 创建和管理求职项目；
- 能力证据录入与编辑；
- 简历编辑、版本保存和差异查看；
- 目标岗位粘贴与本地保存；
- PDF 导出和数据备份；
- Electron 桌面壳、Spring Boot 本地进程和 H2 文件数据库；
- Windows 安装包。

退出条件：普通用户不安装 Node.js、Java 或 MySQL，也能完成安装、创建项目、编辑、导出和卸载。

## v0.2：证据驱动的 AI 修改

目标：AI 能够提高修改质量，同时不替用户制造事实。

- JD 结构化解析；
- 问题、岗位差距与未知项展示；
- 建议引用具体能力证据；
- 原文、依据、建议稿和差异审查；
- 接受、局部修改、拒绝和纠错；
- 用户确认后生成新版本；
- 模型超时、失败、非法输出和重试体验；
- API Key 的系统安全存储。

退出条件：固定测试材料中的每条 AI 建议都有证据或明确的证据缺失标记，异常输出不会写坏用户数据。

## v0.3：面试反馈闭环

目标：面试练习能产生下一轮准备行动，而不是一次性聊天记录。

- 面试绑定求职项目、岗位和简历版本；
- 清晰的创建、进行、中断恢复和复盘流程；
- 单题反馈与整次复盘；
- 将问题转化为补充证据、简历修改或练习事项；
- 反馈驱动下一版简历；
- 历史记录清理和数据删除。

退出条件：用户可以从一次面试复盘创建明确的下一步行动，并在新简历版本中看到反馈如何被处理。

## v1.0：可持续发布的个人产品

目标：通过 GitHub Releases 面向真实用户稳定发布和维护。

- Windows 与 macOS 安装包；
- 安装包签名和基础安全检查；
- 应用内更新或明确的更新提示；
- 数据格式版本、备份恢复和迁移验证；
- 崩溃恢复与用户可理解的错误报告；
- 完整隐私说明和第三方服务披露；
- 自动构建、测试、发布和变更日志；
- Issue 模板、贡献说明和维护节奏。

退出条件：全新设备能够安装和升级，旧版本用户数据能够迁移，核心流程通过自动化回归，发布说明能够解释用户可感知的变化。

## 排序规则

进入开发前，每个事项至少回答三个问题：

1. 它是否让核心求职闭环更完整？
2. 它是否提高数据可信度、隐私或可恢复性？
3. 它是否减少用户完成任务所需的理解和操作成本？

无法回答其中任何一项的功能，不进入当前版本。

- **工作台首页时间轴精修 + 准备区文字/按钮合排（第 10 轮反馈）**：Rail 时间轴从「只加了一个点」重做——每日期组变为里程碑行（小圆点 + `明天 / 2 天后 / 3 天后` 标签），事件节点升级为 28px 类型图标圆（面试对话/笔试笔/跟进时钟，选中品牌绿实心反白图标），2px 轴线改为贯穿每个元素盒的连续线（`::before` 左对齐节点列中心），`.agenda-group` 改 `display:contents` 使轴线跨组不断；首里程碑节点起、末事件节点止。准备区修复「文字和按钮分开」：动作按钮从最右 `auto` 列移入 copy 块、与标题同排成紧凑描边 pill（24px），sub-meta 并入单行 meta（`算法工程师简历 · V4 · 7/15 更新`），准备时间轴连线同步升级 2px 且首尾收口。五档视口（1280/1440/1600/1920/1100）doc/rail/detail 均零滚动；选中切换、unlinked 分支、dark tokens 验证通过；截图存档 `docs/audits/round10-home-{light,dark}-1440.png`。- **工作台首页：功能性按钮恢复旧版 + 下一步按钮左对齐（第 11 轮反馈）**：时间轴保留；准备区（简历/模拟面试/日程）行尾按钮从第 10 轮描边 pill 恢复为旧版文字链接（`grid-template-columns:28px minmax(0,1fr) auto` 三列，按钮在 `auto` 列、`margin-top:19px` 对齐标题行，`border:0;color:var(--copy);font-size:13px;font-weight:500`，hover 变 `--ink`）。「开始模拟面试 · 预计 20 分钟」不再靠右——组件 `.next-actions` 显式 `justify-content:flex-start`（根因：`src/style.css` 存在全局 `.next-actions{justify-content:flex-end}`，scoped 规则未声明 justify-content 时被全局接管）；顺带修复 `.workbench-dashboard button{font:inherit}` 把全部工作台按钮字号重置为浏览器默认值的问题，改为 `font-family:inherit`。五档视口零滚动、双主题验证通过；截图存档 `docs/audits/round11-home-{light,dark}-1440.png`。- **工作台首页：左列响应式收窄 + 右列内容统一宽度（第 12 轮反馈）**：窗口变窄时左列从 310px 分档收窄（`max-width:1219px`→285、`max-width:1099px`→260 并降级 rail 字号：标题 15→14、行标题 15→14、行 meta 13→12、里程碑标签 13→12、行/里程碑内边距收紧、`max-width:959px`→240），宽度档块置于既有 `max-height:899px` 紧凑块之前以保留高度优先级——1000px 宽窗口下左列占比从 35.6% 降到 30.2%、文字 15→14px；右侧 `.detail-header` / `.detail-body` / `.activity-zone` 统一 `max-width:720px`（原无/640/620），header 不再撑满整个面板、三块右缘对齐，全屏下内容列占面板约 80% 不再松散。八档视口（1440/1280/1200/1100/1000/960/920/850）零横向滚动、rail 标题零截断，双主题截图存档 `docs/audits/round12-home-{light-1440,light-1000,dark-1440}.png`。
- **第 13 轮：全应用语义 token 统一 + 四页 master-detail 化 + 面试大厅 composer 化 + 面试绑定一致性修复**：`src/style.css` 成为全局语义 token 唯一来源（浅色 `:root` + 暗色 `body[data-theme='dark']`，覆盖 canvas/surface/elevated/subtle/hover/selected/ink/copy/muted/border 系列/brand/accent-soft/accent-hover/danger/danger-soft/warning/warning-soft 与 `--radius-control:10px/--radius-panel:16px/--radius-large:22px`），重构页面删除分歧 fallback。`DesktopShell` 新增 `meta.fill` 全高变体（`height:100vh;overflow:hidden`，去掉 1224px 居中窄容器），目标/简历/日程/设置四页启用、页面文档滚动清零；抽 `PageHeader` 共享组件；断点统一到 1219/1099/959 共享阶梯。求职目标页重写为 master-detail（300px 目标列表 + 右侧详情面板，接通死代码 `route.query.targetId` 预选、新建求职目标入口、设为当前/重命名/归档/删除）。简历库改为书架 + 360px Inspector（继续编辑主操作 + 概况 + 版本历史，删除底部 1+3+1 卡片堆叠）。日程页去浮卡（平表面 + 1px 分隔 + 事件行类型圆点）。设置页改桌面设置（左侧节导航 + 常规/AI 两节 inspector 化）。面试大厅 → Session Composer：页面平底 `--canvas`、上下文卡/计划预览/面试官卡改「节标题 + 分隔线 + 克制行」、面试官头像中性单色（选中品牌描边）、一个主操作「开始面试」；会话房间与三个子组件（`InterviewHistoryPanel/InterviewRoomSidebar/InterviewPlanReviewDialog`）硬编码色全部 token 化（`#101a33→var(--brand)`、`#10b981→var(--brand)`、琥珀 retry→`--warning`、渐变→平表面）。修复「开始模拟面试绑定与主界面不一致」：大厅改为目标驱动绑定（`from=target` 且目标有效时以目标实体的 `jobDescriptionId`/`resumeVersionId` 为准，校验不存在则明确未绑定态）、`WorkbenchView` 缺 JD/简历时也带目标上下文 push、`open-feedback` 不再误进创建大厅。验证：118 测试全过、build 通过；浏览器五页 light/dark × 1440/1100 截图存档 `docs/audits/round13-{page}-{light,dark}-{1440,1100}.png`，四页零文档滚动、900px 折叠正常，暗色截图像素扫描零遗留硬编码色。
- **第 14 轮：收敛阶段——三页面模板 + token 表 + 表达层收敛（不再逐页大改）**：五页锁定为三种模板（A Timeline/List→Workspace 首页 / B Library/Master→Inspector 目标·简历·日历·设置 / C Composer→Inspector 模拟面试），收敛 token 表与 `--space-1..8` spacing 刻度写入 `docs/decisions.md` 与 `docs/design-qa.md`；`PageHeader .page-title` 26→28px。首页只细修三个点位：rail 310→290px（响应式 270/248/228，实测零截断）、已关联事件头部去重复岗位行改「关联目标 · 目标详情 ›」行（未关联事件保留真实 role 行、绝不虚构目标）、准备区去贯穿竖线改 32×32 中性容器 + 右下角绿色 ✓。求职目标标题下加 identity/meta 三行（岗位/状态·近期安排·简历版本/最近更新），删除「已是当前目标/设为当前目标」按钮（`activeTargetId` 数据兼容保留）。简历库 shelf 980→780（3 列、实测卡宽 247 / 纸面 247×329 落在 230–250×310–330），绿色去装饰化（band 渐变→1px 平线、V 徽章中性化、inspector-kicker 与 import 弹窗装饰绿→中性），selected 绿边/primary/active 保留。日历密度 +15–20%（cell 70→58、gap 6→4），selected day 去绿边改极浅 selected 表面 + 26px 日期绿圆白字。面试大厅 Composer→Inspector（唯一结构性改动，状态机零碰）：左列简历/目标两行（未锁定时 el-select、目标绑定时静态值 + 从工作台更换）、题目 slider 行内、面试官 12 卡网格改水平队列（↑↓×）、重点方向与开始按钮保留；右列 `InterviewHistoryPanel` 重写为「本次面试」四行摘要 + 「最近面试」扁平记录行（filter tabs、状态圆点、`X/X 轮`、查看复盘/继续面试、删除），QUICK ACCESS/HISTORY 英文 eyebrow 与卡套卡全删，`history-*` 测试钩子与空态文案保留。验证：120 测试（+2 新增）全过、`vue-tsc --noEmit` 无错、build 通过；浏览器五页 light/dark × 1440/1100 实测：首页 rail 291px（290+1 divider）、1100 档 271px 无截断；目标页 meta 行 `进行中 · 当前目标 · 2 场近期安排 · 简历 V4`；简历卡 247×329；日历 cell 58px；面试大厅 composer 812px + inspector 360px、plan-summary 4 行、无英文 eyebrow；截图存档 `docs/audits/round14-{page}-{light,dark}-{1440,1100}.png`（20 张）。已知偏差（留待下一轮统一 spacing/token）：首页 rail 行高 86px、Page top 16px 未对齐 token 表。
- **第 15 轮：产品逻辑补全 + 空间密度 + 绿色语义（按用户 P0/P1/P2 执行）**：P0 三处产品逻辑——① Settings 去硬编码模型：空态两栏（中间 providers 栏 collapse，文案「使用你自己的 API / 尚未配置任何服务 / AI 功能只会在你主动使用时调用已配置的服务」+「＋ 添加 AI 服务」）；添加服务流程只含服务商 select + 配置名 + API Key +「验证并继续」（普通用户不接触 Base URL/OpenAI Compatible），验证成功后才经 `fetchAiProviderModels` 填模型下拉（不预设 `deepseek-chat`），高级设置（协议/Base URL/Model ID）默认折叠。② 求职目标页 字段详情 → 求职项目工作台：右栏四段（identity 公司/岗位/进行中·最近更新 → 准备进度三列 简历 V4·打开简历/模拟面试 N 次·查看复盘/日程 N 场·查看日程 → 下一场 时间+标题+反馈驱动建议·开始准备 → 岗位信息 JD 已录入·查看岗位）+ 底部克制操作行；左列行升级为 岗位 + 场次·简历版本·下一场摘要；接入 `listMyInterviewPlans`/`listScheduleEvents` 真实数据。③ 面试大厅 Session Composer 化：针对区简历/目标双「更换」按钮（回工作台换绑）、面试官小卡（头像/名字/角色 + 擅长方向 style 行）、右栏 Inspector「本次练习」上下文卡（目标/简历/面试官/题目/预计 X–Y 分钟 = `estimateSessionMinutes` 3n~4n）、「最近面试」记录行加面试官顺序（张老师 → 李架构）；无「技术面」标签（无数据源）。P1 空间密度——首页 rail 改可拖拽 Split Pane（默认 330 / 最小 270 / 最大 420，1px hairline + 4px 命中区，pointer 拖拽 + 双击恢复 330 + 键盘 ±8，覆盖第 14 轮 290px 决策）；rail <290 时 compact 行内联时间（`8月21日 字节跳动 · 笔试`，隐藏日里程碑）；简历库 shelf `minmax(210px,1fr)` + gap 24（实测卡宽约 214×285 落在用户规定区间）；Resume Inspector 上下文化（identity → 继续编辑唯一实心绿 → 「用于」经 `resumeVersionId` 反查目标 + `getJobDescription` 显示 `腾讯 · Java 后端实习`，JD 无公司名时保留目标名公司前缀 → 内容 项目经历 X 条/技能 Y 项，如实省略页数 → 最近版本前 3 个 + 查看全部）；日程事件行加已关联目标上下文「目标详情 ›」（未关联事件不变）；FirstRunEmptyState 4 步自然顺序（录入岗位 → 关联简历 → 添加日程 → 针对性模拟，CTA「录入第一个岗位」）。P2 绿色语义——每视口唯一 solid-green Primary（面试房间「提交回答」/简历库「继续编辑」/证据库「保存证据」/目标页「新建求职目标」/日程「添加日程」/设置流程主操作），聊天发送、新增证据、创建空白简历等改中性；删死 `.primary-green` 全局 CSS；抽查「查看/编辑/详情/更多」全部浅文字式。验证：136 测试（+16）全过、`vue-tsc -b` + build 通过（修复 ScheduleView/TargetListView 测试夹具 TS 错误）；浏览器双主题 × 1440/1100 实测：rail 330 默认/拖拽钳制 270/420/双击复位/compact 切换、关联目标行 `腾讯Java后端实习 目标详情 ›`（无岗位重复）、简历 Inspector「用于 1 个求职目标 腾讯Java后端实习」、日程 21 日事件上下文行、设置空态两栏 + 添加表单无模型/URL 字段、1100 宽零横向溢出；截图存档 `docs/audits/round15-*.png`（12 张）。已知偏差（留待统一 spacing 轮）：首页 rail 行高 86px、Page top 16px（继承自第 14 轮）。


---

# Career OS Evolution Roadmap

Version: V2.0

Status: Approved (2026-08-21, see docs/decisions.md)


## 产品阶段转型说明


ResumeGo 当前阶段已经完成：

- 本地优先桌面架构
- 求职项目基础模型
- 简历版本管理
- 模拟面试基础流程
- 日程管理
- Master-Detail 应用结构


经过产品重新定义，ResumeGo 将升级为：

职达 Career OS。


产品定位从：

AI Resume Assistant


升级为：

AI Career Development Operating System。


后续开发重点不再扩展孤立功能，而围绕：

Career Pipeline

Resume Version

Knowledge Base

Interview Engine

Growth System

构建完整职业成长闭环。


---

# V1.0 Career OS Core

目标：

建立稳定的职业成长基础系统。


## Career Pipeline

状态：

进行中。


目标：

从单一求职项目模型升级为多目标职业管线。


包括：

- 多岗位并行管理
- 阶段追踪
- JD上下文
- 简历版本绑定
- 面试记录关联


完成标准：

用户可以同时管理多个求职目标，并清晰了解每个目标当前状态。


---

## Resume Version System


目标：

完善岗位适配型简历体系。


包括：

- 基础简历
- 岗位专属版本
- 版本比较
- 修改来源追踪
- AI建议关联


完成标准：

用户可以针对不同岗位维护独立简历版本。


---

## Interview Engine Basic


目标：

形成完整模拟面试流程。


保留已有：

Role Based Interview。


新增：

- 面试会话管理
- 面试历史
- 基础复盘
- Feedback Record


完成标准：

一次面试可以完整经历：

创建 → 进行 → 记录 → 反馈。


---

# V1.5 Knowledge Driven AI


目标：

让 AI 从聊天工具升级为个人职业助手。


## Knowledge Base


新增：

用户职业知识库。


支持：

- 技术资料
- 项目文档
- 面经
- 学习笔记
- 错题记录


能力：

- 文档解析
- 分类管理
- RAG检索


---

## AI Interview Training


新增：

### Knowledge Training Mode

基于用户资料进行训练。


### Experience Simulation Mode

基于真实面经和题库进行模拟。


区别：

AI 不只是生成问题，而是组织真实训练流程。


---

# V2.0 Growth System


目标：

让用户看到自己的能力变化。


新增：

## Growth Dashboard


展示：

- 学习投入
- 模拟面试趋势
- 技术能力变化
- 表达能力变化
- 项目深度变化


---

## Feedback Loop


形成：

面试

↓

分析

↓

弱点

↓

训练

↓

再次面试


完整闭环。


---

# V2.5 Career Agent


目标：

探索 AI Agent 能力。


新增：

## Skill / MCP


提供：

- 查询个人简历
- 分析岗位
- 总结面试
- 制定学习计划


作为外部 AI 调用入口。


---

## Career Agents


探索：

### Job Analysis Agent

分析岗位需求。


### Resume Agent

辅助简历优化。


### Interview Coach Agent

制定训练计划。


### Learning Planner Agent

安排成长路线。



---

# Future Exploration


以下功能暂不进入核心开发：

## 智能投递辅助


方向：

岗位分析

简历匹配

投递记录管理。


暂不实现：

自动批量提交。


原因：

自动投递不是 Career OS 核心价值。


---

# Development Priority Rule


所有新功能必须回答：


1. 是否增强职业成长闭环？

2. 是否提升数据可信度？

3. 是否降低用户完成目标的成本？


无法满足以上条件的功能：

不进入当前版本。


---

# Current Architecture Direction


未来开发必须遵循：


Career Pipeline

管理目标。


Resume Version

管理表达。


Knowledge Base

管理知识资产。


Interview Engine

管理训练。


Growth System

管理反馈。


五者共同组成：

Career OS Core。

