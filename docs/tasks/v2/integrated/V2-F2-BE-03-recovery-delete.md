# V2-F2-BE-03：解析重试与派生数据彻底清理

## Identity

- Status: `INTEGRATED`
- Branch: `codex/v2-f2-be-03-recovery-delete`
- Integrated commits: `2d1fa2a`、`60221fe`、`b5cc6a4`
- Depends on: integrated F2-BE-02

## User result

解析失败的资料可以安全重试；用户确认删除文档后，受管原文件、提取内容、分类标签关联、搜索派生数据和 job 全部清理，不留下可搜索幽灵记录。

## Frozen migration and recovery model

- 新增等价 H2 `V9__knowledge_recovery_cleanup.sql` / MySQL `V29__knowledge_recovery_cleanup.sql`。
- `knowledge_source_files` 新增 nullable `staging_relative_path`；只能保存 V2 data dir 下相对路径。新导入在落位前保存 staging 相对路径，成功移动后清空。
- 新增 `knowledge_delete_confirmations`：随机 token 的 SHA-256、user/document、expires_at、consumed_at；明文 token 不入库、不入日志，10 分钟失效。
- 新增 `knowledge_cleanup_jobs`：删除后的 user、document snapshot、受管相对路径、`PENDING/COMPLETED/FAILED`、稳定 errorCode/timestamps；不对已删除 document 建 FK。

## Frozen retry API

- `POST /api/v2/knowledge/documents/{id}/retry`，仅当前用户 `FILE + FAILED`。
- retryable：`COPY_FAILED`（且 staging 副本仍存在）与 `EXTRACTION_FAILED`（source AVAILABLE）。`INVALID_UTF8`、校验失败、COMPLETED/RUNNING/缺失副本返回 409 和稳定 code。
- repository 用条件更新从 FAILED claim 为 RUNNING；并发第二次请求返回 409，不创建第二个 job。
- COPY_FAILED 从已记录 staging 相对路径安全移动；EXTRACTION_FAILED 从受管 source 读取；成功覆盖/插入 extracted content 并完成原 job，失败保持 FAILED。
- 不接受上传字节或任意路径；不修改原始用户文件。

## Frozen deletion API

- `GET /api/v2/knowledge/documents/{id}/deletion-impact` 返回 title、是否有 source/content/category/tags、短期一次性 confirmationToken 与 expiresAt，不返回路径/hash/正文。
- `DELETE /api/v2/knowledge/documents/{id}` body `{ "confirmationToken": string }`。
- 同一事务校验当前用户、token 未失效未使用，创建 cleanup job（FILE 时）并删除 document；数据库 FK cascade 清 content/import job/category/tag relation/source metadata。
- 事务提交后删除受管副本；成功 job=COMPLETED，失败 job=FAILED，但文档保持已删除且搜索立即不可见。
- `POST /api/v2/knowledge/cleanup-jobs/{id}/retry` 只允许当前用户 FAILED/PENDING job；启动恢复只处理 PENDING，避免无限重试 FAILED。
- `KnowledgeFileStore` 只删除 `knowledge/staging/{managed-name}` 或 `knowledge/sources/{userId}/{managed-name}`；路径越界/用户目录不匹配直接拒绝。

## Scope out

不提供无确认批量删除、回收站、云端恢复、打开原文、前端或 AI 修复解析内容。

## File ownership to freeze

仅允许 V9/V29 migration、`backend/src/main/java/com/resumego/knowledge/**`、对应 Knowledge tests、H2 migration test 与 `knowledge_schema.sql`。不得修改前端、Electron、V1 或其他领域。

## Required tests

只覆盖 retry claim/并发/状态拒绝、staging/source 路径约束、删除影响摘要、token hash/失效/一次性/跨用户、数据库回滚不删文件、提交后文件失败形成 FAILED cleanup、PENDING 启动补偿、删除后搜索不可见和其他文档不受影响。先 Knowledge 模块测试，交付前仅一次后端全量与 diff-check；不运行 Electron 测试。

## Direct rejection

- 接受 renderer 路径、保存绝对路径、记录明文 token/文件名/正文。
- 先删文件再提交数据库事务。
- 文件删除失败时恢复文档或伪造全部清理成功。
- 允许 INVALID_UTF8 盲目重试，或并发创建多个 retry job。
- 顺带实现回收站、前端、打开文件或 AI。

## Delivery

Commit: `feat(knowledge): add retry and recoverable deletion`

通过 DSH 回传最终 commit、状态机、事务/文件边界、恢复策略和必要测试结果；不得自行合并。
