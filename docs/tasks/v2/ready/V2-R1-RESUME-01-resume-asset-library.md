# V2-R1-RESUME-01：简历资产、版本谱系与简历库

## Identity

- Status: `READY`
- Owner: External Feature Agent
- Branch: `codex/v2-r1-resume-asset-library`
- Base commit: `6a93f8e`（或 Core Controller 在当前 Pipeline/Schedule 契约修复集成后指定的后继提交）
- Spec: `docs/superpowers/specs/2026-08-25-resume-interview-workspace-contract.md`
- Execution plan: `docs/superpowers/plans/2026-08-25-resume-system-v2.md`
- Acceptance owner: Core Controller

## User result

用户能够把简历当作独立本地资产维护：同一简历拥有不可变线性版本；可以从任意版本手动创建独立的岗位表达副本；切换求职目标、保存新版本或修改副本都不会静默改变其他简历或 Pipeline 的绑定。

## Required execution order

严格按执行计划 Task 1–5 顺序实施。每个 Task 单独提交并记录 RED/GREEN 证据；不得把后端迁移、API 和整个页面压成一个提交。

## Frozen contracts

1. ResumeAsset kind 只有 `GENERAL` 与 `JOB_EXPRESSION`。
2. `parentVersionId` 只表示同一 ResumeAsset 内的直接父版本。
3. 跨资产 fork 只记录 `ResumeAsset.forkedFromVersionId`。
4. fork 请求只允许提交新标题；正文由服务端读取源版本复制。
5. `resumes.target_job_description_id` 不再作为 V2 关系真值，任务不得写入或扩展它。
6. Pipeline 只绑定一个明确 `resumeVersionId`；任务不得自动创建、更新或替换 Pipeline 绑定。
7. 版本不可原地覆盖；物理删除不在本任务范围，只做归档/恢复。
8. AI 建议、面试、Workspace 和 Knowledge 均不在本任务范围。

## Allowed files

- `backend/src/main/resources/db/migration/V39__add_resume_asset_lineage.sql`
- `backend/src/main/resources/db/migration-h2/V19__add_resume_asset_lineage.sql`
- `backend/src/main/java/com/resumego/resume/**`
- `backend/src/test/java/com/resumego/resume/**`
- `frontend/src/types/resume.ts`
- `frontend/src/api/resume.ts`
- `frontend/src/composables/useResumeLibrary.ts`
- `frontend/src/composables/useResumeLibrary.test.ts`
- `frontend/src/components/resume-library/**`
- `frontend/src/views/resumes/ResumeLibraryView.vue`
- `frontend/src/views/resumes/ResumeLibraryView.test.ts`
- 本任务交付报告文件

未列出的文件禁止修改。特别禁止修改 `WorkbenchView.vue`、`InterviewView.vue`、Pipeline、Schedule、Knowledge、Electron、全局导航和构建配置。

## Functional acceptance

1. 新建/导入的普通简历显示为通用简历，V1/V2/V3 作为同一资产版本展示。
2. 从通用简历 V2 创建岗位表达副本，返回新的 ResumeAsset 与 V1，内容与当时 V2 相同。
3. 源简历与副本分别保存后正文、版本号、currentVersionId 互不污染。
4. 历史版本只读；普通保存只能从当前版本创建下一版本，用户若要继续历史内容必须显式 fork，确保同一资产不形成分支。
5. 归档后默认列表隐藏，恢复后重新出现，历史引用仍然有效。
6. 跨用户读取、fork、改名、归档、恢复均按不存在处理且无副作用。
7. 页面提供加载、空、失败、重试、fork loading 和归档确认。
8. 页面不自动选择或修改任何 Pipeline，不展示伪造引用。

## Verification

```bash
cd backend
mvn -q -Dtest=ResumeRepositoryTest,ResumeServiceTest,ResumeControllerTest test

cd ../frontend
npx vitest run src/composables/useResumeLibrary.test.ts src/views/resumes/ResumeLibraryView.test.ts src/views/resumes/ResumeEditorView.test.ts src/components/resume-library
npm run build
```

只声明实际执行的测试。提交前运行 `git diff --check` 并确认相对 Base commit 的文件全部属于 Allowed files。

## Delivery

提交：每个计划 Task 一个范围清楚的提交。最终回传完整提交序列、迁移/API 契约、真实测试命令/退出码/数量、四条手工流程结果、风险和未执行项。不得自行合并到 `main`。
