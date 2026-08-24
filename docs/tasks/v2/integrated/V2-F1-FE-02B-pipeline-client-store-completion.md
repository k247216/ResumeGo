# V2-F1-FE-02B：Pipeline 更新与历史前端契约补齐

## 1. 任务身份

| 字段 | 值 |
| --- | --- |
| Owner role | Frontend Feature Agent |
| Milestone | V2 F1 Career Pipeline |
| Status | `INTEGRATED` |
| Base commit | `42435cb3e75e539d14fa365b56f349c00710598f` |
| Branch | `codex/v2-f1-fe-02b-pipeline-contract-completion` |
| Integration target | `codex/v2-career-os`，仅由 Core Controller 集成 |
| Integration commit | `b58115e` |

执行前阅读 `docs/architecture/agent-collaboration.md`。必须从精确 Base commit 创建独立 worktree；不得从旧 FE-02 功能分支继续开发。

## 2. 用户结果

为即将开发的 Pipeline 页面补齐最后两条数据通路：用户可以安全更新公司、岗位、JD 与简历关联，也可以读取当前 Pipeline 的真实阶段历史。页面无需自行拼接 HTTP，也不会绕过唯一 store 状态入口。

## 3. 原因与依赖

- BE-01 已提供 `GET /api/v2/pipelines/{id}/transitions`。
- BE-02 已提供 `PATCH /api/v2/pipelines/{id}`。
- FE-01/FE-02 尚未接入这两条后续后端契约。
- 本卡完成前，`V2-F1-FE-03` 禁止开始。

## 4. Scope in

- 增加 Pipeline 全量更新请求和阶段历史响应类型。
- API client 增加 update 与 transition history 两个调用。
- Store 增加 `update`、`loadTransitionHistory` 及按 Pipeline 保存的历史状态。
- 更新成功以服务端完整 Pipeline 替换同 id 项，不改变当前选择。
- 历史读取保持服务端顺序，切换 Pipeline 时不伪造、不混合历史。

## 5. Scope out

- 不创建页面、组件、路由、对话框或样式。
- 不自动加载历史，不因选择 Pipeline 改变简历、日程或模拟面试上下文。
- 不修改后端、V1 targets store、Workspace 或 DesktopShell。
- 不增加历史编辑/删除、乐观更新、缓存过期策略或分页。

## 6. Allowed files

仅允许修改：

```text
frontend/src/types/pipeline.ts
frontend/src/api/pipeline.ts
frontend/src/api/pipeline.test.ts
frontend/src/stores/pipelines.ts
frontend/src/stores/pipelines.test.ts
```

需要任何其他文件时停止并通过 DSH 通道申请，不得把 UI 提前塞入本卡。

## 7. Frozen contracts

### Types

`UpdatePipelineRequest` 五个字段全部必需；后两个类型允许 `null`，但不得声明为 optional：

```ts
interface UpdatePipelineRequest {
  name: string
  companyName: string
  roleTitle: string
  jobDescriptionId: number | null
  resumeVersionId: number | null
}
```

`PipelineStageTransition` 字段固定为：

```ts
id: number
pipelineId: number
fromStageId: number | null
toStageId: number
actor: string
note: string | null
occurredAt: string
```

### API

```text
updatePipeline(id, request)          PATCH /api/v2/pipelines/{id}
listPipelineTransitions(id)          GET   /api/v2/pipelines/{id}/transitions
```

- PATCH JSON 必须显式包含五个字段，包括两个 `null` 字段。
- 两个函数继续使用 `apiFetch` 和现有 `parseResponse` 错误语义。
- 历史数组不在 client 排序或改写。

### Store

在 `usePipelinesStore` 增加：

```text
state: transitionHistoryByPipelineId, historyLoadingPipelineId, historyErrorMessage
actions: update, loadTransitionHistory
```

- `update(id, request)` 成功后替换 Pipeline，保持 `selectedPipelineId`；失败不改变 Pipeline 与选择，并沿用 mutation 错误规则。
- `loadTransitionHistory(id)` 成功后仅替换该 id 的历史；失败保留其旧历史，写入 `historyErrorMessage` 并抛出原错误。
- 每次历史读取开始前清除旧历史错误；完成后 `historyLoadingPipelineId=null`。
- 不因 `select` 自动调用 history API。

## 8. 功能验收

1. 更新请求完整发送五字段，显式 `null` 不被省略。
2. 更新成功替换对象且保持选择；失败不污染状态。
3. 两条 Pipeline 的历史分别保存，不互相覆盖。
4. 历史失败后旧数据仍可见；再次成功会清除旧错误。
5. Store 不触发 Resume、Schedule、Interview store 或 API 的隐式联动。

## 9. 验证与交付

先完成上述针对性测试，再运行一次前端全量测试与生产构建，不做额外测试扩张：

```bash
cd frontend
npm test -- pipeline.test.ts pipelines.test.ts
npm test
npm run build
git diff --check
```

Commit：`feat(pipeline): complete frontend update and history contract`

通过 `read_dsh_delivery` 可读取的 DSH delivery 报告交付最终提交、功能行为、验证结果与已知风险；不得自行合并。

## 10. 直接退回条件

遗漏 nullable 字段、组件自行 fetch、历史跨 Pipeline 混合、更新失败污染状态、隐式切换简历/日程/面试、修改 Allowed files 之外内容时直接退回。
