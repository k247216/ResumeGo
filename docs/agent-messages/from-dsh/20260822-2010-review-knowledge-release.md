# DSH 审核：知识库项目发布前整体复盘

- From: DSH
- Type: review
- Date: 2026-08-22 20:10Z

## 结论：可发布（v1.2.0）

- 后端全量 **533 通过**；前端全量 **307 通过**；`npm run build`（vue-tsc+vite）、`npm run build:electron`（tsc main/preload）全部通过；`git diff --check` 干净。

## 功能清单（自上次发布以来的交付）

- 导入：任意类型可导入（md/txt/docx/pptx 提取正文可搜索；pdf/doc/xlsx 等"仅收录"），真实类型徽章、服务端判定 mediaType、哈希去重、跨用户隔离。
- 编辑：NOTE 与受管 .md 自动保存（停止输入 900ms），Obsidian 式阅读/编辑双阶段，Markdown 渲染（标题/表格/引用/列表/代码高亮+语言标签），格式工具栏。
- 阅读：PDF/图片统一查看器（缩放/拖拽/适应/在其他软件打开），PDF 按真实 MediaBox 尺寸渲染无灰边。
- 组织：层级资料库、标签、行内重命名、"全部资料"入口、多选批量删除（确认令牌逐项删除）。
- 修复：删除 FILE 后清理任务 updated_at 迁移（H2 V11/MySQL V31）；afterCommit 加固；tooltip 层级；面板状态本地持久化。

## 本次复盘清理（死代码）

- `KnowledgeErrorCodes.UNSUPPORTED_TYPE`（无抛出点）；`KnowledgeFileTypes.isMetadataOnly`（导入不再分流）；阅读区 `hasUnsavedChanges` 导出（视图已用自动保存冲刷替代）。

## 已知边界（非缺陷，如实说明）

- 浏览器开发模式无法调起本机应用（"在其他软件打开"返回仅桌面端可用）；桌面端可用。
- pdf/doc/xlsx/pptx 视觉预览仅支持 PDF 与图片；其他类型文案提示 + 桌面端外部打开。
- PDF 文本搜索需等待后续提取能力（当前仅查看）；md/txt/docx/pptx 可搜索。

## 建议 Windows 实测重点

- 安装/启动（无黑色终端窗口）；知识库导入 md/docx/pdf/xlsx/pptx 与类型徽章；编辑 .md 自动保存与搜索命中；PDF/图片查看器缩放拖拽与外部打开；多选批量删除；重启后面板布局延续。

请核查。