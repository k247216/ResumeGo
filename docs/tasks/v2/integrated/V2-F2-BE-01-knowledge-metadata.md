# V2-F2-BE-01：Knowledge 元数据基础

## Identity

- Status: `INTEGRATED`
- Branch: `codex/v2-f2-be-01-knowledge-metadata`
- Base commit: `5ea03a73274ccc534e44e6acaeb721fb631f5782`
- Depends on: frozen F2-ARCH-01 contract

## User result

用户能够创建一条本地知识文档记录、查看列表与详情，并看见真实来源类型、文件状态和处理状态；尚未导入或解析时不会伪造内容。

## Required reading

- `docs/superpowers/specs/2026-08-22-v2-knowledge-library-foundation.md`
- `docs/architecture/agent-collaboration.md`

## Scope

- H2 `V6__knowledge_library_foundation.sql` 与 MySQL `V26__knowledge_library_foundation.sql` 等价迁移。
- 新 `com.resumego.knowledge` domain/repository/service/controller/dto 模块。
- 严格按冻结契约实现 NOTE 元数据创建、列表和详情 API；当前用户所有权、时间和状态映射。
- 空库、缺失记录和不可用关联的稳定错误。

## Scope out

不复制文件、不解析、不搜索、不打标签、不删除、不调用 AI。

## Allowed files

仅允许：

```text
backend/src/main/resources/db/migration-h2/V6__knowledge_library_foundation.sql
backend/src/main/resources/db/migration/V26__knowledge_library_foundation.sql
backend/src/main/java/com/resumego/knowledge/**
backend/src/test/java/com/resumego/knowledge/**
```

禁止修改 V1 Evidence、Resume、Pipeline、公共异常契约、前端或构建配置。

## Required tests

只覆盖空库、NOTE 创建/list/detail、标题规范化、非法 FILE 创建、404 与当前用户隔离、H2 migration 和 MySQL migration 静态一致性。先跑模块测试，交付前只运行一次 `cd backend && mvn test` 与 `git diff --check`。

## Direct rejection

- 接受 FILE 但没有真实 staging/copy。
- 查询未带 user ownership。
- 保存绝对路径、正文或敏感数据到日志。
- 顺带实现解析、搜索、标签、删除、AI 或前端。

## Delivery

Commit: `feat(knowledge): add local metadata foundation`

Integrated commits: `4946c0c`, `65c25c0`.

通过 DSH 返回最终 commit、端点行为、迁移版本、测试结果和已知未完成项；不得自行合并。
