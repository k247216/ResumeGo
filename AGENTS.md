# 职达仓库的核心指南

职达是面向求职者的 AI 智能简历与求职助手，核心闭环为：
诊断 → 定向优化 → 模拟面试 → 反馈迭代

开始任务前按需阅读：

- 产品范围：`harness-docs/product/mvp.md`
- 架构与模块边界：`.agent/architecture.md`
- 编码和完成标准：`.agent/conventions.md`
- 历史决策：`.agent/decisions.md`
- AI 禁飞区：`.agent/no-fly-zone.md`
- AI 使用与日志：`.agent/ai-policy.md`
- 功能开发流程：`.agent/workflows/feature.md`

强制约束：

1. 不得生成或代写简历评分权重、岗位匹配排序、面试状态机三个禁飞区的实现。
2. AI 不得编造用户经历；所有简历建议必须有事实证据或标记证据缺失。
3. 模型输出必须经过结构化校验，不能直接控制权限、状态、分数和排序。
4. 不得将简历敏感信息、访问令牌和模型密钥写入日志或提交到仓库。
5. 不得自行扩展 MVP 范围或引入复杂基础设施；重大变化先记录 ADR。
