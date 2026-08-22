# V2-F1-FE-01：Pipeline 类型与 API Client

## 1. 任务身份

| 字段 | 值 |
| --- | --- |
| Owner role | Frontend Feature Agent |
| Milestone | V2 F1 Career Pipeline |
| Status | `READY` |
| Base commit | `1f83b1ac02e62793813d174a2f451a42fc5c3e57` |
| Branch | `codex/v2-f1-fe-01-pipeline-api` |
| Integration target | `codex/v2-career-os`，仅由 Core Controller 集成 |

执行前必须阅读 `docs/architecture/agent-collaboration.md`。如果当前仓库不包含精确 `Base commit`，或授权文件已有非本任务改动，立即停止并报告。

## 2. 用户结果

为 Pipeline 页面和状态管理提供唯一、类型安全、可测试的前端数据入口。此任务不创建 UI；完成后上层模块可以调用现有 `/api/v2/pipelines` 能力，而不重复拼接路径或自行解析错误。

## 3. 依赖

- 只依赖 `Base commit` 中现有 `CareerPipelineController` 路由和 `frontend/src/api/http.ts`。
- 不依赖阶段历史查询任务；本卡不得提前加入 `/transitions` client。

## 4. Scope in

- 定义 Pipeline、阶段、生命周期、请求体和 `ApiResponse` 类型。
- 新增 API client：列表、详情、创建、阶段新增/重命名/排序/推进、归档/恢复、日程关联/解除、模拟面试计划关联/解除。
- 统一使用 `apiFetch`、现有成功包装与错误消息惯例。
- 使用 mocked HTTP 完成 URL、method、JSON body、成功结果和失败消息测试。

## 5. Scope out

- 不实现 Pinia store、路由、页面、组件、样式或工作台联动。
- 不修改 `http.ts`、后端、V1 client、构建依赖或公共导航。
- 不增加阶段历史、删除 Pipeline、更新公司/岗位等后端尚未提供的接口。

## 6. Allowed files

仅允许创建：

```text
frontend/src/types/pipeline.ts
frontend/src/api/pipeline.ts
frontend/src/api/pipeline.test.ts
```

任何其他文件默认禁止。若发现必须修改 `http.ts` 或共享类型，停止并请求重新授权，不得复制一套绕过现有运行时配置的 fetch。

## 7. Frozen contracts

Base URL 固定为 `/api/v2/pipelines`。API client 必须覆盖：

| 函数语义 | Method | Path |
| --- | --- | --- |
| list | GET | `/api/v2/pipelines` |
| get | GET | `/api/v2/pipelines/{id}` |
| create | POST | `/api/v2/pipelines` |
| add stage | POST | `/api/v2/pipelines/{id}/stages` |
| rename stage | PATCH | `/api/v2/pipelines/{id}/stages/{stageId}` |
| reorder stages | PUT | `/api/v2/pipelines/{id}/stages/order` |
| transition | POST | `/api/v2/pipelines/{id}/transitions` |
| archive | POST | `/api/v2/pipelines/{id}/archive` |
| restore | POST | `/api/v2/pipelines/{id}/restore` |
| link schedule | PUT | `/api/v2/pipelines/{id}/schedule-events/{eventId}` |
| unlink schedule | DELETE | 同上 |
| link interview plan | PUT | `/api/v2/pipelines/{id}/interview-plans/{planId}` |
| unlink interview plan | DELETE | 同上 |

- 类型名称应表达 V2 Pipeline 语义，禁止沿用 `JobProject` 名称。
- `lifecycle` 至少准确表示后端现有枚举；`outcome` 和可空关联字段不得用假默认值填充。
- 请求 JSON 字段必须与后端 DTO 一致；不得在 client 中悄悄改写业务值。
- 成功返回现有 `ApiResponse<T>` 包装，与当前 client 约定一致。
- 非 2xx 或 `success=false` 时抛出 `body.message`；无服务端消息时使用每个操作明确的中文 fallback。
- 所有请求必须经过 `apiFetch`，保留桌面运行时地址、workspace token、超时和 AI 配置事件行为。

## 8. Data, privacy and AI

- client 不持久化数据，不记录请求正文，不调用第三方或模型服务。
- 测试只使用虚构公司、岗位、ID 和阶段。
- 不在错误中拼接 JD、简历内容或面试回答。

## 9. Observable behavior and required tests

| ID | 场景 | 必须观察到的结果 |
| --- | --- | --- |
| FE-01 | 列表/详情 | 使用准确 GET 路径并返回类型化包装 |
| FE-02 | 创建与阶段写操作 | method、Content-Type 与 JSON body 准确 |
| FE-03 | 归档/恢复 | 使用 POST 且无伪造请求正文 |
| FE-04 | 关联/解除 | 对相同资源路径分别使用 PUT/DELETE |
| FE-05 | HTTP 失败 | 优先抛出服务端 message |
| FE-06 | 无法解析失败体 | 抛出对应操作的中文 fallback |

## 10. RED evidence

先写 `pipeline.test.ts`，在 API client 尚不存在或行为缺失时运行并保存真实失败输出：

```bash
cd frontend
npm test -- pipeline.test.ts
```

记录命令、退出码、测试数和实际失败原因；禁止完成实现后补写 RED。

## 11. GREEN and regression

```bash
cd frontend
npm test -- pipeline.test.ts
npm test
npm run build
git diff --check
git status --short
```

必须报告实际测试文件数、用例数和 build 结果。完整 `npm test` 或 `npm run build` 未运行/失败时不得声称完成。

## 12. Commit and delivery

- 提交信息：`feat(pipeline): add typed frontend client`
- 最终提交只包含三个 `Allowed files`。
- 不自行 merge、rebase、push 或修改集成分支。
- 返回最终提交完整哈希，并按 `docs/templates/v2-agent-delivery-report.md` 填写交付报告。

## 13. Integration gates

以下任一项成立即退回：新增 UI/Pinia；路径或 method 漂移；绕开 `apiFetch`；失败被吞掉；用假默认值掩盖可空数据；缺少真实 RED；存在未授权文件。

