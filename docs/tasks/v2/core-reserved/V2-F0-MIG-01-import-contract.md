# V2-F0-MIG-01：V1 导入契约与 Dry-run

## Identity

- Status: `CORE_RESERVED`
- Owner: Core Controller
- Branch: `codex/v2-f0-mig-01-import-contract`
- Base commit: assigned when started

## Outcome

冻结 V1 备份如何被识别、只读检查、去重、报告与回执记录，使后续 Agent 不需要自行解释迁移安全边界。

## Deliverables

- 版本化 import manifest、source fingerprint、dry-run result、warning、receipt schema。
- 支持/不支持的 V1 schema 版本和记录映射表。
- 幂等键、计数核验、原子激活、失败回滚与再次导入规则。
- 证明 V1 源文件未变的 hash/mtime 验收方法。
- 导入前备份、空间不足、损坏备份、未知关联和部分不支持记录的确定行为。

## Allowed files when activated

```text
docs/superpowers/specs/*v1-import*.md
docs/architecture/architectureV2.md
docs/decisions.md
docs/product/roadmap.md
```

如需 DTO/迁移代码，必须拆到 MIG-02，不得在契约任务中顺带实现。

## Frozen boundaries

- V1 永远只读；不直接连接正在运行的 V1 数据库写入。
- 先复制到 staging，再检查和转换。
- 相同 fingerprint 重复导入不产生重复 Pipeline/Resume。
- receipt 只保存版本、计数、状态、时间和 fingerprint，不保存敏感正文。
- 导入失败时现有 V2 workspace 和 V1 source 都不变。

## Verification

```bash
rg -n "fingerprint|dry-run|idempotent|rollback|receipt|read-only" docs/superpowers/specs/*v1-import*.md
git diff --check
```

完成后由用户审阅契约；只有明确批准后才能将 MIG-02 升为 `READY`。
