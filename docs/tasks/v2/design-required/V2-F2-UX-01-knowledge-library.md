# V2-F2-UX-01：Knowledge Library 交互契约

## Identity

- Status: `DESIGN_REQUIRED`
- Owner: Core Controller with user approval
- Code branch: none
- Depends on: approved F2-ARCH-01

## Product question

设计一个桌面资料库，而不是上传中心或 AI 聊天页：用户要清楚资料在哪里、是否处理成功、如何分类搜索、如何回到原文以及删除会影响什么。

## Required design output

- 左侧/顶部分类与搜索，文档列表和详情/来源信息的桌面布局。
- 导入入口、文件选择反馈、processing/failed/retryable 状态。
- 标签编辑、关键词命中片段和“打开原文”。
- 删除影响摘要与二次确认。
- 空库、无结果、解析失败、文件丢失、后端离线和重试状态。
- 最小窗口、宽屏、浅色/深色、键盘和 focus 规则。

## Product boundaries

- 不把文档内容自动标为用户掌握的技能。
- 不显示 RAG 聊天、AI 总结、Embedding、学习路线或面试训练入口。
- 不将资料绑定为某一 Pipeline 私有内容；Pipeline 以后只能引用。
- 信息密度适合桌面工具，但保持呼吸感，不做移动端卡片堆叠。

## Exit gate

用户批准布局和主要状态后，冻结组件边界、route、data-test、copy 和响应式规格，才能提升 F2-FE-02。

