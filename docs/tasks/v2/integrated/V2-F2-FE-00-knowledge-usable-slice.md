# V2-F2-FE-00：Knowledge Library 可用前端切片

## Identity

- Status: `INTEGRATED`
- Branch: `codex/v2-f2-fe-00-knowledge-usable-slice`
- Base commit: `65c25c09396c5ce3c9574b8d856011ae37bac334`
- Depends on: integrated F2-BE-01; IO-01 endpoint contract is frozen and may be mocked until integration

## User result

用户从桌面左侧进入“知识库”，可以看见真实本地资料列表、创建一条笔记、选择 `.md/.txt` 导入，并查看文档状态与已提取正文。今天先形成可用工具流程，视觉精修不阻塞功能。

## Frozen route and navigation

- 新 route `/knowledge`，name `knowledge`，使用 `meta.fill=true`。
- DesktopShell 在“简历”与“模拟面试”之间增加“知识库”一级入口，使用现有 Element Plus 图标。
- 旧 `/evidences`、Evidence 页面和能力证据代码全部保留，不复用为 Knowledge 数据源。

## Scope

- 新 `types/knowledge.ts`、`api/knowledge.ts`、`stores/knowledge.ts` 及针对性测试。
- API：NOTE create、list、detail、multipart import、content detail；准确显示服务端错误。
- 新 `views/knowledge/KnowledgeLibraryView.vue` 与 `components/knowledge/**`。
- 桌面双栏：左侧资料列表，右侧详情；顶部只有“新建笔记”“导入文件”“刷新”。
- 初始空态直接提供新建/导入；加载失败保留重试；列表选择不会联动 Pipeline、Resume 或 Interview。
- 文件 input `accept=.md,.txt,text/plain,text/markdown`；只发送 `File` multipart，不发送本地路径。
- 状态文案来自真实 `processingStatus`：未处理、处理中、已完成、失败；失败不伪造正文。
- `COMPLETED` 时按需加载 content；409 显示“内容仍在处理中”，404 显示不可用，不清空其他列表数据。
- 页面在 1080×720 可用，深浅主题沿用现有 CSS variables；不追求最终视觉稿。

## Scope out

分类、标签、搜索、删除、重试、打开系统原文、PDF/DOCX、拖拽、RAG、AI、编辑正文、Pipeline 绑定和全局视觉重构。

## Allowed files

```text
frontend/src/router/index.ts
frontend/src/router/index.test.ts
frontend/src/layouts/DesktopShell.vue
frontend/src/layouts/DesktopShell.test.ts
frontend/src/types/knowledge.ts
frontend/src/api/knowledge.ts
frontend/src/api/knowledge.test.ts
frontend/src/stores/knowledge.ts
frontend/src/stores/knowledge.test.ts
frontend/src/views/knowledge/**
frontend/src/components/knowledge/**
```

## Functional acceptance

1. 导航进入 `/knowledge`，不会加载 Legacy Evidence store。
2. 空库创建 NOTE 后立即出现在列表并选中。
3. 选择 md/txt 只提交 multipart `file`；成功或 duplicate 响应刷新并选中文档。
4. 列表切换保持各文档独立；完成态加载真实 content。
5. list/import/content 任一失败只影响对应区域，可重试，不伪造成功。
6. 1080×720 无页面水平滚动，键盘可操作主要按钮。

只运行本卡针对性测试，交付前一次前端全量、build 与 diff-check。

## Direct rejection

- 继续显示旧能力证据作为 Knowledge。
- 写死资料、正文或处理成功状态。
- 发送系统路径、调用 Node/Electron filesystem。
- 顺带实现搜索/标签/删除/AI 或修改其他页面。

## Delivery

Commit: `feat(knowledge): add usable local library view`

Integrated commits: `4979b04`, `fbe13e2`.

回传 commit、真实用户流程、对 IO-01 mock 的接口假设、测试结果和未完成项；不得自行合并。
