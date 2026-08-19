# 职达项目文档

仓库是项目事实的唯一来源。聊天记录中的结论必须沉淀到这里或 `.agent/` 后才算正式
决策。

## 当前文档

- 产品愿景：`product/vision.md`
- 产品 MVP：`product/mvp.md`
- 产品 Backlog：`product/backlog.md`
- 团队角色与开发责任：`team/roles.md`
- 团队协作流程：`team/collaboration.md`
- Sprint 1 Backlog：`sprints/sprint-1.md`
- Sprint 2 计划：`sprints/sprint-2.md`
- Sprint 3 计划与验收：`sprints/sprint-3.md`
- S3 阶段总结：`reports/S3阶段总结.md`
- Sprint 1 数据模型草案：`data-model/schema.md`
- Sprint 1 API 草案：`api/openapi.yaml`
- AI 使用政策：`ai/ai-policy.md`
- AI 禁飞区：`ai/no-fly-zone.md`
- Prompt 记录：`ai/prompt-records.md`
- AI 使用日志：`ai/ai-usage-log.md`
- 岗位匹配设计文档与测试用例：`testing/match-test-cases.md`
- 项目架构：`../.agent/architecture.md`
- 工程规范：`../.agent/conventions.md`
- 架构决策：`../.agent/decisions.md`
- 功能开发流程：`../.agent/workflows/feature.md`

## 固定文档结构

当前先固定以下三组高频文档结构：

```text
product/
├── vision.md
├── mvp.md
└── backlog.md

team/
├── roles.md
└── collaboration.md

ai/
├── ai-policy.md
├── no-fly-zone.md
├── prompt-records.md
└── ai-usage-log.md
```

## 课程要求的人工设计文档

以下文档必须由团队人工主导完成，并在对应开发开始前补齐：

- `architecture/system-design.md`：架构设计；
- `api/openapi.yaml`：接口设计；
- `data-model/schema.md`：数据模型设计；
- `testing/test-plan.md`：测试用例设计；
- `deployment/deployment.md`：部署方案设计。

## 后续文档建议

- `sprints/`：Backlog、验收结果和 Sprint 反思；
- `runbooks/bug-injection.md`：30 分钟 Bug 注入处置手册。
