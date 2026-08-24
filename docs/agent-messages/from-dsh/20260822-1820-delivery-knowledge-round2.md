# DSH 交付：知识库图标/编辑体验/工具栏/导入与删除修复

- From: DSH
- Type: delivery
- Related: 协调者需求（书本图标、Markdown 双阶段+自动保存、工具栏成熟化、Word/PDF 导入、删除 bug）
- Date: 2026-08-22 18:20Z

## 提交

- `bc43113` 本轮全部改动（24 文件）；前置 `baad6ff` 为上一轮文字/间距收口。

## 1. 知识库图标 → 书本形状

- 新增 `BookIcon.vue`（Lucide book-open 风格 open-book SVG），替换全局工具栏与页面标题栏的图标；`DesktopShell` 测试断言同步为 Book。

## 2. 正文 Markdown 双阶段 + 自动保存（Obsidian 式）

- **阅读阶段**：渲染真实 Markdown（标题大小、加粗、列表、代码），默认进入。
- **编辑阶段**：源码编辑 + 下方实时预览；可编辑范围仍是 NOTE 与受管 .md。
- **自动保存**：停止输入 900ms 后自动落盘（标题行内改名同样自动保存）；页头显示 保存中…/已保存/保存失败；切换文档前自动冲刷未保存改动，**删除未保存弹窗流程**。
- 只读 TXT 保持渲染视图；PDF/DOC（METADATA_ONLY）显示诚实提示，不假装有正文。

## 3. 左侧工具栏成熟化

- 树增加缩进引导线；操作按钮改为图标（Plus/EditPen/Close）；区头 hover 显示动作、标签选中品牌色胶囊；整体 184px 与全局工具栏 16px 间距。

## 4. Word(.docx)/PDF 真实导入

- DOCX：JDK 内置 zip+XML 提取 `word/document.xml` 段落文本 → COMPLETED + 可搜索正文（只读不可编辑）。
- PDF/DOC：导入成功并落安全副本与元数据，文档状态 **METADATA_ONLY**（新增状态，前端显示"仅收录"），诚实提示暂不支持文本提取；不再 FAILED。
- 未知类型保持 FAILED+UNSUPPORTED_FORMAT。
- 实测：真 docx 导入 COMPLETED 并正确提取 3 段；pdf 导入 METADATA_ONLY。

## 5. 删除 bug 修复

- 根因：`knowledge_cleanup_jobs` 表缺 `updated_at` 列（生产迁移 V9/V29 未含），删除 FILE 文档时清理任务状态回写报 BadSqlGrammar → 事务已提交（文档已删）但请求返回 500 → 前端"删除失败"而文档已消失。
- 修复：新增迁移 H2 V11 / MySQL V31 补列；`afterCommit` 整体 try/catch 加固，清理失败不再反噬为删除失败。
- 实测：删除 FILE 文档 200 成功、文档/文件均清理、无后端错误。

## 验证

- 后端全量 532 通过（新增 docx 提取集成测试、pdf METADATA_ONLY 测试）；前端全量 298 通过；`npm run build` 通过。
- Electron + 真实后端实测（1440×960 / 1080×720）：书本图标、阅读渲染、编辑+预览、自动保存"已保存"状态、PDF"仅收录"提示、无横向溢出；截图 docs/audits/round17-knowledge-light-{1440x960,1080x720}.png。

请核查。