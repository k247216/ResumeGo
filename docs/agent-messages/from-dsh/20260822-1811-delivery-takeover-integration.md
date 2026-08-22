# DSH 交付：接管集成 + Knowledge 编辑/类型/工具栏收口

- From: DSH
- Type: delivery
- Related: V2-F2-BE-06 / BE-07 / FE-02 follow-up / 协调者授权接管
- Date: 2026-08-22 18:11Z

## 背景

Core Controller（Codex）暂无法集中审核。经协调者明确授权，DSH 临时承担提交与集成职责，并在集成分支 codex/v2-career-os 上直接完成剩余工作。

## 提交记录（集成分支 codex/v2-career-os）

- `8a6bd77` 提交 Codex 未提交的 UI 改动：资料库文件夹/标签图标、文件类型彩色徽章、知识库一级 Notebook 图标（含新增测试）。
- `91e74fa` 归档 Codex 的任务卡/评审/指令消息。
- `2622b07` merge codex/v2-f2-be-07-import-type-contract：一次性集成 BE-06（受管 Markdown 编辑器 f167aa3+ceca40b）与 BE-07（类型识别收口 f68afdd），零冲突。
- `ab9ec58` 本轮 UI 收口（见下）。

## 集成后的后端能力（2622b07）

- KnowledgeDocumentResponse 新增 sourceExtension；list/detail/search/recovery 全部返回真实小写扩展名（缺失→unknown）。
- PUT /documents/{id}/content 按类型分派：NOTE 直写提取正文；FILE+md 原子替换受管副本并同步正文/大小/哈希/更新时间（失败回滚、hash 冲突诚实拒绝）；TXT/未知 NOT_EDITABLE。
- 导入：md/txt 提取正文 COMPLETED；pdf/doc/docx/unknown 落安全副本+元数据，FAILED+UNSUPPORTED_FORMAT，服务端判定 mediaType 写入 mime_type。

## 本轮前端收口（ab9ec58）

1. **左侧资料库/标签工具栏**：区头字号 12→13px、加分隔线成工具栏式头部；树/标签项 13→14px、加高行距与悬停态。
2. **列表类型图标**：28px 大图标收敛为 20px + 紧凑类型徽章；unknown 扩展名诚实显示为 FILE（不伪造 PDF/DOCX）。
3. **真实类型展示**：集成后 sourceExtension 真实回流，列表按 md→Markdown、txt→TXT、pdf/doc/docx/ppt/pptx/xls/xlsx→真实扩展名与对应彩色徽章展示。
4. **Obsidian 式直接编辑**：NOTE 与受管 .md 打开即处于文档式编辑器（无“编辑”按钮、无大输入框切换）；标题行内编辑；脏状态显示底部浮动保存条（保存更改/放弃修改），保存仍显式触发、失败保留草稿并显示真实错误。TXT/未知/未完成保持只读渲染视图。
5. **属性栏自定义选择框**：新增 KnowledgeSelect（按钮+浮层选项、选中对勾、点击外部关闭、Esc 关闭），替换分类与添加标签两个原生 select。
6. **面板状态本地持久化**：资料库/列表/属性栏收起状态写入 localStorage，下次打开延续上次布局。

## 验证

- 后端全量 mvn test：531 通过，0 失败（基线 511 + BE-06/BE-07 集成新增）。
- 前端全量 npm test：296 通过（60 文件）；npm run build（vue-tsc+vite）与 npm run build:electron 均通过；git diff --check clean。
- 交互覆盖：直接编辑脏状态守卫（切换文档弹未保存确认）、面板收起→持久化→重开恢复、自定义选择框选项选中、真实类型徽章（md/txt/pdf/docx/epub）均有组件/视图测试。

## 待办（需真实桌面运行）

- 按目标图在 1440×960 与 1080×720（浅/深色）做视觉对照与真实交互验收（规格 2026-08-23-v2-knowledge-library-editor-redesign.md Verification 节）。
- V2-F2-QA-01 纵向验收卡仍在队列，建议桌面验收后执行。

请核查。
