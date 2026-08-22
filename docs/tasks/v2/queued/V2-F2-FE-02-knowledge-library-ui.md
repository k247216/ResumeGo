# V2-F2-FE-02：Knowledge Library 页面

## Identity

- Status: `QUEUED`，禁止开始
- Branch: `codex/v2-f2-fe-02-knowledge-library`
- Base commit: `Assigned when promoted to READY`
- Depends on: integrated F2-BE-03, F2-FE-01 and approved F2-UX-01

## User result

用户可以在独立 Knowledge Library 页面导入、分类、搜索、定位、重试和彻底删除本地资料，所有状态由真实后端数据驱动。

## Planned scope

- 新 Knowledge route/view 和按用户任务拆分的组件。
- 使用已集成 store；补充 BE-03 的 retry/delete client actions。
- import progress、空/加载/失败/重试/无结果/缺失原文件状态。
- 删除影响摘要、明确确认和成功后的列表一致性。
- DesktopShell 增加 Knowledge 一级入口，仅在 UX 契约授权时修改。

## Scope out

不做 RAG、AI 总结、知识训练、网页抓取、拖入任意目录监控、Pipeline 私有知识或全面导航重做。

## File ownership to freeze

router、DesktopShell 的单一导航改动、Knowledge view、`components/knowledge/**`、BE-03 client/store 增量和对应测试。禁止修改 backend、其他 view 或全局视觉系统。

## Required tests

导入流程、状态刷新、分类标签、搜索/无结果、打开原文失败、重试、删除确认与失败恢复、刷新持久化、窄窗口、深浅主题与键盘。运行 `npm test`、`npm run build`、`npm run build:electron`。

