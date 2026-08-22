# V2-F2-BE-02：分类、标签、关键词搜索与原文定位

## Identity

- Status: `QUEUED`，禁止开始
- Branch: `codex/v2-f2-be-02-classification-search`
- Base commit: `Assigned when promoted to READY`
- Depends on: integrated F2-IO-01

## User result

用户能将资料放入技术知识、项目知识、面试资产、个人总结等分类，维护标签，通过关键词找到匹配片段，并安全打开自己导入的原文件位置。

## Planned scope

- 明确的分类值和用户标签 CRUD/关联。
- 关键词搜索标题、元数据和已提取文本，返回来源、短片段和定位信息。
- 空关键词、无结果、未解析/解析失败文档的诚实行为。
- 通过受限 capability 打开受管文件，不接受 renderer 任意路径。

## Scope out

不做语义检索、推荐、自动标签、能力认定、外部搜索或知识图谱。

## File ownership to freeze

只授权 Knowledge 模块分类/标签/search DTO、repository/service/controller、受限 open-source capability 和测试。不得修改 Import job 状态机或 UI。

## Required tests

分类与标签所有权、重复/空标签、大小写和中文关键词、稳定排序、结果片段不越界、无结果、未解析资料、跨用户 source token、任意路径拒绝。运行全量后端与 Electron 相关测试。

