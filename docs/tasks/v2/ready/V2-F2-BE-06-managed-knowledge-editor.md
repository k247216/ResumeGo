# V2-F2-BE-06：知识资料真实类型与受管编辑后端

## Identity

- Status: `READY`
- Branch: `codex/v2-f2-be-06-managed-editor`
- Base commit: `3712637`
- Depends on: integrated F2-BE-05、F2-IO-01、F2-IO-02
- Owner: DSH Feature Agent

## User result

知识库能够明确区分 NOTE、Markdown 和 TXT；用户一键新建的笔记立即拥有真实 ID、空正文和可修改标题；导入 Markdown 后只修改 ResumeGo 的受管副本，并同步更新搜索正文与元数据，绝不写回用户原始文件。

## Frozen API contract

1. 所有 `KnowledgeDocumentResponse` 路径新增安全字段 `sourceExtension`：
   - NOTE 返回 `null`；
   - Markdown FILE 返回规范化小写 `md`；
   - TXT FILE 返回 `txt`；
   - 列表、详情、搜索、分类、恢复/重试响应保持一致。
2. 新增 `PATCH /api/v2/knowledge/documents/{id}`，请求 `{ "title": string }`，返回更新后的 `KnowledgeDocumentResponse`。
3. 创建 NOTE 时，在同一事务持久化空字符串正文，返回 `COMPLETED`；失败不得留下半成品文档。
4. 保留 `PUT /api/v2/knowledge/documents/{id}/content`：
   - NOTE 保存语义不变；
   - 仅 `sourceType=FILE && sourceExtension=md` 可保存；
   - TXT、未知扩展、缺失/不可用源文件拒绝；
   - 跨用户与不存在文档按既有 404 契约处理。
5. 正文允许空字符串，原样 UTF-8 保存，最大 1 MiB UTF-8 bytes；不 trim、不格式化、不调用 AI。

## Managed-copy safety contract

- Markdown 编辑只接收 `documentId + content`，不接受 renderer 传入的任何路径、文件名或 hash。
- 只允许操作当前用户数据库记录指向且位于 V2 data root 内的普通受管文件；拒绝绝对路径、`..`、目录、符号链接和 realpath 越界。
- 不修改导入前的外部原文件。受管文件写入成功后，同步更新：提取正文、`size_bytes`、`sha256`、source/document `updated_at` 和可用状态。
- 新 hash 已被同一用户另一文档占用时稳定拒绝，不覆盖、不合并文档。
- 文件替换与数据库更新必须具备可验证回滚：数据库失败恢复旧受管文件；文件失败不改变数据库；临时/备份文件最终清理。
- 普通日志和 API 响应不得包含正文、绝对路径、相对存储路径、hash、临时路径或原始异常中的路径。

## Required implementation shape

- 新建独立的 `KnowledgeManagedContentService`，由 controller 的 content endpoint 调用；Controller 不承载 NOTE/FILE 分支。
- 使用真实服务端 source record 判断扩展名，不从标题或 `originalName` 临时猜测。
- 使用 `TransactionTemplate` 或等价的明确事务编排，使数据库异常能在返回前被捕获并触发文件恢复。
- `KnowledgeFileStore` 只暴露受 data root 约束的准备替换、提交、恢复、清理原语，不暴露裸 Path 给 renderer。
- 标题更新 SQL 必须同时包含 `id` 与 `user_id`；空白标题、超长标题和跨用户更新失败时保留旧值。
- 不新增 migration；优先复用现有 `knowledge_extracted_contents`、`knowledge_source_files` 与唯一 hash 约束。

## Allowed files

- `backend/src/main/java/com/resumego/knowledge/**`
- `backend/src/test/java/com/resumego/knowledge/**`

不得修改前端、Electron、页面布局、全局导航、V1、AI/RAG、文件夹监控或外部原文件。

## Required functional scenarios

1. NOTE、MD、TXT 在列表/详情/搜索/恢复响应中返回正确 `sourceExtension`。
2. 一键创建 NOTE 后立即可读取空正文，状态为 COMPLETED；数据库异常完整回滚。
3. 当前用户重命名成功；空白/超长、缺失及跨用户重命名不污染旧标题。
4. NOTE 保存、MD 受管副本保存成功，保存后搜索立即命中新正文。
5. TXT、未知类型、MISSING/STAGED/FAILED Markdown 拒绝编辑且不改变文件或数据库。
6. 正好 1 MiB 成功，超 1 byte 失败并保留旧正文、旧 hash、旧 size、旧时间。
7. 新 hash 冲突稳定失败，两个文档及各自文件均保持不变。
8. 数据库更新故障恢复旧受管文件；文件替换故障不写数据库；临时文件被清理。
9. path traversal、绝对路径、目录、符号链接及 symlink 逃逸全部拒绝。
10. 日志与响应不泄露正文、路径、hash 或真实用户数据。

## Verification

严格 RED → GREEN，至少执行：

```bash
cd backend
mvn -q -Dtest=KnowledgeServiceTest,KnowledgeControllerTest,KnowledgeClassificationServiceTest,KnowledgeNoteContentIntegrationTest,KnowledgeManagedContentServiceTest,KnowledgeFileStoreTest test
mvn -q -Dtest='Knowledge*Test' test
```

不运行前端、Electron、打包或扩大到全项目测试。

## Delivery

Commit: `feat(knowledge): support managed document editing`

通过 DSH 回传：最终 commit、API 契约、文件/数据库原子边界、回滚路径、hash 冲突策略、所有定向测试结果及仍存在的限制。不得自行合并；不得附带数据库文件或真实资料。
