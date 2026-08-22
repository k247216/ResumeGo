# V2-F1-BE-02：Pipeline 身份与材料更新

## 1. 任务身份

| 字段 | 值 |
| --- | --- |
| Owner role | Backend Feature Agent |
| Milestone | V2 F1 Career Pipeline |
| Status | `READY` |
| Base commit | `fc98ad0f5ac9edc8749c6ddcbf614429133c1713` |
| Branch | `codex/v2-f1-be-02-pipeline-update` |
| Integration target | `codex/v2-career-os`，仅由 Core Controller 集成 |

执行前阅读 `docs/architecture/agent-collaboration.md`。必须从精确 Base commit 创建独立 worktree；基线不存在或授权文件已有其他改动时立即停止。

## 2. 用户结果

用户能够修改 Pipeline 名称、公司、岗位、JD 和当前关联简历版本，而不重建 Pipeline、不丢失阶段历史，也不污染其他 Pipeline 或简历版本。

## 3. Scope in

- 新增一个全量更新请求 DTO。
- Repository 原子更新当前用户拥有的 Pipeline 身份和两个材料关联。
- Service 复用 create 的规范化/长度与资产所有权规则。
- Controller 暴露冻结的 PATCH 端点及现有 404/400 包装。
- 三层测试覆盖更新、解除关联、所有权、不可编辑生命周期和数据不变性。

## 4. Scope out

- 不修改 lifecycle、outcome、阶段、历史、日程/面试关联。
- 不新增迁移、删除 Pipeline、修改 Resume/JD 内容或触发 AI。
- 不修改 V1、前端、共享异常/安全配置或 Pipeline response 字段。

## 5. Allowed files

```text
backend/src/main/java/com/resumego/pipeline/CareerPipelineRepository.java
backend/src/main/java/com/resumego/pipeline/CareerPipelineService.java
backend/src/main/java/com/resumego/pipeline/CareerPipelineController.java
backend/src/main/java/com/resumego/pipeline/dto/UpdateCareerPipelineRequest.java
backend/src/test/java/com/resumego/pipeline/CareerPipelineRepositoryTest.java
backend/src/test/java/com/resumego/pipeline/CareerPipelineServiceTest.java
backend/src/test/java/com/resumego/pipeline/CareerPipelineControllerTest.java
```

`CareerPipelineResponse` 不需改变，未授权。任何额外文件需求必须停止并重新申请。

## 6. Frozen HTTP contract

```http
PATCH /api/v2/pipelines/{id}
Content-Type: application/json
```

完整请求字段：

```json
{
  "name": "腾讯 Java 后端",
  "companyName": "腾讯",
  "roleTitle": "Java 后端实习",
  "jobDescriptionId": 20,
  "resumeVersionId": 31
}
```

- 五个字段都必须出现；前三个为非空字符串，后两个可显式为 `null` 解除关联。
- `name/companyName` 去除首尾空白后最大 120 字符，`roleTitle` 最大 160。
- 成功：`200`，现有 `ApiResponse<CareerPipelineResponse>`。
- Pipeline 不存在或不属于当前用户：`404`。
- JD/Resume 不存在或不属于用户：`400`，使用现有消息。
- `ARCHIVED` 或 `CLOSED` Pipeline：`400`，不得修改；`ACTIVE` 与 `PAUSED` 允许更新。
- 更新是单事务全量替换；验证失败不得部分写入。

## 7. Data integrity

- 不更新 `current_stage_id`、stage rows、transition rows、lifecycle、outcome、archive time 或 asset link tables。
- 更新一条 Pipeline 不影响其他 Pipeline。
- Resume/JD 只保存经当前用户所有权验证的 id，不写入其领域表。
- 无新日志正文、无 AI、无迁移。

## 8. Required tests

| ID | 场景 | 必须结果 |
| --- | --- | --- |
| UPDATE-01 | 正常全量更新 | 所有字段返回并持久化 |
| UPDATE-02 | 显式 null | JD/Resume 关联解除 |
| UPDATE-03 | 空白/超长 | `400`，无字段被写入 |
| UPDATE-04 | 外部 JD/Resume | `400`，事务不产生部分更新 |
| UPDATE-05 | 不存在/其他用户 Pipeline | `404` |
| UPDATE-06 | ARCHIVED/CLOSED | `400` 且保持原值 |
| UPDATE-07 | ACTIVE/PAUSED | 均允许更新 |
| UPDATE-08 | 历史与关联 | stages/transitions/schedule/interview links 不变 |
| UPDATE-09 | 其他 Pipeline | 完全不受影响 |

## 9. RED/GREEN 与验证

先写失败测试并保存真实 RED，然后最小实现。运行：

```bash
cd backend
mvn -Dtest=CareerPipelineRepositoryTest,CareerPipelineServiceTest,CareerPipelineControllerTest test
mvn test
git diff --check
git status --short
```

必须报告真实退出码和测试数。

## 10. Commit and delivery

- Commit：`feat(pipeline): update identity and material links`
- 最终提交只包含 Allowed files，不自行合并/变基。
- 返回完整 Final commit，并按 `docs/templates/v2-agent-delivery-report.md` 提交报告。

## 11. Direct rejection

部分写入、允许归档/关闭后修改、跨用户资产可关联、历史或其他 Pipeline 被改动、范围越权、缺少真实 RED 或未跑后端全量测试时直接退回。
