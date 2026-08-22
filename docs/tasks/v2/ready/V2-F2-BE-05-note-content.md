# V2-F2-BE-05：本地笔记正文保存

## Identity

- Status: `READY`
- Branch: `codex/v2-f2-be-05-note-content`
- Base commit: `5d26fa1`
- Depends on: integrated F2-BE-04

## User result

用户新建 NOTE 后可以手动编写并保存正文；正文进入现有关键词搜索，但不会被 AI 静默改写或自动认定为能力事实。

## Frozen contract

- `PUT /api/v2/knowledge/documents/{id}/content` body `{ content: string }`，只允许当前用户 `sourceType=NOTE`。
- content 原样保存 UTF-8 文本，允许空字符串，最大 1 MiB UTF-8 bytes；不 trim、不 Markdown 重写。
- 保存使用现有 `knowledge_extracted_contents` upsert，同事务将 NOTE processingStatus 设为 `COMPLETED` 并更新 document updatedAt。
- FILE 文档、跨用户/缺失文档拒绝；失败不改变旧正文或更新时间。
- 响应返回现有 `KnowledgeContentResponse`，搜索立即读取新正文。
- 不记录正文、标题、差异或请求体；不调用 AI。

## Allowed files

- `backend/src/main/java/com/resumego/knowledge/**`
- `backend/src/test/java/com/resumego/knowledge/**`

不得新增 migration、修改 FILE 提取内容、实现自动保存 UI、版本历史、AI 改写或 Resume evidence。

## Required scenarios

覆盖 NOTE 首次保存、覆盖保存、空正文、中文/Markdown 原样、1 MiB 边界、超限回滚、FILE 拒绝、跨用户隔离、搜索立即命中和普通日志不含正文。

## Verification

运行新增定向测试和一次 `mvn -q -Dtest='Knowledge*Test' test`；不运行 Electron 或前端测试。

## Delivery

Commit: `feat(knowledge): save local note content`

回传最终 commit、byte 限制、事务边界、搜索一致性和必要测试；不得自行合并。
