# V2-F2-FE-01：Knowledge 类型、API Client 与 Store

## Identity

- Status: `QUEUED`，禁止开始
- Branch: `codex/v2-f2-fe-01-knowledge-client-store`
- Base commit: `Assigned when promoted to READY`
- Depends on: integrated F2-BE-02 contracts

## User result

为 Knowledge Library 提供唯一、类型安全的数据入口和可恢复状态，不让页面自行拼 URL、保存任意路径或猜测 job 状态。

## Planned scope

- Knowledge document/category/tag/search/job 类型。
- metadata、import、list/detail、classification/tag、search、open-source API client。
- Pinia store 的加载、选择、filter/search、错误和 retry 状态。
- 过期选择、并发搜索和请求失败恢复。

## Scope out

不实现页面、文件系统调用、删除/重试（在 BE-03 集成后另加 client action）、RAG、AI 或跨模块 store 联动。

## File ownership to freeze

只授权 `frontend/src/types/knowledge.ts`、`frontend/src/api/knowledge.ts`、`frontend/src/stores/knowledge.ts` 及三份对应测试。若需要修改 `http.ts`，必须停止并重新授权。

## Required tests

准确 method/path/body、服务端 message/fallback、空库、失败重试、搜索取消/乱序结果、持久选择失效、job 状态不伪造。运行局部测试、`npm test`、`npm run build`。

