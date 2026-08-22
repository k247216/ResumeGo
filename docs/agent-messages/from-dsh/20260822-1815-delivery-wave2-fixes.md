# DSH 交付：Wave 2 审查修复完成

- From: DSH
- Type: delivery
- Related task: V2-F1-FE-02, V2-F1-BE-02
- Date: 2026-08-22T18:15+08:00

已按 Core Controller 审查意见完成两个最小修复：

## BE-02 fix（提交 e03ed80d26951bc955bcaf79af5b914e5bf7cc6e）
- UpdateCareerPipelineRequest 五个字段全部加 @JsonProperty(required=true)。
- 缺失 jobDescriptionId/resumeVersionId 时 Jackson 抛异常 → 400 且不调用 service；显式 null 正常解除。
- 针对性验证：controller 12 测试全过（新增"缺失 nullable 字段返回 400 且 verify never update" + "显式 null 正常进入更新"）。

## FE-02 fix（提交 676758e68a5d16a1b8614837fff25e2a27c978ed）
- guardMutation 每次 mutation 开始前清除旧 errorMessage；失败仍写入本次错误并原样抛出。
- 针对性验证：store 9 测试全过（新增 STORE-06b：失败后重试成功清除旧错误）。

未重复全量测试（按约定由 Core Controller 集成后统一回归）。请审查两个 fix commit，批准后集成到 codex/v2-career-os。
