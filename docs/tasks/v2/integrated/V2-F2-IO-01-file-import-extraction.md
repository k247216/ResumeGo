# V2-F2-IO-01：文件导入与可恢复解析

## Identity

- Status: `INTEGRATED`
- Branch: `codex/v2-f2-io-01-import-extraction`
- Base commit: `65c25c09396c5ce3c9574b8d856011ae37bac334`
- Depends on: integrated F2-BE-01

## User result

用户明确选择本地文件后，V2 将安全副本保存到自己的数据目录并异步提取文本；失败会展示真实状态并可恢复，不损坏原文件和其他知识资产。

## Frozen first slice

- 首批只支持 UTF-8 `.md` 与 `.txt`，单文件最大 10 MiB；不增加解析依赖。
- 前端以后使用标准文件选择器上传 `multipart/form-data`，本卡不增加 Electron IPC，renderer 永远不传系统路径。
- `POST /api/v2/knowledge/imports` 接收单个 `file`；服务端只信任文件字节，不信任客户端 MIME、扩展名或路径。
- 文件暂存到 V2 data dir 的 `knowledge/staging`，校验后原子移动到 `knowledge/sources/{userId}/{sha256}.{ext}`；数据库仅保存相对路径。
- 同一用户相同 sha256 返回既有 document，`duplicate=true`，不重复保存或创建记录。
- 创建 FILE document/source record，并确定性提取 UTF-8 文本；成功为 `COMPLETED`，失败为 `FAILED` 且保留原始副本和稳定 errorCode。
- 新增 H2 V7 / MySQL V27：extracted content 与 import job；正文只存在 extracted content，不进入普通日志或 API 列表。
- `GET /api/v2/knowledge/documents/{id}/content` 返回当前用户的提取文本；未完成返回 409，缺失/他人返回 404。

## Scope

- 文件 fingerprint、类型/大小校验和安全复制。
- import/extraction job 的 `PENDING/RUNNING/COMPLETED/FAILED` 状态与结构化失败类别。
- `.md/.txt` 确定性 UTF-8 文本提取；保留原始文件与提取内容分离。
- API 返回 processing 状态，不把长文本写入日志。

## Scope out

不做 OCR、网页 URL、文件夹监控、云盘、Embedding/RAG 或自动标签。

## Allowed files

仅允许：

```text
backend/src/main/resources/db/migration-h2/V7__knowledge_import_extraction.sql
backend/src/main/resources/db/migration/V27__knowledge_import_extraction.sql
backend/src/main/java/com/resumego/knowledge/**
backend/src/test/java/com/resumego/knowledge/**
backend/src/test/java/com/resumego/migration/H2FileWorkspaceMigrationTest.java
backend/src/test/resources/sql/knowledge_schema.sql
```

不得修改 Electron、前端、构建依赖、V1 或其他领域模块。

## Required tests

只覆盖 md/txt 成功、非法 UTF-8、伪造/不支持扩展名、超限、重复 fingerprint、路径字符无效化、复制/解析失败、跨用户隔离、原文件不变和日志无正文。模块测试后，交付前仅运行一次后端全量与 diff-check。

## Direct rejection

- 保存客户端路径或允许 renderer 指定目标路径。
- 在完成安全副本前创建 AVAILABLE/COMPLETED 记录。
- 重复 fingerprint 创建第二份文档。
- 将正文、文件名或绝对路径写入日志。
- 顺带实现 PDF/DOCX/OCR、搜索、标签、删除、前端或 AI。

## Delivery

Commit: `feat(knowledge): import and extract text files`

Integrated commits: `d218ed7`, `a8d4299`.

通过 DSH 回传 commit、端点行为、存储相对路径规则、失败分类和必要测试结果；不得自行合并。
