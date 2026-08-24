# V2-F1-BE-01：Pipeline 阶段历史查询

## 1. 任务身份

| 字段 | 值 |
| --- | --- |
| Owner role | Backend Feature Agent |
| Milestone | V2 F1 Career Pipeline |
| Status | `INTEGRATED` |
| Base commit | `1f83b1ac02e62793813d174a2f451a42fc5c3e57` |
| Branch | `codex/v2-f1-be-01-transition-history` |
| Integration target | `codex/v2-career-os`，仅由 Core Controller 集成 |
| Integration commits | `9274a5e`、`7974b98` |

执行前必须阅读 `docs/architecture/agent-collaboration.md`。如果当前仓库不包含精确 `Base commit`，或授权文件存在非本任务改动，立即停止并报告。

## 2. 用户结果

Pipeline 的阶段变化已经持久化，但客户端无法读取。完成后，用户查看一条自己拥有的求职管线时，可以按发生顺序取得完整且不可修改的阶段历史；不存在或不属于当前用户的管线返回 `404`。

## 3. 依赖

- 使用现有 `pipeline_stage_transitions` 表和 `CareerPipelineRepository.findTransitions` 语义。
- 不新增数据库迁移，不改变阶段推进规则。
- 不依赖 V2 前端任务。

## 4. Scope in

- 新增阶段历史响应 DTO。
- Repository 提供按当前用户与 Pipeline 查询、有稳定顺序的历史结果。
- Service 在读取历史前执行当前用户所有权校验。
- Controller 暴露冻结的只读端点。
- Repository、Service、Controller 三层测试覆盖正常、空历史、不可用 Pipeline 与字段映射。

## 5. Scope out

- 不修改阶段推进、回退、归档或恢复规则。
- 不新增删除、编辑历史的能力。
- 不修改 Schema、迁移、前端、V1 或产品文档。
- 不增加分页、筛选、AI 总结或事件通知。

## 6. Allowed files

仅允许创建或修改下列文件：

```text
backend/src/main/java/com/resumego/pipeline/CareerPipelineRepository.java
backend/src/main/java/com/resumego/pipeline/CareerPipelineService.java
backend/src/main/java/com/resumego/pipeline/CareerPipelineController.java
backend/src/main/java/com/resumego/pipeline/dto/PipelineStageTransitionResponse.java
backend/src/test/java/com/resumego/pipeline/CareerPipelineRepositoryTest.java
backend/src/test/java/com/resumego/pipeline/CareerPipelineServiceTest.java
backend/src/test/java/com/resumego/pipeline/CareerPipelineControllerTest.java
```

任何其他文件都默认禁止。若实现必须越界，停止编码并说明目标文件、原因、契约影响和替代方案，等待新任务卡或书面重新授权。

## 7. Frozen contracts

### HTTP

```http
GET /api/v2/pipelines/{id}/transitions
```

成功使用现有 `ApiResponse` 包装，`data` 为数组。每项字段固定为：

```json
{
  "id": 12,
  "pipelineId": 3,
  "fromStageId": 21,
  "toStageId": 22,
  "actor": "USER",
  "note": "进入技术面",
  "occurredAt": "2026-08-22T14:30:00"
}
```

- 首次创建记录的 `fromStageId` 可以是 `null`。
- `note` 可以是 `null`，不得用空字符串伪造内容。
- 顺序固定为 `occurredAt ASC, id ASC`。
- Pipeline 不存在或不属于当前用户时返回 `404`，不得泄露其是否属于其他用户。
- 此端点只读；历史记录保持 append-only。

## 8. Data, privacy and AI

- 无迁移、无第三方网络、无 AI 调用。
- 只读取当前本地用户拥有的 Pipeline 数据。
- 测试必须使用虚构公司、岗位、备注，不得复制真实简历或面试内容。
- 不在普通日志输出阶段备注或用户资产。

## 9. Observable behavior and required tests

| ID | 场景 | 必须观察到的结果 |
| --- | --- | --- |
| BE-01 | 已发生多次阶段变化 | 返回全部记录，严格按时间与 id 升序 |
| BE-02 | 只有创建记录 | `fromStageId=null`，其他字段映射准确 |
| BE-03 | 拥有 Pipeline 但历史为空 | `200` 且 `data=[]` |
| BE-04 | Pipeline 不存在 | `404`，响应使用现有失败包装 |
| BE-05 | Pipeline 属于其他用户 | `404`，不得返回历史 |
| BE-06 | 查询历史 | 不更新 Pipeline、阶段或历史表 |

## 10. RED evidence

先只写测试并运行，至少让 Controller 的新端点和 Service 的所有权行为真实失败。记录命令、退出码、测试数与实际失败原因；禁止完成代码后补写 RED。

建议命令：

```bash
cd backend
mvn -Dtest=CareerPipelineRepositoryTest,CareerPipelineServiceTest,CareerPipelineControllerTest test
```

## 11. GREEN and regression

最小实现后依次运行：

```bash
cd backend
mvn -Dtest=CareerPipelineRepositoryTest,CareerPipelineServiceTest,CareerPipelineControllerTest test
mvn test
git diff --check
git status --short
```

所有命令必须给出实际退出码和测试数量。`mvn test` 未运行或失败时不得声称完成。

## 12. Commit and delivery

- 提交信息：`feat(pipeline): expose transition history`
- 最终提交只包含 `Allowed files`。
- 不自行 merge、rebase、push 或修改集成分支。
- 返回最终提交完整哈希，并按 `docs/templates/v2-agent-delivery-report.md` 填写交付报告。

## 13. Integration gates

以下任一项成立即退回：端点或字段漂移；跨用户数据可见；顺序不稳定；修改迁移或状态机；缺少真实 RED；只跑局部测试却声称全量通过；存在未授权文件。
