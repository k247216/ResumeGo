# V2-F2-FE-02：Knowledge Library 页面

## Identity

- Status: `QUEUED`，禁止开始
- Branch: `codex/v2-f2-fe-02-library-ui`
- Base commit: `Assigned when promoted to READY`
- Depends on: integrated F2-IO-02, F2-BE-04, F2-BE-05, F2-FE-01 and approved F2-UX-01

## User result

用户可以在独立 Knowledge Library 页面导入、分类、搜索、定位、重试和彻底删除本地资料，所有状态由真实后端数据驱动。

## Planned scope

- 按批准视觉稿实现层级资料库、可收起资料列表、正文阅读区和可关闭来源检查器。
- 使用已集成 store；补充 BE-03 的 retry/delete client actions。
- import progress、空/加载/失败/重试/无结果/缺失原文件状态。
- 删除影响摘要、明确确认和成功后的列表一致性。
- DesktopShell 的 Knowledge 一级入口已存在，不重做全局工具栏。

## Scope out

不做 RAG、AI 总结、知识训练、网页抓取、拖入任意目录监控、Pipeline 私有知识、虚构资产关系、未支持文件类型或全面导航重做。

## File ownership to freeze

router、DesktopShell 的单一导航改动、Knowledge view、`components/knowledge/**`、BE-03 client/store 增量和对应测试。禁止修改 backend、其他 view 或全局视觉系统。

## Required tests

导入流程、层级分类与标签、列表/检查器收起、搜索与正文定位、NOTE 保存、打开原文失败、重试、删除确认与失败恢复、刷新持久化、1080×720、深浅主题与键盘。运行 Knowledge 定向测试、`npm run build`、`npm run build:electron`；全量只在交付前一次。
