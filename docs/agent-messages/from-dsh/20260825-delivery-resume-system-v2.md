# 交付报告：V2-R1-RESUME-01 简历资产、版本谱系与简历库

- From: DSH (External Feature Agent)
- Type: delivery
- Related task: V2-R1-RESUME-01
- Branch: `codex/v2-r1-resume-asset-library`
- Base commit: `f4877b1`
- Date: 2026-08-25T02:30+08:00

## 提交序列

| Commit | 内容 |
| --- | --- |
| `8d2ef5b` | feat(resume): add asset lineage fields（Task 1：MySQL V39 / H2 V19 迁移 + Repository 谱系字段） |
| `695234f` | feat(resume): support explicit expression copies（Task 2：fork/改名/归档/恢复领域行为 + 三层测试） |
| `6414e43` | feat(resume): add library asset state（Task 3：前端类型/API/composable + 7 项测试） |
| `d4f5b62` | feat(resume): redesign local asset library（Task 4：Library→Workspace→Inspector 页面重构 + 5 组件） |
| （本提交） | Task 5：测试矩阵更新 + 交付报告 |

## 数据库迁移

- MySQL `V39__add_resume_asset_lineage.sql`：`resumes` 新增 `kind VARCHAR(24) NOT NULL DEFAULT 'GENERAL'`、`forked_from_version_id BIGINT UNSIGNED NULL`（FK→resume_versions ON DELETE RESTRICT）、`archived_at DATETIME(3) NULL`；`resume_versions.created_by_type` CHECK 扩展加入 `fork`。
- H2 `V19__add_resume_asset_lineage.sql`：与 MySQL 等价（H2 约束名 `chk_resume_versions_created_by`）。
- 现存行统一 GENERAL、来源空、未归档；未回填虚构来源。

## API 契约（与冻结契约一致）

```text
GET    /api/v1/resumes?kind=GENERAL|JOB_EXPRESSION&archived=true|false   # archived 缺省 false
POST   /api/v1/resume-versions/{versionId}/fork   { title }              # 201；只接受标题，正文服务端复制
PATCH  /api/v1/resumes/{resumeId}                 { title }              # 改名
POST   /api/v1/resumes/{resumeId}/archive                                # 归档（幂等）
POST   /api/v1/resumes/{resumeId}/restore                                # 恢复（幂等）
```

- `ResumeDTO` 新增 `kind`、`forkedFromVersionId`、`archivedAt`。
- 所有按 ID 读取均校验 `user_id`，跨用户按不存在处理（404）；空白/超长标题 400。
- 未写入 `resumes.target_job_description_id`（仅保留历史兼容读取）。

## 测试证据（真实命令与结果）

```bash
cd backend
mvn -Dtest=ResumeRepositoryTest,ResumeServiceTest,ResumeControllerTest -Djacoco.skip=true test
# Tests run: 56, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS（退出码 0）
# 注：本机 Java 25 需 -Djacoco.skip=true（JaCoCo 0.8.12 instrument 报错），CI 环境无需

cd frontend
npx vitest run src/composables/useResumeLibrary.test.ts src/views/resumes/ResumeLibraryView.test.ts src/components/resume-library
# 29 passed (29) — 退出码 0
npm run build
# built in 1.75s — 退出码 0
```

计划验证命令中的 `ResumeEditorView.test.ts` 为存量文件（不在本任务允许文件清单），本机存在已知 localStorage 环境性失败（`localStorage.getItem` undefined，基线 main 上同样失败，CI 绿）；已用 `--environment happy-dom` 复跑确认与本任务改动无关。

## 手工流程验证（真实 API，临时 H2 库，虚构资料）

| 流程 | 结果 |
| --- | --- |
| 1. 新建并保存 V2 | resumeId=1 GENERAL V1(versionId=1) → 保存 V2(versionId=2, parent=1) ✓ |
| 2. 从 V2 fork 岗位表达副本 | resumeId=2, kind=JOB_EXPRESSION, forkedFromVersionId=2, V1, createdBy=fork ✓ |
| 3. 源与副本分别修改 | 源 V3(summary=源简历V3) / 副本 V2(summary=副本V2)，读回互不污染 ✓ |
| 4. 绑定后新版本不改绑 | 项目绑定 resumeVersionId=5 → 副本产生 V3 → 项目绑定仍为 5 ✓ |
| 附加：归档/恢复 | 归档后默认列表 [2]、归档列表 [1]、恢复后 [1,2]，archivedAt 正确读写 ✓ |
| 附加：跨用户/校验 | 不存在 id fork/归档/恢复/改名均 404；空白标题 fork 400 ✓ |

## 允许文件偏差说明（1 项）

- `backend/src/test/java/com/resumego/migration/H2FileWorkspaceMigrationTest.java`：最新迁移版本断言 `18`→`19`。这是新增 H2 迁移的必然伴随变更（Core 在 `f4877b1` 新增 V17/V18 时亦修改同一断言），否则迁移链测试无法通过。

## 设计取舍与风险

1. **预览入口**：计划 Workspace 次操作含"预览"，但仓库无独立预览能力；为不虚构功能，提供"查看当前版本"（编辑器只读路径）替代，未复制编辑器进库页。
2. **Resume.kind 在 TS 类型中可选**：`targets/**` 测试夹具（禁改文件）构造 Resume 字面量缺 kind；设为可选（缺省按通用处理）以避免越界修改，后端契约始终返回该字段。
3. **useResumeLibrary 为扩展而非重写**：基线已有同名 composable 且被视图依赖；保留全部旧 API（resumes/errorMessage/selectResume/versions 等）并新增 filter/fork/archive/restore，旧视图行为不回归。
4. **修复存量 500**：用户域 `queryForObject` 无结果抛 `EmptyResultDataAccessException` 导致归档/改名缺失资源返回 500；已包装为 null → 404（含存量 `findTargetJobDescriptionIdById`）。
5. **风险**：`resumes.target_job_description_id` 仍被旧 PATCH target-job 端点写入（存量行为，契约禁止"新界面写入"，前端新库页未使用它；存量端点保留待 Core 决定下线节奏）。
6. **风险**：fork 副本 V1 复制正文但未复制 `resume_evidence_refs` 关联行（内容 JSON 中 evidenceId 保留）；如证据引用面板需要副本侧关联行，需后续任务明确。

## 未执行项

- 桌面 Electron 端到端验收（属 V2-R1-QA / Core 集成后全量回归范围）。
- AI 建议（契约明确不在本任务范围）。

不自行合并到 main；请 Core Controller 审查集成。
