# V2-F1-FE-02：Pipeline Pinia Store

## 1. 任务身份

| 字段 | 值 |
| --- | --- |
| Owner role | Frontend Feature Agent |
| Milestone | V2 F1 Career Pipeline |
| Status | `READY` |
| Base commit | `fc98ad0f5ac9edc8749c6ddcbf614429133c1713` |
| Branch | `codex/v2-f1-fe-02-pipeline-store` |
| Integration target | `codex/v2-career-os`，仅由 Core Controller 集成 |

执行前阅读 `docs/architecture/agent-collaboration.md`。必须从精确 Base commit 创建独立 worktree；基线不存在或授权文件已有其他改动时立即停止。

## 2. 用户结果

未来的 Pipeline 页面拥有唯一、可预测的状态入口：加载和选择多条 Pipeline、执行已有写操作、失败后重试，并保持 Pipeline 与独立简历库之间没有隐式联动。

## 3. Dependencies

- 使用已集成的 `frontend/src/types/pipeline.ts` 和 `frontend/src/api/pipeline.ts`。
- 不依赖 BE-02；本卡不提供 Pipeline 身份/材料更新 action。

## 4. Scope in

- 新建 composition-style Pinia store。
- 管理 `pipelines`、`selectedPipelineId`、`loading`、`errorMessage`。
- 提供 `selectedPipeline` computed。
- 封装现有 client 的 load/retry/select/create、阶段新增/重命名/排序/推进、归档/恢复、日程与面试计划关联/解除。
- 每次成功 mutation 用 API 返回的完整 Pipeline 替换同 id 项，不手工猜测服务端状态。
- 使用 `resumego:v2:selectedPipelineId` 保存选择；不得复用 V1 `resumego:activeTargetId`。

## 5. Scope out

- 不修改 API client/types、router、页面、组件、样式、Workspace、Legacy targets store 或 Resume store。
- 不增加 update/delete/history action；它们属于其他任务。
- 不因选择 Pipeline 自动改变简历库当前版本、日程页面选择或模拟面试上下文。

## 6. Allowed files

仅允许创建：

```text
frontend/src/stores/pipelines.ts
frontend/src/stores/pipelines.test.ts
```

任何其他文件默认禁止。需要改变 client 或类型时停止并请求重新授权。

## 7. Frozen public contract

Store 导出名固定为 `usePipelinesStore`，公开：

```text
state: pipelines, selectedPipelineId, loading, errorMessage
computed: selectedPipeline
actions: load, retry, select, create,
         addStage, renameStage, reorderStages, transitionStage,
         archive, restore,
         linkScheduleEvent, unlinkScheduleEvent,
         linkInterviewPlan, unlinkInterviewPlan
```

行为规则：

- `load` 成功后保留仍存在的持久选择；否则选择第一条 `ACTIVE`，再退到第一条记录；空列表选择 `null`。
- 无效持久 ID 必须删除；`select` 收到未知 id 时不改变选择。
- 归档当前 Pipeline 后仍保持选中，使未来页面可以查看并恢复；不得自动切换其他目标。
- mutation 失败时不改变数组与选择，只设置中文错误并继续抛出原错误。
- `retry` 等同重新 load；成功后清空旧错误。
- 不自行排序服务端列表；保持 API 顺序。

## 8. Data/privacy/AI

- 只持久化数字 Pipeline id，不保存 Pipeline/JD/Resume 正文。
- 无网络第三方、无 AI、无日志、无真实个人 fixture。

## 9. Required tests

| ID | 场景 | 必须结果 |
| --- | --- | --- |
| STORE-01 | 首次 load | 保持 API 顺序并选择第一条 ACTIVE |
| STORE-02 | 有效/失效持久选择 | 有效恢复；失效清除并回退 |
| STORE-03 | 空列表 | `selectedPipelineId=null`，无伪数据 |
| STORE-04 | load 失败与 retry | 保留旧数据，显示错误，retry 成功清错 |
| STORE-05 | create/mutations | 使用 client，按返回完整对象替换 |
| STORE-06 | mutation 失败 | 数据和选择不变，错误继续抛出 |
| STORE-07 | archive/restore | 归档后保持当前选择，可恢复 |
| STORE-08 | 选择 Pipeline | 不调用 Resume/Schedule/Interview store |

## 10. RED/GREEN 与验证

先写失败测试并记录真实 RED，然后最小实现。运行：

```bash
cd frontend
npm test -- pipelines.test.ts
npm test
npm run build
git diff --check
git status --short
```

必须报告真实退出码和用例数。

## 11. Commit and delivery

- Commit：`feat(pipeline): add frontend state store`
- 最终提交只包含两个 Allowed files，不自行合并/变基。
- 返回完整 Final commit，并按 `docs/templates/v2-agent-delivery-report.md` 提交报告。

## 12. Direct rejection

修改范围外文件、复用 Legacy target、隐式切换简历、失败时乐观污染状态、伪造 Pipeline、缺少真实 RED 或未跑全量前端测试时直接退回。
