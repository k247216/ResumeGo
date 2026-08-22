# V2-F2-BE-02：分类、标签与关键词搜索

## Identity

- Status: `READY`
- Branch: `codex/v2-f2-be-02-classification-search`
- Base commit: `fbe13e2bbc3366371c5ca0955dc5fd8f6356e1f1`
- Depends on: integrated F2-IO-01 and F2-FE-00

## User result

用户能将资料放入自己的分类，维护标签，并通过关键词找到标题或已提取正文中的真实匹配片段。搜索结果能说明匹配位置，不返回整篇正文。

## Frozen data model

- 新增等价的 H2 `V8__knowledge_classification_search.sql` 与 MySQL `V28__knowledge_classification_search.sql`。
- `knowledge_categories`：`id/user_id/name/normalized_name/created_at/updated_at`，同一用户 `normalized_name` 唯一。
- `knowledge_tags`：字段与唯一规则同 category。
- `knowledge_document_categories`：每个 document 最多一个 category，关联时同时校验 document/category 属于当前用户。
- `knowledge_document_tags`：document/tag 多对多，重复关联幂等。
- 名称 trim、合并内部空白、长度 1–40；`normalized_name` 使用 `Locale.ROOT` 小写，不跨用户合并。

## Frozen API

- `GET/POST /api/v2/knowledge/categories`
- `GET/POST /api/v2/knowledge/tags`
- `PUT/DELETE /api/v2/knowledge/documents/{documentId}/category/{categoryId}`
- `PUT/DELETE /api/v2/knowledge/documents/{documentId}/tags/{tagId}`
- `GET /api/v2/knowledge/search?q={query}&categoryId={optional}&tagId={optional}`
- category/tag create body 为 `{ "name": string }`；重复 normalized name 返回既有记录，不制造副本。
- 关联或解除不存在、外部用户的 document/category/tag 一律返回 404，不泄露所有权。
- 搜索 `q` trim 后 1–100；按字面子串匹配，必须转义 SQL wildcard，支持中文与大小写不敏感英文。
- 标题对所有真实文档可搜索；正文只搜索 `COMPLETED` 的 extracted content。`PENDING/RUNNING/FAILED` 不伪造正文命中。
- 可选 category/tag filter 必须属于当前用户；无结果返回空数组。
- 稳定排序 `updated_at DESC, id DESC`，最多 100 条。
- 结果包含 document 元数据、`matchedField=TITLE|CONTENT`、不超过 240 字的 snippet、正文命中时 1-based `lineNumber`；不得返回整篇正文。

## Scope out

不做语义检索、推荐、自动标签、能力认定、外部搜索、知识图谱、前端、删除/重试或 AI。安全打开受管原文需要 Electron IPC 与一次性 capability，拆分到后续 `V2-F2-IO-02`，本卡不得返回绝对路径或接受 renderer 路径。

## Allowed files

仅允许：

```text
backend/src/main/resources/db/migration-h2/V8__knowledge_classification_search.sql
backend/src/main/resources/db/migration/V28__knowledge_classification_search.sql
backend/src/main/java/com/resumego/knowledge/**
backend/src/test/java/com/resumego/knowledge/**
backend/src/test/java/com/resumego/migration/H2FileWorkspaceMigrationTest.java
backend/src/test/resources/sql/knowledge_schema.sql
```

不得修改 import job 状态机、文件副本逻辑、前端、Electron、V1 或其他领域模块。

## Required tests

只覆盖分类/标签当前用户隔离、重复与空名称、关联幂等、跨用户关联拒绝、中文与英文大小写搜索、SQL wildcard 字面匹配、组合 filter、稳定排序、snippet/lineNumber 边界、空结果、未解析或失败资料只允许标题命中。先跑 Knowledge 模块测试，交付前仅一次后端全量与 `git diff --check`。

## Direct rejection

- 查询或关联缺少 `user_id` 所有权约束。
- 用拼接 SQL 接收 query，或让 `%/_` 被解释为通配符。
- 搜索返回整篇正文、路径、hash 或跨用户数据。
- 为了搜索修改现有导入状态机或新增 AI/向量依赖。
- 顺带实现前端、Electron 打开文件、删除或自动分类。

## Delivery

Commit: `feat(knowledge): add classification tags and keyword search`

通过 DSH 回传最终 commit、迁移版本、端点行为、搜索边界与必要测试结果；不得自行合并。
