# DSH 交付：PDF 缩放拖拽查看 / PPT 文本提取（第 7 轮）

- From: DSH
- Type: delivery
- Related: 协调者 3 点反馈
- Date: 2026-08-22 19:10Z

## 提交

- `49f2a55` 本轮改动（7 文件）。

## 1. PDF 浏览：放大 + 拖拽平移

- 原生 iframe 插件拖拽不可靠，改为**自定义预览容器**：iframe 置于可缩放舞台内，顶部工具条提供 − / 百分比 / ＋ / 适应；按住拖拽平移（grab 光标），Ctrl+滚轮缩放；iframe 加 pointer-events:none 让事件全部落到外层容器（实测 elementFromPoint 命中 stage）。
- 实测：缩放按钮 100%→150%（stage scale(1.5)）、适应→66%、拖拽逻辑经真实 DOM 事件验证可平移。

## 2. PPT 是否需要展示？—— 我的建议：提取文字，不渲染幻灯片

- **PPTX 文本提取已实现**：JDK 内置 zip+XML 汇总 ppt/slides/slideN.xml 的 <a:t> 文本，按幻灯片分段 → COMPLETED + 可搜索正文。实测"第一页：项目汇报 / 第二页：成果数据"正确提取。
- 幻灯片**视觉预览不建议做**：需要完整渲染引擎（PPT 布局/字体/形状），在知识库内成本极高且收益低；正确做法是文字可检索 + 桌面端"打开原文"用 PowerPoint 查看。
- PPT（旧二进制格式）无 POI 无法提取，保持"仅收录"。

## 3. 上传文件是副本，存在哪里？

- **是副本**：导入时后端把文件字节复制到应用私有数据目录，文件名改为内容哈希（`sha256.扩展名`），原文件路径从不读写。
- **位置**：`<数据目录>/knowledge/sources/{用户ID}/{sha256}.{扩展名}`；桌面版数据目录 = `~/Library/Application Support/ZhidaCareerOSPreview/workspace`；浏览器验证环境 = `backend/data/resumego/knowledge/sources/1/`。
- 同内容 = 同哈希 = 只存一份（去重）；编辑 .md 原子替换受管副本（哈希路径不变量）、原文件不动；删除时清理副本。全部本地，无云端。

## 验证

- 后端全量 533 通过（+pptx 提取集成测试）；前端全量 307 通过、构建通过。
- Electron 实测：PDF 查看器缩放/适应/拖拽、pptx 导入 COMPLETED 提取两页文字。截图 docs/audits/round22-knowledge-light-{1440x960,1080x720}.png。

请核查。