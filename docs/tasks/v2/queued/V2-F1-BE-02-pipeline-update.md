# V2-F1-BE-02：Pipeline 身份与材料更新

## Identity

- Status: `QUEUED`，禁止开始
- Branch: `codex/v2-f1-be-02-pipeline-update`
- Base commit: `Assigned when promoted to READY`
- Depends on: integrated `V2-F1-BE-01`

## User result

用户可以修改 Pipeline 名称、公司、岗位、JD 和当前关联简历版本，而不重建 Pipeline、不丢失阶段历史，也不影响其他 Pipeline 或简历版本。

## Frozen intent

- 计划端点：`PATCH /api/v2/pipelines/{id}`。
- 计划请求字段：`name`、`companyName`、`roleTitle`、`jobDescriptionId`、`resumeVersionId`。
- 全量替换语义；可空关联显式使用 `null` 解除。
- Pipeline 不存在/不属于用户为 `404`；外部 JD/Resume 为 `400`。
- 归档/关闭 Pipeline 是否允许修改必须在升为 READY 时冻结，不由 Agent 猜测。

## Planned Allowed files

Pipeline repository/service/controller、`UpdateCareerPipelineRequest`、`CareerPipelineResponse`（仅确有契约变化时）和对应三层测试。不得改迁移、V1、Resume 写路径或阶段状态机。

## Required tests

规范化与长度、解除关联、外部资产拒绝、历史/阶段/其他 Pipeline 不变、事务失败回滚、404/400。必须真实 RED/GREEN 并运行 `cd backend && mvn test`。

## Promotion gate

BE-01 集成后填写真实 Base commit，解决归档/关闭编辑规则，列出精确文件并核对与其他进行中任务无重叠。
