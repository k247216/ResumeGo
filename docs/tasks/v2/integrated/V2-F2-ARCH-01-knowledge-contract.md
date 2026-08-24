# V2-F2-ARCH-01：Knowledge 存储与生命周期契约

## Identity

- Status: `INTEGRATED`
- Owner: Core Controller
- Branch: `codex/v2-f2-arch-01-knowledge-contract`
- Frozen contract: `docs/superpowers/specs/2026-08-22-v2-knowledge-library-foundation.md`

## Outcome

冻结 Knowledge Document、Source File、Extracted Content、Category、Tag 和 recoverable job 的所有权与生命周期，防止各 Agent 各自创造文件路径、状态枚举或删除语义。

## Required decisions

- 文档、文件副本、提取文本、分类、标签与 job 的表结构和唯一键。
- 支持的首批格式、单文件大小上限、MIME/扩展名校验与重复文件 fingerprint。
- Electron 选择文件、renderer、backend 和 V2 data directory 之间的最小 capability。
- job 状态：pending/running/completed/failed/retryable 的转换和崩溃恢复。
- 关键词搜索的索引/查询策略；F2 不引入向量库。
- 原文定位的能力令牌，禁止 renderer 传任意路径要求系统打开。
- 删除文档时原文件、提取内容、标签关联和 job 的清理/失败恢复顺序。
- Knowledge 内容不自动成为“用户能力事实”的边界。

## Allowed files when activated

```text
docs/superpowers/specs/*knowledge-library*.md
docs/architecture/architectureV2.md
docs/architecture/privacy-and-ai.md
docs/decisions.md
docs/product/roadmap.md
```

## Explicit non-goals

Embedding、RAG、OCR、云盘同步、网页抓取、知识图谱、Agent、自动能力认证和 F3 面试训练。

## Exit gate

设计必须包含 H2/MySQL 等价 schema、端点和 DTO、目录布局、路径攻击防护、删除恢复、测试矩阵，并由用户批准。之后才可提升 F2-BE-01。
