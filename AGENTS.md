# ResumeGo 仓库指南

ResumeGo（中文名：职达）是一款本地优先、证据驱动的 AI 求职工作台。核心闭环是：

```text
创建求职项目 → 整理能力证据 → 针对岗位修改简历 → 模拟面试 → 反馈进入下一版
```

开始任务前按需阅读：

- 当前真实状态：`docs/architecture/baseline.md`
- 产品范围与非目标：`docs/product/product.md`
- 阶段路线：`docs/product/roadmap.md`
- 架构与模块边界：`docs/architecture/architecture.md`
- 编码和完成标准：`docs/architecture/development.md`
- 隐私与 AI 规则：`docs/architecture/privacy-and-ai.md`
- 重大历史决策：`docs/decisions.md`
- 界面 QA 记录：`docs/design-qa.md`

长期约束：

1. 不得编造用户经历、技能、数字、荣誉或岗位事实；证据不足必须明确提示。
2. 模型输出必须经过结构化校验，不能直接控制权限、最终分数、岗位排序或面试流程状态。
3. AI 建议必须保留原文、依据、建议稿和用户决定；不得静默覆盖简历。
4. 简历、联系方式、面试回答、访问令牌和模型密钥不得进入普通日志或仓库。
5. 默认采用本地优先的数据设计；新增云端传输、遥测或第三方服务前必须更新隐私文档。
6. 不自行扩展为招聘平台、自动投递平台、岗位爬虫或企业筛选工具。
7. `main` 是唯一长期分支；每次变更应范围明确、可验证且保持可发布状态。
8. 重大产品或架构变化先更新 `docs/decisions.md`，阶段范围变化同步更新 `docs/product/roadmap.md`。
